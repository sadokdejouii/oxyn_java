package org.example.services;

import org.example.dao.UserDAO;
import org.example.entities.Client;
import org.example.entities.User;
import org.example.utils.PasswordUtils;

import java.sql.SQLException;

/**
 * Authentification et inscription (délègue au {@link UserDAO}, hash BCrypt côté service).
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Connexion : email connu, mot de passe BCrypt valide, compte actif.
     *
     * @return l'utilisateur typé (Admin / Client / Coach), ou {@code null} si échec
     */
    public User login(String email, String password) throws SQLException {
        return userDAO.login(email, password);
    }

    /**
     * Inscription d'un client (rôle imposé côté entité {@link Client}).
     *
     * @return {@code true} si l'insertion a réussi
     */
    public boolean registerClient(String nom, String prenom, String email, String telephone,
                                  String plainPassword, String confirmPassword) throws SQLException {
        String err = AuthValidation.validateRegistrationForm(nom, prenom, email, telephone, plainPassword, confirmPassword);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }
        String em = email.trim().toLowerCase();
        if (userDAO.findByEmail(em) != null) {
            return false;
        }
        String hash = PasswordUtils.hash(plainPassword);
        Client client = new Client(0, em, hash, nom.trim(), prenom.trim(), telephone.trim(), true);
        return userDAO.register(client);
    }
}
