package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.entities.User;
import org.example.services.AuthService;
import org.example.services.AuthValidation;
import org.example.services.SessionContext;
import org.example.utils.AuthNavigation;
import org.example.utils.FormFieldFeedback;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    private static final boolean LOGIN_THEME = true;

    @FXML
    private TextField emailField;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label passwordErrorLabel;

    private final AuthService authService = new AuthService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (emailField != null) {
            emailField.textProperty().addListener((o, a, b) ->
                    FormFieldFeedback.clearInputError(emailField, emailErrorLabel, LOGIN_THEME));
        }
        if (passwordField != null) {
            passwordField.textProperty().addListener((o, a, b) ->
                    FormFieldFeedback.clearInputError(passwordField, passwordErrorLabel, LOGIN_THEME));
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        clearFieldErrors();

        String email = emailField != null ? emailField.getText().trim() : "";
        String password = passwordField != null ? passwordField.getText() : "";

        boolean ok = true;
        String emailErr = AuthValidation.validateEmailContent(emailField != null ? emailField.getText() : "");
        if (emailErr != null) {
            FormFieldFeedback.setInputError(emailField, emailErrorLabel, emailErr, LOGIN_THEME);
            ok = false;
        }
        if (password == null || password.isEmpty()) {
            FormFieldFeedback.setInputError(passwordField, passwordErrorLabel,
                    "Le mot de passe est obligatoire.", LOGIN_THEME);
            ok = false;
        }
        if (!ok) {
            return;
        }

        try {
            User user = authService.login(email, password);
            if (user == null) {
                FormFieldFeedback.setInputError(passwordField, passwordErrorLabel,
                        "E-mail ou mot de passe incorrect, ou compte désactivé.", LOGIN_THEME);
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

    private void clearFieldErrors() {
        FormFieldFeedback.clearInputError(emailField, emailErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(passwordField, passwordErrorLabel, LOGIN_THEME);
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
