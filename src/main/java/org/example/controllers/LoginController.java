package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.entities.AuthUser;
import org.example.services.DemoLoginService;
import org.example.services.SessionContext;

import java.sql.SQLException;

/**
 * Connexion démo temporaire : e-mail existant en base, sans mot de passe.
 */
public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private Label demoHintLabel;

    private final DemoLoginService demoLogin = new DemoLoginService();

    @FXML
    public void initialize() {
        if (demoHintLabel != null) {
            demoHintLabel.setText("Démo : saisissez l’e-mail d’un compte présent dans la table users (aucun mot de passe).");
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String raw = emailField != null ? emailField.getText() : "";
        if (raw == null || raw.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "E-mail requis", "Veuillez saisir une adresse e-mail.");
            return;
        }
        try {
            var opt = demoLogin.loginByEmail(raw);
            if (opt.isEmpty()) {
                showUnknownEmailDialog(raw.trim());
                return;
            }
            AuthUser user = opt.get();
            SessionContext ctx = SessionContext.getInstance();
            ctx.login(user);
            ctx.setDemoAuthentication(true);
            openMain(event);
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Base de données",
                    ex.getMessage() != null ? ex.getMessage() : ex.toString());
        }
    }

    private static void showUnknownEmailDialog(String entered) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Compte introuvable");
        a.setHeaderText("Connexion démo");
        a.setContentText("« " + entered + " » ne correspond à aucun utilisateur actif dans la table users.");
        a.getDialogPane().setStyle("-fx-min-width: 360px;");
        a.showAndWait();
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void openMain(ActionEvent event) {
        try {
            Node source = emailField;
            Stage stage = (Stage) source.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/MainLayout.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1530, 880);
            SessionContext ctx = SessionContext.getInstance();
            stage.setScene(scene);
            stage.setTitle("OXYN — " + ctx.getRole().displayLabel());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d’ouvrir l’application : " + e.getMessage());
        }
    }
}
