package org.example.totp;

import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;

public final class TotpDAO {

    private final Connection con;

    public TotpDAO() throws SQLException {
        this.con = MyDataBase.requireConnection();
    }

    public void ensureTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS user_totp (
                    id_user INT NOT NULL,
                    secret_base32 VARCHAR(128) NOT NULL,
                    enabled TINYINT(1) NOT NULL DEFAULT 1,
                    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id_user),
                    CONSTRAINT fk_ut_user FOREIGN KEY (id_user) REFERENCES users(id_user) ON DELETE CASCADE
                )
                """;
        try (Statement st = con.createStatement()) {
            st.execute(sql);
        }
    }

    public Optional<Record> getByUserId(int userId) throws SQLException {
        ensureTable();
        String sql = "SELECT secret_base32, enabled, enrolled_at, updated_at FROM user_totp WHERE id_user = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String secret = rs.getString("secret_base32");
                boolean enabled = rs.getBoolean("enabled");
                Instant enrolledAt = rs.getTimestamp("enrolled_at").toInstant();
                Instant updatedAt = rs.getTimestamp("updated_at").toInstant();
                return Optional.of(new Record(userId, secret, enabled, enrolledAt, updatedAt));
            }
        }
    }

    public void upsert(int userId, String secretBase32, boolean enabled) throws SQLException {
        ensureTable();
        String sql = """
                INSERT INTO user_totp (id_user, secret_base32, enabled)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE secret_base32 = VALUES(secret_base32), enabled = VALUES(enabled)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, secretBase32);
            ps.setBoolean(3, enabled);
            ps.executeUpdate();
        }
    }

    public void disable(int userId) throws SQLException {
        ensureTable();
        String sql = "UPDATE user_totp SET enabled = 0 WHERE id_user = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public record Record(int userId, String secretBase32, boolean enabled, Instant enrolledAt, Instant updatedAt) {
    }
}

