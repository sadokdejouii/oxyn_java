package org.example.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Helper pour intégrer le lecteur vidéo dans l'application
 */
public class VideoPlayerHelper {
    
    /**
     * Ouvre le lecteur vidéo dans une nouvelle fenêtre
     */
    public static void openVideoPlayer() {
        try {
            FXMLLoader loader = new FXMLLoader(
                VideoPlayerHelper.class.getResource("/fxml/VideoPlayerPage.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("🎬 Lecteur Vidéo Salle");
            stage.setScene(new Scene(root, 1000, 700));
            stage.initModality(Modality.NONE);
            stage.initStyle(StageStyle.DECORATED);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.show();
            
        } catch (Exception e) {
            System.err.println("Erreur ouverture lecteur vidéo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ouvre le lecteur vidéo avec une vidéo pré-chargée
     */
    public static void openVideoPlayerWithVideo(String videoId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                VideoPlayerHelper.class.getResource("/FXML/VideoPlayerPage.fxml"));
            Parent root = loader.load();
            
            org.example.controllers.VideoPlayerController controller = loader.getController();
            
            Stage stage = new Stage();
            stage.setTitle("🎬 Lecteur Vidéo Salle");
            stage.setScene(new Scene(root, 1000, 700));
            stage.initModality(Modality.NONE);
            stage.initStyle(StageStyle.DECORATED);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.show();
            
            // ⚠️ CORRECTION : Charger la vidéo APRÈS que le stage soit visible
            // pour éviter les conflits d'initialisation JCEF
            javafx.application.Platform.runLater(() -> {
                controller.loadVideoById(videoId);
            });
            
        } catch (Exception e) {
            System.err.println("Erreur ouverture lecteur vidéo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crée un bouton pour ouvrir le lecteur vidéo
     */
    public static javafx.scene.control.Button createVideoPlayerButton() {
        javafx.scene.control.Button button = new javafx.scene.control.Button("🎬 Lecteur Vidéo");
        button.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white; " +
                       "-fx-font-weight: bold; -fx-background-radius: 5; " +
                       "-fx-pref-height: 40px; -fx-pref-width: 150px;");
        button.setOnAction(e -> openVideoPlayer());
        return button;
    }
    
    /**
     * Crée un bouton pour ouvrir le lecteur avec une vidéo spécifique
     */
    public static javafx.scene.control.Button createVideoButton(String videoId, String videoTitle) {
        javafx.scene.control.Button button = new javafx.scene.control.Button("▶️ " + videoTitle);
        button.setStyle("-fx-background-color: #00BFFF; -fx-text-fill: white; " +
                       "-fx-font-weight: bold; -fx-background-radius: 5; " +
                       "-fx-pref-height: 35px;");
        button.setOnAction(e -> openVideoPlayerWithVideo(videoId));
        return button;
    }
}
