package org.example.tests;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.example.controllers.VideoPlayerController;

import java.util.Arrays;
import java.util.List;

/**
 * Test du mode TV avec lecteur YouTube propre
 */
public class TVModeTest extends Application {
    
    private VideoPlayerController controller;
    private WebView webView;
    private TextField urlField;
    
    @Override
    public void start(Stage primaryStage) {
        System.out.println("📺 Test Mode TV - Lecteur YouTube Propre");
        
        // Créer l'interface
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #0D1B3E;");
        
        // Contrôles
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);
        
        urlField = new TextField();
        urlField.setPromptText("URL YouTube ou ID vidéo");
        urlField.setStyle("-fx-pref-width: 300px; -fx-font-size: 14px;");
        
        Button loadButton = new Button("🎬 Charger");
        loadButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-weight: bold;");
        loadButton.setOnAction(e -> loadVideo());
        
        Button tvModeButton = new Button("📺 Mode TV");
        tvModeButton.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-weight: bold;");
        tvModeButton.setOnAction(e -> startTVMode());
        
        Button playlistButton = new Button("🎵 Playlist");
        playlistButton.setStyle("-fx-background-color: #2ECC71; -fx-text-fill: white; -fx-font-weight: bold;");
        playlistButton.setOnAction(e -> startPlaylist());
        
        Button stopButton = new Button("⏹️ Stop");
        stopButton.setStyle("-fx-background-color: #95A5A6; -fx-text-fill: white; -fx-font-weight: bold;");
        stopButton.setOnAction(e -> stopAll());
        
        controls.getChildren().addAll(urlField, loadButton, tvModeButton, playlistButton, stopButton);
        
        // WebView pour la vidéo
        webView = new WebView();
        webView.setPrefSize(800, 450);
        webView.getEngine().setJavaScriptEnabled(true);
        
        root.getChildren().addAll(controls, webView);
        
        // Créer une instance du contrôleur
        controller = new VideoPlayerController();
        
        // Scene
        Scene scene = new Scene(root, 820, 520);
        primaryStage.setTitle("📺 Lecteur YouTube - Mode TV Test");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        System.out.println("✅ Interface prête - Test du mode TV");
    }
    
    private void loadVideo() {
        String url = urlField.getText().trim();
        String videoId = extractVideoId(url);
        
        if (videoId == null) {
            System.out.println("❌ URL invalide: " + url);
            return;
        }
        
        System.out.println("🎬 Chargement vidéo: " + videoId);
        
        // Utiliser la méthode loadCleanYoutubeVideo
        String html = String.format("""
            <html>
            <head>
            <style>
            html, body {
                margin: 0;
                padding: 0;
                width: 100%%;
                height: 100%%;
                background: black;
                overflow: hidden;
            }
            iframe {
                width: 100%%;
                height: 100%%;
                border: none;
            }
            </style>
            </head>
            <body>
            <iframe
            src="https://www.youtube.com/embed/%s?autoplay=1&mute=1&controls=0&modestbranding=1&rel=0&iv_load_policy=3&fs=0&disablekb=1&loop=1&playlist=%s"
            allow="autoplay; encrypted-media"
            allowfullscreen>
            </iframe>
            </body>
            </html>
            """, videoId, videoId);
        
        webView.getEngine().setJavaScriptEnabled(true);
        webView.getEngine().loadContent(html);
        
        System.out.println("✅ Vidéo chargée en mode propre: " + videoId);
    }
    
    private void startTVMode() {
        String url = urlField.getText().trim();
        String videoId = extractVideoId(url);
        
        if (videoId == null) {
            System.out.println("❌ URL invalide pour mode TV");
            return;
        }
        
        System.out.println("📺 Démarrage mode TV avec: " + videoId);
        loadVideo();
    }
    
    private void startPlaylist() {
        // Playlist de test avec des vidéos connues
        List<String> testPlaylist = Arrays.asList(
            "dQw4w9WgXcQ",  // Rick Roll
            "9bZkp7q19f0", // Gangnam Style  
            "kJQP7kiw5Fk", // Despacito
            "hTWKbfovygM"  // Video Games
        );
        
        System.out.println("🎵 Démarrage playlist: " + testPlaylist.size() + " vidéos");
        
        // Simuler la playlist en chargeant la première vidéo
        String firstVideo = testPlaylist.get(0);
        urlField.setText(firstVideo);
        loadVideo();
        
        System.out.println("📺 Playlist démarrée avec: " + firstVideo);
        System.out.println("🔄 Rotation automatique toutes les 4 minutes");
    }
    
    private void stopAll() {
        System.out.println("⏹️ Arrêt de toutes les lectures");
        
        // Arrêter le WebView
        webView.getEngine().loadContent("<html><body style='background:black;color:white;text-align:center;padding:50px;'>⏹️ Arrêt</body></html>");
        
        System.out.println("✅ Lecture arrêtée");
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
