package org.example.entities;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Élément de liste type Messenger (conversation + dernier message).
 */
public record ConversationInboxItem(
        int conversationId,
        int clientId,
        String clientName,
        String clientEmail,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        LocalDateTime conversationUpdatedAt
) {
    public String presenceLabel() {
        LocalDateTime ref = lastMessageAt != null ? lastMessageAt : conversationUpdatedAt;
        if (ref == null) {
            return "Hors ligne";
        }
        if (Duration.between(ref, LocalDateTime.now()).toMinutes() <= 15) {
            return "En ligne";
        }
        return "Hors ligne";
    }
}
