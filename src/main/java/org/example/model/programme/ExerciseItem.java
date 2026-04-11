package org.example.model.programme;

/**
 * Exercice dans le JSON {@code exercices_hebdomadaires} (clé jour → liste).
 */
public record ExerciseItem(
        String nom,
        String duree,
        String repetitions,
        String intensite,
        int calories
) {
    public static ExerciseItem withDuree(String nom, String duree, String intensite, int calories) {
        return new ExerciseItem(nom, duree, null, intensite, calories);
    }

    public static ExerciseItem withRepetitions(String nom, String repetitions, String intensite, int calories) {
        return new ExerciseItem(nom, null, repetitions, intensite, calories);
    }
}
