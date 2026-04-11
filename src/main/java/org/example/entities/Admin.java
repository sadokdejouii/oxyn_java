package org.example.entities;

public class Admin extends User {

    public Admin() {
        super();
    }

    public Admin(int id, String email, String password, String nom, String prenom, String telephone, boolean isActive) {
        super(id, email, password, nom, prenom, telephone, isActive);
    }

    @Override
    public String getRolesJsonValue() {
        return "[\"ROLE_ADMIN\"]";
    }
}
