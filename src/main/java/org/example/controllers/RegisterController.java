package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.services.AuthService;
import org.example.services.AuthValidation;
import org.example.services.UserService;
import org.example.utils.AuthNavigation;
import org.example.utils.FormFieldFeedback;
import org.example.utils.UserDialogHelper;

import java.sql.SQLException;

/**
 * Inscription client (même flux que {@code main}).
 */
public class RegisterController {

    @FXML
    private TextField nomField;

    @FXML
    private Label nomErrorLabel;

    @FXML
    private TextField prenomField;

    @FXML
    private Label prenomErrorLabel;

    @FXML
    private TextField emailField;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private TextField telephoneField;

    @FXML
    private Label telephoneErrorLabel;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label passwordErrorLabel;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label confirmPasswordErrorLabel;

    private final AuthService authService = new AuthService();
    private final UserService userService = new UserService();

    private static final boolean LOGIN_THEME = true;

    @FXML
    private void initialize() {
        nomField.textProperty().addListener((o, a, b) -> FormFieldFeedback.clearInputError(nomField, nomErrorLabel, LOGIN_THEME));
        prenomField.textProperty().addListener((o, a, b) -> FormFieldFeedback.clearInputError(prenomField, prenomErrorLabel, LOGIN_THEME));
        emailField.textProperty().addListener((o, a, b) -> FormFieldFeedback.clearInputError(emailField, emailErrorLabel, LOGIN_THEME));
        telephoneField.textProperty().addListener((o, a, b) -> FormFieldFeedback.clearInputError(telephoneField, telephoneErrorLabel, LOGIN_THEME));
        passwordField.textProperty().addListener((o, a, b) -> FormFieldFeedback.clearInputError(passwordField, passwordErrorLabel, LOGIN_THEME));
        confirmPasswordField.textProperty().addListener((o, a, b) -> FormFieldFeedback.clearInputError(confirmPasswordField, confirmPasswordErrorLabel, LOGIN_THEME));
    }

    @FXML
    private void handleRegister() {
        clearAllFieldErrors();
        String nom = text(nomField);
        String prenom = text(prenomField);
        String email = text(emailField);
        String tel = text(telephoneField);
        String pwd = passwordField.getText() != null ? passwordField.getText() : "";
        String confirm = confirmPasswordField.getText() != null ? confirmPasswordField.getText() : "";

        boolean ok = true;

        String nomErr = AuthValidation.validatePersonName(nomField.getText() != null ? nomField.getText() : "", false);
        if (nomErr != null) {
            FormFieldFeedback.setInputError(nomField, nomErrorLabel, nomErr, LOGIN_THEME);
            ok = false;
        }

        String prenomErr = AuthValidation.validatePersonName(prenomField.getText() != null ? prenomField.getText() : "", true);
        if (prenomErr != null) {
            FormFieldFeedback.setInputError(prenomField, prenomErrorLabel, prenomErr, LOGIN_THEME);
            ok = false;
        }

        String emailErr = AuthValidation.validateEmailContent(emailField.getText() != null ? emailField.getText() : "");
        if (emailErr != null) {
            FormFieldFeedback.setInputError(emailField, emailErrorLabel, emailErr, LOGIN_THEME);
            ok = false;
        }

        if (tel.isEmpty()) {
            FormFieldFeedback.setInputError(telephoneField, telephoneErrorLabel, "Le téléphone est obligatoire.", LOGIN_THEME);
            ok = false;
        }

        String pwdErr = AuthValidation.validatePasswordCreate(pwd, nom, prenom);
        if (pwdErr != null) {
            FormFieldFeedback.setInputError(passwordField, passwordErrorLabel, pwdErr, LOGIN_THEME);
            ok = false;
        }

        String cErr = AuthValidation.validateConfirmPassword(pwd, confirm);
        if (cErr != null) {
            FormFieldFeedback.setInputError(confirmPasswordField, confirmPasswordErrorLabel, cErr, LOGIN_THEME);
            ok = false;
        }

        if (!ok) {
            return;
        }

        String em = email.trim().toLowerCase();
        Stage owner = dialogOwner();
        try {
            if (userService.findByEmail(em) != null) {
                FormFieldFeedback.setInputError(emailField, emailErrorLabel, "Cette adresse e-mail est déjà utilisée.", LOGIN_THEME);
                return;
            }
        } catch (SQLException e) {
            UserDialogHelper.showMessage(owner, "Inscription",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
            return;
        }

        try {
            boolean inserted = authService.registerClient(nom, prenom, email, tel, pwd, confirm);
            if (!inserted) {
                FormFieldFeedback.setInputError(emailField, emailErrorLabel, "Cette adresse e-mail est déjà utilisée.", LOGIN_THEME);
                return;
            }
            UserDialogHelper.showMessage(owner, "Inscription", "Compte créé. Vous pouvez vous connecter.", false);
            navigateToLogin();
        } catch (IllegalArgumentException e) {
            UserDialogHelper.showMessage(owner, "Inscription", e.getMessage(), false);
        } catch (SQLException e) {
            UserDialogHelper.showMessage(owner, "Erreur base de données",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        }
    }

    @FXML
    private void handleBackToLogin() {
        navigateToLogin();
    }

    private void navigateToLogin() {
        try {
            Stage stage = (Stage) emailField.getScene().getWindow();
            AuthNavigation.showLogin(stage);
        } catch (Exception e) {
            UserDialogHelper.showMessage(dialogOwner(), "Navigation",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        }
    }

    private Stage dialogOwner() {
        if (emailField == null || emailField.getScene() == null) {
            return null;
        }
        return (Stage) emailField.getScene().getWindow();
    }

    private void clearAllFieldErrors() {
        FormFieldFeedback.clearInputError(nomField, nomErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(prenomField, prenomErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(emailField, emailErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(telephoneField, telephoneErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(passwordField, passwordErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(confirmPasswordField, confirmPasswordErrorLabel, LOGIN_THEME);
    }

    private static String text(TextField f) {
        return f != null && f.getText() != null ? f.getText().trim() : "";
    }
}
