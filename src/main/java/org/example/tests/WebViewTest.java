package org.example.tests;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * Test simple pour vérifier que WebView fonctionne correctement
 */
public class WebViewTest extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Créer un WebView
            WebView webView = new WebView();
            
            // Activer JavaScript
            webView.getEngine().setJavaScriptEnabled(true);
            
            // Charger une page de test
            webView.getEngine().load("https://www.youtube.com");
            
            // Créer l'interface
            VBox root = new VBox(webView);
            Scene scene = new Scene(root, 800, 600);
            
            primaryStage.setTitle("WebView Test - YouTube");
            primaryStage.setScene(scene);
            primaryStage.show();
            
            System.out.println("✅ WebView fonctionne correctement !");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur WebView: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
