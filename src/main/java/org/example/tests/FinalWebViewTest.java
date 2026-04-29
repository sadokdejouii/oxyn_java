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
 * Test final du fix WebView avec User-Agent Chrome
 */
public class FinalWebViewTest extends Application {
    
    private WebView webView;
    private TextField urlField;
    private Label statusLabel;
    
    @Override
    public void start(Stage primaryStage) {
        System.out.println("🔥 Test Final - Fix Erreur 153 avec User-Agent Chrome");
        
        // Créer l'interface
        VBox root = new VBox(15);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #0D1B3E;");
        
        // Contrôles
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);
        
        urlField = new TextField();
        urlField.setPromptText("URL YouTube ou ID vidéo");
        urlField.setStyle("-fx-pref-width: 400px; -fx-font-size: 14px; -fx-padding: 8px;");
        urlField.setText("https://www.youtube.com/watch?v=dQw4w9WgXcQ"); // Test par défaut
        
        Button loadButton = new Button("🔥 Test Fix");
        loadButton.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px;");
        loadButton.setOnAction(e -> loadVideoWithFix());
        
        Button compareButton = new Button("⚖️ Comparer");
        compareButton.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px;");
        compareButton.setOnAction(e -> compareWithAndWithoutFix());
        
        controls.getChildren().addAll(urlField, loadButton, compareButton);
        
        // Status
        statusLabel = new Label("🔥 Prêt pour test avec User-Agent Chrome");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        // WebView pour la vidéo
        webView = new WebView();
        webView.setPrefSize(800, 450);
        
        root.getChildren().addAll(controls, statusLabel, webView);
        
        // Scene
        Scene scene = new Scene(root, 830, 600);
        primaryStage.setTitle("🔥 Fix Erreur 153 - User-Agent Chrome Test");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        System.out.println("✅ Interface prête - Test du fix WebView");
    }
    
    private void loadVideoWithFix() {
        String url = urlField.getText().trim();
        String videoId = extractVideoId(url);
        
        if (videoId == null) {
            updateStatus("❌ URL invalide");
            return;
        }
        
        updateStatus("🔥 Test AVEC User-Agent Chrome pour: " + videoId);
        
        // 🔥 FIX CRITIQUE - Configuration WebView avec User-Agent Chrome
        webView.getEngine().setJavaScriptEnabled(true);
        webView.getEngine().setUserAgent(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        );
        
        // 🔥 Option pro - garantir Chrome dans User-Agent
        String currentUA = webView.getEngine().getUserAgent();
        if (!currentUA.contains("Chrome")) {
            webView.getEngine().setUserAgent(currentUA + " Chrome");
        }
        
        // Désactiver menu contextuel
        webView.setContextMenuEnabled(false);
        
        // HTML propre avec youtube-nocookie.com
        String html = String.format("""
            <html>
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
            html, body {
                margin: 0; padding: 0; width: 100%%; height: 100%%;
                background: black; overflow: hidden;
            }
            iframe {
                width: 100%%; height: 100%%; border: none;
                position: absolute; top: 0; left: 0;
            }
            </style>
            </head>
            <body>
            <iframe
            src="https://www.youtube-nocookie.com/embed/%s?autoplay=1&mute=1&controls=0&modestbranding=1&rel=0&iv_load_policy=3&fs=0&disablekb=1"
            frameborder="0"
            allow="autoplay; encrypted-media"
            allowfullscreen>
            </iframe>
            </body>
            </html>
            """, videoId);
        
        webView.getEngine().loadContent(html);
        
        System.out.println("🔥 Vidéo chargée AVEC FIX: " + videoId);
        System.out.println("🔥 User-Agent: " + webView.getEngine().getUserAgent());
        
        updateStatus("✅ Test AVEC fix terminé - Regarde si erreur 153 disparaît");
    }
    
    private void compareWithAndWithoutFix() {
        updateStatus("⚖️ Test comparatif AVEC/SANS fix...");
        
        String videoId = extractVideoId(urlField.getText());
        if (videoId == null) {
            updateStatus("❌ URL invalide pour comparaison");
            return;
        }
        
        // Test 1: SANS User-Agent Chrome (probable Erreur 153)
        System.out.println("\n🧪 Test 1: SANS User-Agent Chrome");
        webView.getEngine().setJavaScriptEnabled(true);
        webView.getEngine().setUserAgent("JavaFX/8.0"); // User-Agent de base
        
        String html1 = String.format("""
            <html><body style='background:#222;color:white;padding:20px;'>
            <h2>🧪 Test SANS User-Agent Chrome</h2>
            <p>User-Agent: %s</p>
            <p>Chargement vidéo %s...</p>
            <iframe width="300" height="200"
            src="https://www.youtube.com/embed/%s?autoplay=0"
            frameborder="0"></iframe>
            </body></html>
            """, webView.getEngine().getUserAgent(), videoId, videoId);
        
        webView.getEngine().loadContent(html1);
        
        // Attendre 3 secondes puis tester avec fix
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                
                // Test 2: AVEC User-Agent Chrome
                System.out.println("\n🔥 Test 2: AVEC User-Agent Chrome");
                
                javafx.application.Platform.runLater(() -> {
                    webView.getEngine().setUserAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    );
                    
                    String html2 = String.format("""
                        <html><body style='background:#111;color:white;padding:20px;'>
                        <h2>🔥 Test AVEC User-Agent Chrome</h2>
                        <p>User-Agent: %s</p>
                        <p>Chargement vidéo %s...</p>
                        <iframe width="100%%" height="400"
                        src="https://www.youtube-nocookie.com/embed/%s?autoplay=1&mute=1&controls=0&modestbranding=1"
                        frameborder="0"></iframe>
                        </body></html>
                        """, webView.getEngine().getUserAgent(), videoId, videoId);
                    
                    webView.getEngine().loadContent(html2);
                    updateStatus("🔥 Comparaison terminée - Vérifie les différences !");
                });
                
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
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
