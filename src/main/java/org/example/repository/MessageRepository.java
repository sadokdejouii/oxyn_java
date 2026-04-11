package org.example.repository;



import org.example.entities.MessageEntity;

import org.example.utils.MyDataBase;



import java.sql.Connection;

import java.sql.PreparedStatement;

import java.sql.ResultSet;

import java.sql.SQLException;

import java.sql.Statement;

import java.sql.Timestamp;

import java.util.ArrayList;

import java.util.List;

import java.util.Optional;



/**

 * JDBC — table {@code messages}.

 */

public final class MessageRepository {



    public Optional<MessageEntity> findById(int id) throws SQLException {

        Connection c = conn();

        if (c == null) {

            return Optional.empty();

        }

        String sql = """

                SELECT id, conversation_id, sender_id, contenu, type, created_at, is_ai_generated

                FROM messages WHERE id = ?

                """;

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {

                    return Optional.empty();

                }

                return Optional.of(mapRow(rs));

            }

        }

    }



    public List<MessageEntity> findByConversationId(int conversationId) throws SQLException {

        Connection c = conn();

        if (c == null) {

            return List.of();

        }

        String sql = """

                SELECT id, conversation_id, sender_id, contenu, type, created_at, is_ai_generated

                FROM messages WHERE conversation_id = ?

                ORDER BY id ASC

                """;

        List<MessageEntity> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, conversationId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    list.add(mapRow(rs));

                }

            }

        }

        return list;

    }



    /**

     * Messages dont l’expéditeur est {@code senderId} ({@code sender_id}).

     */

    public List<MessageEntity> findByUserId(int senderId) throws SQLException {

        Connection c = conn();

        if (c == null) {

            return List.of();

        }

        String sql = """

                SELECT id, conversation_id, sender_id, contenu, type, created_at, is_ai_generated

                FROM messages WHERE sender_id = ?

                ORDER BY id DESC

                LIMIT 5000

                """;

        List<MessageEntity> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, senderId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    list.add(mapRow(rs));

                }

            }

        }

        return list;

    }



    public int insert(int conversationId, int senderId, String contenu, String type, boolean isAiGenerated)

            throws SQLException {

        Connection c = conn();

        if (c == null) {

            throw new SQLException("Pas de connexion MySQL");

        }

        String sql = """

                INSERT INTO messages (contenu, type, created_at, is_ai_generated, conversation_id, sender_id)

                VALUES (?, ?, NOW(), ?, ?, ?)

                """;

        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, contenu);

            ps.setString(2, type);

            ps.setBoolean(3, isAiGenerated);

            ps.setInt(4, conversationId);

            ps.setInt(5, senderId);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {

                if (keys.next()) {

                    return keys.getInt(1);

                }

            }

        }

        throw new SQLException("INSERT messages sans clé générée");

    }



    public void update(int messageId, String contenu, String type) throws SQLException {

        Connection c = conn();

        if (c == null) {

            throw new SQLException("Pas de connexion MySQL");

        }

        String sql = "UPDATE messages SET contenu = ?, type = ? WHERE id = ?";

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, contenu);

            ps.setString(2, type);

            ps.setInt(3, messageId);

            int n = ps.executeUpdate();

            if (n == 0) {

                throw new SQLException("Message introuvable");

            }

        }

    }



    public void deleteById(int id) throws SQLException {

        Connection c = conn();

        if (c == null) {

            throw new SQLException("Pas de connexion MySQL");

        }

        try (PreparedStatement ps = c.prepareStatement("DELETE FROM messages WHERE id = ?")) {

            ps.setInt(1, id);

            ps.executeUpdate();

        }

    }



    public static MessageEntity mapRow(ResultSet rs) throws SQLException {

        Timestamp ts = rs.getTimestamp("created_at");

        return new MessageEntity(

                rs.getInt("id"),

                rs.getInt("conversation_id"),

                rs.getInt("sender_id"),

                rs.getString("contenu"),

                rs.getString("type"),

                ts != null ? ts.toLocalDateTime() : null,

                rs.getObject("is_ai_generated") != null && rs.getBoolean("is_ai_generated")

        );

    }



    private static Connection conn() throws SQLException {

        Connection c = MyDataBase.getInstance().getConnection();

        if (c == null || c.isClosed()) {

            return null;

        }

        return c;

    }

}

