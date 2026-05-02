package org.example.services;

import org.example.dao.UserDAO;
import org.example.entities.User;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Couche service : opérations métier sur les utilisateurs (délègue au {@link UserDAO}).
 */
public class UserService {

    private UserDAO userDAO;

    private UserDAO dao() throws SQLException {
        if (userDAO == null) {
            userDAO = new UserDAO();
        }
        return userDAO;
    }

    public void addUser(User user) throws SQLException {
        dao().addUser(user);
    }

    public void updateUser(User user) throws SQLException {
        dao().updateUser(user);
    }

    public void deleteUser(int id) throws SQLException {
        dao().deleteUser(id);
    }

    public User getUserById(int id) throws SQLException {
        return dao().getUserById(id);
    }

    public List<User> getAllUsers() throws SQLException {
        return dao().getAllUsers();
    }

    public User findByEmail(String email) throws SQLException {
        return dao().findByEmail(email);
    }

    public boolean updatePasswordHashByEmail(String email, String passwordHash) throws SQLException {
        return dao().updatePasswordHashByEmail(email, passwordHash);
    }

    public void touchLastSeen(int userId) throws SQLException {
        dao().touchLastSeen(userId);
    }

    /**
     * Met à jour uniquement nom, prénom et téléphone pour l'utilisateur connecté (contrôle d'identité côté appelant).
     */
    public void updateProfilePartial(int userId, String nom, String prenom, String telephone) throws SQLException {
        dao().updateProfilePartial(userId, nom, prenom, telephone);
    }

    /**
     * Returns "FirstName LastName" for a user without triggering role mapping.
     * Falls back to email, then "Utilisateur #id".
     */
    public String getUserDisplayName(int id) {
        String sql = "SELECT first_name_user, last_name_user, email_user FROM users WHERE id_user = ?";
        try (Connection con = MyDataBase.requireConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String first = rs.getString("first_name_user");
                    String last = rs.getString("last_name_user");
                    String fullName = ((first != null ? first.trim() : "") + " " + (last != null ? last.trim() : "")).trim();
                    if (!fullName.isBlank()) {
                        return fullName;
                    }
                    String email = rs.getString("email_user");
                    if (email != null && !email.isBlank()) {
                        return email;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "Utilisateur #" + id;
    }
}
