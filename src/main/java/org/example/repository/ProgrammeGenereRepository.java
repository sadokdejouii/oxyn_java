package org.example.repository;

import org.example.entities.ProgrammeGenereRow;
import org.example.model.programme.ProgrammeGenere;
import org.example.model.programme.ProgrammeGenereJsonMapper;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * JDBC — table {@code programmes_generes} : lecture + remplacement (upsert métier : delete puis insert).
 */
public final class ProgrammeGenereRepository {

    public Optional<ProgrammeGenereRow> findById(long id) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, user_id, calories_par_jour, objectif_principal,
                       exercices_hebdomadaires, plans_repas, conseils_generaux
                FROM programmes_generes WHERE id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    public Optional<ProgrammeGenereRow> findLatestByUserId(int userId) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, user_id, calories_par_jour, objectif_principal,
                       exercices_hebdomadaires, plans_repas, conseils_generaux
                FROM programmes_generes WHERE user_id = ? ORDER BY id DESC LIMIT 1
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

    public void deleteByUserId(int userId) throws SQLException {
        Connection c = conn();
        if (c == null) {
            throw new SQLException("Pas de connexion MySQL");
        }
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM programmes_generes WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Supprime les anciennes lignes pour {@code userId}, insère le programme et retourne l’entité avec {@code id} généré.
     */
    public ProgrammeGenere replaceForUser(ProgrammeGenere programme) throws SQLException {
        if (programme.userId() <= 0) {
            throw new SQLException("user_id invalide");
        }
        Connection c = conn();
        if (c == null) {
            throw new SQLException("Pas de connexion MySQL");
        }
        String exJson = ProgrammeGenereJsonMapper.toExercisesJson(programme.exercicesHebdomadaires());
        String repasJson = ProgrammeGenereJsonMapper.toPlansRepasJson(programme.plansRepas());

        try {
            c.setAutoCommit(false);
            try (PreparedStatement del = c.prepareStatement("DELETE FROM programmes_generes WHERE user_id = ?")) {
                del.setInt(1, programme.userId());
                del.executeUpdate();
            }
            String sql = """
                    INSERT INTO programmes_generes
                    (calories_par_jour, objectif_principal, exercices_hebdomadaires, plans_repas, conseils_generaux, created_at, updated_at, user_id)
                    VALUES (?, ?, ?, ?, ?, NOW(), NOW(), ?)
                    """;
            long newId;
            try (PreparedStatement ins = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ins.setInt(1, programme.caloriesParJour() != null ? programme.caloriesParJour() : 0);
                ins.setString(2, programme.objectifPrincipal());
                ins.setString(3, exJson);
                ins.setString(4, repasJson);
                ins.setString(5, programme.conseilsGeneraux());
                ins.setInt(6, programme.userId());
                ins.executeUpdate();
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("INSERT programmes_generes sans clé générée");
                    }
                    newId = keys.getLong(1);
                }
            }
            c.commit();
            return programme.withId(newId);
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }

    public static ProgrammeGenereRow mapRow(ResultSet rs) throws SQLException {
        return new ProgrammeGenereRow(
                rs.getInt("id"),
                rs.getInt("user_id"),
                (Integer) rs.getObject("calories_par_jour"),
                rs.getString("objectif_principal"),
                rs.getString("exercices_hebdomadaires"),
                rs.getString("plans_repas"),
                rs.getString("conseils_generaux")
        );
    }

    private static Connection conn() throws SQLException {
        Connection c = MyDataBase.getConnection();
        if (c == null || c.isClosed()) {
            return null;
        }
        return c;
    }
}
