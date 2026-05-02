package org.example.temporal;

import org.example.entities.User;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;

/**
 * Service métier pour permissions temporaires (Temporal Trust).
 */
public final class TemporalPermissionService {

    // Permission keys (exemples)
    public static final String PERM_COACH_GROUP_ACCESS = "coach.group.access";
    public static final String PERM_TEMP_ADMIN = "admin.temp";

    private final TemporalPermissionDAO dao = new TemporalPermissionDAO();
    private final TemporalTrustConfig.Cfg cfg = TemporalTrustConfig.load();

    public long grantCoachGroupAccess(int userId, String groupId, Integer grantedByUserId, Duration ttlOverride, String note) throws SQLException {
        Duration ttl = (ttlOverride != null) ? ttlOverride : cfg.coachGroupAccess();
        return dao.grant(userId, PERM_COACH_GROUP_ACCESS, "group", groupId, grantedByUserId, ttl, note);
    }

    public long grantTempAdmin(int userId, Integer grantedByUserId, Duration ttlOverride, String note) throws SQLException {
        Duration ttl = (ttlOverride != null) ? ttlOverride : cfg.tempAdmin();
        return dao.grant(userId, PERM_TEMP_ADMIN, null, null, grantedByUserId, ttl, note);
    }

    public boolean hasPermission(int userId, String permissionKey, String scopeType, String scopeId) throws SQLException {
        return dao.hasActive(userId, permissionKey, scopeType, scopeId);
    }

    public List<TemporalPermissionGrant> timelineForUser(int userId, int limit) throws SQLException {
        return dao.timelineForUser(userId, limit);
    }

    public List<TemporalPermissionGrant> expiringSoonForUser(int userId, Duration within, int limit) throws SQLException {
        return dao.expiringSoonForUser(userId, within != null ? within : cfg.notifyBeforeExpiry(), limit);
    }

    public boolean revoke(long grantId, String note) throws SQLException {
        return dao.revoke(grantId, note);
    }

    public TemporalTrustConfig.Cfg config() {
        return cfg;
    }
}

