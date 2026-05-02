package org.example.digitalwill;

import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tables Digital Will:
 * - digital_will_trusted_contact: contact de confiance par compte
 * - digital_will_events: historique/timeline
 */
public final class DigitalWillDAO {

    private final Connection con;

    public DigitalWillDAO() {
        this.con = MyDataBase.getInstance().getConnection();
        try {
            ensureTables();
        } catch (SQLException e) {
            System.err.println("[DigitalWillDAO] ensureTables failed: " + e.getMessage());
        }
    }

    public void ensureTables() throws SQLException {
        String contact = """
                CREATE TABLE IF NOT EXISTS digital_will_trusted_contact (
                    user_id INT PRIMARY KEY,
                    contact_email VARCHAR(255) NOT NULL,
                    contact_name VARCHAR(255) DEFAULT NULL,
                    enabled TINYINT(1) NOT NULL DEFAULT 1,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """;
        String events = """
                CREATE TABLE IF NOT EXISTS digital_will_events (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    event_type VARCHAR(60) NOT NULL,
                    detail VARCHAR(255) DEFAULT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_dwe_user (user_id),
                    INDEX idx_dwe_time (created_at)
                )
                """;
        try (PreparedStatement ps = con.prepareStatement(contact)) { ps.execute(); }
        try (PreparedStatement ps = con.prepareStatement(events)) { ps.execute(); }
    }

    public void upsertTrustedContact(int userId, String email, String name, boolean enabled) throws SQLException {
        String sql = """
                INSERT INTO digital_will_trusted_contact (user_id, contact_email, contact_name, enabled)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    contact_email = VALUES(contact_email),
                    contact_name = VALUES(contact_name),
                    enabled = VALUES(enabled)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, email);
            ps.setString(3, (name == null || name.isBlank()) ? null : name.trim());
            ps.setBoolean(4, enabled);
            ps.executeUpdate();
        }
    }

    public Optional<TrustedContact> getTrustedContact(int userId) throws SQLException {
        String sql = """
                SELECT user_id, contact_email, contact_name, enabled
                FROM digital_will_trusted_contact
                WHERE user_id = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new TrustedContact(
                            rs.getInt("user_id"),
                            rs.getString("contact_email"),
                            rs.getString("contact_name"),
                            rs.getBoolean("enabled")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    public void addEvent(int userId, String type, String detail) throws SQLException {
        String sql = "INSERT INTO digital_will_events (user_id, event_type, detail) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            ps.setString(3, (detail == null || detail.isBlank()) ? null : detail.trim());
            ps.executeUpdate();
        }
    }

    public List<DigitalWillEvent> timeline(int userId, int limit) throws SQLException {
        int lim = (limit <= 0) ? 200 : Math.min(limit, 1000);
        String sql = """
                SELECT id, user_id, event_type, detail, created_at
                FROM digital_will_events
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """;
        List<DigitalWillEvent> out = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, lim);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new DigitalWillEvent(
                            rs.getLong("id"),
                            rs.getInt("user_id"),
                            rs.getString("event_type"),
                            rs.getString("detail"),
                            ts(rs.getTimestamp("created_at"))
                    ));
                }
            }
        }
        return out;
    }

    private static Instant ts(Timestamp t) {
        return t != null ? t.toInstant() : null;
    }

    public record TrustedContact(int userId, String email, String name, boolean enabled) {}

    public record DigitalWillEvent(long id, int userId, String type, String detail, Instant createdAt) {}
}

