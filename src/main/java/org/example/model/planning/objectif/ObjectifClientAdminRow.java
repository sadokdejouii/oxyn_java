package org.example.model.planning.objectif;

import java.time.LocalDateTime;

/**
 * Vue admin — objectif libre avec identité utilisateur et libellés produits résolus.
 */
public record ObjectifClientAdminRow(
        int id,
        int userId,
        String userDisplayName,
        String userEmail,
        String texteObjectif,
        String reponseIa,
        String motsCles,
        String idsProduitsRecommandes,
        String produitsLibelles,
        LocalDateTime dateEnregistrement,
        String interventionEncadrant
) {
}
