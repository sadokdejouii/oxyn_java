package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Vue « module non géré » (sidebar conservée, pas de logique métier).
 */
public class PlaceholderPageController implements Initializable {

    @FXML
    private Label placeholderTitle;

    @FXML
    private Label placeholderMessage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String module = PlaceholderContext.consumeModuleLabel();
        if (module == null || module.isBlank()) {
            module = "Ce module";
        }
        placeholderTitle.setText(module);
        placeholderMessage.setText(
                "Cette section n’est pas disponible dans l’application desktop OXYN. Utilisez le site web pour la gestion associée.");
    }
}
