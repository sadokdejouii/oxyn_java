package org.example.services;

import java.util.Objects;

/**
 * Holds the signed-in user for the current JVM session (after login).
 */
public final class SessionContext {

    private static final SessionContext INSTANCE = new SessionContext();

    private String displayName = "Guest";
    private UserRole role = UserRole.CLIENT;

    private SessionContext() {
    }

    public static SessionContext getInstance() {
        return INSTANCE;
    }

    public void login(String displayName, UserRole role) {
        this.displayName = Objects.requireNonNullElse(displayName, "User").trim();
        if (this.displayName.isEmpty()) {
            this.displayName = "User";
        }
        this.role = Objects.requireNonNullElse(role, UserRole.CLIENT);
    }

    public void logout() {
        this.displayName = "Guest";
        this.role = UserRole.CLIENT;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isEncadrant() {
        return role == UserRole.ENCADRANT;
    }
}
