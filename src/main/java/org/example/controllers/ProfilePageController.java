package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
import org.example.utils.UserDialogHelper;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Lecture du profil connecté ; mise à jour partielle (nom, prénom, téléphone) pour client et encadrant.
 */
public class ProfilePageController implements Initializable {

    private static final boolean LOGIN_THEME = true;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label emailValue;

    @FXML
    private Label statusValue;

    @FXML
    private Label roleValue;

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
    private Button saveButton;

    @FXML
    private Label avatarLabel;

    @FXML
    private VBox readOnlyBanner;

    @FXML
    private Label formHintLabel;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SessionContext ctx = SessionContext.getInstance();

        nomField.textProperty().addListener((o, a, b) -> FormFieldFeedback.clearInputError(nomField, nomErrorLabel, LOGIN_THEME));
        prenomField.textProperty().addListener((o, a, b) -> FormFieldFeedback.clearInputError(prenomField, prenomErrorLabel, LOGIN_THEME));
        telephoneField.textProperty().addListener((o, a, b) -> FormFieldFeedback.clearInputError(telephoneField, telephoneErrorLabel, LOGIN_THEME));

        boolean admin = ctx.isAdmin();
        if (readOnlyBanner != null) {
            readOnlyBanner.setVisible(admin);
            readOnlyBanner.setManaged(admin);
        }
        if (formHintLabel != null) {
            formHintLabel.setVisible(!admin);
            formHintLabel.setManaged(!admin);
        }
        if (admin) {
            subtitleLabel.setText("Consultation de votre fiche (modification réservée à l’annuaire administrateur).");
            nomField.setEditable(false);
            nomField.setDisable(true);
            prenomField.setEditable(false);
            prenomField.setDisable(true);
            telephoneField.setEditable(false);
            telephoneField.setDisable(true);
            saveButton.setVisible(false);
            saveButton.setManaged(false);
            setProfileFieldsDisabledStyle(true);
        } else {
            subtitleLabel.setText("Consultez vos informations et modifiez votre nom, prénom ou téléphone.");
            nomField.setEditable(true);
            nomField.setDisable(false);
            prenomField.setEditable(true);
            prenomField.setDisable(false);
            telephoneField.setEditable(true);
            telephoneField.setDisable(false);
            saveButton.setVisible(true);
            saveButton.setManaged(true);
            setProfileFieldsDisabledStyle(false);
        }

        reloadFromDatabase();
    }

    private void setProfileFieldsDisabledStyle(boolean disabled) {
        for (TextField f : new TextField[]{nomField, prenomField, telephoneField}) {
            if (f == null) {
                continue;
            }
            if (disabled) {
                if (!f.getStyleClass().contains("pp-field-disabled")) {
                    f.getStyleClass().add("pp-field-disabled");
                }
            } else {
                f.getStyleClass().remove("pp-field-disabled");
            }
        }
    }

    private void reloadFromDatabase() {
        SessionContext ctx = SessionContext.getInstance();
        User sessionUser = ctx.getCurrentUser();
        if (sessionUser == null) {
            subtitleLabel.setText("Aucune session utilisateur.");
            applyRolePresentation(null);
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
        emailValue.setText(u.getEmail() != null ? u.getEmail() : "—");
        applyStatusBadge(u);
        applyRolePresentation(u);
        refreshAvatar(u);
        nomField.setText(u.getNom() != null ? u.getNom() : "");
        prenomField.setText(u.getPrenom() != null ? u.getPrenom() : "");
        telephoneField.setText(u.getTelephone() != null ? u.getTelephone() : "");
        clearFieldErrors();
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
        FormFieldFeedback.clearInputError(nomField, nomErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(prenomField, prenomErrorLabel, LOGIN_THEME);
        FormFieldFeedback.clearInputError(telephoneField, telephoneErrorLabel, LOGIN_THEME);
    }

    @FXML
    private void handleSave() {
        SessionContext ctx = SessionContext.getInstance();
        if (ctx.isAdmin()) {
            return;
        }
        User cur = ctx.getCurrentUser();
        if (cur == null) {
            return;
        }

        clearFieldErrors();
        boolean ok = true;

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

        String tel = text(telephoneField);
        if (!tel.isEmpty() && containsWhitespace(tel)) {
            FormFieldFeedback.setInputError(telephoneField, telephoneErrorLabel,
                    "Le téléphone ne doit pas contenir d’espaces.", LOGIN_THEME);
            ok = false;
        }

        if (!ok) {
            return;
        }

        String n = text(nomField);
        String p = text(prenomField);
        String telNorm = tel.isEmpty() ? null : tel;

        try {
            userService.updateProfilePartial(cur.getId(), n, p, telNorm);
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

    private static boolean containsWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static String raw(TextField f) {
        return f.getText() != null ? f.getText() : "";
    }

    private static String text(TextField f) {
        return f.getText() != null ? f.getText().trim() : "";
    }

    private Stage owner() {
        if (nomField == null || nomField.getScene() == null) {
            return null;
        }
        return (Stage) nomField.getScene().getWindow();
    }
}
