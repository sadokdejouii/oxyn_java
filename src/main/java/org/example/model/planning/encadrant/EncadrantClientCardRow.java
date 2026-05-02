package org.example.model.planning.encadrant;

/**
 * Résumé affiché sur une carte client (vue encadrant — liste).
 */
public record EncadrantClientCardRow(
        int userId,
        String displayName,
        String email,
        String objectif,
        String niveauActivite,
        int tasksTotalWeek,
        int tasksDoneWeek,
        /** Dernier objectif libre saisi (Planning — assistant boutique), extrait court. */
        String objectifLibreExcerpt,
        /** Réponse IA associée (extrait). */
        String reponseIaLibreExcerpt
) {
    public double progressPct() {
        if (tasksTotalWeek <= 0) {
            return -1;
        }
        return Math.min(100, Math.max(0, (tasksDoneWeek * 100.0) / tasksTotalWeek));
    }

    public String statusBadge() {
        if (tasksTotalWeek == 0) {
            return "Semaine vide";
        }
        double p = progressPct();
        if (p >= 99) {
            return "Actif";
        }
        if (p >= 40) {
            return "En cours";
        }
        return "À relancer";
    }
}
