package org.example.services;

import org.example.entities.AuthUser;
import org.example.entities.UserRole;

import java.util.Objects;

/**
 * Session utilisateur courante après connexion.
 */
public final class SessionContext {

    private static final SessionContext INSTANCE = new SessionContext();

    private String displayName = "Guest";
    private UserRole role = UserRole.CLIENT;
    /** Symfony {@code users.id_user}; -1 si non connecté. */
    private int userId = -1;
    private String email = "";
    /** Connexion démo (e-mail sans mot de passe). */
    private boolean demoAuthentication;

    private SessionContext() {
    }

    public static SessionContext getInstance() {
        return INSTANCE;
    }

    public void login(String displayName, UserRole role) {
        login(displayName, role, -1, "");
    }

    public void login(String displayName, UserRole role, int userId, String email) {
        this.displayName = Objects.requireNonNullElse(displayName, "User").trim();
        if (this.displayName.isEmpty()) {
            this.displayName = "User";
        }
        this.role = Objects.requireNonNullElse(role, UserRole.CLIENT);
        this.userId = userId;
        this.email = email == null ? "" : email.trim();
    }

    /** Session après résolution JDBC (table {@code users}). */
    public void login(AuthUser user) {
        Objects.requireNonNull(user, "user");
        login(user.fullDisplayName(), user.role(), user.id(), user.email());
    }

    private Runnable openDiscussionFromPlanningAction;

    public void setOpenDiscussionFromPlanningAction(Runnable action) {
        this.openDiscussionFromPlanningAction = action;
    }

    public void openDiscussionFromPlanning() {
        if (openDiscussionFromPlanningAction != null) {
            openDiscussionFromPlanningAction.run();
        }
    }

    public void logout() {
        this.displayName = "Guest";
        this.role = UserRole.CLIENT;
        this.userId = -1;
        this.email = "";
        this.demoAuthentication = false;
        this.openDiscussionFromPlanningAction = null;
    }

    public void setDemoAuthentication(boolean demoAuthentication) {
        this.demoAuthentication = demoAuthentication;
    }

    public boolean isDemoAuthentication() {
        return demoAuthentication;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public int getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isEncadrant() {
        return role == UserRole.ENCADRANT;
    }

    public boolean isClientUser() {
        return role == UserRole.CLIENT;
    }

    public boolean hasDbUser() {
        return userId > 0;
    }
}
