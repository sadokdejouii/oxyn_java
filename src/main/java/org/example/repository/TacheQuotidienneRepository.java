package org.example.repository;

import org.example.model.planning.task.TacheEtat;
import org.example.model.planning.task.TacheQuotidienne;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC — table {@code taches_quotidiennes}.
 */
public final class TacheQuotidienneRepository {

    public List<TacheQuotidienne> findForUserBetween(int userId, LocalDate from, LocalDate to) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return List.of();
        }
        String sql = """
                SELECT id, user_id, `date`, jour_semaine, description, etat
                FROM taches_quotidiennes
                WHERE user_id = ? AND `date` BETWEEN ? AND ?
                ORDER BY `date` ASC, id ASC
                """;
        List<TacheQuotidienne> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public Optional<TacheQuotidienne> findById(int id, int userId) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, user_id, `date`, jour_semaine, description, etat
                FROM taches_quotidiennes WHERE id = ? AND user_id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    public int insert(int userId, LocalDate date, String jourSemaine, String description, TacheEtat etat) throws SQLException {
        Connection c = conn();
        if (c == null) {
            throw new SQLException("Pas de connexion MySQL");
        }
        String sql = """
                INSERT INTO taches_quotidiennes (user_id, `date`, jour_semaine, description, etat, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, NOW(), NOW())
                """;
        try (PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(date));
            ps.setString(3, jourSemaine);
            ps.setString(4, description);
            ps.setString(5, etat.toDb());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Insertion tâche sans id généré");
    }

    public List<TacheQuotidienne> findByUserId(int userId) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return List.of();
        }
        String sql = """
                SELECT id, user_id, `date`, jour_semaine, description, etat
                FROM taches_quotidiennes
                WHERE user_id = ?
                ORDER BY `date` DESC, id DESC
                LIMIT 10000
                """;
        List<TacheQuotidienne> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public void update(int id, int userId, LocalDate date, String jourSemaine, String description, TacheEtat etat)
            throws SQLException {
        Connection c = conn();
        if (c == null) {
            throw new SQLException("Pas de connexion MySQL");
        }
        String sql = """
                UPDATE taches_quotidiennes SET
                    `date` = ?, jour_semaine = ?, description = ?, etat = ?, updated_at = NOW()
                WHERE id = ? AND user_id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, jourSemaine);
            ps.setString(3, description);
            ps.setString(4, etat.toDb());
            ps.setInt(5, id);
            ps.setInt(6, userId);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new SQLException("Tâche introuvable ou accès refusé");
            }
        }
    }

    public void deleteByIdAndUserId(int id, int userId) throws SQLException {
        Connection c = conn();
        if (c == null) {
            throw new SQLException("Pas de connexion MySQL");
        }
        String sql = "DELETE FROM taches_quotidiennes WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /** Supprime toutes les tâches quotidiennes du compte (ex. reset après suppression fiche admin). */
    public int deleteAllByUserId(int userId) throws SQLException {
        Connection c = conn();
        if (c == null) {
            throw new SQLException("Pas de connexion MySQL");
        }
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM taches_quotidiennes WHERE user_id = ?")) {
            ps.setInt(1, userId);
            return ps.executeUpdate();
        }
    }

    public void updateEtat(int id, int userId, TacheEtat etat) throws SQLException {
        Connection c = conn();
        if (c == null) {
            throw new SQLException("Pas de connexion MySQL");
        }
        String sql = """
                UPDATE taches_quotidiennes SET etat = ?, updated_at = NOW() WHERE id = ? AND user_id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, etat.toDb());
            ps.setInt(2, id);
            ps.setInt(3, userId);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new SQLException("Tâche introuvable ou accès refusé");
            }
        }
    }

    public static TacheQuotidienne mapRow(ResultSet rs) throws SQLException {
        Date d = rs.getDate("date");
        return new TacheQuotidienne(
                rs.getInt("id"),
                rs.getInt("user_id"),
                d != null ? d.toLocalDate() : null,
                rs.getString("jour_semaine"),
                rs.getString("description"),
                TacheEtat.fromDb(rs.getString("etat"))
        );
    }

    private static Connection conn() throws SQLException {
        Connection c = MyDataBase.getInstance().getConnection();
        if (c == null || c.isClosed()) {
            return null;
        }
        return c;
    }
}
