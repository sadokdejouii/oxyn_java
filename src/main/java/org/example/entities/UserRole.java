package org.example.entities;

public enum UserRole {
    ADMIN,
    ENCADRANT,
    CLIENT;

    public String displayLabel() {
        return switch (this) {
            case ADMIN -> "Admin";
            case ENCADRANT -> "Encadrant";
            case CLIENT -> "Client";
        };
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
}
