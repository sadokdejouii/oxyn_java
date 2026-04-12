package org.example.model.planning;

/**
 * Données saisies pour création / édition d’une fiche santé (colonnes {@code fiche_sante}).
 */
public record FicheSanteFormData(
        String genre,
        Integer age,
        Integer tailleCm,
        Double poidsKg,
        String objectif,
        String niveauActivite
) {
    public static FicheSanteFormData empty() {
        return new FicheSanteFormData("M", 30, 170, 70.0, "perte_poids", "peu_actif");
    }
}
