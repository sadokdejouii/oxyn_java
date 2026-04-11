package org.example.services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.example.entities.AuthUser;
import org.example.entities.UserRole;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Charge les utilisateurs depuis la table Symfony {@code users} et vérifie les hash bcrypt ({@code $2y$}).
 */
public final class AuthService {

    public Optional<AuthUser> authenticate(String emailInput, String plainPassword) throws SQLException {
        if (emailInput == null || emailInput.isBlank()) {
            return Optional.empty();
        }
        String email = emailInput.trim();
        Connection c = MyDataBase.getInstance().getConnection();
        if (c == null) {
            return Optional.empty();
        }

        String sql = """
                SELECT id_user, email_user, password_user, roles_user, first_name_user, last_name_user
                FROM users
                WHERE email_user = ? AND is_active_user = 1
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                int id = rs.getInt("id_user");
                String hash = rs.getString("password_user");
                String rolesJson = rs.getString("roles_user");
                String fn = rs.getString("first_name_user");
                String ln = rs.getString("last_name_user");

                if (plainPassword == null || !verifyBcrypt(plainPassword, hash)) {
                    return Optional.empty();
                }
                return Optional.of(new AuthUser(id, email, fn, ln, UserRole.fromSymfonyRolesJson(rolesJson)));
            }
        }
    }

    private static boolean verifyBcrypt(String plain, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) {
            return false;
        }
        String normalized = storedHash.startsWith("$2y$")
                ? "$2a$" + storedHash.substring(4)
                : storedHash;
        return BCrypt.verifyer().verify(plain.toCharArray(), normalized).verified;
    }
}
