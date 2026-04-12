package org.example.controllers.dialog;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.entities.Admin;
import org.example.entities.Client;
import org.example.entities.Coach;
import org.example.entities.User;
import org.example.services.AuthValidation;
import org.example.services.UserService;
import org.example.utils.FormFieldFeedback;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class UserFormDialogController {

    private enum FormRole {
        ADMIN,
        CLIENT,
        ENCADRANT
    }

    @FXML
    private Label titleLabel;

    @FXML
    private Label passwordLabel;

    @FXML
    private TextField emailField;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label passwordErrorLabel;

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
    private ComboBox<FormRole> roleCombo;

    @FXML
    private Label roleErrorLabel;

    @FXML
    private CheckBox activeCheck;

    private Stage stage;
    private User existing;
    private User result;
    private boolean saved;

    private final UserService userService = new UserService();

    private boolean listenersWired;

    public void setup(Stage stage, User existingUser) {
        this.stage = Objects.requireNonNull(stage);
        this.existing = existingUser;
        this.result = null;
        this.saved = false;

        clearAllFieldErrors();

        roleCombo.setItems(FXCollections.observableArrayList(FormRole.values()));
        roleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(FormRole object) {
                return object == null ? "" : formRoleLabel(object);
            }

            @Override
            public FormRole fromString(String string) {
                return null;
            }
        });

        if (existing == null) {
            titleLabel.setText("Nouvel utilisateur");
            passwordLabel.setText("Mot de passe *");
            emailField.clear();
            nomField.clear();
            prenomField.clear();
            telephoneField.clear();
            passwordField.clear();
            roleCombo.getSelectionModel().select(FormRole.CLIENT);
            activeCheck.setSelected(true);
            passwordField.setPromptText("");
        } else {
            titleLabel.setText("Modifier l'utilisateur");
            passwordLabel.setText("Mot de passe");
            emailField.setText(existing.getEmail());
            nomField.setText(existing.getNom());
            prenomField.setText(existing.getPrenom());
            telephoneField.setText(existing.getTelephone() != null ? existing.getTelephone() : "");
            passwordField.clear();
            roleCombo.getSelectionModel().select(formRoleFromUser(existing));
            activeCheck.setSelected(existing.isActive());
            passwordField.setPromptText("Laisser vide pour ne pas changer");
        }

        if (!listenersWired) {
            wireClearListeners();
            listenersWired = true;
        }
    }

    private void wireClearListeners() {
        boolean loginTheme = false;
        emailField.textProperty().addListener((o, a, b) -> FormFieldFeedback.clearInputError(emailField, emailErrorLabel, loginTheme));
        passwordField.textProperty().addListener((o, a, b) -> FormFieldFeedback.clearInputError(passwordField, passwordErrorLabel, loginTheme));
        nomField.textProperty().addListener((o, a, b) -> FormFieldFeedback.clearInputError(nomField, nomErrorLabel, loginTheme));
        prenomField.textProperty().addListener((o, a, b) -> FormFieldFeedback.clearInputError(prenomField, prenomErrorLabel, loginTheme));
        telephoneField.textProperty().addListener((o, a, b) -> FormFieldFeedback.clearInputError(telephoneField, telephoneErrorLabel, loginTheme));
        roleCombo.valueProperty().addListener((o, a, b) -> FormFieldFeedback.clearComboError(roleCombo, roleErrorLabel));
    }

    private void clearAllFieldErrors() {
        boolean lt = false;
        FormFieldFeedback.clearInputError(emailField, emailErrorLabel, lt);
        FormFieldFeedback.clearInputError(passwordField, passwordErrorLabel, lt);
        FormFieldFeedback.clearInputError(nomField, nomErrorLabel, lt);
        FormFieldFeedback.clearInputError(prenomField, prenomErrorLabel, lt);
        FormFieldFeedback.clearInputError(telephoneField, telephoneErrorLabel, lt);
        FormFieldFeedback.clearComboError(roleCombo, roleErrorLabel);
    }

    public void attachEscapeHandler() {
        if (stage != null && stage.getScene() != null) {
            stage.getScene().setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    handleCancel();
                }
            });
        }
    }

    public Optional<User> getResult() {
        return saved && result != null ? Optional.of(result) : Optional.empty();
    }

    @FXML
    private void handleSave() {
        clearAllFieldErrors();
        boolean loginTheme = false;

        String emailRaw = raw(emailField);
        String pwd = passwordField.getText() != null ? passwordField.getText() : "";
        String nomRaw = raw(nomField);
        String prenomRaw = raw(prenomField);
        String tel = text(telephoneField);
        if (tel.isEmpty()) {
            tel = null;
        }

        boolean ok = true;

        String emailErr = AuthValidation.validateEmailContent(emailRaw);
        if (emailErr != null) {
            FormFieldFeedback.setInputError(emailField, emailErrorLabel, emailErr, loginTheme);
            ok = false;
        } else {
            try {
                String emLookup = emailRaw.trim().toLowerCase();
                User other = userService.findByEmail(emLookup);
                if (other != null && (existing == null || other.getId() != existing.getId())) {
                    FormFieldFeedback.setInputError(emailField, emailErrorLabel,
                            "Cette adresse e-mail est déjà utilisée.", loginTheme);
                    ok = false;
                }
            } catch (SQLException e) {
                FormFieldFeedback.setInputError(emailField, emailErrorLabel,
                        e.getMessage() != null ? e.getMessage() : "Erreur base de données.", loginTheme);
                ok = false;
            }
        }

        String nomErr = AuthValidation.validatePersonName(nomRaw, false);
        if (nomErr != null) {
            FormFieldFeedback.setInputError(nomField, nomErrorLabel, nomErr, loginTheme);
            ok = false;
        }

        String prenomErr = AuthValidation.validatePersonName(prenomRaw, true);
        if (prenomErr != null) {
            FormFieldFeedback.setInputError(prenomField, prenomErrorLabel, prenomErr, loginTheme);
            ok = false;
        }

        String n = nomRaw.trim();
        String p = prenomRaw.trim();
        String pwdErr;
        if (existing == null) {
            pwdErr = AuthValidation.validatePasswordCreate(pwd, n, p);
        } else {
            pwdErr = AuthValidation.validatePasswordEdit(pwd, n, p);
        }
        if (pwdErr != null) {
            FormFieldFeedback.setInputError(passwordField, passwordErrorLabel, pwdErr, loginTheme);
            ok = false;
        }

        FormRole rolePick = roleCombo.getSelectionModel().getSelectedItem();
        if (rolePick == null) {
            FormFieldFeedback.setComboError(roleCombo, roleErrorLabel, "Sélectionnez un rôle.");
            ok = false;
        }

        if (!ok) {
            return;
        }

        String em = emailRaw.trim().toLowerCase();
        int id = existing != null ? existing.getId() : 0;
        if (existing != null && pwd.isEmpty()) {
            pwd = existing.getPassword();
        }
        result = buildUser(rolePick, id, em, pwd, n, p, tel, activeCheck.isSelected());
        saved = true;
        stage.close();
    }

    @FXML
    private void handleCancel() {
        saved = false;
        result = null;
        if (stage != null) {
            stage.close();
        }
    }

    private static String text(TextField f) {
        return f.getText() != null ? f.getText().trim() : "";
    }

    private static String raw(TextField f) {
        return f.getText() != null ? f.getText() : "";
    }

    private static String formRoleLabel(FormRole r) {
        return switch (r) {
            case ADMIN -> "Administrateur";
            case CLIENT -> "Client";
            case ENCADRANT -> "Encadrant (coach)";
        };
    }

    private static FormRole formRoleFromUser(User u) {
        if (u instanceof Admin) {
            return FormRole.ADMIN;
        }
        if (u instanceof Coach) {
            return FormRole.ENCADRANT;
        }
        return FormRole.CLIENT;
    }

    private static User buildUser(FormRole role, int id, String email, String password, String nom, String prenom,
                                  String telephone, boolean active) {
        return switch (role) {
            case ADMIN -> new Admin(id, email, password, nom, prenom, telephone, active);
            case CLIENT -> new Client(id, email, password, nom, prenom, telephone, active);
            case ENCADRANT -> new Coach(id, email, password, nom, prenom, telephone, active);
        };
    }
}
