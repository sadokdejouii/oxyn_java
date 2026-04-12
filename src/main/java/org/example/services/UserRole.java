package org.example.services;

import org.example.entities.Admin;
import org.example.entities.Client;
import org.example.entities.Coach;
import org.example.entities.User;

/**
 * Rôle applicatif (aligné sur {@code oxyn_java} / branche {@code main}) + parsing Symfony {@code roles_user}.
 */
public enum UserRole {
    ADMIN,
    CLIENT,
    ENCADRANT;

    public static UserRole fromUser(User user) {
        if (user instanceof Admin) {
            return ADMIN;
        }
        if (user instanceof Coach) {
            return ENCADRANT;
        }
        return CLIENT;
    }

    /** Parse le JSON {@code roles_user} Symfony. */
    public static UserRole fromSymfonyRolesJson(String rolesJson) {
        if (rolesJson == null) {
            return CLIENT;
        }
        if (rolesJson.contains("ROLE_ADMIN")) {
            return ADMIN;
        }
        if (rolesJson.contains("ROLE_ENCADRANT")) {
            return ENCADRANT;
        }
        return CLIENT;
    }

    public String displayLabel() {
        return switch (this) {
            case ADMIN -> "Admin";
            case CLIENT -> "Client";
            case ENCADRANT -> "Encadrant";
        };
    }
}
