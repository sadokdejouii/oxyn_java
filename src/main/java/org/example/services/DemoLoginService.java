package org.example.services;

import org.example.entities.AuthUser;
import org.example.entities.UserRole;
import org.example.repository.UserRepository;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Connexion démo temporaire : recherche de l’e-mail dans {@code users}, sans mot de passe.
 */
public final class DemoLoginService {

    private final UserRepository users = new UserRepository();

    /**
     * @return utilisateur avec {@code id_user} et rôle dérivé de {@code roles_user} (JSON type Symfony),
     *         ou vide si e-mail absent, compte inactif ou base indisponible.
     */
    public Optional<AuthUser> loginByEmail(String emailInput) throws SQLException {
        if (emailInput == null || emailInput.isBlank()) {
            return Optional.empty();
        }
        String email = emailInput.trim();
        var row = users.findByEmail(email);
        if (row.isEmpty()) {
            return Optional.empty();
        }
        var u = row.get();
        if (!u.isActiveUser()) {
            return Optional.empty();
        }
        UserRole role = UserRole.fromSymfonyRolesJson(u.rolesUser());
        return Optional.of(new AuthUser(
                u.idUser(),
                u.emailUser(),
                u.firstNameUser(),
                u.lastNameUser(),
                role
        ));
    }
}
