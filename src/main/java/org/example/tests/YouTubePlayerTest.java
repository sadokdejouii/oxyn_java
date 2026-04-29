package org.example.tests;

import org.example.config.YouTubeConfig;
import org.example.models.VideoValidationResult;
import org.example.services.YouTubeService;
import org.example.utils.VideoPlayerHelper;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Tests complets du système YouTube Player
 */
public class YouTubePlayerTest {
    
    private static final YouTubeService youTubeService = new YouTubeService();
    
    /**
     * Lance tous les tests
     */
    public static void runAllTests() {
        System.out.println("🧪 DÉMARRAGE DES TESTS YOUTUBE PLAYER v2.0");
        System.out.println("=" .repeat(60));
        
        testConfiguration();
        testUrlExtraction();
        testVideoValidation();
        testCacheSystem();
        testIntegration();
        
        System.out.println("=" .repeat(60));
        System.out.println("🎉 TOUS LES TESTS TERMINÉS");
    }
    
    /**
     * Test de la configuration API
     */
    private static void testConfiguration() {
        System.out.println("\n🔑 Test 1: Configuration API");
        
        YouTubeConfig.printStatus();
        
        boolean isConfigured = YouTubeConfig.isConfigured();
        System.out.println("✅ Configuré: " + (isConfigured ? "OUI" : "NON"));
        
        if (!isConfigured) {
            System.out.println("⚠️ Configurez votre clé API dans:");
            System.out.println("   - Variable d'environnement: YOUTUBE_API_KEY");
            System.out.println("   - Fichier: src/main/resources/youtube-config.properties");
        }
    }
    
    /**
     * Test d'extraction d'ID depuis différents formats d'URL
     */
    private static void testUrlExtraction() {
        System.out.println("\n🔗 Test 2: Extraction URL");
        
        String[] testUrls = {
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            "dQw4w9WgXcQ",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=30s",
            "URL_INVALIDE",
            "",
            null
        };
        
        for (String url : testUrls) {
            String videoId = YouTubeService.extractVideoId(url);
            String status = (videoId != null) ? "✅" : "❌";
            System.out.println(status + " " + (url != null ? url : "null") + " → " + videoId);
        }
    }
    
    /**
     * Test de validation de vidéos
     */
    private static void testVideoValidation() {
        System.out.println("\n🔍 Test 3: Validation vidéos");
        
        String[] testVideos = {
            "dQw4w9WgXcQ", // Vidéo publique connue
            "invalid123456", // ID invalide
            "private123456"  // ID probablement privé/inexistant
        };
        
        for (String videoId : testVideos) {
            System.out.println("🎬 Test vidéo: " + videoId);
            
            try {
                CompletableFuture<VideoValidationResult> future = 
                    youTubeService.validateVideoAsync(videoId);
                
                VideoValidationResult result = future.get();
                
                System.out.println("   Statut: " + result.getStatus().getMessage());
                System.out.println("   Titre: " + (result.getTitle() != null ? result.getTitle() : "N/A"));
                System.out.println("   Durée: " + result.getFormattedDuration());
                System.out.println("   Valide: " + result.isValid());
                
            } catch (Exception e) {
                System.out.println("   ❌ Erreur: " + e.getMessage());
            }
            
            System.out.println();
        }
    }
    
    /**
     * Test du système de cache
     */
    private static void testCacheSystem() {
        System.out.println("📋 Test 4: Système de cache");
        
        String testVideoId = "dQw4w9WgXcQ";
        
        // Premier appel (sans cache)
        System.out.println("🔄 Premier appel (sans cache)...");
        long start1 = System.currentTimeMillis();
        long time1 = 0;
        
        try {
            youTubeService.validateVideoAsync(testVideoId).get();
            time1 = System.currentTimeMillis() - start1;
            System.out.println("   ⏱️ Temps: " + time1 + "ms");
        } catch (Exception e) {
            System.out.println("   ❌ Erreur: " + e.getMessage());
            time1 = Long.MAX_VALUE; // Valeur haute si erreur
        }
        
        // Deuxième appel (avec cache)
        System.out.println("📋 Deuxième appel (avec cache)...");
        long start2 = System.currentTimeMillis();
        
        try {
            youTubeService.validateVideoAsync(testVideoId).get();
            long time2 = System.currentTimeMillis() - start2;
            System.out.println("   ⏱️ Temps: " + time2 + "ms");
            
            if (time2 < time1) {
                System.out.println("   ✅ Cache fonctionnel (gain: " + (time1 - time2) + "ms)");
            } else {
                System.out.println("   ⚠️ Cache peut-être inefficace");
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Erreur: " + e.getMessage());
        }
        
        // Statistiques du cache
        System.out.println("📊 " + YouTubeService.getCacheStats());
    }
    
    /**
     * Test d'intégration avec l'interface
     */
    private static void testIntegration() {
        System.out.println("🔧 Test 5: Intégration interface");
        
        try {
            // Test helper
            System.out.println("🎬 Création bouton lecteur...");
            javafx.scene.control.Button button = VideoPlayerHelper.createVideoPlayerButton();
            System.out.println("   ✅ Bouton créé: " + button.getText());
            
            // Test bouton vidéo spécifique
            System.out.println("🎬 Création bouton vidéo spécifique...");
            javafx.scene.control.Button videoButton = VideoPlayerHelper.createVideoButton(
                "dQw4w9WgXcQ", "Test Video");
            System.out.println("   ✅ Bouton vidéo créé: " + videoButton.getText());
            
            // Test playlist
            System.out.println("📋 Test playlist...");
            List<String> playlist = Arrays.asList(
                "dQw4w9WgXcQ",
                "9bZkp7q19f0",
                "kJQP7kiw5Fk"
            );
            
            System.out.println("   📺 Playlist créée avec " + playlist.size() + " vidéos");
            
        } catch (Exception e) {
            System.out.println("   ❌ Erreur intégration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test des cas d'erreur
     */
    public static void testErrorCases() {
        System.out.println("\n🚨 Test 6: Gestion des erreurs");
        
        String[] errorCases = {
            "", // URL vide
            null, // URL null
            "not_a_url", // URL invalide
            "https://www.facebook.com/video", // Mauvais domaine
            "https://www.youtube.com/watch?v=123", // ID trop court
            "https://www.youtube.com/watch?v=toolong123456" // ID trop long
        };
        
        for (String url : errorCases) {
            String videoId = YouTubeService.extractVideoId(url);
            boolean isValid = videoId != null && videoId.matches("[a-zA-Z0-9_-]{11}");
            
            String status = isValid ? "✅" : "❌";
            System.out.println(status + " " + (url != null ? url : "null") + " → " + videoId);
        }
    }
    
    /**
     * Test de performance
     */
    public static void testPerformance() {
        System.out.println("\n⚡ Test 7: Performance");
        
        String[] videoIds = {
            "dQw4w9WgXcQ",
            "9bZkp7q19f0",
            "kJQP7kiw5Fk"
        };
        
        long totalTime = 0;
        int successCount = 0;
        
        for (String videoId : videoIds) {
            long start = System.currentTimeMillis();
            
            try {
                VideoValidationResult result = youTubeService.validateVideoAsync(videoId).get();
                long time = System.currentTimeMillis() - start;
                totalTime += time;
                
                if (result.isValid()) {
                    successCount++;
                    System.out.println("✅ " + videoId + ": " + time + "ms");
                } else {
                    System.out.println("❌ " + videoId + ": " + result.getStatus().getMessage());
                }
                
            } catch (Exception e) {
                System.out.println("❌ " + videoId + ": Erreur - " + e.getMessage());
            }
        }
        
        if (successCount > 0) {
            double avgTime = (double) totalTime / successCount;
            System.out.println("📊 Temps moyen: " + String.format("%.2f", avgTime) + "ms");
            System.out.println("📊 Succès: " + successCount + "/" + videoIds.length);
        }
    }
    
    /**
     * Point d'entrée principal pour les tests
     */
    public static void main(String[] args) {
        runAllTests();
        testErrorCases();
        testPerformance();
        
        System.out.println("\n🎯 Tests terminés ! Vérifiez les résultats ci-dessus.");
        System.out.println("📝 Pour utiliser le lecteur:");
        System.out.println("   1. Configurez votre clé API YouTube");
        System.out.println("   2. Lancez: VideoPlayerHelper.openVideoPlayer()");
        System.out.println("   3. Testez avec des URLs YouTube valides");
    }
}
