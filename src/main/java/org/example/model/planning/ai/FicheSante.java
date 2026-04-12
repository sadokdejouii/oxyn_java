package org.example.model.planning.ai;

/**
 * Vue minimale de la fiche santé pour la génération de conseils (hors persistance JDBC).
 */
public record FicheSante(
        String genre,
        Integer age,
        Integer tailleCm,
        Double poidsKg,
        String objectif,
        String niveauActivite
) {
}
