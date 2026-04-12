package org.example.services;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.example.entities.User;

import java.util.Objects;

/**
 * Session applicative après authentification (utilisateur typé + rôle dérivé).
 */
public final class SessionContext {

    private static final SessionContext INSTANCE = new SessionContext();

    private User currentUser;
    private String displayName = "Guest";
    private UserRole role = UserRole.CLIENT;

    /** Lié à la barre supérieure pour se mettre à jour après modification du profil. */
    private final ReadOnlyStringWrapper displayNameWrapper = new ReadOnlyStringWrapper("Guest");

    private SessionContext() {
    }

    public static SessionContext getInstance() {
        return INSTANCE;
    }

    public ReadOnlyStringProperty displayNameProperty() {
        return displayNameWrapper.getReadOnlyProperty();
    }

    private void pushDisplayName(String name) {
        this.displayName = name != null ? name : "User";
        displayNameWrapper.set(this.displayName);
    }

    /**
     * Connexion à partir du modèle polymorphe {@link User} (Admin / Client / Coach).
     */
    public void login(User user) {
        this.currentUser = Objects.requireNonNull(user);
        String nom = user.getNom() != null ? user.getNom().trim() : "";
        String prenom = user.getPrenom() != null ? user.getPrenom().trim() : "";
        String composed = (prenom + " " + nom).trim();
        if (composed.isEmpty()) {
            composed = user.getEmail() != null ? user.getEmail() : "User";
        }
        pushDisplayName(composed);
        this.role = UserRole.fromUser(user);
    }

    /** @deprecated Utiliser {@link #login(User)} */
    @Deprecated
    public void login(String displayName, UserRole role) {
        this.currentUser = null;
        String n = Objects.requireNonNullElse(displayName, "User").trim();
        if (n.isEmpty()) {
            n = "User";
        }
        pushDisplayName(n);
        this.role = Objects.requireNonNullElse(role, UserRole.CLIENT);
    }

    public void logout() {
        this.currentUser = null;
        pushDisplayName("Guest");
        this.role = UserRole.CLIENT;
    }

    /**
     * Après mise à jour du profil en base : remplace l'utilisateur en session et rafraîchit le nom affiché.
     */
    public void applyRefreshedUser(User user) {
        Objects.requireNonNull(user);
        if (currentUser != null && user.getId() != currentUser.getId()) {
            throw new IllegalStateException("Identifiant utilisateur incohérent avec la session.");
        }
        login(user);
    }

    public User getCurrentUser() {
        return currentUser;
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
