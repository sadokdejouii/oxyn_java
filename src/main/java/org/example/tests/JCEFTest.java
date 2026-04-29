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
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import me.friwi.jcefmaven.*;
import org.cef.*;
import org.cef.browser.*;

/**
 * Test JCEF (Chromium embarqué) pour YouTube - Solution définitive Erreur 153
 */
public class JCEFTest extends Application {
    
    private AnchorPane videoContainer;
    private TextField urlField;
    private Label statusLabel;
    private CefBrowser cefBrowser;
    
    @Override
    public void start(Stage primaryStage) {
        System.out.println("🚀 Test JCEF (Chromium) - Solution définitive Erreur 153");
        
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
        
        Button loadButton = new Button("🚀 Charger JCEF");
        loadButton.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px;");
        loadButton.setOnAction(e -> loadVideoWithJCEF());
        
        Button testButton = new Button("🧪 Test YouTube");
        testButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px;");
        testButton.setOnAction(e -> testYouTubeDirect());
        
        controls.getChildren().addAll(urlField, loadButton, testButton);
        
        // Status
        statusLabel = new Label("🚀 JCEF Prêt - Plus d'erreur 153 !");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Conteneur vidéo pour JCEF
        videoContainer = new AnchorPane();
        videoContainer.setPrefSize(800, 450);
        videoContainer.setStyle("-fx-background-color: #000; -fx-background-radius: 10; -fx-border-color: #34495E; -fx-border-width: 2;");
        
        root.getChildren().addAll(controls, statusLabel, videoContainer);
        
        // Scene
        Scene scene = new Scene(root, 830, 600);
        primaryStage.setTitle("🚀 JCEF Chromium Test - Fin de l'erreur 153");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Initialiser JCEF
        initJCEF();
        
        System.out.println("✅ Interface prête - JCEF initialisé");
    }
    
    /**
     * Initialise JCEF (Chromium embarqué)
     */
    private void initJCEF() {
        try {
            System.out.println("🚀 Initialisation JCEF (Chromium)...");
            
            CefAppBuilder builder = new CefAppBuilder();
            builder.install(); // télécharge Chromium si absent
            CefApp cefApp = builder.build();
            CefClient client = cefApp.createClient();
            
            // Créer le navigateur avec une page par défaut
            String defaultUrl = "about:blank";
            cefBrowser = client.createBrowser(defaultUrl, false, false);
            
            java.awt.Component canvas = cefBrowser.getUIComponent();
            javafx.embed.swing.SwingNode swingNode = 
                new javafx.embed.swing.SwingNode();
                
            swingNode.setContent(new javax.swing.JPanel() {{
                setLayout(new java.awt.BorderLayout());
                add(canvas, java.awt.BorderLayout.CENTER);
            }});
            
            javafx.application.Platform.runLater(() -> {
                videoContainer.getChildren().clear();
                videoContainer.getChildren().add(swingNode);
                AnchorPane.setTopAnchor(swingNode, 0.0);
                AnchorPane.setBottomAnchor(swingNode, 0.0);
                AnchorPane.setLeftAnchor(swingNode, 0.0);
                AnchorPane.setRightAnchor(swingNode, 0.0);
            });
            
            System.out.println("✅ JCEF initialisé avec succès !");
            updateStatus("🚀 JCEF Chromium prêt - Plus d'erreur 153 !");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur initialisation JCEF: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: afficher message d'erreur
            javafx.application.Platform.runLater(() -> {
                Label errorLabel = new Label("❌ JCEF non disponible\n" + e.getMessage());
                errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16px; -fx-alignment: center;");
                videoContainer.getChildren().clear();
                videoContainer.getChildren().add(errorLabel);
                updateStatus("❌ JCEF non disponible");
            });
        }
    }
    
    private void loadVideoWithJCEF() {
        String url = urlField.getText().trim();
        String videoId = extractVideoId(url);
        
        if (videoId == null || videoId.isEmpty()) {
            updateStatus("❌ URL invalide");
            return;
        }
        
        updateStatus("🚀 Chargement YouTube avec JCEF: " + videoId);
        
        if (cefBrowser == null) {
            updateStatus("❌ JCEF non initialisé");
            return;
        }
        
        // Construire l'URL YouTube embed pour JCEF
        String embedUrl = "https://www.youtube.com/embed/" 
            + videoId 
            + "?autoplay=1&rel=0&modestbranding=1&playsinline=1"
            + "&controls=1&fs=1";
        
        // Charger directement dans JCEF (Chromium supporte YouTube parfaitement)
        cefBrowser.loadURL(embedUrl);
        
        System.out.println("✅ Vidéo YouTube chargée dans JCEF: " + videoId);
        updateStatus("✅ Vidéo YouTube (JCEF): " + videoId);
    }
    
    private void testYouTubeDirect() {
        updateStatus("🧪 Test direct YouTube dans JCEF...");
        
        if (cefBrowser != null) {
            // Test avec une vidéo connue
            String testUrl = "https://www.youtube.com/embed/dQw4w9WgXcQ?autoplay=1&controls=1";
            cefBrowser.loadURL(testUrl);
            updateStatus("🧪 Test YouTube en cours...");
            
            System.out.println("🧪 Test URL: " + testUrl);
        } else {
            updateStatus("❌ JCEF non disponible pour le test");
        }
    }
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
        System.out.println("📊 " + message);
    }
    
    private String extractVideoId(String url) {
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
