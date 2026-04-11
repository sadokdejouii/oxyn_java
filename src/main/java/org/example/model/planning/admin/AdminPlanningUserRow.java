package org.example.model.planning.admin;

/**
 * Utilisateur avec fiche santé — vue admin Planning (carte + détail).
 */
public record AdminPlanningUserRow(
        int userId,
        String displayName,
        String email,
        String roleSummary,
        String genre,
        Integer age,
        Double poids,
        String objectif,
        String niveauActivite,
        int nbTaches,
        boolean programmeGenere
) {
}
