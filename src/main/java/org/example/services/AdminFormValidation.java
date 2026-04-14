package org.example.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Contrôles de saisie pour les formulaires admin (salles, équipements, abonnements).
 */
public final class AdminFormValidation {

    /** Noms de salle / offre : lettres, chiffres, espaces et ponctuation courante. */
    private static final Pattern ADMIN_ENTITY_NAME = Pattern.compile(
            "^[\\p{L}0-9'’\\-.,/+&()\\s°#:%]{2,150}$");

    private static final Pattern EQUIP_NAME = Pattern.compile(
            "^[\\p{L}0-9'’\\-.,/+&()\\s°#:%]{2,120}$");

    private AdminFormValidation() {
    }

    /**
     * Adresse structurée : au moins trois parties non vides (rue / complément, ville, pays),
     * séparées par des virgules ou des points-virgules.
     */
    public static String validateAddressRueVillePays(String raw) {
        if (raw == null || raw.isBlank()) {
            return "L'adresse est obligatoire.";
        }
        String t = raw.trim();
        if (t.length() < 12) {
            return "L'adresse est trop courte : précisez rue, ville et pays.";
        }
        if (t.length() > 500) {
            return "L'adresse ne peut pas dépasser 500 caractères.";
        }
        String[] parts = t.split("[,;]");
        List<String> segs = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty()) {
                segs.add(s);
            }
        }
        if (segs.size() < 3) {
            return "Format attendu : rue (et complément), ville, pays — au moins trois parties séparées par des virgules.";
        }
        for (String s : segs) {
            if (s.length() < 2) {
                return "Chaque partie (rue, ville, pays) doit comporter au moins 2 caractères.";
            }
        }
        return null;
    }

    public static String validateEmailIfPresent(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return AuthValidation.validateEmailContent(email.trim());
    }

    /**
     * Nom de salle ou d'offre : obligatoire, caractères courants, longueur bornée.
     */
    public static String validateAdminDisplayName(String value, String fieldLabel, int minLen, int maxLen) {
        if (value == null || value.isBlank()) {
            return fieldLabel + " : ce champ est obligatoire.";
        }
        String t = value.trim();
        if (t.length() < minLen || t.length() > maxLen) {
            return fieldLabel + " : entre " + minLen + " et " + maxLen + " caractères.";
        }
        if (!ADMIN_ENTITY_NAME.matcher(t).matches()) {
            return fieldLabel + " : caractères non autorisés ou format incorrect.";
        }
        return null;
    }

    public static String validateEquipmentName(String name) {
        if (name == null || name.isBlank()) {
            return "Le nom de l'équipement est obligatoire.";
        }
        String t = name.trim();
        if (t.length() < 2 || t.length() > 120) {
            return "Le nom : entre 2 et 120 caractères.";
        }
        if (!EQUIP_NAME.matcher(t).matches()) {
            return "Le nom contient des caractères non autorisés.";
        }
        return null;
    }

    public static String validateOptionalDescription(String desc, int maxLen, String fieldLabel) {
        if (desc == null || desc.trim().isEmpty()) {
            return null;
        }
        if (desc.length() > maxLen) {
            return fieldLabel + " : " + maxLen + " caractères maximum.";
        }
        return null;
    }

    public static String validateQuantityInt(String raw, int minInclusive, int maxInclusive) {
        if (raw == null || raw.isBlank()) {
            return "La quantité est obligatoire.";
        }
        String t = raw.trim();
        if (!t.matches("^\\d+$")) {
            return "La quantité doit être un nombre entier positif (sans espace ni décimale).";
        }
        try {
            long v = Long.parseLong(t);
            if (v < minInclusive || v > maxInclusive) {
                return "La quantité doit être comprise entre " + minInclusive + " et " + maxInclusive + ".";
            }
        } catch (NumberFormatException e) {
            return "Quantité invalide.";
        }
        return null;
    }

    public static String validateDurationMonths(String raw, int minMonths, int maxMonths) {
        if (raw == null || raw.isBlank()) {
            return "La durée est obligatoire.";
        }
        String t = raw.trim();
        if (!t.matches("^\\d+$")) {
            return "La durée doit être un nombre entier de mois (ex. 1, 3, 12).";
        }
        try {
            int m = Integer.parseInt(t);
            if (m < minMonths || m > maxMonths) {
                return "La durée doit être entre " + minMonths + " et " + maxMonths + " mois.";
            }
        } catch (NumberFormatException e) {
            return "Durée invalide.";
        }
        return null;
    }

    /**
     * Prix en TND : nombre positif ou nul, au plus deux décimales, plafond raisonnable.
     */
    public static String validatePriceTnd(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Le prix est obligatoire.";
        }
        String s = raw.trim().replace(" ", "").replace(',', '.');
        if (s.startsWith("+")) {
            return "Ne pas utiliser de signe « + » devant le prix.";
        }
        BigDecimal bd;
        try {
            bd = new BigDecimal(s);
        } catch (NumberFormatException e) {
            return "Prix invalide (ex. 49.90 ou 49,90).";
        }
        if (bd.signum() < 0) {
            return "Le prix ne peut pas être négatif.";
        }
        try {
            bd.multiply(new BigDecimal("100")).toBigIntegerExact();
        } catch (ArithmeticException e) {
            return "Le prix : au plus deux décimales (ex. 29,90).";
        }
        if (bd.compareTo(new BigDecimal("1000000")) > 0) {
            return "Le prix dépasse le plafond autorisé (1 000 000 TND).";
        }
        return null;
    }

    public static BigDecimal parsePriceTnd(String raw) {
        String s = raw.trim().replace(" ", "").replace(',', '.');
        return new BigDecimal(s).setScale(2, RoundingMode.HALF_UP);
    }

    public static String validateSalleAdminForm(String nom, String description, String adresse,
                                                String telephone, String email) {
        String err = validateAdminDisplayName(nom, "Le nom de la salle", 2, 150);
        if (err != null) {
            return err;
        }
        err = validateOptionalDescription(description, 4000, "La description");
        if (err != null) {
            return err;
        }
        err = validateAddressRueVillePays(adresse);
        if (err != null) {
            return err;
        }
        err = AuthValidation.validateTelephone(telephone, false);
        if (err != null) {
            return err;
        }
        err = validateEmailIfPresent(email);
        if (err != null) {
            return err;
        }
        return null;
    }

    public static String validateEquipmentAdminForm(String name, String desc, String qtyRaw, Integer salleId) {
        String err = validateEquipmentName(name);
        if (err != null) {
            return err;
        }
        err = validateOptionalDescription(desc, 2000, "La description");
        if (err != null) {
            return err;
        }
        err = validateQuantityInt(qtyRaw, 0, 999_999);
        if (err != null) {
            return err;
        }
        if (salleId == null || salleId <= 0) {
            return "Veuillez sélectionner une salle pour cet équipement.";
        }
        return null;
    }

    public static String validateSubscriptionOfferForm(String name, String durationRaw, String priceRaw,
                                                       String desc, Integer salleId) {
        String err = validateAdminDisplayName(name, "Le nom de l'offre", 2, 120);
        if (err != null) {
            return err;
        }
        err = validateDurationMonths(durationRaw, 1, 120);
        if (err != null) {
            return err;
        }
        err = validatePriceTnd(priceRaw);
        if (err != null) {
            return err;
        }
        err = validateOptionalDescription(desc, 2000, "La description");
        if (err != null) {
            return err;
        }
        if (salleId == null || salleId <= 0) {
            return "Veuillez sélectionner la salle concernée par cette offre.";
        }
        return null;
    }
}
