package org.example.services;

import org.example.config.YouTubeConfig;
import org.example.models.VideoValidationResult;

/**
 * Service YouTube avec mode fallback sans validation API
 * Permet de fonctionner même si l'API YouTube ne répond pas
 */
public class YouTubeServiceFallback {
    
    /**
     * Mode de fonctionnement
     */
    public enum ValidationMode {
        API_FULL,      // Validation complète via API
        FALLBACK,      // Mode fallback sans API
        DISABLED       // Pas de validation du tout
    }
    
    private static ValidationMode currentMode = ValidationMode.API_FULL;
    
    /**
     * Validation avec fallback automatique
     */
    public static VideoValidationResult validateVideoWithFallback(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            return new VideoValidationResult(VideoValidationResult.ValidationStatus.INVALID_URL, null);
        }
        
        // Vérifier le format de l'ID
        if (!videoId.matches("[a-zA-Z0-9_-]{11}")) {
            return new VideoValidationResult(VideoValidationResult.ValidationStatus.INVALID_URL, videoId);
        }
        
        switch (currentMode) {
            case API_FULL:
                return tryApiValidation(videoId);
                
            case FALLBACK:
                return fallbackValidation(videoId);
                
            case DISABLED:
                return disabledValidation(videoId);
                
            default:
                return fallbackValidation(videoId);
        }
    }
    
    /**
     * Tente la validation via API avec fallback automatique
     */
    private static VideoValidationResult tryApiValidation(String videoId) {
        try {
            YouTubeService youTubeService = new YouTubeService();
            
            // Test rapide de la clé API
            if (!YouTubeConfig.isConfigured()) {
                System.out.println("⚠️ Clé API non configurée - passage en mode fallback");
                currentMode = ValidationMode.FALLBACK;
                return fallbackValidation(videoId);
            }
            
            // Appel API avec timeout court
            var future = youTubeService.validateVideoAsync(videoId);
            VideoValidationResult result = future.get(); // Timeout géré par le service
            
            if (result.getStatus() == VideoValidationResult.ValidationStatus.API_ERROR) {
                System.out.println("⚠️ Erreur API - passage en mode fallback");
                currentMode = ValidationMode.FALLBACK;
                return fallbackValidation(videoId);
            }
            
            return result;
            
        } catch (Exception e) {
            System.out.println("⚠️ Exception API - passage en mode fallback: " + e.getMessage());
            currentMode = ValidationMode.FALLBACK;
            return fallbackValidation(videoId);
        }
    }
    
    /**
     * Validation fallback sans API (validation basique)
     */
    private static VideoValidationResult fallbackValidation(String videoId) {
        System.out.println("🔄 Mode fallback - validation basique pour: " + videoId);
        
        // Validation basique du format
        if (!videoId.matches("[a-zA-Z0-9_-]{11}")) {
            return new VideoValidationResult(VideoValidationResult.ValidationStatus.INVALID_URL, videoId);
        }
        
        // Simulation de validation réussie
        return new VideoValidationResult(
            VideoValidationResult.ValidationStatus.VALID,
            videoId,
            "Vidéo (mode fallback)", // Titre générique
            "Validation sans API YouTube", // Description
            "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg", // Miniature
            0 // Durée inconnue
        );
    }
    
    /**
     * Validation désactivée (accepte tout)
     */
    private static VideoValidationResult disabledValidation(String videoId) {
        System.out.println("🚫 Mode validation désactivée - acceptation directe: " + videoId);
        
        return new VideoValidationResult(
            VideoValidationResult.ValidationStatus.VALID,
            videoId,
            "Vidéo (validation désactivée)",
            "Validation API désactivée",
            "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg",
            0
        );
    }
    
    /**
     * Change le mode de validation
     */
    public static void setValidationMode(ValidationMode mode) {
        currentMode = mode;
        System.out.println("🔧 Mode validation changé vers: " + mode);
    }
    
    /**
     * Retourne le mode actuel
     */
    public static ValidationMode getValidationMode() {
        return currentMode;
    }
    
    /**
     * Test rapide de l'API
     */
    public static boolean testApiConnectivity() {
        try {
            if (!YouTubeConfig.isConfigured()) {
                return false;
            }
            
            YouTubeService youTubeService = new YouTubeService();
            var result = youTubeService.validateVideoAsync("dQw4w9WgXcQ").get();
            
            return result.getStatus() != VideoValidationResult.ValidationStatus.API_ERROR;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Affiche le statut du service
     */
    public static void printServiceStatus() {
        System.out.println("📊 Statut Service YouTube:");
        System.out.println("   Mode actuel: " + currentMode);
        System.out.println("   API configurée: " + YouTubeConfig.isConfigured());
        System.out.println("   Connectivité API: " + (testApiConnectivity() ? "✅" : "❌"));
        
        if (currentMode == ValidationMode.FALLBACK) {
            System.out.println("   ⚠️ Mode fallback actif - les vidéos sont acceptées sans validation API");
        }
    }
}
