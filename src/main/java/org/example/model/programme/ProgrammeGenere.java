package org.example.model.programme;

/**
 * Programme personnalisé (domaine) avant/après persistance.
 * {@code id == 0} si pas encore enregistré.
 */
public record ProgrammeGenere(
        long id,
        int userId,
        Integer caloriesParJour,
        String objectifPrincipal,
        WeeklyExercisePlan exercicesHebdomadaires,
        PlansRepas plansRepas,
        String conseilsGeneraux
) {
    public ProgrammeGenere withoutId() {
        return new ProgrammeGenere(0, userId, caloriesParJour, objectifPrincipal,
                exercicesHebdomadaires, plansRepas, conseilsGeneraux);
    }

    public ProgrammeGenere withId(long newId) {
        return new ProgrammeGenere(newId, userId, caloriesParJour, objectifPrincipal,
                exercicesHebdomadaires, plansRepas, conseilsGeneraux);
    }
}
