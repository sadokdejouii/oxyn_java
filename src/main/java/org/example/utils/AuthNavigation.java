package org.example.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Bascule entre les écrans d'authentification (Login / Register).
 */
public final class AuthNavigation {

    private AuthNavigation() {
    }

    public static void showLogin(Stage stage) throws IOException {
        Objects.requireNonNull(stage);
        Parent root = FXMLLoader.load(AuthNavigation.class.getResource("/FXML/Login.fxml"));
        stage.setScene(new Scene(root, 1080, 720));
        stage.setTitle("OXYN — Connexion");
    }

    public static void showRegister(Stage stage) throws IOException {
        Objects.requireNonNull(stage);
        Parent root = FXMLLoader.load(AuthNavigation.class.getResource("/FXML/Register.fxml"));
        stage.setScene(new Scene(root, 1080, 780));
        stage.setTitle("OXYN — Inscription");
    }
}
