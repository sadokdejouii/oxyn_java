package org.example.temporal;

import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Stockage des permissions temporaires (grants) avec expiration.
 *
 * Modèle "API-like" dans le client JavaFX (DAO + Service).
 */
public final class TemporalPermissionDAO {

    private final Connection con;

    public TemporalPermissionDAO() {
        this.con = MyDataBase.getInstance().getConnection();
        try {
            ensureTable();
        } catch (SQLException e) {
            System.err.println("[TemporalPermissionDAO] ensureTable failed: " + e.getMessage());
        }
    }

    public void ensureTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS temporal_permission_grants (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    permission_key VARCHAR(190) NOT NULL,
                    scope_type VARCHAR(60) DEFAULT NULL,
                    scope_id VARCHAR(190) DEFAULT NULL,
                    granted_by_user_id INT DEFAULT NULL,
                    starts_at TIMESTAMP NULL,
                    expires_at TIMESTAMP NOT NULL,
                    revoked_at TIMESTAMP NULL,
                    note VARCHAR(255) DEFAULT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    last_notified_at TIMESTAMP NULL,
                    INDEX idx_tpg_user (user_id),
                    INDEX idx_tpg_perm (permission_key),
                    INDEX idx_tpg_expires (expires_at),
                    INDEX idx_tpg_scope (scope_type, scope_id)
                )
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.execute();
        }
    }

    public long grant(int userId,
                      String permissionKey,
                      String scopeType,
                      String scopeId,
                      Integer grantedByUserId,
                      Duration ttl,
                      String note) throws SQLException {
        if (userId <= 0) throw new SQLException("userId invalide");
        if (permissionKey == null || permissionKey.isBlank()) throw new SQLException("permissionKey obligatoire");
        Duration effectiveTtl = (ttl != null) ? ttl : Duration.ofHours(24);
        Instant now = Instant.now();
        Instant expires = now.plus(effectiveTtl);

        String sql = """
                INSERT INTO temporal_permission_grants
                (user_id, permission_key, scope_type, scope_id, granted_by_user_id, starts_at, expires_at, note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, permissionKey.trim());
            ps.setString(3, (scopeType == null || scopeType.isBlank()) ? null : scopeType.trim());
            ps.setString(4, (scopeId == null || scopeId.isBlank()) ? null : scopeId.trim());
            if (grantedByUserId != null) {
                ps.setInt(5, grantedByUserId);
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }
            ps.setTimestamp(6, Timestamp.from(now));
            ps.setTimestamp(7, Timestamp.from(expires));
            ps.setString(8, (note == null || note.isBlank()) ? null : note.trim());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Impossible de créer le grant");
    }

    public boolean revoke(long grantId, String note) throws SQLException {
        String sql = """
                UPDATE temporal_permission_grants
                SET revoked_at = CURRENT_TIMESTAMP,
                    note = COALESCE(?, note)
                WHERE id = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, (note == null || note.isBlank()) ? null : note.trim());
            ps.setLong(2, grantId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean markNotified(long grantId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE temporal_permission_grants SET last_notified_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setLong(1, grantId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean hasActive(int userId, String permissionKey, String scopeType, String scopeId) throws SQLException {
        String sql = """
                SELECT 1
                FROM temporal_permission_grants
                WHERE user_id = ?
                  AND permission_key = ?
                  AND revoked_at IS NULL
                  AND (starts_at IS NULL OR starts_at <= CURRENT_TIMESTAMP)
                  AND expires_at > CURRENT_TIMESTAMP
                  AND ( ? IS NULL OR scope_type = ? )
                  AND ( ? IS NULL OR scope_id = ? )
                LIMIT 1
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, permissionKey);
            ps.setString(3, scopeType);
            ps.setString(4, scopeType);
            ps.setString(5, scopeId);
            ps.setString(6, scopeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<TemporalPermissionGrant> timelineForUser(int userId, int limit) throws SQLException {
        int lim = (limit <= 0) ? 200 : Math.min(limit, 1000);
        String sql = """
                SELECT id, user_id, permission_key, scope_type, scope_id, granted_by_user_id,
                       starts_at, expires_at, revoked_at, note, created_at, last_notified_at
                FROM temporal_permission_grants
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """;
        List<TemporalPermissionGrant> out = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, lim);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        }
        return out;
    }

    public List<TemporalPermissionGrant> expiringSoonForUser(int userId, Duration within, int limit) throws SQLException {
        Duration w = Objects.requireNonNullElse(within, Duration.ofDays(3));
        int lim = (limit <= 0) ? 50 : Math.min(limit, 200);
        Instant now = Instant.now();
        Instant end = now.plus(w);

        String sql = """
                SELECT id, user_id, permission_key, scope_type, scope_id, granted_by_user_id,
                       starts_at, expires_at, revoked_at, note, created_at, last_notified_at
                FROM temporal_permission_grants
                WHERE user_id = ?
                  AND revoked_at IS NULL
                  AND expires_at > ?
                  AND expires_at <= ?
                ORDER BY expires_at ASC
                LIMIT ?
                """;
        List<TemporalPermissionGrant> out = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.from(now));
            ps.setTimestamp(3, Timestamp.from(end));
            ps.setInt(4, lim);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        }
        return out;
    }

    public Optional<TemporalPermissionGrant> getById(long id) throws SQLException {
        String sql = """
                SELECT id, user_id, permission_key, scope_type, scope_id, granted_by_user_id,
                       starts_at, expires_at, revoked_at, note, created_at, last_notified_at
                FROM temporal_permission_grants
                WHERE id = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    private static TemporalPermissionGrant map(ResultSet rs) throws SQLException {
        return new TemporalPermissionGrant(
                rs.getLong("id"),
                rs.getInt("user_id"),
                rs.getString("permission_key"),
                rs.getString("scope_type"),
                rs.getString("scope_id"),
                (Integer) rs.getObject("granted_by_user_id"),
                ts(rs.getTimestamp("starts_at")),
                ts(rs.getTimestamp("expires_at")),
                ts(rs.getTimestamp("revoked_at")),
                rs.getString("note"),
                ts(rs.getTimestamp("created_at")),
                ts(rs.getTimestamp("last_notified_at"))
        );
    }

    private static Instant ts(Timestamp t) {
        return t != null ? t.toInstant() : null;
    }
}

