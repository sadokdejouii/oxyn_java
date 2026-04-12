package org.example.entities;

import java.time.LocalDateTime;

/**
 * Ligne table {@code conversations} (colonnes projet existantes).
 */
public record ConversationEntity(
        int id,
        int clientId,
        Integer encadrantId,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
