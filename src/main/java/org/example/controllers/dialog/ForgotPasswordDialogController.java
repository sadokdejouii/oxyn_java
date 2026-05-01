package org.example.controllers.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.dao.PasswordResetDAO;
import org.example.entities.User;
import org.example.notifications.PasswordResetEmailer;
import org.example.services.AuthValidation;
import org.example.services.UserService;
import org.example.utils.FormFieldFeedback;
import org.example.utils.PasswordStrengthEvaluator;
import org.example.utils.PasswordUtils;

import java.sql.SQLException;
import java.time.Duration;

public class ForgotPasswordDialogController {

    private static final boolean LOGIN_THEME = true;
    private static final Duration CODE_TTL = Duration.ofMinutes(10);

    @FXML
    private TextField emailField;
    @FXML
    private Label emailErrorLabel;

    @FXML
    private Button sendCodeButton;
    @FXML
    private Label sendStatusLabel;

    @FXML
    private TextField codeField;
    @FXML
    private Label codeErrorLabel;

    @FXML
    private PasswordField newPasswordField;
    @FXML
    private Label passwordStrengthLabel;
    @FXML
    private Label newPasswordErrorLabel;

    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label confirmPasswordErrorLabel;

    @FXML
    private Button confirmButton;

    private Stage stage;

    private final UserService userService = new UserService();
    private final PasswordResetDAO resetDAO = new PasswordResetDAO();

    public void setup(Stage stage, String emailPrefill) {
        this.stage = stage;
        if (emailField != null && emailPrefill != null) {
            emailField.setText(emailPrefill);
        }
    }

    @FXML
    private void initialize() {
        if (emailField != null) {
            emailField.textProperty().addListener((o, a, b) ->
                    FormFieldFeedback.clearInputError(emailField, emailErrorLabel, LOGIN_THEME));
        }
        if (codeField != null) {
            codeField.textProperty().addListener((o, a, b) ->
                    FormFieldFeedback.clearInputError(codeField, codeErrorLabel, LOGIN_THEME));
        }
        if (newPasswordField != null) {
            newPasswordField.textProperty().addListener((o, oldVal, newVal) -> {
                FormFieldFeedback.clearInputError(newPasswordField, newPasswordErrorLabel, LOGIN_THEME);
                if (passwordStrengthLabel != null) {
                    if (newVal == null || newVal.isEmpty()) {
                        passwordStrengthLabel.setText("");
                    } else {
                        PasswordStrengthEvaluator.Strength s = PasswordStrengthEvaluator.evaluate(newVal);
                        passwordStrengthLabel.setText("Force : " + PasswordStrengthEvaluator.label(s));
                        passwordStrengthLabel.setStyle(
                                "-fx-font-size: 11px; -fx-padding: 2 0 0 2; -fx-text-fill: "
                                        + PasswordStrengthEvaluator.color(s) + ";");
                    }
                }
            });
        }
        if (confirmPasswordField != null) {
            confirmPasswordField.textProperty().addListener((o, a, b) ->
                    FormFieldFeedback.clearInputError(confirmPasswordField, confirmPasswordErrorLabel, LOGIN_THEME));
        }
    }

    @FXML
    private void handleSendCode() {
        clearErrors();
        String emailRaw = emailField != null ? emailField.getText() : "";
        String email = emailRaw != null ? emailRaw.trim() : "";

        String emailErr = AuthValidation.validateEmailContent(emailRaw != null ? emailRaw : "");
        if (emailErr != null) {
            FormFieldFeedback.setInputError(emailField, emailErrorLabel, emailErr, LOGIN_THEME);
            return;
        }

        setBusy(true);
        if (sendStatusLabel != null) {
            sendStatusLabel.setText("Envoi en cours…");
        }

        // Anti-enumération: on affiche un message "générique" même si le compte n'existe pas.
        new Thread(() -> {
            try {
                User u = userService.findByEmail(email);
                if (u != null && u.isActive()) {
                    String code = resetDAO.createResetCode(email, CODE_TTL);
                    PasswordResetEmailer.sendCodeAsync(email, code, CODE_TTL);
                }
            } catch (Exception e) {
                System.err.println("[ForgotPassword] sendCode: " + e.getMessage());
            } finally {
                javafx.application.Platform.runLater(() -> {
                    setBusy(false);
                    if (sendStatusLabel != null) {
                        sendStatusLabel.setText("Si un compte existe, le code a été envoyé.");
                    }
                });
            }
        }, "forgotpwd-sendcode").start();
    }

    @FXML
    private void handleConfirm() {
        clearErrors();

        String emailRaw = emailField != null ? emailField.getText() : "";
        String email = emailRaw != null ? emailRaw.trim() : "";
        String code = codeField != null && codeField.getText() != null ? codeField.getText().trim() : "";
        String newPwd = newPasswordField != null && newPasswordField.getText() != null ? newPasswordField.getText() : "";
        String confirm = confirmPasswordField != null && confirmPasswordField.getText() != null ? confirmPasswordField.getText() : "";

        boolean ok = true;
        String emailErr = AuthValidation.validateEmailContent(emailRaw != null ? emailRaw : "");
        if (emailErr != null) {
            FormFieldFeedback.setInputError(emailField, emailErrorLabel, emailErr, LOGIN_THEME);
            ok = false;
        }
        if (code.isEmpty() || !code.matches("\\d{6}")) {
            FormFieldFeedback.setInputError(codeField, codeErrorLabel, "Saisissez un code à 6 chiffres.", LOGIN_THEME);
            ok = false;
        }
        if (newPwd.isEmpty()) {
            FormFieldFeedback.setInputError(newPasswordField, newPasswordErrorLabel, "Saisissez un nouveau mot de passe.", LOGIN_THEME);
            ok = false;
        }
        if (!ok) return;

        User u;
        try {
            u = userService.findByEmail(email);
        } catch (SQLException e) {
            FormFieldFeedback.setInputError(codeField, codeErrorLabel,
                    "Impossible de vérifier le code (base). Réessayez.", LOGIN_THEME);
            return;
        }
        if (u == null || !u.isActive()) {
            FormFieldFeedback.setInputError(codeField, codeErrorLabel,
                    "Code invalide ou expiré.", LOGIN_THEME);
            return;
        }

        String pwdErr = AuthValidation.validatePasswordCreate(newPwd, u.getNom() != null ? u.getNom() : "", u.getPrenom() != null ? u.getPrenom() : "");
        if (pwdErr != null) {
            FormFieldFeedback.setInputError(newPasswordField, newPasswordErrorLabel, pwdErr, LOGIN_THEME);
            return;
        }
        String cErr = AuthValidation.validateConfirmPassword(newPwd, confirm);
        if (cErr != null) {
            FormFieldFeedback.setInputError(confirmPasswordField, confirmPasswordErrorLabel, cErr, LOGIN_THEME);
            return;
        }

        setBusy(true);
        if (sendStatusLabel != null) {
            sendStatusLabel.setText("Réinitialisation en cours…");
        }

        new Thread(() -> {
            boolean valid = false;
            try {
                valid = resetDAO.consumeCodeIfValid(email, code);
                if (valid) {
                    String hash = PasswordUtils.hash(newPwd);
                    userService.updatePasswordHashByEmail(email, hash);
                }
            } catch (Exception e) {
                System.err.println("[ForgotPassword] confirm: " + e.getMessage());
            }
            final boolean okFinal = valid;
            javafx.application.Platform.runLater(() -> {
                setBusy(false);
                if (!okFinal) {
                    FormFieldFeedback.setInputError(codeField, codeErrorLabel, "Code invalide ou expiré.", LOGIN_THEME);
                    if (sendStatusLabel != null) {
                        sendStatusLabel.setText("");
                    }
                    return;
                }
                if (sendStatusLabel != null) {
                    sendStatusLabel.setText("Mot de passe réinitialisé.");
                }
                if (stage != null) {
                    stage.close();
                }
            });
        }, "forgotpwd-confirm").start();
    }

    @FXML
    private void handleClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private void clearErrors() {
        FormFieldFeedback.clearInputError(emailField, emailErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(codeField, codeErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(newPasswordField, newPasswordErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(confirmPasswordField, confirmPasswordErrorLabel, LOGIN_THEME);
    }

    private void setBusy(boolean busy) {
        if (sendCodeButton != null) sendCodeButton.setDisable(busy);
        if (confirmButton != null) confirmButton.setDisable(busy);
        if (emailField != null) emailField.setDisable(busy);
        if (codeField != null) codeField.setDisable(busy);
        if (newPasswordField != null) newPasswordField.setDisable(busy);
        if (confirmPasswordField != null) confirmPasswordField.setDisable(busy);
    }
}

