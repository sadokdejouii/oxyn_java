package org.example.controllers.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ConfirmDialogController {

    @FXML
    private Label titleLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private Button confirmButton;

    private Stage stage;
    private boolean confirmed;

    public void setup(Stage stage, String title, String message, String confirmText, boolean dangerStyle) {
        this.stage = stage;
        this.confirmed = false;
        titleLabel.setText(title);
        messageLabel.setText(message);
        if (confirmText != null && !confirmText.isBlank()) {
            confirmButton.setText(confirmText);
        }
        confirmButton.getStyleClass().setAll(dangerStyle ? "dialog-btn-danger" : "dialog-btn-primary");
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    @FXML
    private void handleConfirm() {
        confirmed = true;
        if (stage != null) {
            stage.close();
        }
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        if (stage != null) {
            stage.close();
        }
    }
}
