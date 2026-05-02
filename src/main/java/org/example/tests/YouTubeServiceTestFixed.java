package org.example.tests;

import org.example.config.YouTubeConfig;
import org.example.models.VideoValidationResult;
import org.example.services.YouTubeServiceFixed;

/**
 * Test complet du service YouTube corrigé
 */
public class YouTubeServiceTestFixed {
    
    public static void main(String[] args) {
        System.out.println("🧪 TEST SERVICE YOUTUBE CORRIGÉ");
        System.out.println("=" .repeat(50));
        
        // 1. Configuration
        YouTubeConfig.printStatus();
        
        // 2. Initialiser le service
        YouTubeServiceFixed service = new YouTubeServiceFixed();
        
        // 3. Vider le cache pour test propre
        YouTubeServiceFixed.clearCache();
        
        // 4. Activer bypass cache pour forcer appel API
        YouTubeServiceFixed.setBypassCache(true);
        
        // 5. Tester avec une vidéo connue
        String testVideoId = "dQw4w9WgXcQ"; // Rick Roll - vidéo publique
        System.out.println("\n🎬 Test avec vidéo: " + testVideoId);
        System.out.println("🔗 URL: https://www.youtube.com/watch?v=" + testVideoId);
        
        try {
            VideoValidationResult result = service.validateVideoAsync(testVideoId).get();
            
            System.out.println("\n📊 RÉSULTAT FINAL:");
            System.out.println("   Statut: " + result.getStatus());
            System.out.println("   Video ID: " + result.getVideoId());
            System.out.println("   Titre: " + result.getTitle());
            System.out.println("   Durée: " + result.getDurationSeconds() + " secondes");
            System.out.println("   Miniature: " + result.getThumbnailUrl());
            
            if (result.getStatus() == VideoValidationResult.ValidationStatus.VALID) {
                System.out.println("✅ SUCCÈS - La vidéo est valide et intégrable !");
            } else {
                System.out.println("❌ ERREUR - Statut: " + result.getStatus());
            }
            
        } catch (Exception e) {
            System.err.println("💥 Erreur test: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 6. Afficher les stats du cache
        System.out.println("\n📈 " + YouTubeServiceFixed.getCacheStats());
        
        // 7. Test sans bypass (avec cache)
        System.out.println("\n🔄 Test avec cache activé (bypass désactivé)...");
        YouTubeServiceFixed.setBypassCache(false);
        
        try {
            VideoValidationResult cachedResult = service.validateVideoAsync(testVideoId).get();
            System.out.println("📋 Résultat du cache: " + cachedResult.getStatus());
        } catch (Exception e) {
            System.err.println("💥 Erreur test cache: " + e.getMessage());
        }
        
        System.out.println("\n🎯 TEST TERMINÉ");
    }
}
