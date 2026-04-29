package org.example.services;



import javafx.beans.property.ReadOnlyStringProperty;

import javafx.beans.property.ReadOnlyStringWrapper;

import org.example.entities.User;



import java.util.Objects;



/**

 * Session après connexion : modèle {@link User}, propriété JavaFX pour la barre du haut,

 * hook navigation discussion depuis le planning, et identifiant client pour les modules boutique/commandes.

 */

public final class SessionContext {



    private static final SessionContext INSTANCE = new SessionContext();



    private User currentUser;



    private String displayName = "Guest";

    private UserRole role = UserRole.CLIENT;

    private int legacyUserId = -1;

    private String legacyEmail = "";



    private final ReadOnlyStringWrapper displayNameWrapper = new ReadOnlyStringWrapper("Guest");



    private Runnable openDiscussionFromPlanningAction;



    /** Client ciblé à l’ouverture de la messagerie (ex. depuis Planning encadrant), ou -1. */

    private int pendingDiscussionClientUserId = -1;



    private SessionContext() {

    }



    public static SessionContext getInstance() {

        return INSTANCE;

    }



    public ReadOnlyStringProperty displayNameProperty() {

        return displayNameWrapper.getReadOnlyProperty();

    }



    private void pushDisplayName(String name) {

        if (name == null || name.isBlank()) {

            this.displayName = "User";

        } else {

            this.displayName = name.trim();

        }

        displayNameWrapper.set(this.displayName);

    }



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

        this.legacyUserId = user.getId();

        this.legacyEmail = user.getEmail() != null ? user.getEmail().trim() : "";

    }



    /** @deprecated Préférer {@link #login(User)}. */

    @Deprecated

    public void login(String displayName, UserRole role) {

        login(displayName, role, -1, "");

    }



    /** @deprecated Préférer {@link #login(User)}. */

    @Deprecated

    public void login(String displayName, UserRole role, int userId, String email) {

        this.currentUser = null;

        String n = Objects.requireNonNullElse(displayName, "User").trim();

        if (n.isEmpty()) {

            n = "User";

        }

        pushDisplayName(n);

        this.role = Objects.requireNonNullElse(role, UserRole.CLIENT);

        this.legacyUserId = userId;

        this.legacyEmail = email == null ? "" : email.trim();

    }



    public void applyRefreshedUser(User user) {

        Objects.requireNonNull(user);

        if (currentUser != null && user.getId() != currentUser.getId()) {

            throw new IllegalStateException("Identifiant utilisateur incohérent avec la session.");

        }

        login(user);

    }



    public void setOpenDiscussionFromPlanningAction(Runnable action) {

        this.openDiscussionFromPlanningAction = action;

    }



    public void openDiscussionFromPlanning() {

        if (openDiscussionFromPlanningAction != null) {

            openDiscussionFromPlanningAction.run();

        }

    }



    public void setPendingDiscussionClientUserId(int clientUserId) {

        this.pendingDiscussionClientUserId = clientUserId > 0 ? clientUserId : -1;

    }



    public int getPendingDiscussionClientUserId() {

        return pendingDiscussionClientUserId;

    }



    public void clearPendingDiscussionClientUserId() {

        this.pendingDiscussionClientUserId = -1;

    }



    public void logout() {

        this.currentUser = null;

        pushDisplayName("Guest");

        this.role = UserRole.CLIENT;

        this.legacyUserId = -1;

        this.legacyEmail = "";

        this.openDiscussionFromPlanningAction = null;

        this.pendingDiscussionClientUserId = -1;

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



    public int getUserId() {

        if (currentUser != null) {

            return currentUser.getId();

        }

        return legacyUserId;

    }



    public String getEmail() {

        if (currentUser != null && currentUser.getEmail() != null) {

            return currentUser.getEmail();

        }

        return legacyEmail;

    }



    /**

     * Identifiant client pour boutique / commandes (API attendue par la branche {@code main}).

     */

    public int getClientDatabaseId() {

        int id = getUserId();

        return id > 0 ? id : 1;

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

        return getUserId() > 0;

    }

}

