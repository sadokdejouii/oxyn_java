package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.entities.User;
import org.example.services.AuthService;
import org.example.services.SessionContext;
import org.example.utils.AuthNavigation;

import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField != null ? emailField.getText().trim() : "";
        String password = passwordField != null ? passwordField.getText() : "";

        if (email.isEmpty() || password.isEmpty()) {
            showError("Champs requis", "Saisissez votre e-mail et votre mot de passe.");
            return;
        }

        try {
            User user = authService.login(email, password);
            if (user == null) {
                showError("Connexion refusée",
                        "E-mail ou mot de passe incorrect, ou compte désactivé.");
                return;
            }
            SessionContext.getInstance().login(user);

            Node source = emailField != null ? emailField : passwordField;
            Stage stage = (Stage) source.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/MainLayout.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1530, 880);
            stage.setScene(scene);
            stage.setTitle("OXYN — " + SessionContext.getInstance().getRole().displayLabel());
            stage.show();
        } catch (SQLException e) {
            showError("Erreur base de données",
                    e.getMessage() != null ? e.getMessage() : e.toString());
        } catch (Exception e) {
            showError("Erreur", e.getMessage() != null ? e.getMessage() : e.toString());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoRegister(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            AuthNavigation.showRegister(stage);
        } catch (Exception e) {
            showError("Navigation", e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private static void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
