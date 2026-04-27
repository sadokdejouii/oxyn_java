package org.example.temporal;

import java.time.Instant;

public record TemporalPermissionGrant(
        long id,
        int userId,
        String permissionKey,
        String scopeType,
        String scopeId,
        Integer grantedByUserId,
        Instant startsAt,
        Instant expiresAt,
        Instant revokedAt,
        String note,
        Instant createdAt,
        Instant lastNotifiedAt
) {
    public boolean isActiveAt(Instant now) {
        if (now == null) return false;
        if (revokedAt != null) return false;
        if (startsAt != null && now.isBefore(startsAt)) return false;
        return expiresAt != null && now.isBefore(expiresAt);
    }
}

