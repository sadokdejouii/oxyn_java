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
    /** Identifiant client en base (table commandes.id_client_commande). */
    private int clientDatabaseId = 1;

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
        login(displayName, role, 1);
    }

    public void login(String displayName, UserRole role, int clientDatabaseId) {
        this.displayName = Objects.requireNonNullElse(displayName, "User").trim();
        if (this.displayName.isEmpty()) {
            this.displayName = "User";
        this.currentUser = null;
        String n = Objects.requireNonNullElse(displayName, "User").trim();
        if (n.isEmpty()) {
            n = "User";
        }
        pushDisplayName(n);
        this.role = Objects.requireNonNullElse(role, UserRole.CLIENT);
        this.clientDatabaseId = clientDatabaseId > 0 ? clientDatabaseId : 1;
    }

    public void logout() {
        this.currentUser = null;
        pushDisplayName("Guest");
        this.role = UserRole.CLIENT;
        this.clientDatabaseId = 1;
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

    public int getClientDatabaseId() {
        return clientDatabaseId;
    public boolean isEncadrant() {
        return role == UserRole.ENCADRANT;
    }
}
