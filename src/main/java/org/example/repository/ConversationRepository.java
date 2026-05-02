package org.example.repository;



import org.example.entities.ConversationEntity;

import org.example.utils.MyDataBase;



import java.sql.Connection;

import java.sql.PreparedStatement;

import java.sql.ResultSet;

import java.sql.SQLException;

import java.sql.Statement;

import java.sql.Timestamp;

import java.sql.Types;

import java.util.ArrayList;

import java.util.List;

import java.util.Optional;



/**

 * JDBC — table {@code conversations}.

 */

public final class ConversationRepository {



    public Optional<ConversationEntity> findById(int id) throws SQLException {

        Connection c = conn();

        if (c == null) {

            return Optional.empty();

        }

        String sql = """

                SELECT id, client_id, encadrant_id, is_active, created_at, updated_at

                FROM conversations WHERE id = ?

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



    /**

     * Toutes les conversations dont l’utilisateur est le client ({@code client_id}).

     */

    public List<ConversationEntity> findByUserId(int clientUserId) throws SQLException {

        Connection c = conn();

        if (c == null) {

            return List.of();

        }

        String sql = """

                SELECT id, client_id, encadrant_id, is_active, created_at, updated_at

                FROM conversations WHERE client_id = ?

                ORDER BY id DESC

                """;

        List<ConversationEntity> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, clientUserId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    list.add(mapRow(rs));

                }

            }

        }

        return list;

    }



    public Optional<ConversationEntity> findLatestByClientId(int clientUserId) throws SQLException {

        Connection c = conn();

        if (c == null) {

            return Optional.empty();

        }

        String sql = """

                SELECT id, client_id, encadrant_id, is_active, created_at, updated_at

                FROM conversations WHERE client_id = ? ORDER BY id DESC LIMIT 1

                """;

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, clientUserId);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {

                    return Optional.empty();

                }

                return Optional.of(mapRow(rs));

            }

        }

    }



    public int insert(boolean isActive, int clientId, Integer encadrantId) throws SQLException {

        Connection c = conn();

        if (c == null) {

            throw new SQLException("Pas de connexion MySQL");

        }

        String sql = """

                INSERT INTO conversations (is_active, created_at, updated_at, client_id, encadrant_id)

                VALUES (?, NOW(), NOW(), ?, ?)

                """;

        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setBoolean(1, isActive);

            ps.setInt(2, clientId);

            if (encadrantId != null) {

                ps.setInt(3, encadrantId);

            } else {

                ps.setNull(3, Types.INTEGER);

            }

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {

                if (keys.next()) {

                    return keys.getInt(1);

                }

            }

        }

        throw new SQLException("INSERT conversations sans clé générée");

    }



    public void update(ConversationEntity row) throws SQLException {

        Connection c = conn();

        if (c == null) {

            throw new SQLException("Pas de connexion MySQL");

        }

        String sql = """

                UPDATE conversations SET

                    client_id = ?, encadrant_id = ?, is_active = ?, updated_at = NOW()

                WHERE id = ?

                """;

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, row.clientId());

            if (row.encadrantId() != null) {

                ps.setInt(2, row.encadrantId());

            } else {

                ps.setNull(2, Types.INTEGER);

            }

            ps.setBoolean(3, row.isActive());

            ps.setInt(4, row.id());

            int n = ps.executeUpdate();

            if (n == 0) {

                throw new SQLException("Conversation introuvable");

            }

        }

    }



    public void deleteById(int id) throws SQLException {

        Connection c = conn();

        if (c == null) {

            throw new SQLException("Pas de connexion MySQL");

        }

        try (PreparedStatement ps = c.prepareStatement("DELETE FROM conversations WHERE id = ?")) {

            ps.setInt(1, id);

            ps.executeUpdate();

        }

    }



    public static ConversationEntity mapRow(ResultSet rs) throws SQLException {

        Timestamp ca = rs.getTimestamp("created_at");

        Timestamp ua = rs.getTimestamp("updated_at");

        return new ConversationEntity(

                rs.getInt("id"),

                rs.getInt("client_id"),

                (Integer) rs.getObject("encadrant_id"),

                rs.getObject("is_active") != null && rs.getBoolean("is_active"),

                ca != null ? ca.toLocalDateTime() : null,

                ua != null ? ua.toLocalDateTime() : null

        );

    }



    private static Connection conn() throws SQLException {

        Connection c = MyDataBase.getConnection();

        if (c == null || c.isClosed()) {

            return null;

        }

        return c;

    }

}

