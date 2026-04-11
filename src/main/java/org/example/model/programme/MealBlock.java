package org.example.model.programme;

import java.util.List;

/**
 * Bloc repas (cible kcal + exemples) pour petit-déj., déjeuner, collation ou dîner.
 */
public record MealBlock(
        int calories,
        List<MealExample> exemples
) {
}
