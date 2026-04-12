package org.example.model.planning.admin;

/**
 * Indicateurs agrégés pour le tableau de bord admin (module Planning uniquement).
 */
public record AdminPlanningModuleStats(
        int fichesSante,
        int programmesGeneres,
        int tachesQuotidiennesTotal,
        int conversationsSuivi,
        int messagesSuivi
) {
}
