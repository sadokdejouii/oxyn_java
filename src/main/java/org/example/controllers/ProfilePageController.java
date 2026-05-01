package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entities.Admin;
import org.example.entities.Client;
import org.example.entities.Coach;
import org.example.entities.User;
import org.example.services.AuthValidation;
import org.example.services.SessionContext;
import org.example.services.UserRole;
import org.example.services.UserService;
import org.example.utils.FormFieldFeedback;
import org.example.utils.PasswordUtils;
import org.example.utils.UserDialogHelper;
import org.example.windowshello.WindowsHelloBridge;
import org.example.windowshello.WindowsHelloLinkDAO;
import org.example.windowshello.WindowsHelloResult;
import org.example.facerec.FaceCaptureService;
import org.example.facerec.FaceEmbeddingCodec;
import org.example.facerec.FaceEmbeddingDAO;
import org.example.facerec.FaceEmbeddingModel;
import org.example.totp.Base32;
import org.example.totp.QrCode;
import org.example.totp.Totp;
import org.example.totp.TotpDAO;
import org.example.avatar.AvatarDAO;
import org.example.avatar.AvatarGenerator;
import org.example.avatar.AvatarAiService;

import java.net.URL;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ResourceBundle;
import java.io.ByteArrayInputStream;

/**
 * Profil connecté : e-mail, nom, prénom, téléphone et changement de mot de passe (admin, client, encadrant).
 */
public class ProfilePageController implements Initializable {

    private static final boolean LOGIN_THEME = true;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label statusValue;

    @FXML
    private Label roleValue;

    @FXML
    private TextField emailField;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private TextField nomField;

    @FXML
    private Label nomErrorLabel;

    @FXML
    private TextField prenomField;

    @FXML
    private Label prenomErrorLabel;

    @FXML
    private TextField telephoneField;

    @FXML
    private Label telephoneErrorLabel;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private Label currentPasswordErrorLabel;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private Label newPasswordErrorLabel;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label confirmPasswordErrorLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Label windowsHelloStatusLabel;

    @FXML
    private Button enableWindowsHelloButton;

    @FXML
    private Button disableWindowsHelloButton;

    @FXML
    private Label faceStatusLabel;

    @FXML
    private Button enrollFaceButton;

    @FXML
    private Button disableFaceButton;

    @FXML
    private Label totpStatusLabel;

    @FXML
    private Button enableTotpButton;

    @FXML
    private Button disableTotpButton;

    @FXML
    private Label avatarLabel;

    @FXML
    private ImageView avatarImageView;

    @FXML
    private Label avatarStatusLabel;

    @FXML
    private Button generateAvatarButton;

    @FXML
    private Button generateAvatarAiButton;

    @FXML
    private Button removeAvatarButton;

    @FXML
    private Label formHintLabel;

    @FXML
    private ScrollPane profileScroll;

    @FXML
    private VBox profileRoot;

    private final UserService userService = new UserService();
    private final WindowsHelloLinkDAO windowsHelloLinkDAO = new WindowsHelloLinkDAO();
    private final FaceEmbeddingDAO faceEmbeddingDAO = new FaceEmbeddingDAO();
    private final TotpDAO totpDAO = new TotpDAO();
    private final AvatarDAO avatarDAO = new AvatarDAO();
    private final AvatarAiService avatarAiService = new AvatarAiService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SessionContext ctx = SessionContext.getInstance();
        subtitleLabel.setText("Modifiez vos informations et votre mot de passe. Les changements s’appliquent immédiatement à votre session.");

        wireClear(emailField, emailErrorLabel);
        wireClear(nomField, nomErrorLabel);
        wireClear(prenomField, prenomErrorLabel);
        wireClear(telephoneField, telephoneErrorLabel);
        wireClear(currentPasswordField, currentPasswordErrorLabel);
        wireClear(newPasswordField, newPasswordErrorLabel);
        wireClear(confirmPasswordField, confirmPasswordErrorLabel);

        reloadFromDatabase();
    }

    /**
     * Client & encadrant : fond clair (dashboard « front »). Admin : conserve le thème sombre actuel.
     */
    private void applyProfileTheme(User u) {
        if (profileScroll == null || profileRoot == null) {
            return;
        }
        SessionContext ctx = SessionContext.getInstance();
        boolean admin = u != null
                ? u instanceof Admin
                : ctx.getRole() == UserRole.ADMIN;

        profileScroll.getStyleClass().remove("pp-page--light");
        profileRoot.getStyleClass().remove("pp-root--light");

        if (!admin) {
            profileScroll.getStyleClass().add("pp-page--light");
            profileRoot.getStyleClass().add("pp-root--light");
        }
    }

    private void wireClear(TextInputControl input, Label err) {
        input.textProperty().addListener((o, a, b) -> FormFieldFeedback.clearInputError(input, err, LOGIN_THEME));
    }

    private void reloadFromDatabase() {
        SessionContext ctx = SessionContext.getInstance();
        User sessionUser = ctx.getCurrentUser();
        if (sessionUser == null) {
            subtitleLabel.setText("Aucune session utilisateur.");
            applyRolePresentation(null);
            applyProfileTheme(null);
            return;
        }
        try {
            User fresh = userService.getUserById(sessionUser.getId());
            if (fresh == null) {
                UserDialogHelper.showMessage(owner(), "Profil", "Utilisateur introuvable en base.", true);
                return;
            }
            fillReadOnlyAndFields(fresh);
        } catch (SQLException e) {
            UserDialogHelper.showMessage(owner(), "Profil",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        }
    }

    private void fillReadOnlyAndFields(User u) {
        if (emailField != null) {
            emailField.setText(u.getEmail() != null ? u.getEmail() : "");
        }
        applyStatusBadge(u);
        applyRolePresentation(u);
        refreshAvatar(u);
        nomField.setText(u.getNom() != null ? u.getNom() : "");
        prenomField.setText(u.getPrenom() != null ? u.getPrenom() : "");
        telephoneField.setText(u.getTelephone() != null ? u.getTelephone() : "");
        clearPasswordFields();
        clearFieldErrors();
        applyProfileTheme(u);
        refreshWindowsHelloBlock(u);
        refreshFaceBlock(u);
        refreshTotpBlock(u);
        refreshAvatarImage(u);
    }

    private void refreshAvatarImage(User u) {
        if (avatarImageView == null || avatarLabel == null || avatarStatusLabel == null
                || generateAvatarButton == null || removeAvatarButton == null) {
            return;
        }
        if (u == null || u.getId() <= 0) {
            avatarImageView.setVisible(false);
            avatarImageView.setManaged(false);
            avatarLabel.setVisible(true);
            avatarLabel.setManaged(true);
            avatarStatusLabel.setText("Avatar : indisponible (pas de session).");
            generateAvatarButton.setDisable(true);
            removeAvatarButton.setDisable(true);
            return;
        }
        try {
            var recOpt = avatarDAO.getByUserId(u.getId());
            if (recOpt.isEmpty() || recOpt.get().avatarPng() == null || recOpt.get().avatarPng().length == 0) {
                avatarImageView.setVisible(false);
                avatarImageView.setManaged(false);
                avatarLabel.setVisible(true);
                avatarLabel.setManaged(true);
                avatarStatusLabel.setText("Avatar : non généré.");
                generateAvatarButton.setDisable(false);
                removeAvatarButton.setDisable(true);
                return;
            }
            Image img = new Image(new ByteArrayInputStream(recOpt.get().avatarPng()));
            avatarImageView.setImage(img);
            avatarImageView.setVisible(true);
            avatarImageView.setManaged(true);
            avatarLabel.setVisible(false);
            avatarLabel.setManaged(false);
            avatarStatusLabel.setText("Avatar : généré.");
            generateAvatarButton.setDisable(false);
            removeAvatarButton.setDisable(false);
        } catch (SQLException e) {
            avatarStatusLabel.setText("Avatar : erreur DB.");
            generateAvatarButton.setDisable(false);
            removeAvatarButton.setDisable(false);
        }
    }

    @FXML
    private void handleGenerateAvatar() {
        SessionContext ctx = SessionContext.getInstance();
        User cur = ctx.getCurrentUser();
        if (cur == null) return;
        try {
            // Réutilise la capture visage (webcam) existante: détecte visage puis crop.
            var face96 = FaceCaptureService.captureSingleFace96x96Bgr(0, Duration.ofSeconds(12));
            byte[] png = AvatarGenerator.generateAvatarPng(face96);
            avatarDAO.upsert(cur.getId(), png);
            UserDialogHelper.showMessage(owner(), "Avatar", "Avatar généré et enregistré.", false);
        } catch (SQLException e) {
            UserDialogHelper.showMessage(owner(), "Avatar",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        } catch (Exception e) {
            UserDialogHelper.showMessage(owner(), "Avatar",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        } finally {
            refreshAvatarImage(cur);
        }
    }

    @FXML
    private void handleGenerateAvatarAi() {
        SessionContext ctx = SessionContext.getInstance();
        User cur = ctx.getCurrentUser();
        if (cur == null) return;

        // Désactiver les boutons pendant le traitement (appel réseau ~10-30s)
        if (generateAvatarAiButton != null) generateAvatarAiButton.setDisable(true);
        if (generateAvatarButton   != null) generateAvatarButton.setDisable(true);
        if (avatarStatusLabel      != null) avatarStatusLabel.setText("Avatar IA : génération en cours…");

        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                // Capture le visage depuis la webcam (réutilise l'infrastructure existante)
                var face96 = org.example.facerec.FaceCaptureService
                        .captureSingleFace96x96Bgr(0, java.time.Duration.ofSeconds(12));

                // Encode le Mat OpenCV en JPEG pour l'envoyer à l'API
                org.bytedeco.javacpp.BytePointer buf = new org.bytedeco.javacpp.BytePointer();
                org.bytedeco.opencv.global.opencv_imgcodecs.imencode(".jpg", face96, buf);
                byte[] jpegBytes = new byte[(int) buf.limit()];
                buf.get(jpegBytes);
                buf.close();

                return avatarAiService.generateAnimeAvatar(jpegBytes);
            }
        };

        task.setOnSucceeded(e -> {
            byte[] result = task.getValue();
            Platform.runLater(() -> {
                if (result != null && result.length > 0) {
                    try {
                        avatarDAO.upsert(cur.getId(), result);
                        UserDialogHelper.showMessage(owner(), "Avatar IA", "Avatar anime généré et enregistré ✨", false);
                    } catch (java.sql.SQLException ex) {
                        UserDialogHelper.showMessage(owner(), "Avatar IA", "Erreur DB: " + ex.getMessage(), true);
                    }
                } else {
                    UserDialogHelper.showMessage(owner(), "Avatar IA",
                            "La génération IA a échoué (API HuggingFace indisponible ou quota dépassé).", true);
                }
                refreshAvatarImage(cur);
                if (generateAvatarAiButton != null) generateAvatarAiButton.setDisable(false);
                if (generateAvatarButton   != null) generateAvatarButton.setDisable(false);
            });
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> {
                UserDialogHelper.showMessage(owner(), "Avatar IA",
                        ex != null ? ex.getMessage() : "Erreur inconnue", true);
                refreshAvatarImage(cur);
                if (generateAvatarAiButton != null) generateAvatarAiButton.setDisable(false);
                if (generateAvatarButton   != null) generateAvatarButton.setDisable(false);
            });
        });

        new Thread(task, "avatar-ai-thread").start();
    }

    @FXML
    private void handleRemoveAvatar() {
        SessionContext ctx = SessionContext.getInstance();
        User cur = ctx.getCurrentUser();
        if (cur == null) return;
        try {
            avatarDAO.delete(cur.getId());
            UserDialogHelper.showMessage(owner(), "Avatar", "Avatar supprimé.", false);
        } catch (SQLException e) {
            UserDialogHelper.showMessage(owner(), "Avatar",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        } finally {
            refreshAvatarImage(cur);
        }
    }

    private void refreshTotpBlock(User u) {
        if (totpStatusLabel == null || enableTotpButton == null || disableTotpButton == null) {
            return;
        }
        if (u == null || u.getId() <= 0) {
            totpStatusLabel.setText("2FA : indisponible (pas de session).");
            enableTotpButton.setDisable(true);
            disableTotpButton.setDisable(true);
            return;
        }
        try {
            var recOpt = totpDAO.getByUserId(u.getId());
            if (recOpt.isEmpty()) {
                totpStatusLabel.setText("2FA : non activée.");
                enableTotpButton.setDisable(false);
                disableTotpButton.setDisable(true);
                return;
            }
            var rec = recOpt.get();
            if (rec.enabled()) {
                totpStatusLabel.setText("2FA : activée (TOTP).");
                enableTotpButton.setDisable(true);
                disableTotpButton.setDisable(false);
            } else {
                totpStatusLabel.setText("2FA : désactivée (réactivable).");
                enableTotpButton.setDisable(false);
                disableTotpButton.setDisable(true);
            }
        } catch (SQLException e) {
            totpStatusLabel.setText("2FA : erreur DB.");
            enableTotpButton.setDisable(false);
            disableTotpButton.setDisable(false);
        }
    }

    @FXML
    private void handleEnableTotp() {
        SessionContext ctx = SessionContext.getInstance();
        User cur = ctx.getCurrentUser();
        if (cur == null) {
            return;
        }
        String secret = Base32.randomSecret(20);
        String issuer = "OXYN";
        String account = cur.getEmail() != null ? cur.getEmail().trim() : ("user#" + cur.getId());
        String uri = Totp.buildOtpAuthUri(issuer, account, secret);

        javafx.scene.image.Image qr = null;
        try {
            qr = QrCode.toFxImage(uri, 240);
        } catch (Exception ignored) {
        }
        String hint = "Clé (si QR indisponible) : " + secret;

        var codeOpt = UserDialogHelper.showTotpCodeDialog(owner(),
                "Activer 2FA (TOTP)",
                "Scannez le QR code avec Google Authenticator, puis saisissez le code à 6 chiffres.",
                qr, hint);
        if (codeOpt.isEmpty()) {
            refreshTotpBlock(cur);
            return;
        }
        String code = codeOpt.get();
        boolean ok = Totp.verifyCode(secret, code, System.currentTimeMillis());
        if (!ok) {
            UserDialogHelper.showMessage(owner(), "2FA (TOTP)", "Code invalide. Réessayez.", true);
            refreshTotpBlock(cur);
            return;
        }
        try {
            totpDAO.upsert(cur.getId(), secret, true);
            UserDialogHelper.showMessage(owner(), "2FA (TOTP)", "2FA activée. Un code sera demandé à la connexion.", false);
        } catch (SQLException e) {
            UserDialogHelper.showMessage(owner(), "2FA (TOTP)",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        } finally {
            refreshTotpBlock(cur);
        }
    }

    @FXML
    private void handleDisableTotp() {
        SessionContext ctx = SessionContext.getInstance();
        User cur = ctx.getCurrentUser();
        if (cur == null) {
            return;
        }
        boolean confirm = UserDialogHelper.showConfirm(owner(), "2FA (TOTP)",
                "Désactiver la 2FA ? Vous pourrez vous connecter sans code temporaire.", "Désactiver", true);
        if (!confirm) {
            refreshTotpBlock(cur);
            return;
        }
        try {
            totpDAO.disable(cur.getId());
            UserDialogHelper.showMessage(owner(), "2FA (TOTP)", "2FA désactivée.", false);
        } catch (SQLException e) {
            UserDialogHelper.showMessage(owner(), "2FA (TOTP)",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        } finally {
            refreshTotpBlock(cur);
        }
    }

    private void refreshFaceBlock(User u) {
        if (faceStatusLabel == null || enrollFaceButton == null || disableFaceButton == null) {
            return;
        }
        if (u == null || u.getId() <= 0) {
            faceStatusLabel.setText("Reconnaissance faciale : indisponible (pas de session).");
            enrollFaceButton.setDisable(true);
            disableFaceButton.setDisable(true);
            return;
        }
        try {
            var recOpt = faceEmbeddingDAO.getByUserId(u.getId());
            if (recOpt.isEmpty()) {
                faceStatusLabel.setText("Reconnaissance faciale : non enregistrée.");
                enrollFaceButton.setDisable(false);
                disableFaceButton.setDisable(true);
                return;
            }
            var rec = recOpt.get();
            if (rec.enabled()) {
                faceStatusLabel.setText("Reconnaissance faciale : activée.");
                enrollFaceButton.setDisable(false);
                disableFaceButton.setDisable(false);
            } else {
                faceStatusLabel.setText("Reconnaissance faciale : désactivée (réactivable).");
                enrollFaceButton.setDisable(false);
                disableFaceButton.setDisable(true);
            }
        } catch (SQLException e) {
            faceStatusLabel.setText("Reconnaissance faciale : erreur DB.");
            enrollFaceButton.setDisable(false);
            disableFaceButton.setDisable(false);
        }
    }

    @FXML
    private void handleEnrollFace() {
        SessionContext ctx = SessionContext.getInstance();
        User cur = ctx.getCurrentUser();
        if (cur == null) {
            return;
        }
        try {
            var face96 = FaceCaptureService.captureSingleFace96x96Bgr(0, Duration.ofSeconds(12));
            float[] emb = FaceEmbeddingModel.embedFaceBgr96(face96);
            byte[] bytes = FaceEmbeddingCodec.toBytes(emb);
            faceEmbeddingDAO.upsert(cur.getId(), bytes, true);
            UserDialogHelper.showMessage(owner(), "Reconnaissance faciale",
                    "Visage enregistré. Vous pourrez vous connecter via la webcam.", false);
        } catch (SQLException e) {
            UserDialogHelper.showMessage(owner(), "Reconnaissance faciale",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || msg.isBlank() || msg.trim().matches("^\\d+$")) {
                msg = e.toString();
            }
            UserDialogHelper.showMessage(owner(), "Reconnaissance faciale", msg, true);
        } finally {
            refreshFaceBlock(cur);
        }
    }

    @FXML
    private void handleDisableFace() {
        SessionContext ctx = SessionContext.getInstance();
        User cur = ctx.getCurrentUser();
        if (cur == null) {
            return;
        }
        try {
            faceEmbeddingDAO.disable(cur.getId());
            UserDialogHelper.showMessage(owner(), "Reconnaissance faciale",
                    "Reconnaissance faciale désactivée pour ce compte.", false);
        } catch (SQLException e) {
            UserDialogHelper.showMessage(owner(), "Reconnaissance faciale",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        } finally {
            refreshFaceBlock(cur);
        }
    }

    private void refreshWindowsHelloBlock(User u) {
        if (windowsHelloStatusLabel == null || enableWindowsHelloButton == null || disableWindowsHelloButton == null) {
            return;
        }
        if (u == null || u.getId() <= 0) {
            windowsHelloStatusLabel.setText("Windows Hello : indisponible (pas de session).");
            enableWindowsHelloButton.setDisable(true);
            disableWindowsHelloButton.setDisable(true);
            return;
        }
        try {
            var linkOpt = windowsHelloLinkDAO.getByUserId(u.getId());
            if (linkOpt.isEmpty()) {
                windowsHelloStatusLabel.setText("Windows Hello : non activé sur ce PC.");
                enableWindowsHelloButton.setDisable(false);
                disableWindowsHelloButton.setDisable(true);
                return;
            }
            var link = linkOpt.get();
            if (link.enabled()) {
                windowsHelloStatusLabel.setText("Windows Hello : activé (SID " + link.windowsSid() + ").");
                enableWindowsHelloButton.setDisable(true);
                disableWindowsHelloButton.setDisable(false);
            } else {
                windowsHelloStatusLabel.setText("Windows Hello : désactivé (réactivable).");
                enableWindowsHelloButton.setDisable(false);
                disableWindowsHelloButton.setDisable(true);
            }
        } catch (SQLException e) {
            windowsHelloStatusLabel.setText("Windows Hello : erreur DB.");
            enableWindowsHelloButton.setDisable(false);
            disableWindowsHelloButton.setDisable(false);
        }
    }

    @FXML
    private void handleEnableWindowsHello() {
        SessionContext ctx = SessionContext.getInstance();
        User cur = ctx.getCurrentUser();
        if (cur == null) {
            return;
        }
        WindowsHelloResult r = WindowsHelloBridge.verify("Activer Windows Hello pour OXYN");
        if (r == null || !r.ok()) {
            UserDialogHelper.showMessage(owner(), "Windows Hello",
                    (r != null && r.error() != null && !r.error().isBlank())
                            ? r.error()
                            : "Vérification Windows Hello non validée.", true);
            refreshWindowsHelloBlock(cur);
            return;
        }
        if (r.sid() == null || r.sid().isBlank()) {
            UserDialogHelper.showMessage(owner(), "Windows Hello",
                    "Impossible de lire le SID Windows. Activation annulée.", true);
            refreshWindowsHelloBlock(cur);
            return;
        }
        try {
            windowsHelloLinkDAO.upsert(cur.getId(), r.sid(), true);
            UserDialogHelper.showMessage(owner(), "Windows Hello",
                    "Windows Hello activé sur ce PC. Vous pourrez vous connecter sans mot de passe.", false);
        } catch (SQLException e) {
            UserDialogHelper.showMessage(owner(), "Windows Hello",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        } finally {
            refreshWindowsHelloBlock(cur);
        }
    }

    @FXML
    private void handleDisableWindowsHello() {
        SessionContext ctx = SessionContext.getInstance();
        User cur = ctx.getCurrentUser();
        if (cur == null) {
            return;
        }
        try {
            windowsHelloLinkDAO.disable(cur.getId());
            UserDialogHelper.showMessage(owner(), "Windows Hello",
                    "Windows Hello désactivé pour ce compte.", false);
        } catch (SQLException e) {
            UserDialogHelper.showMessage(owner(), "Windows Hello",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        } finally {
            refreshWindowsHelloBlock(cur);
        }
    }

    private void clearPasswordFields() {
        if (currentPasswordField != null) {
            currentPasswordField.clear();
        }
        if (newPasswordField != null) {
            newPasswordField.clear();
        }
        if (confirmPasswordField != null) {
            confirmPasswordField.clear();
        }
    }

    private void refreshAvatar(User u) {
        if (avatarLabel == null || u == null) {
            return;
        }
        String p = u.getPrenom() != null ? u.getPrenom().trim() : "";
        String n = u.getNom() != null ? u.getNom().trim() : "";
        String initials;
        if (!p.isEmpty() && !n.isEmpty()) {
            initials = (firstLetter(p) + firstLetter(n)).toUpperCase();
        } else if (!p.isEmpty()) {
            initials = p.length() >= 2 ? p.substring(0, 2).toUpperCase() : p.toUpperCase();
        } else if (!n.isEmpty()) {
            initials = n.length() >= 2 ? n.substring(0, 2).toUpperCase() : n.toUpperCase();
        } else {
            initials = "?";
        }
        avatarLabel.setText(initials);
    }

    private static String firstLetter(String s) {
        return s.isEmpty() ? "" : s.substring(0, 1);
    }

    private void applyStatusBadge(User u) {
        statusValue.setText(u.isActive() ? "Actif" : "Inactif");
        statusValue.getStyleClass().removeIf(s -> "pp-badge-status-on".equals(s) || "pp-badge-status-off".equals(s));
        if (u.isActive()) {
            statusValue.getStyleClass().add("pp-badge-status-on");
        } else {
            statusValue.getStyleClass().add("pp-badge-status-off");
        }
    }

    private void applyRolePresentation(User u) {
        SessionContext ctx = SessionContext.getInstance();
        if (u != null) {
            roleValue.setText(UserRole.fromUser(u).displayLabel());
            styleRoleBadgeFromUser(u);
            return;
        }
        UserRole r = ctx.getRole();
        roleValue.setText(r.displayLabel());
        roleValue.getStyleClass().removeIf(s -> s.startsWith("pp-badge-role-"));
        switch (r) {
            case ADMIN -> roleValue.getStyleClass().add("pp-badge-role-admin");
            case ENCADRANT -> roleValue.getStyleClass().add("pp-badge-role-coach");
            case CLIENT -> roleValue.getStyleClass().add("pp-badge-role-client");
        }
    }

    private void styleRoleBadgeFromUser(User u) {
        roleValue.getStyleClass().removeIf(s -> s.startsWith("pp-badge-role-"));
        if (u instanceof Admin) {
            roleValue.getStyleClass().add("pp-badge-role-admin");
        } else if (u instanceof Coach) {
            roleValue.getStyleClass().add("pp-badge-role-coach");
        } else if (u instanceof Client) {
            roleValue.getStyleClass().add("pp-badge-role-client");
        } else {
            roleValue.getStyleClass().add("pp-badge-role-unknown");
        }
    }

    private void clearFieldErrors() {
        FormFieldFeedback.clearInputError(emailField, emailErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(nomField, nomErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(prenomField, prenomErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(telephoneField, telephoneErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(currentPasswordField, currentPasswordErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(newPasswordField, newPasswordErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(confirmPasswordField, confirmPasswordErrorLabel, LOGIN_THEME);
    }

    @FXML
    private void handleSave() {
        SessionContext ctx = SessionContext.getInstance();
        User cur = ctx.getCurrentUser();
        if (cur == null) {
            return;
        }

        clearFieldErrors();
        boolean ok = true;

        String email = text(emailField);
        String emailErr = AuthValidation.validateEmailContent(email);
        if (emailErr != null) {
            FormFieldFeedback.setInputError(emailField, emailErrorLabel, emailErr, LOGIN_THEME);
            ok = false;
        }

        String nomErr = AuthValidation.validatePersonName(raw(nomField), false);
        if (nomErr != null) {
            FormFieldFeedback.setInputError(nomField, nomErrorLabel, nomErr, LOGIN_THEME);
            ok = false;
        }

        String prenomErr = AuthValidation.validatePersonName(raw(prenomField), true);
        if (prenomErr != null) {
            FormFieldFeedback.setInputError(prenomField, prenomErrorLabel, prenomErr, LOGIN_THEME);
            ok = false;
        }

        /* Téléphone obligatoire + format strict (Admin, Client et Encadrant — même écran profil). */
        String telRaw = raw(telephoneField);
        String telErr = AuthValidation.validateTelephone(telRaw, false);
        if (telErr != null) {
            FormFieldFeedback.setInputError(telephoneField, telephoneErrorLabel, telErr, LOGIN_THEME);
            ok = false;
        }

        String n = text(nomField);
        String p = text(prenomField);
        String tel = text(telephoneField);
        String telNorm = tel.isEmpty() ? null : tel;

        String currentPwd = passwordText(currentPasswordField);
        String newPwd = passwordText(newPasswordField);
        String confirmPwd = passwordText(confirmPasswordField);

        User dbUser;
        try {
            dbUser = userService.getUserById(cur.getId());
        } catch (SQLException e) {
            UserDialogHelper.showMessage(owner(), "Profil",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
            return;
        }
        if (dbUser == null) {
            UserDialogHelper.showMessage(owner(), "Profil", "Utilisateur introuvable en base.", true);
            return;
        }

        if (emailErr == null) {
            try {
                User taken = userService.findByEmail(email);
                if (taken != null && taken.getId() != cur.getId()) {
                    FormFieldFeedback.setInputError(emailField, emailErrorLabel,
                            "Cette adresse e-mail est déjà utilisée par un autre compte.", LOGIN_THEME);
                    ok = false;
                }
            } catch (SQLException e) {
                UserDialogHelper.showMessage(owner(), "Profil",
                        e.getMessage() != null ? e.getMessage() : e.toString(), true);
                return;
            }
        }

        String pwdBlockErr = validatePasswordBlock(dbUser, currentPwd, newPwd, confirmPwd, n, p);
        if (pwdBlockErr != null) {
            ok = false;
            if (pwdBlockErr.startsWith("CURRENT:")) {
                FormFieldFeedback.setInputError(currentPasswordField, currentPasswordErrorLabel,
                        pwdBlockErr.substring("CURRENT:".length()), LOGIN_THEME);
            } else if (pwdBlockErr.startsWith("NEW:")) {
                FormFieldFeedback.setInputError(newPasswordField, newPasswordErrorLabel,
                        pwdBlockErr.substring("NEW:".length()), LOGIN_THEME);
            } else if (pwdBlockErr.startsWith("CONFIRM:")) {
                FormFieldFeedback.setInputError(confirmPasswordField, confirmPasswordErrorLabel,
                        pwdBlockErr.substring("CONFIRM:".length()), LOGIN_THEME);
            }
        }

        if (!ok) {
            return;
        }

        String passwordHash = dbUser.getPassword();
        if (!newPwd.isEmpty()) {
            passwordHash = PasswordUtils.hash(newPwd);
        }

        User toSave = rebuildUser(dbUser, email, passwordHash, n, p, telNorm, dbUser.isActive());
        try {
            userService.updateUser(toSave);
            User refreshed = userService.getUserById(cur.getId());
            if (refreshed != null) {
                ctx.applyRefreshedUser(refreshed);
                fillReadOnlyAndFields(refreshed);
            }
            UserDialogHelper.showMessage(owner(), "Profil", "Vos informations ont été mises à jour.", false);
        } catch (SQLException e) {
            UserDialogHelper.showMessage(owner(), "Profil",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        }
    }

    /**
     * @return null si OK ; sinon préfixe CURRENT:, NEW: ou CONFIRM: + message
     */
    private static String validatePasswordBlock(User dbUser, String currentPwd, String newPwd, String confirmPwd,
                                                String nom, String prenom) {
        boolean any = !currentPwd.isEmpty() || !newPwd.isEmpty() || !confirmPwd.isEmpty();
        if (!any) {
            return null;
        }
        if (currentPwd.isEmpty()) {
            return "CURRENT:Saisissez votre mot de passe actuel pour le modifier.";
        }
        if (!PasswordUtils.matches(currentPwd, dbUser.getPassword())) {
            return "CURRENT:Mot de passe actuel incorrect.";
        }
        if (newPwd.isEmpty()) {
            return "NEW:Saisissez un nouveau mot de passe.";
        }
        String newErr = AuthValidation.validatePasswordCreate(newPwd, nom, prenom);
        if (newErr != null) {
            return "NEW:" + newErr;
        }
        String cErr = AuthValidation.validateConfirmPassword(newPwd, confirmPwd);
        if (cErr != null) {
            return "CONFIRM:" + cErr;
        }
        return null;
    }

    private static User rebuildUser(User db, String email, String passwordHash, String nom, String prenom,
                                    String telephone, boolean active) {
        if (db instanceof Admin) {
            return new Admin(db.getId(), email, passwordHash, nom, prenom, telephone, active);
        }
        if (db instanceof Coach) {
            return new Coach(db.getId(), email, passwordHash, nom, prenom, telephone, active);
        }
        if (db instanceof Client) {
            return new Client(db.getId(), email, passwordHash, nom, prenom, telephone, active);
        }
        throw new IllegalStateException("Type utilisateur non géré");
    }

    private static String raw(TextField f) {
        return f.getText() != null ? f.getText() : "";
    }

    private static String text(TextField f) {
        return f.getText() != null ? f.getText().trim() : "";
    }

    private static String passwordText(PasswordField f) {
        return f.getText() != null ? f.getText() : "";
    }

    private Stage owner() {
        if (emailField != null && emailField.getScene() != null) {
            return (Stage) emailField.getScene().getWindow();
        }
        if (nomField != null && nomField.getScene() != null) {
            return (Stage) nomField.getScene().getWindow();
        }
        return null;
    }
}
