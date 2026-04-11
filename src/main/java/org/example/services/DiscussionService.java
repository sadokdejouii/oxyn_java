package org.example.services;

import org.example.entities.ConversationInboxItem;
import org.example.entities.ConversationItem;
import org.example.entities.MessageRow;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Conversations / messages — tables {@code conversations} et {@code messages}.
 */
public final class DiscussionService {

    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_CONSEIL = "CONSEIL";

    public Optional<Integer> findConversationIdByClientId(int clientId) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return Optional.empty();
        }
        String sql = "SELECT id FROM conversations WHERE client_id = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("id"));
                }
            }
        }
        return Optional.empty();
    }

    public int ensureConversationForClient(int clientId, int preferredEncadrantId) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return -1;
        }
        Optional<Integer> existing = findConversationIdByClientId(clientId);
        int enc = preferredEncadrantId > 0 ? preferredEncadrantId : findDefaultEncadrantUserId(c);
        if (existing.isPresent()) {
            int cid = existing.get();
            String upd = """
                    UPDATE conversations SET is_active = 1, encadrant_id = COALESCE(encadrant_id, ?),
                    updated_at = NOW() WHERE id = ?
                    """;
            try (PreparedStatement ps = c.prepareStatement(upd)) {
                if (enc > 0) {
                    ps.setInt(1, enc);
                } else {
                    ps.setNull(1, Types.INTEGER);
                }
                ps.setInt(2, cid);
                ps.executeUpdate();
            }
            return cid;
        }
        String ins = """
                INSERT INTO conversations (is_active, created_at, updated_at, client_id, encadrant_id)
                VALUES (1, NOW(), NOW(), ?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, clientId);
            if (enc > 0) {
                ps.setInt(2, enc);
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return findConversationIdByClientId(clientId).orElse(-1);
    }

    public void assignEncadrantToConversation(int conversationId, int encadrantUserId) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return;
        }
        String sql = "UPDATE conversations SET encadrant_id = ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, encadrantUserId);
            ps.setInt(2, conversationId);
            ps.executeUpdate();
        }
    }

    public List<ConversationItem> listActiveConversations() throws SQLException {
        Connection c = conn();
        if (c == null) {
            return List.of();
        }
        String sql = """
                SELECT c.id, c.client_id, c.encadrant_id, c.is_active, c.updated_at,
                       u.first_name_user, u.last_name_user, u.email_user
                FROM conversations c
                JOIN users u ON u.id_user = c.client_id
                WHERE c.is_active = 1
                ORDER BY c.updated_at DESC
                """;
        List<ConversationItem> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String fn = rs.getString("first_name_user");
                String ln = rs.getString("last_name_user");
                String name = ((fn == null ? "" : fn) + " " + (ln == null ? "" : ln)).trim();
                Timestamp ts = rs.getTimestamp("updated_at");
                list.add(new ConversationItem(
                        rs.getInt("id"),
                        rs.getInt("client_id"),
                        name,
                        rs.getString("email_user"),
                        (Integer) rs.getObject("encadrant_id"),
                        rs.getBoolean("is_active"),
                        ts != null ? ts.toLocalDateTime() : null
                ));
            }
        }
        return list;
    }

    /** Liste conversations actives avec aperçu du dernier message (vue type Messenger). */
    public List<ConversationInboxItem> listConversationInbox() throws SQLException {
        Connection c = conn();
        if (c == null) {
            return List.of();
        }
        String sql = """
                SELECT c.id, c.client_id, c.updated_at,
                       u.first_name_user, u.last_name_user, u.email_user,
                       (SELECT m.contenu FROM messages m
                        WHERE m.conversation_id = c.id
                        ORDER BY m.id DESC LIMIT 1) AS last_body,
                       (SELECT m.created_at FROM messages m
                        WHERE m.conversation_id = c.id
                        ORDER BY m.id DESC LIMIT 1) AS last_at
                FROM conversations c
                JOIN users u ON u.id_user = c.client_id
                WHERE c.is_active = 1
                ORDER BY c.updated_at DESC
                """;
        List<ConversationInboxItem> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String fn = rs.getString("first_name_user");
                String ln = rs.getString("last_name_user");
                String name = ((fn == null ? "" : fn) + " " + (ln == null ? "" : ln)).trim();
                if (name.isBlank()) {
                    name = "Client";
                }
                Timestamp up = rs.getTimestamp("updated_at");
                Timestamp la = rs.getTimestamp("last_at");
                String preview = rs.getString("last_body");
                if (preview != null && preview.length() > 120) {
                    preview = preview.substring(0, 117) + "…";
                }
                list.add(new ConversationInboxItem(
                        rs.getInt("id"),
                        rs.getInt("client_id"),
                        name,
                        rs.getString("email_user") != null ? rs.getString("email_user") : "",
                        preview != null ? preview : "Aucun message",
                        la != null ? la.toLocalDateTime() : null,
                        up != null ? up.toLocalDateTime() : null
                ));
            }
        }
        return list;
    }

    public List<MessageRow> loadMessages(int conversationId, int afterId) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return List.of();
        }
        String sql = """
                SELECT m.id, m.sender_id, m.contenu, m.type, m.created_at,
                       u.first_name_user, u.last_name_user
                FROM messages m
                JOIN users u ON u.id_user = m.sender_id
                WHERE m.conversation_id = ? AND m.id > ?
                  AND m.type NOT IN ('IA_SUGGESTION')
                ORDER BY m.id ASC
                LIMIT 300
                """;
        List<MessageRow> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.setInt(2, afterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String fn = rs.getString("first_name_user");
                    String ln = rs.getString("last_name_user");
                    String name = ((fn == null ? "" : fn) + " " + (ln == null ? "" : ln)).trim();
                    Timestamp ts = rs.getTimestamp("created_at");
                    list.add(new MessageRow(
                            rs.getInt("id"),
                            rs.getInt("sender_id"),
                            name,
                            rs.getString("contenu"),
                            rs.getString("type"),
                            ts != null ? ts.toLocalDateTime() : null
                    ));
                }
            }
        }
        return list;
    }

    public int sendMessage(int conversationId, int senderId, String contenu, boolean fromEncadrant) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return -1;
        }
        String type = fromEncadrant ? TYPE_CONSEIL : TYPE_MESSAGE;
        String sql = """
                INSERT INTO messages (contenu, type, created_at, is_ai_generated, conversation_id, sender_id)
                VALUES (?, ?, NOW(), 0, ?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, contenu);
            ps.setString(2, type);
            ps.setInt(3, conversationId);
            ps.setInt(4, senderId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int mid = keys.getInt(1);
                    touchConversation(c, conversationId);
                    return mid;
                }
            }
        }
        return -1;
    }

    private static void touchConversation(Connection c, int conversationId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE conversations SET updated_at = NOW() WHERE id = ?")) {
            ps.setInt(1, conversationId);
            ps.executeUpdate();
        }
    }

    private static int findDefaultEncadrantUserId(Connection c) throws SQLException {
        String sql = """
                SELECT id_user FROM users
                WHERE roles_user LIKE '%ROLE_ENCADRANT%'
                ORDER BY id_user ASC LIMIT 1
                """;
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id_user");
            }
        }
        return -1;
    }

    private static Connection conn() throws SQLException {
        Connection c = MyDataBase.getInstance().getConnection();
        if (c == null || c.isClosed()) {
            return null;
        }
        return c;
    }
}
