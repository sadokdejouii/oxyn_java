package org.example.windowshello;

import org.example.entities.User;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;

/**
 * Stockage du lien "compte applicatif" ↔ "compte Windows (SID)" pour autoriser la connexion Windows Hello.
 * On reste volontairement simple (pas WebAuthn/FIDO2) : on vérifie Windows Hello + on pinne le SID Windows.
 */
public final class WindowsHelloLinkDAO {

    private final Connection con;

    public WindowsHelloLinkDAO() {
        this.con = MyDataBase.getInstance().getConnection();
    }

    public void ensureTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS user_windows_hello (
                    id_user INT NOT NULL,
                    windows_sid VARCHAR(128) NOT NULL,
                    enabled TINYINT(1) NOT NULL DEFAULT 1,
                    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id_user),
                    CONSTRAINT fk_uwh_user FOREIGN KEY (id_user) REFERENCES users(id_user) ON DELETE CASCADE
                )
                """;
        try (Statement st = con.createStatement()) {
            st.execute(sql);
        }
    }

    public Optional<Link> getByUserId(int userId) throws SQLException {
        ensureTable();
        String sql = "SELECT windows_sid, enabled, enrolled_at, updated_at FROM user_windows_hello WHERE id_user = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String sid = rs.getString("windows_sid");
                boolean enabled = rs.getBoolean("enabled");
                Instant enrolledAt = rs.getTimestamp("enrolled_at").toInstant();
                Instant updatedAt = rs.getTimestamp("updated_at").toInstant();
                return Optional.of(new Link(userId, sid, enabled, enrolledAt, updatedAt));
            }
        }
    }

    public void upsert(int userId, String windowsSid, boolean enabled) throws SQLException {
        ensureTable();
        String sql = """
                INSERT INTO user_windows_hello (id_user, windows_sid, enabled)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE windows_sid = VALUES(windows_sid), enabled = VALUES(enabled)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, windowsSid);
            ps.setBoolean(3, enabled);
            ps.executeUpdate();
        }
    }

    public void disable(int userId) throws SQLException {
        ensureTable();
        String sql = "UPDATE user_windows_hello SET enabled = 0 WHERE id_user = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public Optional<User> findUserEligibleForHelloLogin(String email, String currentWindowsSid) throws SQLException {
        ensureTable();
        if (email == null || email.isBlank() || currentWindowsSid == null || currentWindowsSid.isBlank()) {
            return Optional.empty();
        }
        String sql = """
                SELECT u.id_user, u.email_user, u.password_user, u.roles_user, u.first_name_user, u.last_name_user, u.phone_user, u.is_active_user
                FROM users u
                JOIN user_windows_hello h ON h.id_user = u.id_user
                WHERE LOWER(TRIM(u.email_user)) = LOWER(TRIM(?))
                  AND u.is_active_user = 1
                  AND h.enabled = 1
                  AND h.windows_sid = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, currentWindowsSid);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                int id = rs.getInt("id_user");
                String em = rs.getString("email_user");
                String pw = rs.getString("password_user");
                String roles = rs.getString("roles_user");
                String fn = rs.getString("first_name_user");
                String ln = rs.getString("last_name_user");
                String phone = rs.getString("phone_user");
                boolean active = rs.getBoolean("is_active_user");
                return Optional.of(org.example.dao.UserDAO.createUserFromRolesJson(roles, id, em, pw, fn, ln, phone, active));
            }
        }
    }

    public record Link(int userId, String windowsSid, boolean enabled, Instant enrolledAt, Instant updatedAt) {
    }
}

