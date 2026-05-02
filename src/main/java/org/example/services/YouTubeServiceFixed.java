package org.example.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.YouTubeConfig;
import org.example.models.VideoValidationResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Service YouTube CORRIGÉ - Cache fixé, API fonctionnelle, gestion erreurs robuste
 */
public class YouTubeServiceFixed {
    
    private static final String YOUTUBE_API_BASE = "https://www.googleapis.com/youtube/v3";
    private static final Duration API_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration CACHE_EXPIRY = Duration.ofMinutes(5); // Réduit à 5 minutes pour tests
    
    // Cache des résultats de validation
    private static final ConcurrentHashMap<String, CachedValidationResult> validationCache = 
        new ConcurrentHashMap<>();
    
    // Flag pour forcer le contournement du cache
    private static boolean bypassCache = false;
    
    // Patterns robustes pour tous les formats YouTube
    private static final Pattern[] URL_PATTERNS = {
        Pattern.compile(".*youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11}).*"),
        Pattern.compile(".*youtu\\.be/([a-zA-Z0-9_-]{11}).*"),
        Pattern.compile(".*youtube\\.com/embed/([a-zA-Z0-9_-]{11}).*"),
        Pattern.compile(".*youtube\\.com/shorts/([a-zA-Z0-9_-]{11}).*"),
        Pattern.compile("^([a-zA-Z0-9_-]{11})$") // ID direct
    };
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public YouTubeServiceFixed() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(API_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
        
        System.out.println("🔧 YouTubeServiceFixed initialisé");
        YouTubeConfig.printStatus();
    }
    
    /**
     * Valide une vidéo YouTube avec cache corrigé et debug complet
     */
    public CompletableFuture<VideoValidationResult> validateVideoAsync(String videoId) {
        return CompletableFuture.supplyAsync(() -> {
            if (videoId == null || videoId.trim().isEmpty()) {
                System.out.println("❌ Video ID null ou vide");
                return new VideoValidationResult(VideoValidationResult.ValidationStatus.INVALID_URL, null);
            }
            
            // FORCER LE CONTOURNEMENT DU CACHE POUR TESTS
            if (bypassCache) {
                System.out.println("🔄 BYPASS CACHE ACTIF - Forçage appel API pour: " + videoId);
                return performApiCall(videoId);
            }
            
            // Vérifier le cache (log amélioré)
            CachedValidationResult cached = validationCache.get(videoId);
            if (cached != null && !cached.isExpired()) {
                System.out.println("📋 RÉSULTAT DU CACHE pour: " + videoId + " (statut: " + cached.getResult().getStatus() + ")");
                System.out.println("⏰ Cache expirera dans: " + (CACHE_EXPIRY.toMillis() - (System.currentTimeMillis() - cached.getTimestamp())) + "ms");
                return cached.getResult();
            } else if (cached != null) {
                System.out.println("⏰ CACHE EXPIRÉ pour: " + videoId + " - nouvel appel API");
                validationCache.remove(videoId); // Nettoyer l'entrée expirée
            } else {
                System.out.println("🆕 PAS EN CACHE pour: " + videoId + " - appel API");
            }
            
            return performApiCall(videoId);
        });
    }
    
    /**
     * Effectue l'appel API avec gestion d'erreurs complète
     */
    private VideoValidationResult performApiCall(String videoId) {
        try {
            String apiKey = YouTubeConfig.getApiKey();
            if (!YouTubeConfig.isConfigured()) {
                System.out.println("❌ Clé API non configurée");
                return new VideoValidationResult(VideoValidationResult.ValidationStatus.API_ERROR, videoId);
            }
            
            // URL API CORRECTE avec tous les paramètres nécessaires
            String url = String.format("%s/videos?id=%s&key=%s&part=snippet,status,contentDetails", 
                YOUTUBE_API_BASE, videoId, apiKey);
            
            System.out.println("🌐 APPEL API vers: " + url.replace(apiKey, maskApiKey(apiKey)));
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(API_TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            
            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - startTime;
            
            System.out.println("📊 Réponse API: " + response.statusCode() + " (temps: " + responseTime + "ms)");
            
            VideoValidationResult result = parseApiResponse(response, videoId);
            
            // Mettre en cache SEULEMENT si succès (pas les erreurs)
            if (result.getStatus() == VideoValidationResult.ValidationStatus.VALID) {
                validationCache.put(videoId, new CachedValidationResult(result));
                System.out.println("✅ MIS EN CACHE pour: " + videoId);
            } else {
                System.out.println("❌ ERREUR - PAS DE CACHE pour: " + videoId + " (statut: " + result.getStatus() + ")");
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("💥 ERREUR APPPEL API: " + e.getMessage());
            e.printStackTrace();
            
            VideoValidationResult.ValidationStatus status = 
                e.getMessage().contains("timeout") ? 
                VideoValidationResult.ValidationStatus.NETWORK_ERROR : 
                VideoValidationResult.ValidationStatus.API_ERROR;
                
            VideoValidationResult result = new VideoValidationResult(status, videoId);
            // NE PAS METTRE EN CACHE les erreurs
            System.out.println("❌ ERREUR RÉSEAU/API - PAS DE CACHE pour: " + videoId);
            return result;
        }
    }
    
    /**
     * Parse la réponse API avec gestion d'erreurs améliorée
     */
    private VideoValidationResult parseApiResponse(HttpResponse<String> response, String videoId) {
        try {
            int statusCode = response.statusCode();
            String responseBody = response.body();
            
            System.out.println("📄 Corps réponse (premiers 200 chars): " + responseBody.substring(0, Math.min(200, responseBody.length())));
            
            // Gestion des codes d'erreur HTTP
            switch (statusCode) {
                case 403:
                    System.out.println("🚫 ERREUR 403 - Quota dépassé ou clé invalide");
                    return new VideoValidationResult(VideoValidationResult.ValidationStatus.RATE_LIMITED, videoId);
                    
                case 404:
                    System.out.println("🚫 ERREUR 404 - API YouTube Data v3 non trouvée");
                    return new VideoValidationResult(VideoValidationResult.ValidationStatus.API_ERROR, videoId);
                    
                case 400:
                    System.out.println("🚫 ERREUR 400 - Requête invalide");
                    return new VideoValidationResult(VideoValidationResult.ValidationStatus.API_ERROR, videoId);
            }
            
            if (statusCode != 200) {
                System.out.println("🚫 ERREUR HTTP " + statusCode + " - Réponse: " + responseBody);
                return new VideoValidationResult(VideoValidationResult.ValidationStatus.API_ERROR, videoId);
            }
            
            // Parsing JSON
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            // Vérifier si la vidéo existe
            if (!jsonNode.has("items") || jsonNode.get("items").size() == 0) {
                System.out.println("🔍 VIDÉO NON TROUVÉE: " + videoId);
                return new VideoValidationResult(VideoValidationResult.ValidationStatus.NOT_FOUND, videoId);
            }
            
            JsonNode video = jsonNode.get("items").get(0);
            JsonNode status = video.get("status");
            JsonNode snippet = video.get("snippet");
            JsonNode contentDetails = video.get("contentDetails");
            
            // Extraire les métadonnées
            String title = snippet.has("title") ? snippet.get("title").asText() : "Titre inconnu";
            String description = snippet.has("description") ? snippet.get("description").asText() : "";
            String thumbnailUrl = null;
            if (snippet.has("thumbnails") && snippet.get("thumbnails").has("high")) {
                thumbnailUrl = snippet.get("thumbnails").get("high").get("url").asText();
            }
            
            long durationSeconds = 0;
            if (contentDetails != null && contentDetails.has("duration")) {
                durationSeconds = parseDuration(contentDetails.get("duration").asText());
            }
            
            // Vérifier le statut de confidentialité
            String privacyStatus = status.get("privacyStatus").asText();
            System.out.println("🔒 Statut confidentialité: " + privacyStatus);
            
            if (!"public".equals(privacyStatus)) {
                System.out.println("🚫 VIDÉO PRIVÉE: " + videoId);
                return new VideoValidationResult(
                    VideoValidationResult.ValidationStatus.PRIVATE, 
                    videoId, title, description, thumbnailUrl, durationSeconds
                );
            }
            
            // Vérifier si la vidéo peut être intégrée
            boolean embeddable = status.has("embeddable") && status.get("embeddable").asBoolean();
            System.out.println("📺 Embeddable: " + embeddable);
            
            if (!embeddable) {
                System.out.println("🚫 VIDÉO NON INTÉGRABLE: " + videoId);
                return new VideoValidationResult(
                    VideoValidationResult.ValidationStatus.NOT_EMBEDDABLE, 
                    videoId, title, description, thumbnailUrl, durationSeconds
                );
            }
            
            // VIDÉO VALIDE !
            System.out.println("✅ VIDÉO VALIDE: " + videoId + " - " + title);
            return new VideoValidationResult(
                VideoValidationResult.ValidationStatus.VALID, 
                videoId, title, description, thumbnailUrl, durationSeconds
            );
            
        } catch (Exception e) {
            System.err.println("💥 ERREUR PARSING JSON: " + e.getMessage());
            e.printStackTrace();
            return new VideoValidationResult(VideoValidationResult.ValidationStatus.API_ERROR, videoId);
        }
    }
    
    /**
     * Parse la durée ISO 8601 (PT4M13S) en secondes
     */
    private long parseDuration(String isoDuration) {
        try {
            long seconds = 0;
            String[] parts = isoDuration.substring(2).split("[HMS]");
            
            for (int i = 0; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    long value = Long.parseLong(parts[i]);
                    char unit = isoDuration.charAt(isoDuration.indexOf(parts[i]) + parts[i].length());
                    
                    switch (unit) {
                        case 'H': seconds += value * 3600; break;
                        case 'M': seconds += value * 60; break;
                        case 'S': seconds += value; break;
                    }
                }
            }
            return seconds;
        } catch (Exception e) {
            System.err.println("Erreur parsing durée: " + isoDuration);
            return 0;
        }
    }
    
    /**
     * Extrait l'ID vidéo de manière robuste depuis tous les formats YouTube
     */
    public static String extractVideoId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        url = url.trim();
        
        for (Pattern pattern : URL_PATTERNS) {
            java.util.regex.Matcher matcher = pattern.matcher(url);
            if (matcher.matches() && matcher.groupCount() >= 1) {
                String videoId = matcher.group(1);
                // Validation supplémentaire du format ID
                if (videoId.matches("[a-zA-Z0-9_-]{11}")) {
                    return videoId;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Vide le cache des validations
     */
    public static void clearCache() {
        int size = validationCache.size();
        validationCache.clear();
        System.out.println("🗑️ Cache YouTube vidé (" + size + " entrées supprimées)");
    }
    
    /**
     * Active/désactive le contournement du cache pour tests
     */
    public static void setBypassCache(boolean bypass) {
        bypassCache = bypass;
        System.out.println("🔄 Bypass cache: " + (bypass ? "ACTIVÉ" : "DÉSACTIVÉ"));
    }
    
    /**
     * Retourne les statistiques du cache
     */
    public static String getCacheStats() {
        int total = validationCache.size();
        long expired = validationCache.values().stream()
            .mapToLong(c -> c.isExpired() ? 1 : 0)
            .sum();
        
        return String.format("Cache: %d entrées, %d expirées, bypass: %s", total, expired, bypassCache ? "ON" : "OFF");
    }
    
    /**
     * Masque la clé API pour les logs
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null) return "null";
        if (apiKey.length() < 8) return "***";
        return apiKey.substring(0, 8) + "***" + apiKey.substring(apiKey.length() - 4);
    }
    
    /**
     * Classe interne pour le cache avec expiration
     */
    private static class CachedValidationResult {
        private final VideoValidationResult result;
        private final long timestamp;
        
        public CachedValidationResult(VideoValidationResult result) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY.toMillis();
        }
        
        public VideoValidationResult getResult() {
            return result;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}
