package org.example.controllers;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.example.services.SessionContext;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Accueil client après connexion — page légère (stats détaillées dans les modules dédiés).
 */
public class ClientHomeController implements Initializable {

    @FXML
    private Label welcomeSubtitle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SessionContext ctx = SessionContext.getInstance();
        if (welcomeSubtitle != null) {
            welcomeSubtitle.textProperty().bind(Bindings.createStringBinding(() -> {
                String name = ctx.displayNameProperty().get();
                String greet = (name != null && !name.isBlank()) ? "Bonjour, " + name + ". " : "";
                return greet + "Espace client OXYN — événements, salles, planning et boutique.";
            }, ctx.displayNameProperty()));
        }
    }
}
