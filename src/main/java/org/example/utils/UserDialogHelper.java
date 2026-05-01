package org.example.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.controllers.dialog.ConfirmDialogController;
import org.example.controllers.dialog.MessageDialogController;
import org.example.controllers.dialog.TotpCodeDialogController;
import org.example.controllers.dialog.UserFormDialogController;
import org.example.entities.User;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Dialogues modaux FXML (message, confirmation, formulaire utilisateur) pour le CRUD admin.
 */
public final class UserDialogHelper {

    private static final String MESSAGE_FXML = "/FXML/dialogs/MessageDialog.fxml";
    private static final String CONFIRM_FXML = "/FXML/dialogs/ConfirmDialog.fxml";
    private static final String USER_FORM_FXML = "/FXML/dialogs/UserFormDialog.fxml";
    private static final String TOTP_CODE_FXML = "/FXML/dialogs/TotpCodeDialog.fxml";
    private static final String FORGOT_PASSWORD_FXML = "/FXML/dialogs/ForgotPasswordDialog.fxml";

    private UserDialogHelper() {
    }

    private static void centerOnOwnerWhenShown(Stage dialog, Stage owner) {
        dialog.setOnShown(ev -> {
            if (owner != null) {
                dialog.setX(owner.getX() + (owner.getWidth() - dialog.getWidth()) / 2);
                dialog.setY(owner.getY() + (owner.getHeight() - dialog.getHeight()) / 2);
            } else {
                dialog.centerOnScreen();
            }
        });
    }

    public static void showMessage(Stage owner, String title, String message, boolean error) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    UserDialogHelper.class.getResource(MESSAGE_FXML)));
            Parent root = loader.load();
            MessageDialogController ctrl = loader.getController();

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            ctrl.setup(stage, title, message, error);
            stage.setResizable(false);
            stage.sizeToScene();
            centerOnOwnerWhenShown(stage, owner);
            stage.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param confirmText libellé du bouton principal (ex. « Supprimer »), ou null pour « Confirmer »
     * @param dangerStyle   si true, style « danger » sur le bouton de confirmation
     */
    public static boolean showConfirm(Stage owner, String title, String message, String confirmText,
                                      boolean dangerStyle) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    UserDialogHelper.class.getResource(CONFIRM_FXML)));
            Parent root = loader.load();
            ConfirmDialogController ctrl = loader.getController();

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            ctrl.setup(stage, title, message, confirmText, dangerStyle);
            stage.setResizable(false);
            stage.sizeToScene();
            centerOnOwnerWhenShown(stage, owner);
            stage.showAndWait();
            return ctrl.isConfirmed();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<User> showUserForm(Stage owner, User existing) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    UserDialogHelper.class.getResource(USER_FORM_FXML)));
            Parent root = loader.load();
            UserFormDialogController ctrl = loader.getController();

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            ctrl.setup(stage, existing);
            ctrl.attachEscapeHandler();
            stage.setResizable(false);
            stage.sizeToScene();
            centerOnOwnerWhenShown(stage, owner);
            stage.showAndWait();
            return ctrl.getResult();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<String> showTotpCodeDialog(Stage owner, String title, String subtitle,
                                                      javafx.scene.image.Image qr, String secretHint) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    UserDialogHelper.class.getResource(TOTP_CODE_FXML)));
            Parent root = loader.load();
            TotpCodeDialogController ctrl = loader.getController();

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            ctrl.setup(stage, title, subtitle);
            if (qr != null || (secretHint != null && !secretHint.isBlank())) {
                ctrl.setQr(qr, secretHint);
            }
            stage.setResizable(false);
            stage.sizeToScene();
            centerOnOwnerWhenShown(stage, owner);
            stage.showAndWait();
            if (!ctrl.isConfirmed()) {
                return Optional.empty();
            }
            String code = ctrl.getCode();
            return Optional.ofNullable(code);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void showForgotPasswordDialog(Stage owner, String emailPrefill) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    UserDialogHelper.class.getResource(FORGOT_PASSWORD_FXML)));
            Parent root = loader.load();
            org.example.controllers.dialog.ForgotPasswordDialogController ctrl = loader.getController();

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            ctrl.setup(stage, emailPrefill);
            stage.setResizable(false);
            stage.sizeToScene();
            centerOnOwnerWhenShown(stage, owner);
            stage.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
