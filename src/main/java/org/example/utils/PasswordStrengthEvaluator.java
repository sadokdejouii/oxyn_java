package org.example.utils;

public final class PasswordStrengthEvaluator {

    public enum Strength { WEAK, MEDIUM, STRONG }

    private PasswordStrengthEvaluator() {}

    /**
     * Évalue la force d'un mot de passe.
     * Faible  : < 8 chars OU un seul type de caractère
     * Moyenne : 8+ chars avec 2-3 types de caractères
     * Fort    : 8+ chars avec les 4 types (maj + min + chiffre + spécial)
     */
    public static Strength evaluate(String password) {
        if (password == null || password.isEmpty()) return Strength.WEAK;

        int score = 0;
        if (password.length() >= 8)                          score++;
        if (password.matches(".*[a-z].*"))                   score++;
        if (password.matches(".*[A-Z].*"))                   score++;
        if (password.matches(".*\\d.*"))                     score++;
        if (password.matches(".*[^A-Za-z0-9\\s].*"))        score++;

        if (score <= 2) return Strength.WEAK;
        if (score <= 3) return Strength.MEDIUM;
        return Strength.STRONG;
    }

    public static String label(Strength s) {
        return switch (s) {
            case WEAK   -> "Faible";
            case MEDIUM -> "Moyen";
            case STRONG -> "Fort";
        };
    }

    /** Couleur CSS JavaFX selon la force */
    public static String color(Strength s) {
        return switch (s) {
            case WEAK   -> "#e74c3c";   // rouge
            case MEDIUM -> "#f39c12";   // orange
            case STRONG -> "#27ae60";   // vert
        };
    }
}

