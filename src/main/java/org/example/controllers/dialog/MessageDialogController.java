package org.example.controllers.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MessageDialogController {

    @FXML
    private BorderPane dialogRoot;

    @FXML
    private Label titleLabel;

    @FXML
    private Label messageLabel;

    private Stage stage;

    public void setup(Stage stage, String title, String message, boolean error) {
        this.stage = stage;
        titleLabel.setText(title);
        messageLabel.setText(message);
        if (error) {
            dialogRoot.getStyleClass().add("dialog-error-accent");
        }
    }

    @FXML
    private void handleClose() {
        if (stage != null) {
            stage.close();
        }
    }
}
