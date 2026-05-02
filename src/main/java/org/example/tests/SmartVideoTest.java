package org.example.tests;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.example.controllers.VideoPlayerController;
import org.example.services.YouTubeService;

import java.util.Arrays;
import java.util.List;

/**
 * Test du lecteur intelligent avec détection embeddable
 */
public class SmartVideoTest extends Application {
    
    private VideoPlayerController controller;
    private WebView webView;
    private TextField urlField;
    private Label statusLabel;
    private YouTubeService youTubeService;
    
    @Override
    public void start(Stage primaryStage) {
        System.out.println("🧠 Test Lecteur Intelligent - Détection Embeddable");
        
        // Créer l'interface
        VBox root = new VBox(15);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #1a1a2e;");
        
        // Services
        youTubeService = new YouTubeService();
        controller = new VideoPlayerController();
        
        // Contrôles
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);
        
        urlField = new TextField();
        urlField.setPromptText("URL YouTube complète");
        urlField.setStyle("-fx-pref-width: 400px; -fx-font-size: 14px; -fx-padding: 8px;");
        
        Button testButton = new Button("🧠 Test Intelligent");
        testButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px;");
        testButton.setOnAction(e -> testSmartVideo());
        
        Button embedTestButton = new Button("🔍 Test Embed");
        embedTestButton.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px;");
        embedTestButton.setOnAction(e -> testEmbeddableOnly());
        
        Button playlistTestButton = new Button("🎵 Playlist Test");
        playlistTestButton.setStyle("-fx-background-color: #2ECC71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px;");
        playlistTestButton.setOnAction(e -> testPlaylist());
        
        controls.getChildren().addAll(urlField, testButton, embedTestButton, playlistTestButton);
        
        // Status
        statusLabel = new Label("🧠 Lecteur Intelligent Prêt");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        // WebView pour la vidéo
        webView = new WebView();
        webView.setPrefSize(800, 450);
        webView.getEngine().setJavaScriptEnabled(true);
        
        root.getChildren().addAll(controls, statusLabel, webView);
        
        // Scene
        Scene scene = new Scene(root, 830, 600);
        primaryStage.setTitle("🧠 Lecteur YouTube Intelligent - Test Embeddable");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        System.out.println("✅ Interface prête - Test du lecteur intelligent");
    }
    
    private void testSmartVideo() {
        String url = urlField.getText().trim();
        String videoId = extractVideoId(url);
        
        if (videoId == null) {
            updateStatus("❌ URL invalide");
            return;
        }
        
        updateStatus("🧠 Test intelligent pour: " + videoId);
        
        // Simuler la méthode playVideoSmart
        boolean embeddable = youTubeService.isEmbeddable(videoId);
        
        if (embeddable) {
            updateStatus("✅ Vidéo embeddable - Chargement dans WebView");
            loadVideoInWebView(videoId);
        } else {
            updateStatus("⚠️ Vidéo non embeddable - Fallback navigateur");
            showFallbackMessage(videoId, url);
        }
    }
    
    private void testEmbeddableOnly() {
        String url = urlField.getText().trim();
        String videoId = extractVideoId(url);
        
        if (videoId == null) {
            updateStatus("❌ URL invalide");
            return;
        }
        
        updateStatus("🔍 Vérification embeddable pour: " + videoId);
        
        boolean embeddable = youTubeService.isEmbeddable(videoId);
        
        if (embeddable) {
            updateStatus("✅ EMBEDDABLE - Peut être affichée");
        } else {
            updateStatus("❌ NON EMBEDDABLE - Erreur 153 si chargée");
        }
    }
    
    private void testPlaylist() {
        updateStatus("🎵 Test playlist intelligente");
        
        // Playlist de test avec vidéos connues
        List<String> testPlaylist = Arrays.asList(
            "dQw4w9WgXcQ",  // Rick Roll (embeddable)
            "hTWKbfovygM", // Video Games (embeddable)
            "9bZkp7q19f0", // Gangnam Style (embeddable)
            "abc123def456"  // ID invalide (non embeddable)
        );
        
        System.out.println("🎵 Playlist test: " + testPlaylist.size() + " vidéos");
        
        // Tester chaque vidéo
        int validCount = 0;
        for (int i = 0; i < testPlaylist.size(); i++) {
            String videoId = testPlaylist.get(i);
            boolean embeddable = youTubeService.isEmbeddable(videoId);
            
            if (embeddable) {
                validCount++;
                System.out.println("✅ " + (i + 1) + ". " + videoId + " -> EMBEDDABLE");
            } else {
                System.out.println("❌ " + (i + 1) + ". " + videoId + " -> NON EMBEDDABLE");
            }
        }
        
        updateStatus("🎵 Playlist testée: " + validCount + "/" + testPlaylist.size() + " vidéos valides");
        
        // Charger la première vidéo valide
        for (String videoId : testPlaylist) {
            if (youTubeService.isEmbeddable(videoId)) {
                loadVideoInWebView(videoId);
                updateStatus("🎬 Première vidéo valide chargée: " + videoId);
                return;
            }
        }
        
        updateStatus("❌ Aucune vidéo valide dans la playlist");
    }
    
    private void loadVideoInWebView(String videoId) {
        String html = String.format("""
            <html>
            <head>
            <style>
            html, body {
                margin: 0; padding: 0; width: 100%%; height: 100%%;
                background: black; overflow: hidden;
            }
            iframe { width: 100%%; height: 100%%; border: none; }
            </style>
            </head>
            <body>
            <iframe
            src="https://www.youtube-nocookie.com/embed/%s?autoplay=1&mute=1&controls=0&modestbranding=1&rel=0"
            frameborder="0"
            allow="autoplay; encrypted-media"
            allowfullscreen>
            </iframe>
            </body>
            </html>
            """, videoId);
        
        webView.getEngine().setJavaScriptEnabled(true);
        webView.getEngine().loadContent(html);
        
        System.out.println("🎬 Vidéo chargée dans WebView: " + videoId);
    }
    
    private void showFallbackMessage(String videoId, String url) {
        String fallbackHtml = String.format("""
            <html>
            <head>
            <style>
            body {
                margin: 0; padding: 20px; background: #1a1a2e; color: white;
                font-family: Arial, sans-serif; text-align: center;
            }
            .warning { color: #f39c12; font-size: 24px; margin-bottom: 20px; }
            .info { color: #ecf0f1; font-size: 16px; margin-bottom: 30px; }
            .button {
                background: #3498DB; color: white; padding: 15px 30px;
                text-decoration: none; border-radius: 5px; font-weight: bold;
            }
            </style>
            </head>
            <body>
            <div class="warning">⚠️ VIDÉO NON EMBEDDABLE</div>
            <div class="info">
                La vidéo %s ne peut pas être affichée ici.<br>
                Erreur 153 - Configuration du lecteur vidéo<br><br>
                <a href="%s" class="button" target="_blank">Ouvrir dans YouTube</a>
            </div>
            </body>
            </html>
            """, videoId, url);
        
        webView.getEngine().loadContent(fallbackHtml);
    }
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
        System.out.println("📊 " + message);
    }
    
    private String extractVideoId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        url = url.trim();
        
        // ID direct
        if (url.matches("[a-zA-Z0-9_-]{11}")) {
            return url;
        }
        
        // URLs YouTube
        if (url.contains("youtube.com/watch?v=")) {
            return url.substring(url.indexOf("v=") + 2, url.indexOf("v=") + 13);
        }
        
        if (url.contains("youtu.be/")) {
            return url.substring(url.indexOf("youtu.be/") + 9, url.indexOf("youtu.be/") + 20);
        }
        
        if (url.contains("youtube.com/embed/")) {
            return url.substring(url.indexOf("embed/") + 6, url.indexOf("embed/") + 17);
        }
        
        return null;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
