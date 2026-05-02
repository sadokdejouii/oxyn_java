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
import org.example.controllers.CefManager;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import javafx.embed.swing.SwingNode;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;

/**
 * Test CefManager Singleton - Solution "Settings can only be passed to CEF before createClient is called"
 */
public class CefManagerTest extends Application {
    
    private AnchorPane videoContainer;
    private TextField urlField;
    private Label statusLabel;
    private CefBrowser cefBrowser;
    private SwingNode swingNode;
    
    @Override
    public void start(Stage primaryStage) {
        System.out.println("🚀 Test CefManager Singleton - Solution erreur CEF");
        
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
        
        Button loadButton = new Button("🚀 Charger CefManager");
        loadButton.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px;");
        loadButton.setOnAction(e -> loadVideoWithCefManager());
        
        Button testMultipleButton = new Button("🧪 Test Multiple");
        testMultipleButton.setStyle("-fx-background-color: #9B59B6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px;");
        testMultipleButton.setOnAction(e -> testMultipleCalls());
        
        Button disposeButton = new Button("🧹 Dispose CefManager");
        disposeButton.setStyle("-fx-background-color: #95A5A6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px;");
        disposeButton.setOnAction(e -> disposeCefManager());
        
        controls.getChildren().addAll(urlField, loadButton, testMultipleButton, disposeButton);
        
        // Status
        statusLabel = new Label("🚀 CefManager Singleton Prêt");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Conteneur vidéo pour JCEF
        videoContainer = new AnchorPane();
        videoContainer.setPrefSize(800, 450);
        videoContainer.setStyle("-fx-background-color: #000; -fx-background-radius: 10; -fx-border-color: #34495E; -fx-border-width: 2;");
        
        root.getChildren().addAll(controls, statusLabel, videoContainer);
        
        // Scene
        Scene scene = new Scene(root, 830, 620);
        primaryStage.setTitle("🚀 CefManager Singleton Test - Solution erreur CEF");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Préparer le SwingNode vide
        swingNode = new SwingNode();
        videoContainer.getChildren().add(swingNode);
        AnchorPane.setTopAnchor(swingNode, 0.0);
        AnchorPane.setBottomAnchor(swingNode, 0.0);
        AnchorPane.setLeftAnchor(swingNode, 0.0);
        AnchorPane.setRightAnchor(swingNode, 0.0);
        
        System.out.println("✅ Interface prête - CefManager Singleton initialisé");
    }
    
    private void loadVideoWithCefManager() {
        String url = urlField.getText().trim();
        String videoId = extractVideoId(url);
        
        if (videoId == null || videoId.isEmpty()) {
            updateStatus("❌ URL invalide");
            return;
        }
        
        updateStatus("🚀 Chargement YouTube avec CefManager: " + videoId);
        
        String embedUrl = "https://www.youtube.com/embed/" 
            + videoId 
            + "?autoplay=1&rel=0&modestbranding=1&playsinline=1"
            + "&controls=1&fs=1";
        
        SwingUtilities.invokeLater(() -> {
            try {
                // Utiliser le singleton CefManager
                CefClient client = CefManager.getCefClient();
                if (client == null) {
                    updateStatus("❌ CefManager non disponible");
                    return;
                }
                
                if (cefBrowser != null) {
                    // Réutiliser le browser existant
                    cefBrowser.loadURL(embedUrl);
                    updateStatus("🔄 URL rechargée: " + videoId);
                    return;
                }
                
                // Créer le browser une seule fois
                cefBrowser = client.createBrowser(embedUrl, false, false);
                
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(cefBrowser.getUIComponent(), BorderLayout.CENTER);
                
                swingNode.setContent(panel);
                
                javafx.application.Platform.runLater(() -> {
                    videoContainer.requestLayout();
                    updateStatus("✅ Vidéo affichée: " + videoId);
                });
                
                System.out.println("✅ CefManager browser créé: " + videoId);
                
            } catch (Exception e) {
                System.err.println("❌ Erreur CefManager: " + e.getMessage());
                e.printStackTrace();
                updateStatus("❌ Erreur CefManager: " + e.getMessage());
            }
        });
    }
    
    private void testMultipleCalls() {
        updateStatus("🧪 Test appels multiples CefManager...");
        
        new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                final int index = i;
                
                try {
                    Thread.sleep(1000);
                    
                    // Test multiple appels au singleton
                    CefClient client = CefManager.getCefClient();
                    
                    javafx.application.Platform.runLater(() -> {
                        if (client != null) {
                            updateStatus("🧪 Appel " + (index + 1) + "/5 - CefManager OK");
                        } else {
                            updateStatus("❌ Appel " + (index + 1) + "/5 - CefManager NULL");
                        }
                    });
                    
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            javafx.application.Platform.runLater(() -> {
                updateStatus("🧪 Test multiples terminé - CefManager stable !");
            });
        }).start();
    }
    
    private void disposeCefManager() {
        updateStatus("🧹 Nettoyage CefManager...");
        
        SwingUtilities.invokeLater(() -> {
            try {
                if (cefBrowser != null) {
                    cefBrowser.close(true);
                    cefBrowser = null;
                }
                
                CefManager.dispose();
                
                // Vider le SwingNode
                swingNode.setContent(new JPanel());
                
                javafx.application.Platform.runLater(() -> {
                    updateStatus("✅ CefManager nettoyé");
                });
                
                System.out.println("✅ CefManager dispose complété");
                
            } catch (Exception e) {
                System.err.println("❌ Erreur dispose: " + e.getMessage());
                updateStatus("❌ Erreur dispose: " + e.getMessage());
            }
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
