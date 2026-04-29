package org.example.models;

/**
 * Résultat de validation vidéo avec statuts détaillés
 */
public class VideoValidationResult {
    
    public enum ValidationStatus {
        VALID("✅ Vidéo valide"),
        PRIVATE("🔒 Vidéo privée"),
        NOT_EMBEDDABLE("🚫 Lecture interdite"),
        NOT_FOUND("❌ Vidéo introuvable"),
        API_ERROR("🌐 Erreur API"),
        NETWORK_ERROR("🌐 Erreur réseau"),
        INVALID_URL("🔗 URL invalide"),
        RATE_LIMITED("⏸️ Quota dépassé");
        
        private final String message;
        
        ValidationStatus(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    private final ValidationStatus status;
    private final String videoId;
    private final String title;
    private final String description;
    private final String thumbnailUrl;
    private final long durationSeconds;
    
    public VideoValidationResult(ValidationStatus status, String videoId) {
        this(status, videoId, null, null, null, 0);
    }
    
    public VideoValidationResult(ValidationStatus status, String videoId, String title, 
                                String description, String thumbnailUrl, long durationSeconds) {
        this.status = status;
        this.videoId = videoId;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.durationSeconds = durationSeconds;
    }
    
    // Getters
    public ValidationStatus getStatus() { return status; }
    public String getVideoId() { return videoId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public long getDurationSeconds() { return durationSeconds; }
    
    public boolean isValid() {
        return status == ValidationStatus.VALID;
    }
    
    public boolean canEmbed() {
        return status == ValidationStatus.VALID || status == ValidationStatus.NOT_EMBEDDABLE;
    }
    
    public String getFormattedDuration() {
        if (durationSeconds <= 0) return "Inconnue";
        
        long hours = durationSeconds / 3600;
        long minutes = (durationSeconds % 3600) / 60;
        long seconds = durationSeconds % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }
    
    @Override
    public String toString() {
        return String.format("VideoValidationResult{status=%s, videoId='%s', title='%s'}", 
                           status, videoId, title);
    }
}
