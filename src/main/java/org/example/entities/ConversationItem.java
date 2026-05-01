package org.example.entities;

import java.time.LocalDateTime;

public record ConversationItem(int id, int clientId, String clientName, String clientEmail,
                               Integer encadrantId, boolean active, LocalDateTime updatedAt) {
}
