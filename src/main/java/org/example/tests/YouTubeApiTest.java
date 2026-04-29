package org.example.tests;

import org.example.config.YouTubeConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Test direct de l'API YouTube pour diagnostiquer les erreurs
 */
public class YouTubeApiTest {
    
    private static final String TEST_VIDEO_ID = "dQw4w9WgXcQ"; // Vidéo publique connue
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    public static void main(String[] args) {
        System.out.println("🔍 TEST API YOUTUBE - DIAGNOSTIC COMPLET");
        System.out.println("=" .repeat(50));
        
        // 1. Vérifier la configuration
        testConfiguration();
        
        // 2. Tester l'API directement
        testApiCall();
        
        // 3. Tester avec une clé valide (temporaire)
        testWithValidKey();
        
        System.out.println("=" .repeat(50));
        System.out.println("🎯 DIAGNOSTIC TERMINÉ");
    }
    
    /**
     * Test la configuration de la clé API
     */
    private static void testConfiguration() {
        System.out.println("\n🔑 1. CONFIGURATION CLÉ API");
        
        YouTubeConfig.printStatus();
        
        String apiKey = YouTubeConfig.getApiKey();
        System.out.println("Clé API: " + maskApiKey(apiKey));
        
        boolean isConfigured = YouTubeConfig.isConfigured();
        System.out.println("Configurée: " + (isConfigured ? "✅ OUI" : "❌ NON"));
        
        if (!isConfigured) {
            System.out.println("⚠️ SOLUTION: Configurez votre clé API:");
            System.out.println("   1. Variable environnement: YOUTUBE_API_KEY");
            System.out.println("   2. Fichier: src/main/resources/youtube-config.properties");
            System.out.println("   3. Google Cloud: https://console.cloud.google.com/apis/credentials");
        }
    }
    
    /**
     * Test l'appel API avec la clé configurée
     */
    private static void testApiCall() {
        System.out.println("\n🌐 2. TEST APPEL API DIRECT");
        
        String apiKey = YouTubeConfig.getApiKey();
        if (apiKey == null || apiKey.contains("VOTRE_CLÉ")) {
            System.out.println("❌ Clé API non configurée - test impossible");
            return;
        }
        
        String url = String.format(
            "https://www.googleapis.com/youtube/v3/videos?id=%s&key=%s&part=snippet,status",
            TEST_VIDEO_ID, apiKey
        );
        
        System.out.println("URL test: " + url.replace(apiKey, maskApiKey(apiKey)));
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            int responseCode = response.statusCode();
            String responseBody = response.body();
            
            System.out.println("📊 Code réponse: " + responseCode);
            
            if (responseCode == 200) {
                System.out.println("✅ API fonctionne !");
                System.out.println("📄 Réponse (premiers 200 chars): " + responseBody.substring(0, Math.min(200, responseBody.length())));
            } else {
                System.out.println("❌ Erreur API - Code: " + responseCode);
                System.out.println("📄 Réponse: " + responseBody);
                
                // Analyse des erreurs communes
                if (responseCode == 400) {
                    System.out.println("🔍 Cause probable: Clé API invalide ou mauvais format");
                } else if (responseCode == 403) {
                    System.out.println("🔍 Cause probable: Quota dépassé ou API non activée");
                } else if (responseCode == 404) {
                    System.out.println("🔍 Cause probable: API YouTube Data v3 non activée");
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ Erreur réseau: " + e.getMessage());
        }
    }
    
    /**
     * Test avec une clé de démonstration (limitée)
     */
    private static void testWithValidKey() {
        System.out.println("\n🧪 3. TEST SANS VALIDATION API (MODE DÉMO)");
        
        System.out.println("💡 POUR ÉVITER LES ERREURS API TEMPORAIREMENT:");
        System.out.println("   1. Désactiver la validation API");
        System.out.println("   2. Afficher directement la vidéo");
        System.out.println("   3. Configurer une vraie clé API plus tard");
        
        // Simuler une validation réussie
        String testVideoId = TEST_VIDEO_ID;
        System.out.println("🎬 Vidéo test: " + testVideoId);
        System.out.println("✅ Validation simulée: VALID");
        System.out.println("📺 URL embed: https://www.youtube.com/embed/" + testVideoId + "?autoplay=1");
    }
    
    /**
     * Masque la clé API pour l'affichage
     */
    private static String maskApiKey(String apiKey) {
        if (apiKey == null) return "null";
        if (apiKey.length() < 8) return "***";
        return apiKey.substring(0, 8) + "***" + apiKey.substring(apiKey.length() - 4);
    }
    
    /**
     * Instructions pour corriger les problèmes API
     */
    public static void printFixInstructions() {
        System.out.println("\n🔧 INSTRUCTIONS POUR CORRIGER L'ERREUR API:");
        System.out.println("1. Obtenir une clé API YouTube Data v3:");
        System.out.println("   - https://console.cloud.google.com/apis/credentials");
        System.out.println("2. Activer YouTube Data API v3:");
        System.out.println("   - https://console.cloud.google.com/apis/library/youtube.googleapis.com");
        System.out.println("3. Configurer la clé:");
        System.out.println("   - Variable environnement: export YOUTUBE_API_KEY=votre_clé");
        System.out.println("   - Ou fichier: youtube-config.properties");
        System.out.println("4. Tester l'API:");
        System.out.println("   - https://www.googleapis.com/youtube/v3/videos?id=dQw4w9WgXcQ&key=VOTRE_CLÉ&part=snippet,status");
    }
}
