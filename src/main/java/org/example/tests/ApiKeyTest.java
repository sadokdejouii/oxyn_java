package org.example.tests;

import org.example.config.YouTubeConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Test rapide pour valider la nouvelle clé API YouTube
 */
public class ApiKeyTest {
    
    private static final String TEST_VIDEO_ID = "dQw4w9WgXcQ"; // Rick Roll - vidéo publique
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    public static void main(String[] args) {
        System.out.println("🔑 TEST CLÉ API YOUTUBE");
        System.out.println("=" .repeat(40));
        
        // 1. Vérifier la configuration
        YouTubeConfig.printStatus();
        String apiKey = YouTubeConfig.getApiKey();
        
        if (apiKey == null || apiKey.contains("VOTRE_CLÉ")) {
            System.out.println("❌ Clé API non configurée");
            return;
        }
        
        System.out.println("✅ Clé API trouvée: " + maskApiKey(apiKey));
        
        // 2. Tester l'API
        testApiCall(apiKey);
        
        System.out.println("=" .repeat(40));
        System.out.println("🎯 TEST TERMINÉ");
    }
    
    private static void testApiCall(String apiKey) {
        System.out.println("\n🌐 Test appel API...");
        
        String url = String.format(
            "https://www.googleapis.com/youtube/v3/videos?id=%s&key=%s&part=snippet,status,contentDetails",
            TEST_VIDEO_ID, apiKey
        );
        
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
                System.out.println("✅ API fonctionne parfaitement !");
                
                // Extraire quelques infos pour validation
                if (responseBody.contains("\"title\"")) {
                    String title = extractField(responseBody, "title");
                    System.out.println("📹 Titre: " + title);
                }
                
                if (responseBody.contains("\"duration\"")) {
                    String duration = extractField(responseBody, "duration");
                    System.out.println("⏱️ Durée: " + duration);
                }
                
                System.out.println("🎬 Vidéo test: https://www.youtube.com/watch?v=" + TEST_VIDEO_ID);
                
            } else {
                System.out.println("❌ Erreur API - Code: " + responseCode);
                System.out.println("📄 Réponse: " + responseBody.substring(0, Math.min(300, responseBody.length())));
                
                // Analyse des erreurs
                if (responseCode == 400) {
                    System.out.println("🔍 Cause: Clé API invalide ou mauvais format");
                } else if (responseCode == 403) {
                    System.out.println("🔍 Cause: Quota dépassé ou restrictions");
                } else if (responseCode == 404) {
                    System.out.println("🔍 Cause: API YouTube Data v3 non activée");
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ Erreur réseau: " + e.getMessage());
        }
    }
    
    private static String extractField(String json, String field) {
        String pattern = "\"" + field + "\":\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "Non trouvé";
    }
    
    private static String maskApiKey(String apiKey) {
        if (apiKey == null) return "null";
        if (apiKey.length() < 8) return "***";
        return apiKey.substring(0, 8) + "***" + apiKey.substring(apiKey.length() - 4);
    }
}
