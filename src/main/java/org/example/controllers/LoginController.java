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
import org.example.notifications.FailedLoginAlertEmailer;
import org.example.services.AuthService;
import org.example.services.AuthValidation;
import org.example.services.SessionContext;
import org.example.utils.AuthNavigation;
import org.example.utils.FormFieldFeedback;
import org.example.utils.PasswordStrengthEvaluator;
import org.example.utils.PrimaryStageLayout;
import org.example.facerec.FaceCaptureService;
import org.example.facerec.FaceEmbeddingModel;
import org.example.totp.Totp;
import org.example.totp.TotpDAO;
import org.example.utils.UserDialogHelper;
import org.example.notifications.LoginEmailNotifier;
import org.example.notifications.TemporalPermissionNotifier;
import javafx.concurrent.Task;
import org.example.digitalwill.DigitalWillService;

import java.net.URL;
import java.sql.SQLException;
import java.time.Duration;
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

    @FXML
    private Label passwordStrengthLabel;

    /** Compteur d'échecs par e-mail (réinitialisé à chaque changement d'e-mail ou succès). */
    private int failedAttempts = 0;
    private String lastFailedEmail = "";
    private static final int MAX_ATTEMPTS_BEFORE_ALERT = 3;

    private final AuthService authService = new AuthService();
    private final TotpDAO totpDAO = new TotpDAO();
    private final org.example.services.UserService userService = new org.example.services.UserService();
    private final DigitalWillService digitalWillService = new DigitalWillService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (emailField != null) {
            emailField.textProperty().addListener((o, oldVal, newVal) -> {
                FormFieldFeedback.clearInputError(emailField, emailErrorLabel, LOGIN_THEME);
                // Réinitialise le compteur si l'utilisateur change d'e-mail
                if (newVal != null && !newVal.trim().equals(lastFailedEmail)) {
                    failedAttempts = 0;
                }
            });
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
            try {
                userService.touchLastSeen(user.getId());
            } catch (SQLException ignored) {
            }
            LoginEmailNotifier.notifyLoginAsync(user.getEmail(), user.getPrenom() + " " + user.getNom());
            TemporalPermissionNotifier.notifyExpiringSoonAsync(user);
            digitalWillService.startMaintenanceOnceAsync();
            openMain(event);
        } catch (SQLException e) {
            showError("Erreur base de données",
                    e.getMessage() != null ? e.getMessage() : e.toString());
        } catch (Exception e) {
            showError("Erreur", e.getMessage() != null ? e.getMessage() : e.toString());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleWindowsHelloLogin(ActionEvent event) {
        clearFieldErrors();

        String email = emailField != null ? emailField.getText().trim() : "";
        String emailErr = AuthValidation.validateEmailContent(emailField != null ? emailField.getText() : "");
        if (emailErr != null) {
            FormFieldFeedback.setInputError(emailField, emailErrorLabel, emailErr, LOGIN_THEME);
            return;
        }

        try {
            User user = authService.loginWithWindowsHello(email);
            if (user == null) {
                showError("Windows Hello",
                        "Connexion refusée. Vérifiez que Windows Hello est disponible, que vous l’avez activé dans votre profil, et que ce PC correspond à votre compte.");
                return;
            }
            if (!passTotpIfEnabled(user)) {
                return;
            }
            SessionContext.getInstance().login(user);
            LoginEmailNotifier.notifyLoginAsync(user.getEmail(), user.getPrenom() + " " + user.getNom());
            openMain(event);
        } catch (SQLException e) {
            showError("Erreur base de données",
                    e.getMessage() != null ? e.getMessage() : e.toString());
        } catch (Exception e) {
            showError("Windows Hello", e.getMessage() != null ? e.getMessage() : e.toString());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFaceLogin(ActionEvent event) {
        clearFieldErrors();

        String email = emailField != null ? emailField.getText().trim() : "";
        String emailErr = AuthValidation.validateEmailContent(emailField != null ? emailField.getText() : "");
        if (emailErr != null) {
            FormFieldFeedback.setInputError(emailField, emailErrorLabel, emailErr, LOGIN_THEME);
            return;
        }

        try {
            var face96 = FaceCaptureService.captureSingleFace96x96Bgr(0, Duration.ofSeconds(12));
            float[] emb = FaceEmbeddingModel.embedFaceBgr96(face96);

            User user = authService.loginWithFaceEmbedding(email, emb);
            if (user == null) {
                Double d = authService.faceDistanceForEmail(email, emb);
                showError("Reconnaissance faciale",
                        d != null
                                ? ("Connexion refusée. Distance=" + String.format(java.util.Locale.ROOT, "%.4f", d) + " (seuil=0.12).")
                                : "Connexion refusée. Vérifiez que vous avez enregistré votre visage dans le profil et que la webcam détecte correctement votre visage.");
                return;
            }
            if (!passTotpIfEnabled(user)) {
                return;
            }
            SessionContext.getInstance().login(user);
            LoginEmailNotifier.notifyLoginAsync(user.getEmail(), user.getPrenom() + " " + user.getNom());
            openMain(event);
        } catch (SQLException e) {
            showError("Erreur base de données",
                    e.getMessage() != null ? e.getMessage() : e.toString());
        } catch (Exception e) {
            showError("Reconnaissance faciale",
                    e.getMessage() != null ? e.getMessage() : e.toString());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        String emailPrefill = emailField != null ? emailField.getText() : "";
        UserDialogHelper.showForgotPasswordDialog(ownerStage(), emailPrefill);
    }

    private Stage ownerStage() {
        if (emailField != null && emailField.getScene() != null) {
            return (Stage) emailField.getScene().getWindow();
        }
        if (passwordField != null && passwordField.getScene() != null) {
            return (Stage) passwordField.getScene().getWindow();
        }
        return null;
    }

    private void clearFieldErrors() {
        FormFieldFeedback.clearInputError(emailField, emailErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(passwordField, passwordErrorLabel, LOGIN_THEME);
    }

    private boolean passTotpIfEnabled(User user) throws SQLException {
        var recOpt = totpDAO.getByUserId(user.getId());
        if (recOpt.isEmpty() || !recOpt.get().enabled()) {
            return true;
        }
        String secret = recOpt.get().secretBase32();
        var codeOpt = UserDialogHelper.showTotpCodeDialog(
                (Stage) (emailField != null && emailField.getScene() != null ? emailField.getScene().getWindow() : null),
                "2FA (TOTP)",
                "Saisissez le code à 6 chiffres de Google Authenticator pour finaliser la connexion.",
                null, null);
        if (codeOpt.isEmpty()) {
            return false;
        }
        boolean ok = Totp.verifyCode(secret, codeOpt.get(), System.currentTimeMillis());
        if (!ok) {
            showError("2FA (TOTP)", "Code invalide.");
            return false;
        }
        return true;
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
            SessionContext ctx = SessionContext.getInstance();
            stage.setScene(scene);
            stage.setTitle("OXYN — " + ctx.getRole().displayLabel());
            PrimaryStageLayout.applyFullScreen(stage);
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
}
