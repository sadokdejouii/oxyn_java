package org.example.repository.planning;

import org.example.entities.FicheSanteRow;
import org.example.model.planning.FicheSanteFormData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Optional;

/**
 * JDBC — table {@code fiche_sante}.
 */
public final class FicheSanteRepository {

    public Optional<FicheSanteRow> findById(int id) throws SQLException {
        Connection c = JdbcPlanningSupport.connectionOrNull();
        if (c == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, user_id, genre, age, taille, poids, objectif, niveau_activite
                FROM fiche_sante WHERE id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    public Optional<FicheSanteRow> findByUserId(int userId) throws SQLException {
        Connection c = JdbcPlanningSupport.connectionOrNull();
        if (c == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, user_id, genre, age, taille, poids, objectif, niveau_activite
                FROM fiche_sante WHERE user_id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    public int insert(int userId, FicheSanteFormData d) throws SQLException {
        Connection c = JdbcPlanningSupport.requireConnection();
        String sql = """
                INSERT INTO fiche_sante (genre, age, taille, poids, objectif, niveau_activite, created_at, updated_at, user_id)
                VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindForm(ps, d, 1);
            ps.setInt(7, userId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("INSERT fiche_sante sans clé générée");
    }

    public void update(int ficheId, int userId, FicheSanteFormData d) throws SQLException {
        Connection c = JdbcPlanningSupport.requireConnection();
        String sql = """
                UPDATE fiche_sante SET
                    genre = ?, age = ?, taille = ?, poids = ?, objectif = ?, niveau_activite = ?, updated_at = NOW()
                WHERE id = ? AND user_id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bindForm(ps, d, 1);
            ps.setInt(7, ficheId);
            ps.setInt(8, userId);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new SQLException("Fiche introuvable ou accès refusé");
            }
        }
    }

    public void deleteByIdAndUserId(int ficheId, int userId) throws SQLException {
        Connection c = JdbcPlanningSupport.requireConnection();
        String sql = "DELETE FROM fiche_sante WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ficheId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Si une fiche existe pour {@code userId} : {@link #update}, sinon {@link #insert}.
     */
    public FicheSanteRow upsertByUserId(int userId, FicheSanteFormData d) throws SQLException {
        Optional<FicheSanteRow> existing = findByUserId(userId);
        if (existing.isPresent()) {
            update(existing.get().id(), userId, d);
            return findByUserId(userId).orElseThrow();
        }
        insert(userId, d);
        return findByUserId(userId).orElseThrow(() -> new SQLException("Upsert fiche_sante : lecture après insert impossible"));
    }

    private static void bindForm(PreparedStatement ps, FicheSanteFormData d, int start) throws SQLException {
        int i = start;
        ps.setString(i++, d.genre());
        if (d.age() != null) {
            ps.setInt(i++, d.age());
        } else {
            ps.setNull(i++, Types.INTEGER);
        }
        if (d.tailleCm() != null) {
            ps.setInt(i++, d.tailleCm());
        } else {
            ps.setNull(i++, Types.INTEGER);
        }
        if (d.poidsKg() != null) {
            ps.setDouble(i++, d.poidsKg());
        } else {
            ps.setNull(i++, Types.DOUBLE);
        }
        ps.setString(i++, d.objectif());
        ps.setString(i, d.niveauActivite());
    }

    public static FicheSanteRow mapRow(ResultSet rs) throws SQLException {
        return new FicheSanteRow(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("genre"),
                (Integer) rs.getObject("age"),
                (Integer) rs.getObject("taille"),
                rs.getObject("poids") != null ? rs.getDouble("poids") : null,
                rs.getString("objectif"),
                rs.getString("niveau_activite")
        );
    }
}
