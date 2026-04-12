package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.services.SessionContext;
import org.example.services.UserRole;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleLogin(ActionEvent event) {
        String user = usernameField != null ? usernameField.getText().trim() : "";
        if (user.isEmpty()) {
            user = "client";
        }
        UserRole role;
        String display;
        if (user.equalsIgnoreCase("admin")) {
            role = UserRole.ADMIN;
            display = "Administrator";
        } else if (user.equalsIgnoreCase("encadrant")) {
            role = UserRole.ENCADRANT;
            display = "Encadrant";
        } else {
            role = UserRole.CLIENT;
            display = capitalize(user);
        }
        SessionContext.getInstance().login(display, role);

        try {
            Node source = usernameField != null ? usernameField : passwordField;
            Stage stage = (Stage) source.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/MainLayout.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1530, 880);
            stage.setScene(scene);
            stage.setTitle("OXYN — " + role.displayLabel());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return "Client";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
