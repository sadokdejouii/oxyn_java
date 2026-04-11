package org.example.services;

import java.util.Objects;

/**
 * Holds the signed-in user for the current JVM session (after login).
 */
public final class SessionContext {

    private static final SessionContext INSTANCE = new SessionContext();

    private String displayName = "Guest";
    private UserRole role = UserRole.CLIENT;
    /** Identifiant client en base (table commandes.id_client_commande). */
    private int clientDatabaseId = 1;

    private SessionContext() {
    }

    public static SessionContext getInstance() {
        return INSTANCE;
    }

    public void login(String displayName, UserRole role) {
        login(displayName, role, 1);
    }

    public void login(String displayName, UserRole role, int clientDatabaseId) {
        this.displayName = Objects.requireNonNullElse(displayName, "User").trim();
        if (this.displayName.isEmpty()) {
            this.displayName = "User";
        }
        this.role = Objects.requireNonNullElse(role, UserRole.CLIENT);
        this.clientDatabaseId = clientDatabaseId > 0 ? clientDatabaseId : 1;
    }

    public void logout() {
        this.displayName = "Guest";
        this.role = UserRole.CLIENT;
        this.clientDatabaseId = 1;
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

    public int getClientDatabaseId() {
        return clientDatabaseId;
    }
}
