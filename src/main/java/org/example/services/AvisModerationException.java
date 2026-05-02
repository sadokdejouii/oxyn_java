package org.example.services;

import java.util.ArrayList;
import java.util.List;

public class AvisModerationException extends RuntimeException {

    private final AvisAiModerationService.ModerationResult moderationResult;

    public AvisModerationException(String message) {
        super(message);
        this.moderationResult = null;
    }

    public AvisModerationException(AvisAiModerationService.ModerationResult moderationResult) {
        super(moderationResult.userMessage());
        this.moderationResult = moderationResult;
    }

    public AvisAiModerationService.ModerationResult getModerationResult() {
        return moderationResult;
    }

    public List<String> getPopupRows() {
        if (moderationResult == null) {
            return List.of();
        }

        List<String> rows = new ArrayList<>();
        rows.add("Categorie detectee : " + moderationResult.displayCategory());
        rows.add("Score de moderation : " + String.format("%.2f", moderationResult.toxicityScore()));

        if (!moderationResult.reasons().isEmpty()) {
            rows.add("Signaux detectes : " + String.join(", ", moderationResult.reasons()));
        }

        if (!moderationResult.recommendation().isBlank()) {
            rows.add("Suggestion : " + moderationResult.recommendation());
        }

        return rows;
    }
}