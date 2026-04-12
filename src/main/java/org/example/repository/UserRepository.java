package org.example.repository;

import org.example.entities.UserEntity;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * JDBC — table {@code users}.
 */
public final class UserRepository {

    public Optional<UserEntity> findById(int idUser) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id_user, email_user, password_user, roles_user,
                       first_name_user, last_name_user, is_active_user
                FROM users WHERE id_user = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    public Optional<UserEntity> findByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        Connection c = conn();
        if (c == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id_user, email_user, password_user, roles_user,
                       first_name_user, last_name_user, is_active_user
                FROM users WHERE email_user = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    /**
     * Premier {@code id_user} avec rôle client actif — suivi par défaut pour la vue encadrant (sans saisie d’id).
     */
    public Optional<Integer> findFirstActiveClientUserId() throws SQLException {
        Connection c = conn();
        if (c == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id_user FROM users
                WHERE is_active_user = 1
                  AND roles_user LIKE '%ROLE_CLIENT%'
                ORDER BY id_user ASC
                LIMIT 1
                """;
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.of(rs.getInt("id_user"));
            }
        }
        return Optional.empty();
    }

    public static UserEntity mapRow(ResultSet rs) throws SQLException {
        return new UserEntity(
                rs.getInt("id_user"),
                rs.getString("email_user"),
                rs.getString("password_user"),
                rs.getString("roles_user"),
                rs.getString("first_name_user"),
                rs.getString("last_name_user"),
                rs.getObject("is_active_user") != null && rs.getBoolean("is_active_user")
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
