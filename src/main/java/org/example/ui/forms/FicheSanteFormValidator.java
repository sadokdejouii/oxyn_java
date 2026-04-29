package org.example.planning.form;

import org.example.model.planning.FicheSanteFormData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validation stricte des champs {@code fiche_sante} (logique métier, sans JavaFX).
 */
public final class FicheSanteFormValidator {

    private static final Set<String> GENRES = Set.of("M", "F");
    private static final Set<String> OBJECTIFS = Set.of("perte_poids", "gain_poids", "devenir_muscle", "maintien");
    private static final Set<String> NIVEAUX = Set.of("sedentaire", "peu_actif", "moderement_actif", "tres_actif");

    /** 7 &lt; âge &lt; 100 → entiers 8 … 99 (même bornes que la validation champ UI). */
    public static final int AGE_MIN = 8;
    public static final int AGE_MAX = 99;
    /** 80 &lt; taille &lt; 210 (cm) → 81 … 209 */
    public static final int TAILLE_MIN_CM = 81;
    public static final int TAILLE_MAX_CM = 209;
    public static final double POIDS_MAX_KG = 220.0;
    /** Poids strictement supérieur à cette valeur (kg). */
    public static final double POIDS_MIN_STRICTLY_ABOVE_KG = 30.0;

    public record Result(boolean ok, List<String> errors, FicheSanteFormData data) {
        public static Result success(FicheSanteFormData d) {
            return new Result(true, List.of(), d);
        }

        public static Result failure(List<String> errors) {
            return new Result(false, List.copyOf(errors), null);
        }
    }

    private FicheSanteFormValidator() {
    }

    public static Result validate(FicheSanteFormData draft) {
        List<String> err = new ArrayList<>();

        if (draft.genre() == null || draft.genre().isBlank()) {
            err.add("Le genre est obligatoire.");
        } else if (!GENRES.contains(draft.genre())) {
            err.add("Genre invalide (attendu : M ou F).");
        }

        if (draft.age() == null) {
            err.add("L’âge est obligatoire.");
        } else if (draft.age() < AGE_MIN || draft.age() > AGE_MAX) {
            err.add("L’âge doit être strictement entre 7 et 100 ans (saisie autorisée : " + AGE_MIN + " à " + AGE_MAX + " ans).");
        }

        if (draft.tailleCm() == null) {
            err.add("La taille est obligatoire.");
        } else if (draft.tailleCm() < TAILLE_MIN_CM || draft.tailleCm() > TAILLE_MAX_CM) {
            err.add("La taille doit être strictement entre 80 et 210 cm (saisie autorisée : " + TAILLE_MIN_CM + " à " + TAILLE_MAX_CM + " cm).");
        }

        if (draft.poidsKg() == null) {
            err.add("Le poids est obligatoire.");
        } else if (draft.poidsKg() <= POIDS_MIN_STRICTLY_ABOVE_KG || draft.poidsKg() > POIDS_MAX_KG) {
            err.add("Le poids doit être strictement supérieur à 30 kg et au plus égal à " + (int) POIDS_MAX_KG + " kg.");
        }

        if (draft.objectif() == null || draft.objectif().isBlank()) {
            err.add("L’objectif est obligatoire.");
        } else if (!OBJECTIFS.contains(draft.objectif())) {
            err.add("Objectif non reconnu.");
        }

        if (draft.niveauActivite() == null || draft.niveauActivite().isBlank()) {
            err.add("Le niveau d’activité est obligatoire.");
        } else if (!NIVEAUX.contains(draft.niveauActivite())) {
            err.add("Niveau d’activité non reconnu.");
        }

        if (!err.isEmpty()) {
            return Result.failure(err);
        }
        return Result.success(draft);
    }
}
