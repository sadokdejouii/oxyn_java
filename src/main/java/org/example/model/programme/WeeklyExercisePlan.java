package org.example.model.programme;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Planning hebdomadaire : jours en français → liste d’exercices.
 */
public record WeeklyExercisePlan(Map<String, List<ExerciseItem>> byDay) {

    private static final String[] JOURS = {
            "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"
    };

    public WeeklyExercisePlan {
        byDay = Map.copyOf(byDay);
    }

    public static WeeklyExercisePlan fromOrderedMap(LinkedHashMap<String, List<ExerciseItem>> ordered) {
        return new WeeklyExercisePlan(ordered);
    }

    public static String[] dayKeys() {
        return JOURS.clone();
    }
}
