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
        Scene scene = new Scene(root, 1080, 720);
        AppStyles.apply(scene);
        stage.setScene(scene);
        stage.setTitle("OXYN — Connexion");
        PrimaryStageLayout.applyFullScreen(stage);
    }

    public static void showRegister(Stage stage) throws IOException {
        Objects.requireNonNull(stage);
        Parent root = FXMLLoader.load(AuthNavigation.class.getResource("/FXML/Register.fxml"));
        Scene scene = new Scene(root, 1080, 780);
        AppStyles.apply(scene);
        stage.setScene(scene);
        stage.setTitle("OXYN — Inscription");
        PrimaryStageLayout.applyFullScreen(stage);
    }
}
