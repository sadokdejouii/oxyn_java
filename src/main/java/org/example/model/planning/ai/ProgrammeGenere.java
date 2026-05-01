package org.example.model.planning.ai;

/**
 * Résumé du programme généré pour l’assistant conseils (évite de coupler au domaine JSON/JDBC).
 */
public record ProgrammeGenere(
        Integer caloriesParJour,
        String objectifPrincipal,
        String conseilsGeneraux,
        int entrainementsParSemaine
) {
    public static ProgrammeGenere absent() {
        return new ProgrammeGenere(null, null, null, 0);
    }

    public boolean isPresent() {
        return caloriesParJour != null || (objectifPrincipal != null && !objectifPrincipal.isBlank());
    }
}
