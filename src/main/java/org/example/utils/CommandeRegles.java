package org.example.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Règles métier commandes (testables sans base de données).
 */
public final class CommandeRegles {

    private CommandeRegles() {
    }

    public static boolean estStatutAnnule(String statut) {
        if (statut == null) {
            return false;
        }
        return statut.toLowerCase().contains("annul");
    }

    /**
     * @return true si la date de création est au moins {@code jours} jours avant {@code aujourdhui} (calendrier).
     */
    public static boolean ageEnJoursAuMoins(LocalDateTime dateCreation, LocalDate aujourdhui, int jours) {
        if (dateCreation == null || aujourdhui == null || jours < 0) {
            return false;
        }
        return ChronoUnit.DAYS.between(dateCreation.toLocalDate(), aujourdhui) >= jours;
    }
}
