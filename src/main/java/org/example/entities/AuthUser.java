package org.example.entities;

/**
 * Utilisateur authentifié (table {@code users} Symfony).
 */
public record AuthUser(int id, String email, String firstName, String lastName, UserRole role) {

    public String fullDisplayName() {
        String fn = firstName == null ? "" : firstName.trim();
        String ln = lastName == null ? "" : lastName.trim();
        String full = (fn + " " + ln).trim();
        return full.isEmpty() ? email : full;
    }
}
