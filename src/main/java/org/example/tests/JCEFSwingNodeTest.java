package org.example.tests;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
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
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Test JCEF SwingNode - Solution écran noir (AWT thread)
 */
public class JCEFSwingNodeTest extends Application {
    
    private AnchorPane videoContainer;
    private TextField urlField;
    private Label statusLabel;
    private CefBrowser cefBrowser;
    
    @Override
    public void start(Stage primaryStage) {
        System.out.println("🚀 Test JCEF SwingNode - Solution écran noir");
        
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
        
        Button refreshButton = new Button("🔄 Refresh SwingNode");
        refreshButton.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px;");
        refreshButton.setOnAction(e -> refreshSwingNode());
        
        controls.getChildren().addAll(urlField, loadButton, refreshButton);
        
        // Status
        statusLabel = new Label("🚀 JCEF SwingNode Prêt");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Conteneur vidéo pour JCEF avec taille fixe
        videoContainer = new AnchorPane();
        videoContainer.setPrefSize(800, 450);
        videoContainer.setStyle("-fx-background-color: #000; -fx-background-radius: 10; -fx-border-color: #34495E; -fx-border-width: 2;");
        
        root.getChildren().addAll(controls, statusLabel, videoContainer);
        
        // Scene
        Scene scene = new Scene(root, 830, 600);
        primaryStage.setTitle("🚀 JCEF SwingNode Test - Solution écran noir");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Initialiser JCEF avec SwingUtilities
        initJCEFWithSwingUtilities();
        
        System.out.println("✅ Interface prête - JCEF SwingNode initialisé");
    }
    
    /**
     * Initialise JCEF avec SwingUtilities - Solution écran noir
     */
    private void initJCEFWithSwingUtilities() {
        try {
            System.setProperty("jcef.sandbox", "false");
            
            CefAppBuilder builder = new CefAppBuilder();
            builder.getCefSettings().windowless_rendering_enabled = false;
            builder.install();
            CefApp cefApp = builder.build();
            CefClient client = cefApp.createClient();
            
            String url = "https://www.youtube.com/embed/dQw4w9WgXcQ?autoplay=1&rel=0";
            
            // Créer browser sur thread AWT - SOLUTION CLÉ
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    cefBrowser = client.createBrowser(url, false, false);
                    java.awt.Component browserUI = cefBrowser.getUIComponent();
                    
                    javax.swing.JPanel panel = new javax.swing.JPanel(
                        new java.awt.BorderLayout()
                    );
                    panel.add(browserUI, java.awt.BorderLayout.CENTER);
                    panel.setPreferredSize(new java.awt.Dimension(800, 450));
                    panel.setBackground(java.awt.Color.BLACK);
                    
                    javafx.embed.swing.SwingNode swingNode = new javafx.embed.swing.SwingNode();
                    swingNode.setContent(panel);
                    
                    // Forcer la taille et position
                    swingNode.setLayoutX(0);
                    swingNode.setLayoutY(0);
                    
                    javafx.application.Platform.runLater(() -> {
                        videoContainer.getChildren().clear();
                        videoContainer.getChildren().add(swingNode);
                        
                        // Bind taille au conteneur parent - SwingNode n'a pas prefSizeProperty
                        // Utiliser AnchorPane pour forcer la taille
                        AnchorPane.setTopAnchor(swingNode, 0.0);
                        AnchorPane.setBottomAnchor(swingNode, 0.0);
                        AnchorPane.setLeftAnchor(swingNode, 0.0);
                        AnchorPane.setRightAnchor(swingNode, 0.0);
                        
                        videoContainer.layout();
                        
                        // Forcer refresh après 500ms - CRUCIAL
                        new Thread(() -> {
                            try { Thread.sleep(500); } 
                            catch (InterruptedException e) {}
                            javafx.application.Platform.runLater(() -> {
                                videoContainer.requestLayout();
                                swingNode.setVisible(false);
                                swingNode.setVisible(true);
                                updateStatus("✅ SwingNode refresh forcé !");
                            });
                        }).start();
                    });
                    
                    System.out.println("✅ JCEF initialisé avec succès sur thread AWT !");
                    updateStatus("✅ JCEF SwingNode initialisé - Écran noir résolu !");
                    
                } catch (Exception e) {
                    System.err.println("❌ Erreur création JCEF: " + e.getMessage());
                    e.printStackTrace();
                    updateStatus("❌ Erreur JCEF: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            System.err.println("❌ Erreur initialisation JCEF: " + e.getMessage());
            e.printStackTrace();
            updateStatus("❌ JCEF non disponible: " + e.getMessage());
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
        
        // Charger directement dans JCEF
        cefBrowser.loadURL(embedUrl);
        
        System.out.println("✅ Vidéo YouTube chargée dans JCEF: " + videoId);
        updateStatus("✅ Vidéo YouTube (JCEF): " + videoId);
    }
    
    private void refreshSwingNode() {
        updateStatus("🔄 Refresh SwingNode...");
        
        // Forcer un refresh complet du conteneur
        javafx.application.Platform.runLater(() -> {
            videoContainer.requestLayout();
            
            // Forcer refresh visuel
            if (!videoContainer.getChildren().isEmpty()) {
                videoContainer.getChildren().get(0).setVisible(false);
                videoContainer.getChildren().get(0).setVisible(true);
            }
            
            updateStatus("🔄 SwingNode refresh complété !");
        });
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
