package org.example.entities;

import java.time.LocalDateTime;

/**
 * Ligne table {@code messages} (sans jointure utilisateur).
 */
public record MessageEntity(
        int id,
        int conversationId,
        int senderId,
        String contenu,
        String type,
        LocalDateTime createdAt,
        boolean isAiGenerated
) {
}
