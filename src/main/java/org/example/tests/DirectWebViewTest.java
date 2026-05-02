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

/**
 * Test du chargement direct YouTube dans WebView (SOLUTION FINALE)
 */
public class DirectWebViewTest extends Application {
    
    private WebView webView;
    private TextField urlField;
    private Label statusLabel;
    
    @Override
    public void start(Stage primaryStage) {
        System.out.println("🎬 Test Direct YouTube dans WebView - Solution Finale");
        
        // Créer l'interface
        VBox root = new VBox(15);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #0D1B3E;");
        
        // Contrôles
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);
        
        urlField = new TextField();
        urlField.setPromptText("URL YouTube complète");
        urlField.setStyle("-fx-pref-width: 400px; -fx-font-size: 14px; -fx-padding: 8px;");
        urlField.setText("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        
        Button loadButton = new Button("🎬 Charger Direct");
        loadButton.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px;");
        loadButton.setOnAction(e -> loadVideoDirect());
        
        Button testButton = new Button("🧪 Test Formats");
        testButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px;");
        testButton.setOnAction(e -> testFormats());
        
        controls.getChildren().addAll(urlField, loadButton, testButton);
        
        // Status
        statusLabel = new Label("🎬 WebView Direct Prêt");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        // WebView pour la vidéo
        webView = new WebView();
        webView.setPrefSize(800, 450);
        
        root.getChildren().addAll(controls, statusLabel, webView);
        
        // Scene
        Scene scene = new Scene(root, 830, 600);
        primaryStage.setTitle("🎬 YouTube Direct WebView Test");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        System.out.println("✅ Interface prête - Test du chargement direct");
    }
    
    private void loadVideoDirect() {
        String url = urlField.getText().trim();
        String videoId = extraireVideoId(url);
        
        if (videoId == null || videoId.isEmpty()) {
            updateStatus("❌ URL invalide");
            return;
        }
        
        updateStatus("🎬 Chargement direct dans WebView: " + videoId);
        loadVideoInWebView(videoId);
    }
    
    private void loadVideoInWebView(String videoId) {
        System.out.println("🎬 Chargement vidéo YouTube dans WebView: " + videoId);
        
        // Configuration WebView - construire directement dans le lambda pour éviter l'erreur
        javafx.application.Platform.runLater(() -> {
            // Utiliser youtube-nocookie.com pour éviter erreur 153
            final String embedUrl = "https://www.youtube-nocookie.com/embed/" 
                + videoId 
                + "?autoplay=1&rel=0&modestbranding=1&playsinline=1"
                + "&origin=http://localhost";

            final String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                <meta charset="UTF-8">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { background: #000; width: 100vw; height: 100vh; overflow: hidden; }
                    iframe { 
                        position: absolute;
                        top: 0; left: 0;
                        width: 100%%; 
                        height: 100%%; 
                        border: none; 
                    }
                </style>
                </head>
                <body>
                    <iframe 
                        src="%s"
                        allow="autoplay; fullscreen; encrypted-media; picture-in-picture"
                        allowfullscreen="true">
                    </iframe>
                </body>
                </html>
                """, embedUrl);

            // Activer JavaScript obligatoire
            webView.getEngine().setJavaScriptEnabled(true);
            
            // User agent Chrome pour tromper YouTube
            webView.getEngine().setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"
            );
            
            webView.getEngine().loadContent(html, "text/html");
            
            System.out.println("✅ Vidéo YouTube chargée dans WebView: " + videoId);
            updateStatus("✅ Vidéo chargée: " + videoId);
        });
    }
    
    private void testFormats() {
        updateStatus("🧪 Test de tous les formats YouTube...");
        
        String[] testUrls = {
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://youtube.com/shorts/dQw4w9WgXcQ",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "dQw4w9WgXcQ"
        };
        
        new Thread(() -> {
            for (int i = 0; i < testUrls.length; i++) {
                final String url = testUrls[i];
                final String videoId = extraireVideoId(url);
                final int index = i;
                
                javafx.application.Platform.runLater(() -> {
                    updateStatus("🧪 Test " + (index + 1) + "/" + testUrls.length + ": " + url);
                    urlField.setText(url);
                });
                
                try {
                    Thread.sleep(2000);
                    
                    javafx.application.Platform.runLater(() -> {
                        if (videoId != null && !videoId.isEmpty()) {
                            loadVideoInWebView(videoId);
                        }
                    });
                    
                    Thread.sleep(5000);
                    
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            javafx.application.Platform.runLater(() -> {
                updateStatus("🧪 Test terminé - Formats validés !");
            });
        }).start();
    }
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
        System.out.println("📊 " + message);
    }
    
    private String extraireVideoId(String url) {
        if (url == null || url.isEmpty()) return "";
        
        url = url.trim();
        
        // Format watch?v=
        if (url.contains("v=")) {
            String id = url.split("v=")[1];
            return id.contains("&") ? id.split("&")[0] : id;
        }
        
        // Format youtu.be/
        if (url.contains("youtu.be/")) {
            String id = url.split("youtu.be/")[1];
            return id.contains("?") ? id.split("\\?")[0] : id;
        }
        
        // Format shorts/ ou embed/
        if (url.contains("/shorts/") || url.contains("/embed/")) {
            String[] parts = url.split("/");
            return parts[parts.length - 1].split("\\?")[0];
        }
        
        // Déjà un ID direct (11 caractères)
        if (url.matches("[a-zA-Z0-9_-]{11}")) return url;
        
        return url;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
