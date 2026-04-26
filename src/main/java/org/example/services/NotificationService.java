package org.example.services;

import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Agrège les messages non lus pour l'utilisateur courant à partir des tables
 * existantes {@code conversations} / {@code messages} (aucune modification de schéma).
 *
 * <p>Comme la table {@code messages} ne possède pas de colonne {@code is_read},
 * l'état « lu / non lu » est mémorisé côté poste client via {@link Preferences} :
 * pour chaque couple (utilisateur, conversation), on conserve l'identifiant du
 * dernier message « vu ». Est considéré comme non lu tout message émis par
 * l'interlocuteur dont l'{@code id} dépasse cette borne.</p>
 */
public final class NotificationService {

    private static final String PREF_NODE = "org/example/planning/notifications";
    private static final String PREF_KEY_FMT = "u%d.c%d";
    private static final String ALLOWED_TYPES = "'MESSAGE','CONSEIL'";

    /**
     * Agrégat par conversation : le correspondant, l'aperçu du dernier message
     * non lu, l'horodatage et le nombre de messages non lus.
     */
    public record UnreadNotification(
            int conversationId,
            int otherUserId,
            String otherUserName,
            String otherUserEmail,
            String lastMessagePreview,
            LocalDateTime lastMessageAt,
            int lastMessageId,
            int unreadCount
    ) {
    }

    /** Total des messages non lus — pratique pour le badge de la cloche. */
    public int countUnreadMessages(int userId) {
        int total = 0;
        for (UnreadNotification n : getUnreadMessages(userId)) {
            total += n.unreadCount();
        }
        return total;
    }

    /**
     * Liste des conversations contenant des messages non lus pour {@code userId}.
     * Trié du plus récent au plus ancien ; les conversations sans message non lu
     * sont omises (spécification : « afficher uniquement les non lus »).
     */
    public List<UnreadNotification> getUnreadMessages(int userId) {
        if (userId <= 0) {
            return List.of();
        }
        try {
            Connection c = conn();
            if (c == null) {
                return List.of();
            }
            List<ConversationRef> refs = loadConversationRefs(c, userId);
            List<UnreadNotification> notifications = new ArrayList<>();
            for (ConversationRef ref : refs) {
                int lastSeen = getLastSeenId(userId, ref.conversationId);
                UnreadSummary summary = loadUnreadForConversation(c, ref, userId, lastSeen);
                if (summary != null && summary.count > 0) {
                    notifications.add(new UnreadNotification(
                            ref.conversationId,
                            ref.otherUserId,
                            ref.otherUserName,
                            ref.otherUserEmail,
                            summary.preview,
                            summary.createdAt,
                            summary.lastMessageId,
                            summary.count
                    ));
                }
            }
            notifications.sort((a, b) -> {
                LocalDateTime ta = a.lastMessageAt();
                LocalDateTime tb = b.lastMessageAt();
                if (ta == null && tb == null) return 0;
                if (ta == null) return 1;
                if (tb == null) return -1;
                return tb.compareTo(ta);
            });
            return notifications;
        } catch (SQLException ignored) {
            return List.of();
        }
    }

    /**
     * Marque les messages de {@code conversationId} comme lus pour {@code userId}.
     * On stocke l'identifiant du dernier message présent côté base ; les suivants
     * redeviendront non lus s'ils sont émis par l'interlocuteur.
     */
    public void markConversationAsRead(int userId, int conversationId) {
        if (userId <= 0 || conversationId <= 0) {
            return;
        }
        try {
            Connection c = conn();
            if (c == null) {
                return;
            }
            int maxId = 0;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT MAX(id) FROM messages WHERE conversation_id = ?")) {
                ps.setInt(1, conversationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        maxId = rs.getInt(1);
                    }
                }
            }
            int current = getLastSeenId(userId, conversationId);
            if (maxId > current) {
                setLastSeenId(userId, conversationId, maxId);
            }
        } catch (SQLException ignored) {
        }
    }

    /** Marque toutes les conversations de l'utilisateur comme lues (dernier message vu). */
    public void markAllAsRead(int userId) {
        if (userId <= 0) {
            return;
        }
        try {
            Connection c = conn();
            if (c == null) {
                return;
            }
            for (ConversationRef ref : loadConversationRefs(c, userId)) {
                markConversationAsRead(userId, ref.conversationId);
            }
        } catch (SQLException ignored) {
        }
    }

    // ---------------------------------------------------------------------
    // Implémentation
    // ---------------------------------------------------------------------

    private record ConversationRef(int conversationId,
                                   int otherUserId,
                                   String otherUserName,
                                   String otherUserEmail) {
    }

    private record UnreadSummary(int count, int lastMessageId, String preview, LocalDateTime createdAt) {
    }

    /**
     * Conversations actives auxquelles {@code userId} participe (client ou encadrant),
     * avec l'identité du correspondant.
     */
    private List<ConversationRef> loadConversationRefs(Connection c, int userId) throws SQLException {
        String sql = """
                SELECT c.id                  AS conversation_id,
                       c.client_id           AS client_id,
                       c.encadrant_id        AS encadrant_id,
                       CASE WHEN c.client_id = ? THEN c.encadrant_id ELSE c.client_id END AS other_id,
                       u.first_name_user     AS other_first,
                       u.last_name_user      AS other_last,
                       u.email_user          AS other_email
                FROM conversations c
                LEFT JOIN users u
                       ON u.id_user = CASE WHEN c.client_id = ? THEN c.encadrant_id ELSE c.client_id END
                WHERE c.is_active = 1
                  AND (c.client_id = ? OR c.encadrant_id = ?)
                ORDER BY c.updated_at DESC
                """;
        List<ConversationRef> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            ps.setInt(4, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int otherId = rs.getInt("other_id");
                    if (rs.wasNull() || otherId <= 0) {
                        continue;
                    }
                    String fn = rs.getString("other_first");
                    String ln = rs.getString("other_last");
                    String fullName = ((fn == null ? "" : fn) + " " + (ln == null ? "" : ln)).trim();
                    if (fullName.isEmpty()) {
                        fullName = "Utilisateur";
                    }
                    String email = rs.getString("other_email");
                    list.add(new ConversationRef(
                            rs.getInt("conversation_id"),
                            otherId,
                            fullName,
                            email == null ? "" : email
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Compte / dernier message non lu émis par l'interlocuteur depuis {@code afterMessageId}
     * pour la conversation {@code ref}.
     */
    private UnreadSummary loadUnreadForConversation(Connection c, ConversationRef ref, int userId, int afterMessageId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*) AS cnt,
                       MAX(id)  AS max_id
                FROM messages
                WHERE conversation_id = ?
                  AND sender_id <> ?
                  AND type IN (%s)
                  AND id > ?
                """.formatted(ALLOWED_TYPES);
        int cnt = 0;
        int maxId = 0;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ref.conversationId);
            ps.setInt(2, userId);
            ps.setInt(3, afterMessageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cnt = rs.getInt("cnt");
                    maxId = rs.getInt("max_id");
                    if (rs.wasNull()) {
                        maxId = 0;
                    }
                }
            }
        }
        if (cnt <= 0 || maxId <= 0) {
            return null;
        }
        String preview = "";
        LocalDateTime at = null;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT contenu, created_at FROM messages WHERE id = ?")) {
            ps.setInt(1, maxId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    preview = rs.getString("contenu");
                    if (preview == null) {
                        preview = "";
                    }
                    if (preview.length() > 120) {
                        preview = preview.substring(0, 117) + "…";
                    }
                    Timestamp ts = rs.getTimestamp("created_at");
                    at = ts != null ? ts.toLocalDateTime() : null;
                }
            }
        }
        return new UnreadSummary(cnt, maxId, preview, at);
    }

    // ---------------------------------------------------------------------
    // Persistance « last seen » (Preferences locales, hors base de données)
    // ---------------------------------------------------------------------

    private static int getLastSeenId(int userId, int conversationId) {
        return prefs().getInt(keyFor(userId, conversationId), 0);
    }

    private static void setLastSeenId(int userId, int conversationId, int messageId) {
        prefs().putInt(keyFor(userId, conversationId), messageId);
    }

    private static String keyFor(int userId, int conversationId) {
        return String.format(PREF_KEY_FMT, userId, conversationId);
    }

    private static Preferences prefs() {
        return Preferences.userRoot().node(PREF_NODE);
    }

    private static Connection conn() throws SQLException {
        Connection c = MyDataBase.getInstance().getConnection();
        if (c == null || c.isClosed()) {
            return null;
        }
        return c;
    }
}
