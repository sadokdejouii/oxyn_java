package org.example.avatar;

import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;

public final class AvatarDAO {

    private final Connection con;

    public AvatarDAO() {
        this.con = MyDataBase.getInstance().getConnection();
    }

    public void ensureTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS user_avatar (
                    id_user INT NOT NULL,
                    avatar_png LONGBLOB NOT NULL,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id_user),
                    CONSTRAINT fk_ua_user FOREIGN KEY (id_user) REFERENCES users(id_user) ON DELETE CASCADE
                )
                """;
        try (Statement st = con.createStatement()) {
            st.execute(sql);
        }
    }

    public Optional<Record> getByUserId(int userId) throws SQLException {
        ensureTable();
        String sql = "SELECT avatar_png, updated_at FROM user_avatar WHERE id_user = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                byte[] png = rs.getBytes("avatar_png");
                Instant updatedAt = rs.getTimestamp("updated_at").toInstant();
                return Optional.of(new Record(userId, png, updatedAt));
            }
        }
    }

    public void upsert(int userId, byte[] avatarPng) throws SQLException {
        ensureTable();
        String sql = """
                INSERT INTO user_avatar (id_user, avatar_png)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE avatar_png = VALUES(avatar_png)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setBytes(2, avatarPng);
            ps.executeUpdate();
        }
    }

    public void delete(int userId) throws SQLException {
        ensureTable();
        String sql = "DELETE FROM user_avatar WHERE id_user = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public record Record(int userId, byte[] avatarPng, Instant updatedAt) {
    }
}

