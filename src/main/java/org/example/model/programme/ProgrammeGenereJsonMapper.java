package org.example.model.programme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Sérialisation JSON pour colonnes {@code exercices_hebdomadaires} et {@code plans_repas}.
 */
public final class ProgrammeGenereJsonMapper {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    private ProgrammeGenereJsonMapper() {
    }

    public static String toExercisesJson(WeeklyExercisePlan plan) {
        return GSON.toJson(plan.byDay());
    }

    public static String toPlansRepasJson(PlansRepas plans) {
        return GSON.toJson(plans);
    }
}
