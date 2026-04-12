package org.example.model.planning.ai;

/**
 * Synthèse de progression hebdomadaire (objectif semaine en base).
 */
public record WeeklyProgress(
        boolean defined,
        int weekNumber,
        int year,
        double tauxRealisationPct,
        int tachesPrevues,
        int tachesRealisees,
        String statut,
        String messageEncadrant,
        String messageIa
) {
    public static WeeklyProgress absent() {
        return new WeeklyProgress(false, 0, 0, 0, 0, 0, null, null, null);
    }
}
