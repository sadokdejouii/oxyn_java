package org.example.entities;

/**
 * Utilisateur avec le rôle encadrant (coach), mappé sur {@code ROLE_ENCADRANT} en base.
 */
public class Coach extends User {

    public Coach() {
        super();
    }

    public Coach(int id, String email, String password, String nom, String prenom, String telephone, boolean isActive) {
        super(id, email, password, nom, prenom, telephone, isActive);
    }

    @Override
    public String getRolesJsonValue() {
        return "[\"ROLE_ENCADRANT\"]";
    }
}
