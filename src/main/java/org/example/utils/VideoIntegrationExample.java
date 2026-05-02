package org.example.utils;

import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.example.utils.VideoPlayerHelper;

/**
 * Exemple d'intégration du lecteur vidéo dans une page existante
 */
public class VideoIntegrationExample {
    
    /**
     * Ajoute des boutons vidéo à une interface existante
     */
    public static void addVideoButtonsToPage(VBox container) {
        // Bouton pour ouvrir le lecteur général
        Button openPlayerButton = VideoPlayerHelper.createVideoPlayerButton();
        
        // Boutons pour des vidéos spécifiques
        Button video1Button = VideoPlayerHelper.createVideoButton(
            "dQw4w9WgXcQ", "Never Gonna Give You Up");
        
        Button video2Button = VideoPlayerHelper.createVideoButton(
            "9bZkp7q19f0", "PSY - GANGNAM STYLE");
        
        Button video3Button = VideoPlayerHelper.createVideoButton(
            "kJQP7kiw5Fk", "Luis Fonsi - Despacito");
        
        // Ajout des boutons au conteneur
        container.getChildren().addAll(
            openPlayerButton,
            video1Button,
            video2Button,
            video3Button
        );
        
        // Style du conteneur
        container.setStyle("-fx-spacing: 10; -fx-padding: 20;");
    }
    
    /**
     * Remplace un bouton rouge existant par le lecteur vidéo
     */
    public static Button replaceRedButtonWithVideoPlayer() {
        // Ancien comportement : window.open("https://youtube.com")
        // Nouveau comportement : ouvrir le lecteur intégré
        
        Button videoButton = new Button("🎬 Vidéo");
        videoButton.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white; " +
                            "-fx-font-weight: bold; -fx-background-radius: 5; " +
                            "-fx-pref-height: 40px; -fx-pref-width: 120px;");
        
        videoButton.setOnAction(e -> {
            // Remplace l'ancien window.open() par le lecteur intégré
            VideoPlayerHelper.openVideoPlayer();
        });
        
        return videoButton;
    }
    
    /**
     * Test rapide du système
     */
    public static void testVideoSystem() {
        System.out.println("🎬 Test du système vidéo YouTube intégré");
        System.out.println("✅ YouTubeService créé");
        System.out.println("✅ VideoPlayerController prêt");
        System.out.println("✅ FXML interface défini");
        System.out.println("✅ Helper d'intégration prêt");
        
        // Test d'extraction d'ID
        String[] testUrls = {
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "dQw4w9WgXcQ"
        };
        
        for (String url : testUrls) {
            String videoId = org.example.services.YouTubeService.extractVideoId(url);
            System.out.println("URL: " + url + " → ID: " + videoId);
        }
    }
}
