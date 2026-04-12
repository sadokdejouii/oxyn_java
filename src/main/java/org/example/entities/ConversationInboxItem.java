package org.example.entities;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Élément de liste type Messenger (conversation + dernier message).
 *
 * @param lastSenderId expéditeur du dernier message (null si aucun message) — sert au tri « non lus » côté staff.
 */
public record ConversationInboxItem(
        int conversationId,
        int clientId,
        String clientName,
        String clientEmail,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        LocalDateTime conversationUpdatedAt,
        Integer lastSenderId
) {
    /**
     * Dernier message émis par le client : l’encadrant / l’admin peut considérer la conversation comme « à traiter ».
     */
    public boolean awaitingStaffReply() {
        return lastSenderId != null && lastSenderId == clientId;
    }

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
