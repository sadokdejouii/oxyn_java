package org.example.model.planning.task;

import java.util.Locale;

/**
 * État d’une ligne {@code taches_quotidiennes} (aligné badge UI).
 */
public enum TacheEtat {
    FAIT,
    EN_COURS,
    NON_FAIT;

    public static TacheEtat fromDb(String raw) {
        if (raw == null || raw.isBlank()) {
            return NON_FAIT;
        }
        String n = raw.trim().toUpperCase(Locale.ROOT);
        return switch (n) {
            case "FAIT", "DONE", "TERMINE", "TERMINÉ" -> FAIT;
            case "EN_COURS", "ENCOURS", "IN_PROGRESS", "COURS" -> EN_COURS;
            default -> NON_FAIT;
        };
    }

    public String toDb() {
        return name();
    }
}
