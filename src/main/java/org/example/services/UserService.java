package org.example.services;

import org.example.dao.UserDAO;
import org.example.entities.User;

import java.sql.SQLException;
import java.util.List;

/**
 * Couche service : opérations métier sur les utilisateurs (délègue au {@link UserDAO}).
 */
public class UserService {

    private final UserDAO userDAO = new UserDAO();

    public void addUser(User user) throws SQLException {
        userDAO.addUser(user);
    }

    public void updateUser(User user) throws SQLException {
        userDAO.updateUser(user);
    }

    public void deleteUser(int id) throws SQLException {
        userDAO.deleteUser(id);
    }

    public User getUserById(int id) throws SQLException {
        return userDAO.getUserById(id);
    }

    public List<User> getAllUsers() throws SQLException {
        return userDAO.getAllUsers();
    }

    public User findByEmail(String email) throws SQLException {
        return userDAO.findByEmail(email);
    }

    public boolean updatePasswordHashByEmail(String email, String passwordHash) throws SQLException {
        return userDAO.updatePasswordHashByEmail(email, passwordHash);
    }

    public void touchLastSeen(int userId) throws SQLException {
        userDAO.touchLastSeen(userId);
    }

    /**
     * Met à jour uniquement nom, prénom et téléphone pour l'utilisateur connecté (contrôle d'identité côté appelant).
     */
    public void updateProfilePartial(int userId, String nom, String prenom, String telephone) throws SQLException {
        userDAO.updateProfilePartial(userId, nom, prenom, telephone);
    }
}
