package org.example.services;

import java.util.regex.Pattern;

/**
 * Règles métier : e-mail, nom/prénom, mot de passe (inscription + formulaires admin).
 */
public final class AuthValidation {

    /** Local@domaine.tld — ASCII, pas d'espaces ; caractères courants pour la partie locale. */
    private static final Pattern EMAIL_STRICT = Pattern.compile(
            "^[a-zA-Z0-9][a-zA-Z0-9._%+-]*@[a-zA-Z0-9][a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    /** Lettres Unicode + apostrophes + tiret ; 2–30 caractères (sans espaces). */
    private static final Pattern PERSON_NAME = Pattern.compile("^[\\p{L}'’\\-]{2,30}$");

    private static final Pattern PASSWORD_COMPLEX = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,}$");

    private AuthValidation() {
    }

    /**
     * @return message d'erreur ou {@code null} si valide
     */
    public static String validateEmailContent(String email) {
        if (email == null || email.isEmpty()) {
            return "L'e-mail est obligatoire.";
        }
        if (!email.equals(email.trim())) {
            return "Supprimez les espaces en début ou en fin.";
        }
        if (containsWhitespace(email)) {
            return "Les espaces ne sont pas autorisés dans l'e-mail.";
        }
        if (!email.contains("@")) {
            return "L'e-mail doit contenir le signe @.";
        }
        if (!email.contains(".")) {
            return "L'e-mail doit contenir un point (ex. .com).";
        }
        if (!EMAIL_STRICT.matcher(email).matches()) {
            return "Format invalide ou caractères non autorisés.";
        }
        return null;
    }

    /**
     * @param isPrenom {@code true} pour le prénom, {@code false} pour le nom
     */
    public static String validatePersonName(String value, boolean isPrenom) {
        String label = isPrenom ? "Le prénom" : "Le nom";
        if (value == null || value.isEmpty()) {
            return label + " est obligatoire.";
        }
        if (!value.equals(value.trim())) {
            return label + " : pas d'espaces en début ou en fin.";
        }
        if (containsWhitespace(value)) {
            return label + " : les espaces ne sont pas autorisés.";
        }
        if (value.length() < 2 || value.length() > 30) {
            return label + " : entre 2 et 30 caractères.";
        }
        if (!PERSON_NAME.matcher(value).matches()) {
            return label + " : uniquement des lettres (accents autorisés), tiret ou apostrophe — pas de chiffre ni symbole.";
        }
        return null;
    }

    /**
     * Mot de passe pour création ou modification (champ renseigné).
     */
    public static String validatePasswordCreate(String password, String nom, String prenom) {
        if (password == null || password.isEmpty()) {
            return "Le mot de passe est obligatoire.";
        }
        if (containsWhitespace(password)) {
            return "Les espaces ne sont pas autorisés dans le mot de passe.";
        }
        if (password.length() < 8) {
            return "Au moins 8 caractères.";
        }
        if (!PASSWORD_COMPLEX.matcher(password).matches()) {
            return "Une majuscule, une minuscule, un chiffre et un caractère spécial sont requis.";
        }
        if ("12345678".equals(password)) {
            return "Ce mot de passe est interdit.";
        }
        if (password.equalsIgnoreCase("password")) {
            return "Ce mot de passe est interdit.";
        }
        String n = nom != null ? nom.trim().toLowerCase() : "";
        String p = prenom != null ? prenom.trim().toLowerCase() : "";
        String lower = password.toLowerCase();
        if (n.length() >= 2 && lower.contains(n)) {
            return "Le mot de passe ne doit pas contenir votre nom.";
        }
        if (p.length() >= 2 && lower.contains(p)) {
            return "Le mot de passe ne doit pas contenir votre prénom.";
        }
        return null;
    }

    /** Édition : mot de passe vide = conserver l'existant (pas de validation). */
    public static String validatePasswordEdit(String password, String nom, String prenom) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        return validatePasswordCreate(password, nom, prenom);
    }

    public static String validateConfirmPassword(String password, String confirm) {
        if (confirm == null || confirm.isEmpty()) {
            return "Confirmez le mot de passe.";
        }
        if (!password.equals(confirm)) {
            return "La confirmation ne correspond pas.";
        }
        return null;
    }

    /**
     * Inscription : validation globale (téléphone obligatoire inchangé).
     *
     * @return premier message d'erreur ou {@code null}
     */
    public static String validateRegistrationForm(String nom, String prenom, String email, String telephone,
                                                  String password, String confirmPassword) {
        String err = validatePersonName(nom, false);
        if (err != null) {
            return err;
        }
        err = validatePersonName(prenom, true);
        if (err != null) {
            return err;
        }
        err = validateEmailContent(email);
        if (err != null) {
            return err;
        }
        if (isBlank(telephone)) {
            return "Le téléphone est obligatoire.";
        }
        err = validatePasswordCreate(password, nom.trim(), prenom.trim());
        if (err != null) {
            return err;
        }
        return validateConfirmPassword(password, confirmPassword);
    }

    private static boolean containsWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
