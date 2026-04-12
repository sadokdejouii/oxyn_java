package org.example.entities;

import java.time.LocalDateTime;

public record MessageRow(int id, int senderId, String senderName, String contenu, String type,
                           LocalDateTime createdAt) {
}
