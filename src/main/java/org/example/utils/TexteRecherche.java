package org.example.utils;

import java.util.Locale;

/**
 * Filtre texte insensible à la casse pour listes (produits, commandes).
 */
public final class TexteRecherche {

    private TexteRecherche() {
    }

    /** Si {@code filtre} est vide, tout passe ; sinon cherche dans {@code texte} (null-safe). */
    public static boolean correspond(String texte, String filtre) {
        if (filtre == null || filtre.isBlank()) {
            return true;
        }
        if (texte == null) {
            return false;
        }
        String n = filtre.trim().toLowerCase(Locale.ROOT);
        return texte.toLowerCase(Locale.ROOT).contains(n);
    }
}
