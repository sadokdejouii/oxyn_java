package org.example.entities;

/**
 * Ligne table {@code users} (champs utilisés par l’app Symfony / JDBC existant).
 */
public record UserEntity(
        int idUser,
        String emailUser,
        String passwordUser,
        String rolesUser,
        String firstNameUser,
        String lastNameUser,
        boolean isActiveUser
) {
}
