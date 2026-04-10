package org.example.services;

public enum UserRole {
    ADMIN,
    CLIENT;

    public String displayLabel() {
        return this == ADMIN ? "Admin" : "Client";
    }
}
