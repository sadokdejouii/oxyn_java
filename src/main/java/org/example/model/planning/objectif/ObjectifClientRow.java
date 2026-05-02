package org.example.model.planning.objectif;

import java.time.LocalDateTime;

/**
 * Ligne objectif libre (stockée dans objectifs_hebdomadaires) — saisie, réponse IA et intervention encadrant.
 */
public record ObjectifClientRow(
        int id,
        int userId,
        String texteObjectif,
        String reponseIa,
        String motsCles,
        String idsProduitsRecommandes,
        LocalDateTime dateEnregistrement,
        String interventionEncadrant
) {
}
