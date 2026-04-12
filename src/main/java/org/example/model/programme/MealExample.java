package org.example.model.programme;

import java.util.List;

/**
 * Exemple de repas dans {@code plans_repas} (structure Symfony).
 */
public record MealExample(
        String nom,
        int calories,
        List<String> ingredients
) {
    public MealExample(String nom, int calories) {
        this(nom, calories, List.of());
    }
}
