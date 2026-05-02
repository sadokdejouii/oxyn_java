package org.example.controllers.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class TotpCodeDialogController {

    @FXML
    private BorderPane dialogRoot;

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private ImageView qrView;

    @FXML
    private TextField codeField;

    @FXML
    private Label errorLabel;

    @FXML
    private Label secretHintLabel;

    private Stage stage;
    private boolean confirmed;

    public void setup(Stage stage, String title, String subtitle) {
        this.stage = stage;
        titleLabel.setText(title != null ? title : "2FA");
        subtitleLabel.setText(subtitle != null ? subtitle : "");
        confirmed = false;
        clearError();
    }

    public void setQr(Image img, String secretHint) {
        if (img != null && qrView != null) {
            qrView.setImage(img);
            qrView.setVisible(true);
            qrView.setManaged(true);
        }
        if (secretHint != null && !secretHint.isBlank() && secretHintLabel != null) {
            secretHintLabel.setText(secretHint);
            secretHintLabel.setVisible(true);
            secretHintLabel.setManaged(true);
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getCode() {
        return codeField != null ? codeField.getText() : "";
    }

    public void showError(String msg) {
        if (errorLabel == null) return;
        errorLabel.setText(msg != null ? msg : "Code invalide.");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    public void clearError() {
        if (errorLabel == null) return;
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void handleConfirm() {
        confirmed = true;
        if (stage != null) stage.close();
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        if (stage != null) stage.close();
    }
}

