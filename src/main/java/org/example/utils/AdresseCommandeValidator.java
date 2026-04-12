package org.example.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Contrôle de saisie : adresse au format {@code quartier, ville, pays}
 * (ex. {@code cité Ibn Khaldoun, Tunis, Tunisie}).
 */
public final class AdresseCommandeValidator {

    public static final int LONGUEUR_MAX = 255;
    /** Longueur minimale pour chaque segment (quartier, ville, pays). */
    public static final int SEGMENT_MIN = 2;

    private AdresseCommandeValidator() {
    }

    /**
     * @return {@code null} si l’adresse est acceptée, sinon un message d’erreur.
     */
    public static String valider(String raw) {
        if (raw == null) {
            return "L’adresse est obligatoire.";
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return "L’adresse est obligatoire.";
        }
        if (s.length() > LONGUEUR_MAX) {
            return String.format("L’adresse ne doit pas dépasser %d caractères.", LONGUEUR_MAX);
        }
        if (s.contains("<") || s.contains(">")) {
            return "Les caractères < et > ne sont pas autorisés.";
        }

        List<String> segments = decouperParVirgules(s);
        if (segments.size() != 3) {
            return "L’adresse doit comporter exactement trois parties séparées par des virgules : quartier, ville, pays.\n"
                    + "Exemple : cité Ibn Khaldoun, Tunis, Tunisie";
        }

        for (int i = 0; i < 3; i++) {
            String part = segments.get(i);
            if (part.length() < SEGMENT_MIN) {
                String libelle = i == 0 ? "quartier" : (i == 1 ? "ville" : "pays");
                return "Le " + libelle + " est trop court (au moins " + SEGMENT_MIN + " caractères).";
            }
            if (part.chars().noneMatch(Character::isLetter)) {
                String libelle = i == 0 ? "quartier" : (i == 1 ? "ville" : "pays");
                return "Le " + libelle + " doit contenir au moins une lettre.";
            }
        }

        return null;
    }

    /**
     * Reformate l’adresse validée en {@code quartier, ville, pays} avec espaces après les virgules.
     */
    public static String formaterPourEnregistrement(String raw) {
        List<String> segments = decouperParVirgules(raw.trim());
        return segments.get(0) + ", " + segments.get(1) + ", " + segments.get(2);
    }

    private static List<String> decouperParVirgules(String s) {
        String[] brut = s.split(",", -1);
        List<String> out = new ArrayList<>(3);
        for (String p : brut) {
            String t = p.trim().replaceAll("\\s+", " ");
            out.add(t);
        }
        return out;
    }
}
