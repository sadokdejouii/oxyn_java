package org.example.utils;

import javafx.stage.Stage;

/**
 * Fenêtre principale : vrai plein écran sur tout le flux (connexion, inscription, application).
 * Sortie du plein écran : touche Échap (comportement JavaFX standard).
 */
public final class PrimaryStageLayout {

    private PrimaryStageLayout() {
    }

    /**
     * Plein écran exclusif (sans barre de titre). À rappeler après chaque {@link Stage#setScene}
     * sur cette fenêtre, car certains changements de scène peuvent réinitialiser l’état.
     */
    public static void applyFullScreen(Stage stage) {
        if (stage == null) {
            return;
        }
        stage.setMaximized(false);
        stage.setFullScreen(true);
    }
}
