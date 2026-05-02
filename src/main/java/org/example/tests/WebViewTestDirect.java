package org.example.tests;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * Test direct du WebView avec iframe YouTube (sans validation API)
 */
public class WebViewTestDirect extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("🧪 Test WebView Direct - Iframe YouTube");
            
            // Créer WebView
            WebView webView = new WebView();
            webView.getEngine().setJavaScriptEnabled(true);
            
            // Vidéo test
            String videoId = "dQw4w9WgXcQ";
            
            // HTML simple avec iframe embed - CORRIGÉ
            String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { 
                            margin: 0; 
                            padding: 0; 
                            background: #000; 
                            overflow: hidden;
                        }
                        iframe {
                            position: absolute;
                            top: 0;
                            left: 0;
                            width: 100%%;
                            height: 100%%;
                            border: none;
                        }
                    </style>
                </head>
                <body>
                    <iframe 
                        src="https://www.youtube.com/embed/%s?autoplay=1&rel=0&modestbranding=1&controls=1"
                        frameborder="0" 
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                        allowfullscreen>
                    </iframe>
                </body>
                </html>
                """, videoId);
            
            System.out.println("🎬 Chargement vidéo: " + videoId);
            System.out.println("🔗 URL embed: https://www.youtube.com/embed/" + videoId);
            
            // Charger HTML
            webView.getEngine().loadContent(html, "text/html");
            
            // Interface
            Scene scene = new Scene(webView, 800, 600);
            primaryStage.setTitle("WebView Test - YouTube Iframe Embed");
            primaryStage.setScene(scene);
            primaryStage.show();
            
            System.out.println("✅ WebView lancé avec succès !");
            System.out.println("🎯 Si la vidéo s'affiche, le problème est résolu !");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur WebView: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
