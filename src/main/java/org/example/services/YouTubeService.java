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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

/**
 * Service YouTube amélioré avec cache, validation complète et gestion robuste des erreurs
 */
public class YouTubeService {
    
    private static final String YOUTUBE_API_BASE = "https://www.googleapis.com/youtube/v3";
    private static final Duration API_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration CACHE_EXPIRY = Duration.ofMinutes(30);
    
    // Cache des résultats de validation
    private static final ConcurrentHashMap<String, CachedValidationResult> validationCache = 
        new ConcurrentHashMap<>();
    
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
    
    public YouTubeService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(API_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
        
        // Afficher le statut de configuration
        YouTubeConfig.printStatus();
    }
    
    /**
     * Valide une vidéo YouTube avec vérification complète
     */
    public CompletableFuture<VideoValidationResult> validateVideoAsync(String videoId) {
        return CompletableFuture.supplyAsync(() -> {
            if (videoId == null || videoId.trim().isEmpty()) {
                return new VideoValidationResult(VideoValidationResult.ValidationStatus.INVALID_URL, null);
            }
            
            // Vérifier le cache d'abord
            CachedValidationResult cached = validationCache.get(videoId);
            if (cached != null && !cached.isExpired()) {
                System.out.println("📋 Résultat récupéré du cache pour: " + videoId);
                return cached.getResult();
            }
            
            try {
                String apiKey = YouTubeConfig.getApiKey();
                if (!YouTubeConfig.isConfigured()) {
                    return new VideoValidationResult(VideoValidationResult.ValidationStatus.API_ERROR, videoId);
                }
                
                // Appel API avec toutes les parties nécessaires
                String url = String.format("%s/videos?id=%s&key=%s&part=status,snippet,contentDetails", 
                    YOUTUBE_API_BASE, videoId, apiKey);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(API_TIMEOUT)
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                
                VideoValidationResult result = parseApiResponse(response, videoId);
                
                // Mettre en cache le résultat
                validationCache.put(videoId, new CachedValidationResult(result));
                
                return result;
                
            } catch (Exception e) {
                System.err.println("Erreur validation vidéo YouTube: " + e.getMessage());
                VideoValidationResult.ValidationStatus status = 
                    e.getMessage().contains("timeout") ? 
                    VideoValidationResult.ValidationStatus.NETWORK_ERROR : 
                    VideoValidationResult.ValidationStatus.API_ERROR;
                
                VideoValidationResult result = new VideoValidationResult(status, videoId);
                validationCache.put(videoId, new CachedValidationResult(result));
                return result;
            }
        });
    }
    
    /**
     * Parse la réponse API et détermine le statut de validation
     */
    private VideoValidationResult parseApiResponse(HttpResponse<String> response, String videoId) {
        try {
            if (response.statusCode() == 403) {
                return new VideoValidationResult(VideoValidationResult.ValidationStatus.RATE_LIMITED, videoId);
            }
            
            if (response.statusCode() != 200) {
                return new VideoValidationResult(VideoValidationResult.ValidationStatus.API_ERROR, videoId);
            }
            
            JsonNode jsonNode = objectMapper.readTree(response.body());
            
            // Vérifier si la vidéo existe
            if (!jsonNode.has("items") || jsonNode.get("items").size() == 0) {
                return new VideoValidationResult(VideoValidationResult.ValidationStatus.NOT_FOUND, videoId);
            }
            
            JsonNode video = jsonNode.get("items").get(0);
            JsonNode status = video.get("status");
            JsonNode snippet = video.get("snippet");
            JsonNode contentDetails = video.get("contentDetails");
            
            // Extraire les métadonnées
            String title = snippet.has("title") ? snippet.get("title").asText() : null;
            String description = snippet.has("description") ? snippet.get("description").asText() : null;
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
            if (!"public".equals(privacyStatus)) {
                return new VideoValidationResult(
                    VideoValidationResult.ValidationStatus.PRIVATE, 
                    videoId, title, description, thumbnailUrl, durationSeconds
                );
            }
            
            // Vérifier si la vidéo peut être intégrée
            boolean embeddable = status.has("embeddable") && status.get("embeddable").asBoolean();
            if (!embeddable) {
                return new VideoValidationResult(
                    VideoValidationResult.ValidationStatus.NOT_EMBEDDABLE, 
                    videoId, title, description, thumbnailUrl, durationSeconds
                );
            }
            
            // Vidéo valide
            return new VideoValidationResult(
                VideoValidationResult.ValidationStatus.VALID, 
                videoId, title, description, thumbnailUrl, durationSeconds
            );
            
        } catch (Exception e) {
            System.err.println("Erreur parsing réponse API: " + e.getMessage());
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
        validationCache.clear();
        System.out.println("🗑️ Cache YouTube vidé");
    }
    
    /**
     * Retourne les statistiques du cache
     */
    public static String getCacheStats() {
        int total = validationCache.size();
        long expired = validationCache.values().stream()
            .mapToLong(c -> c.isExpired() ? 1 : 0)
            .sum();
        
        return String.format("Cache: %d entrées, %d expirées", total, expired);
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
    }
    
    /**
     * Vérifie si une vidéo YouTube peut être intégrée (embeddable)
     * @param videoId ID de la vidéo YouTube
     * @return true si la vidéo peut être intégrée, false sinon
     */
    public boolean isEmbeddable(String videoId) {
        try {
            String apiKey = YouTubeConfig.getApiKey();
            if (!YouTubeConfig.isConfigured()) {
                System.out.println("⚠️ Clé API non configurée pour vérification embeddable");
                return false;
            }
            
            String url = "https://www.googleapis.com/youtube/v3/videos"
                    + "?id=" + videoId
                    + "&key=" + apiKey
                    + "&part=status";

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.out.println("❌ Erreur API vérification embeddable: " + responseCode);
                return false;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            String response = reader.lines().collect(Collectors.joining());
            reader.close();

            boolean embeddable = response.contains("\"embeddable\": true");
            
            System.out.println("🔍 Vérification embeddable pour " + videoId + ": " + embeddable);
            System.out.println("📄 Réponse API: " + response.substring(0, Math.min(200, response.length())));
            
            return embeddable;

        } catch (Exception e) {
            System.out.println("❌ Erreur vérification embed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
