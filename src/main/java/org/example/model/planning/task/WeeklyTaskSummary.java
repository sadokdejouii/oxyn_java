package org.example.model.planning.task;

/**
 * Agrégat calculé sur les tâches de la semaine courante.
 */
public record WeeklyTaskSummary(
        int total,
        int fait,
        int enCours,
        int nonFait,
        /** Pourcentage de complétion (uniquement les tâches {@link TacheEtat#FAIT}). */
        double tauxCompletionPct
) {
    public static WeeklyTaskSummary empty() {
        return new WeeklyTaskSummary(0, 0, 0, 0, 0);
    }

    public static WeeklyTaskSummary from(java.util.List<TacheQuotidienne> taches) {
        if (taches == null || taches.isEmpty()) {
            return empty();
        }
        int f = 0;
        int e = 0;
        int n = 0;
        for (TacheQuotidienne t : taches) {
            switch (t.etat()) {
                case FAIT -> f++;
                case EN_COURS -> e++;
                case NON_FAIT -> n++;
            }
        }
        int tot = taches.size();
        double taux = tot == 0 ? 0 : (f * 100.0 / tot);
        return new WeeklyTaskSummary(tot, f, e, n, taux);
    }
}
