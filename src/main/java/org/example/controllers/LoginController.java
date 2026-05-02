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
import javafx.concurrent.Task;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.entities.User;
import org.example.entities.PanierSession;
import org.example.notifications.FailedLoginAlertEmailer;
import org.example.services.AuthService;
import org.example.services.AuthValidation;
import org.example.services.SessionContext;
import org.example.totp.Totp;
import org.example.totp.TotpDAO;
import org.example.utils.AppStyles;
import org.example.utils.AuthNavigation;
import org.example.utils.FormFieldFeedback;
import org.example.utils.PrimaryStageLayout;
import org.example.utils.UserDialogHelper;

import java.net.URL;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Connexion standard (e-mail + mot de passe), alignée sur le flux {@code main} / DAO.
 */
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

    private AuthService authService;

    private int failedAttempts;

    private String lastFailedEmail = "";

    private static final int MAX_ATTEMPTS_BEFORE_ALERT = 3;

    private AuthService getAuthService() {
        if (authService == null) {
            authService = new AuthService();
        }
        return authService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("🔧 LoginController.initialize() - Début");
        
        // Temporairement désactivé pour tester
        /*
        if (emailField != null) {
            emailField.textProperty().addListener((o, a, b) ->
                    FormFieldFeedback.clearInputError(emailField, emailErrorLabel, LOGIN_THEME));
        }
        if (passwordField != null) {
            passwordField.textProperty().addListener((o, oldVal, newVal) -> {
                FormFieldFeedback.clearInputError(passwordField, passwordErrorLabel, LOGIN_THEME);

                // Indicateur de force
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
        */
        
        System.out.println("✅ LoginController.initialize() - Fin");
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
            User user = getAuthService().login(email, password);
            if (user == null) {
                // Incrémenter le compteur d'échecs pour cet e-mail
                if (email.equals(lastFailedEmail)) {
                    failedAttempts++;
                } else {
                    failedAttempts = 1;
                    lastFailedEmail = email;
                }

                FormFieldFeedback.setInputError(passwordField, passwordErrorLabel,
                        "E-mail ou mot de passe incorrect, ou compte désactivé. "
                                + "(" + failedAttempts + "/" + MAX_ATTEMPTS_BEFORE_ALERT + ")", LOGIN_THEME);

                // Au 3e échec : capturer une photo et envoyer l'alerte
                if (failedAttempts >= MAX_ATTEMPTS_BEFORE_ALERT) {
                    failedAttempts = 0; // reset pour ne pas re-déclencher à chaque tentative suivante
                    final String alertEmail = email;
                    Task<byte[]> captureTask = new Task<>() {
                        @Override
                        protected byte[] call() {
                            try {
                                // Capture webcam — timeout 8 secondes
                                var face96 = org.example.facerec.FaceCaptureService
                                        .captureSingleFace96x96Bgr(0, java.time.Duration.ofSeconds(8));
                                if (face96 == null || face96.empty()) return null;

                                // Encoder le Mat OpenCV en JPEG
                                org.bytedeco.javacpp.BytePointer buf = new org.bytedeco.javacpp.BytePointer();
                                org.bytedeco.opencv.global.opencv_imgcodecs.imencode(".jpg", face96, buf);
                                byte[] jpegBytes = new byte[(int) buf.limit()];
                                buf.get(jpegBytes);
                                buf.close();
                                return jpegBytes;
                            } catch (Exception e) {
                                System.err.println("[LoginAlert] Capture webcam échouée: " + e.getMessage());
                                return null; // Envoie quand même l'e-mail sans photo
                            }
                        }
                    };
                    captureTask.setOnSucceeded(ev -> {
                        byte[] photo = captureTask.getValue();
                        FailedLoginAlertEmailer.sendAlertAsync(alertEmail, photo);
                    });
                    captureTask.setOnFailed(ev -> {
                        // Envoie l'alerte sans photo si la webcam plante
                        FailedLoginAlertEmailer.sendAlertAsync(alertEmail, null);
                    });
                    new Thread(captureTask, "login-alert-thread").start();
                }
                return;
            }

            // Connexion réussie : réinitialiser le compteur
            failedAttempts = 0;
            lastFailedEmail = "";

            if (!passTotpIfEnabled(user)) {
                return;
            }
            SessionContext.getInstance().login(user);
            openMain(event);
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

    private void openMain(ActionEvent event) {
        try {
            Node source = emailField != null ? emailField : passwordField;
            if (source == null || source.getScene() == null) {
                source = (Node) event.getSource();
            }
            Stage stage = (Stage) source.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/MainLayout.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1530, 880);
            AppStyles.apply(scene);
            SessionContext ctx = SessionContext.getInstance();
            stage.setScene(scene);
            stage.setTitle("OXYN — " + ctx.getRole().displayLabel());
            stage.setMaximized(true); // ✅ remplace applyFullScreen
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur",
                    "Impossible d’ouvrir l’application : " + e.getMessage());
        }
    }

    private static void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private boolean passTotpIfEnabled(User user) {
        try {
            TotpDAO dao = new TotpDAO();
            Optional<TotpDAO.Record> rec = dao.getByUserId(user.getId());
            if (rec.isEmpty() || !rec.get().enabled()) {
                return true;
            }
            Stage owner = emailField != null && emailField.getScene() != null
                    ? (Stage) emailField.getScene().getWindow()
                    : null;
            if (owner == null) {
                return false;
            }
            Optional<String> code = UserDialogHelper.showTotpCodeDialog(
                    owner,
                    "Verification 2FA",
                    "Entrez le code a six chiffres de votre application d'authentification.",
                    null,
                    "");
            if (code.isEmpty()) {
                return false;
            }
            return Totp.verifyCode(rec.get().secretBase32(), code.get(), System.currentTimeMillis());
        } catch (Exception e) {
            System.err.println("[Login] TOTP: " + e.getMessage());
            return true;
        }
    }
}
