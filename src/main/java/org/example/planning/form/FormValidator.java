package org.example.planning.form;

import javafx.scene.control.Spinner;

import java.util.Optional;

/**
 * Validation visuelle des métriques fiche santé (âge, taille, poids), alignée sur les bornes
 * {@link FicheSanteFormValidator} sans dupliquer la logique métier des autres champs.
 */
public final class FormValidator {

    private FormValidator() {
    }

    /**
     * @return vide si valide, sinon message d’erreur à afficher sous le champ
     */
    public static Optional<String> validateAge(Spinner<Integer> spinner) {
        String raw = spinner.getEditor().getText();
        if (raw == null || raw.trim().isEmpty()) {
            return Optional.of("L'âge est requis");
        }
        String t = raw.trim().replace(',', '.');
        int parsed;
        try {
            parsed = Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return Optional.of("Veuillez entrer un âge valide");
        }
        if (parsed < FicheSanteFormValidator.AGE_MIN || parsed > FicheSanteFormValidator.AGE_MAX) {
            return Optional.of(String.format(
                    "L'âge doit être entre %d et %d ans",
                    FicheSanteFormValidator.AGE_MIN,
                    FicheSanteFormValidator.AGE_MAX));
        }
        return Optional.empty();
    }

    public static Optional<String> validateHeight(Spinner<Integer> spinner) {
        String raw = spinner.getEditor().getText();
        if (raw == null || raw.trim().isEmpty()) {
            return Optional.of("La taille est requise");
        }
        String t = raw.trim().replace(',', '.');
        int parsed;
        try {
            parsed = Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return Optional.of("Veuillez entrer une taille valide");
        }
        if (parsed < FicheSanteFormValidator.TAILLE_MIN_CM || parsed > FicheSanteFormValidator.TAILLE_MAX_CM) {
            return Optional.of(String.format(
                    "La taille doit être entre %d et %d cm",
                    FicheSanteFormValidator.TAILLE_MIN_CM,
                    FicheSanteFormValidator.TAILLE_MAX_CM));
        }
        return Optional.empty();
    }

    public static Optional<String> validateWeight(Spinner<Double> spinner) {
        String raw = spinner.getEditor().getText();
        if (raw == null || raw.trim().isEmpty()) {
            return Optional.of("Le poids est requis");
        }
        String t = raw.trim().replace(',', '.');
        double parsed;
        try {
            parsed = Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return Optional.of("Veuillez entrer un poids valide");
        }
        if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
            return Optional.of("Veuillez entrer un poids valide");
        }
        if (parsed <= FicheSanteFormValidator.POIDS_MIN_STRICTLY_ABOVE_KG
                || parsed > FicheSanteFormValidator.POIDS_MAX_KG) {
            return Optional.of(String.format(
                    "Le poids doit être supérieur à %.0f kg et inférieur ou égal à %.0f kg.",
                    FicheSanteFormValidator.POIDS_MIN_STRICTLY_ABOVE_KG,
                    FicheSanteFormValidator.POIDS_MAX_KG));
        }
        return Optional.empty();
    }
}
