package org.example.services;

import org.example.entities.Admin;
import org.example.entities.Coach;
import org.example.entities.User;

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

    public String displayLabel() {
        return switch (this) {
            case ADMIN -> "Admin";
            case CLIENT -> "Client";
            case ENCADRANT -> "Encadrant";
        };
    }
}
