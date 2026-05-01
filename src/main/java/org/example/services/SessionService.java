package org.example.services;

import org.example.entities.Session;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements ICrud<Session>.
 * Each method opens its own fresh connection via try-with-resources
 * to avoid MySQL's per-connection result cache causing stale reads.
 */
public class SessionService implements ICrud<Session> {

    private static Connection fresh() throws SQLException {
        Connection c = DriverManager.getConnection(
            MyDataBase.getURL(), MyDataBase.getUSERNAME(), MyDataBase.getPASSWORD());
        c.setAutoCommit(true);
        return c;
    }

    @Override
    public void ajouter(Session s) throws SQLException {
        try (Connection c = fresh()) {
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO training_sessions " +
                "(title, description, start_at, end_at, capacity, price, is_active, created_at, gymnasium_id, coach_user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, 1, NOW(), ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, s.getTitle());
            ps.setString(2, s.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(s.getStartAt()));
            ps.setTimestamp(4, s.getEndAt() != null ? Timestamp.valueOf(s.getEndAt()) : null);
            ps.setInt(5, s.getCapacity());
            ps.setDouble(6, s.getPrice());
            if (s.getGymnasiumId() != null) ps.setInt(7, s.getGymnasiumId());
            else ps.setNull(7, Types.INTEGER);
            Integer coachId = s.getCoachUserId();
            if (coachId == null || coachId <= 0) {
                throw new SQLException("Identifiant encadrant (coach_user_id) manquant ou invalide.");
            }
            ps.setInt(8, coachId);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) s.setId(keys.getInt(1));
            s.setCoachUserId(coachId);
        }
    }

    /**
     * Sessions créées par l’encadrant connecté (colonne {@code coach_user_id}).
     * Retourne une liste vide si {@code coachUserId} &lt;= 0.
     */
    public List<Session> afficherPourCoach(int coachUserId) throws SQLException {
        if (coachUserId <= 0) {
            return new ArrayList<>();
        }
        try (Connection c = fresh()) {
            List<Session> list = new ArrayList<>();
            PreparedStatement ps = c.prepareStatement(
                "SELECT ts.*, g.name AS gym_name " +
                "FROM training_sessions ts " +
                "LEFT JOIN gymnasia g ON ts.gymnasium_id = g.id " +
                "WHERE ts.coach_user_id = ? " +
                "ORDER BY ts.start_at DESC");
            ps.setInt(1, coachUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Session s = new Session();
                s.setId(rs.getInt("id"));
                s.setTitle(rs.getString("title"));
                s.setDescription(rs.getString("description"));
                Timestamp start = rs.getTimestamp("start_at");
                if (start != null) s.setStartAt(start.toLocalDateTime());
                Timestamp end = rs.getTimestamp("end_at");
                if (end != null) s.setEndAt(end.toLocalDateTime());
                s.setCapacity(rs.getInt("capacity"));
                s.setPrice(rs.getDouble("price"));
                s.setActive(rs.getInt("is_active") == 1);
                s.setCreatedAt(rs.getTimestamp("created_at"));
                int gymId = rs.getInt("gymnasium_id");
                s.setGymnasiumId(rs.wasNull() ? null : gymId);
                s.setGymnasiumName(rs.getString("gym_name"));
                int cid = rs.getInt("coach_user_id");
                s.setCoachUserId(rs.wasNull() ? null : cid);
                s.setPlacesRestantes(s.getCapacity());
                list.add(s);
            }
            return list;
        }
    }

    @Override
    public List<Session> afficher() throws SQLException {
        try (Connection c = fresh()) {
            List<Session> list = new ArrayList<>();
            ResultSet rs = c.createStatement().executeQuery(
                "SELECT ts.*, g.name AS gym_name " +
                "FROM training_sessions ts " +
                "LEFT JOIN gymnasia g ON ts.gymnasium_id = g.id " +
                "ORDER BY ts.start_at DESC");
            while (rs.next()) {
                Session s = new Session();
                s.setId(rs.getInt("id"));
                s.setTitle(rs.getString("title"));
                s.setDescription(rs.getString("description"));
                Timestamp start = rs.getTimestamp("start_at");
                if (start != null) s.setStartAt(start.toLocalDateTime());
                Timestamp end = rs.getTimestamp("end_at");
                if (end != null) s.setEndAt(end.toLocalDateTime());
                s.setCapacity(rs.getInt("capacity"));
                s.setPrice(rs.getDouble("price"));
                s.setActive(rs.getInt("is_active") == 1);
                s.setCreatedAt(rs.getTimestamp("created_at"));
                int gymId = rs.getInt("gymnasium_id");
                s.setGymnasiumId(rs.wasNull() ? null : gymId);
                s.setGymnasiumName(rs.getString("gym_name"));
                int coachId = rs.getInt("coach_user_id");
                s.setCoachUserId(rs.wasNull() ? null : coachId);
                s.setPlacesRestantes(s.getCapacity());
                list.add(s);
            }
            return list;
        }
    }

    /** Update by full Session object (preferred over modifier(int id)) */
    public void modifier(Session s) throws SQLException {
        try (Connection c = fresh()) {
            PreparedStatement ps = c.prepareStatement(
                "UPDATE training_sessions SET " +
                "title=?, description=?, start_at=?, end_at=?, capacity=?, price=?, gymnasium_id=? " +
                "WHERE id=?");
            ps.setString(1, s.getTitle());
            ps.setString(2, s.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(s.getStartAt()));
            ps.setTimestamp(4, s.getEndAt() != null ? Timestamp.valueOf(s.getEndAt()) : null);
            ps.setInt(5, s.getCapacity());
            ps.setDouble(6, s.getPrice());
            if (s.getGymnasiumId() != null) ps.setInt(7, s.getGymnasiumId());
            else ps.setNull(7, Types.INTEGER);
            ps.setInt(8, s.getId());
            ps.executeUpdate();
        }
    }

    /** ICrud contract — not used directly; delegates to modifier(Session) */
    @Override
    public void modifier(int id) throws SQLException {
        // No-op: use modifier(Session) instead
    }

    /** Soft delete: sets is_active = 0 */
    @Override
    public void supprimer(int id) throws SQLException {
        try (Connection c = fresh()) {
            PreparedStatement ps = c.prepareStatement(
                "UPDATE training_sessions SET is_active = 0 WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
