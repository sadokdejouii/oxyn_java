package org.example.services;

public enum UserRole {
    ADMIN,
    CLIENT,
    ENCADRANT;

    public String displayLabel() {
        switch (this) {
            case ADMIN: return "Admin";
            case ENCADRANT: return "Encadrant";
            default: return "Client";
        }
    }
}
