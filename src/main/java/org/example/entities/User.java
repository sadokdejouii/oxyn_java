package org.example.entities;

import java.util.Objects;

/**
 * Modèle utilisateur commun. Mapping SQL :
 * id_user → id, email_user → email, password_user → password,
 * first_name_user → nom, last_name_user → prenom, phone_user → telephone,
 * is_active_user → isActive
 */
public abstract class User {

    protected int id;
    protected String email;
    protected String password;
    protected String nom;
    protected String prenom;
    protected String telephone;
    protected boolean isActive;

    protected User() {
    }

    protected User(int id, String email, String password, String nom, String prenom, String telephone, boolean isActive) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.isActive = isActive;
    }

    /** Jeton JSON pour la colonne roles_user (ex. ["ROLE_ADMIN"]). */
    public abstract String getRolesJsonValue();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getFullName() {
        return (nom != null ? nom : "") + " " + (prenom != null ? prenom : "");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", telephone='" + telephone + '\'' +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return id == user.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
