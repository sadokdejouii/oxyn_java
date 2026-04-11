package org.example.model.programme;

import com.google.gson.annotations.SerializedName;

/**
 * Plan journalier structuré (clés JSON alignées base {@code plans_repas}).
 */
public record PlansRepas(
        @SerializedName("petit_dejeuner") MealBlock petitDejeuner,
        @SerializedName("dejeuner") MealBlock dejeuner,
        @SerializedName("collation") MealBlock collation,
        @SerializedName("diner") MealBlock diner
) {
}
