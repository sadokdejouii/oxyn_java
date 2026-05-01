package org.example.model.planning.task;

import java.time.LocalDate;

/**
 * Tâche quotidienne (table {@code taches_quotidiennes}).
 */
public record TacheQuotidienne(
        int id,
        int userId,
        LocalDate date,
        String jourSemaine,
        String description,
        TacheEtat etat
) {
}
