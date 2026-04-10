package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import org.example.services.SessionContext;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfilePageController implements Initializable {

    @FXML
    private Label nameValue;

    @FXML
    private Label roleValue;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SessionContext ctx = SessionContext.getInstance();
        nameValue.setText(ctx.getDisplayName());
        roleValue.setText(ctx.getRole().displayLabel());
    }

    @FXML
    private void handleSave() {
        info("Save", "Profile persistence can be wired to your user store.");
    }

    @FXML
    private void handleSecurity() {
        info("Security", "MFA, password rotation, and device sessions can plug in here.");
    }

    private static void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
