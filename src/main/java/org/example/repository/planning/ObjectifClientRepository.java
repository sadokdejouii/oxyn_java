package org.example.repository.planning;

import org.example.model.planning.objectif.ObjectifClientAdminRow;
import org.example.model.planning.objectif.ObjectifClientRow;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persistance des objectifs libres sur la table existante {@code objectifs_hebdomadaires}. */
public final class ObjectifClientRepository {

    public Optional<ObjectifClientRow> findLatestByUserId(int userId) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, user_id,
                       objectifs AS texte_objectif,
                       message_ia AS reponse_ia,
                       message_encadrant AS intervention_encadrant,
                       COALESCE(updated_at, created_at) AS date_enregistrement
                FROM objectifs_hebdomadaires
                WHERE user_id = ?
                ORDER BY COALESCE(updated_at, created_at) DESC, id DESC
                LIMIT 1
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    public Optional<ObjectifClientRow> findById(int id) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, user_id,
                       objectifs AS texte_objectif,
                       message_ia AS reponse_ia,
                       message_encadrant AS intervention_encadrant,
                       COALESCE(updated_at, created_at) AS date_enregistrement
                FROM objectifs_hebdomadaires
                WHERE id = ?
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

    public int insert(int userId, String texteObjectif, String reponseIa, String motsCles, String idsProduitsCsv)
            throws SQLException {
        Connection c = conn();
        if (c == null) {
            throw new SQLException("Connexion base indisponible.");
        }
        LocalDate today = LocalDate.now();
        int week = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = today.get(IsoFields.WEEK_BASED_YEAR);

        String objectifPrincipal = motsCles != null && !motsCles.isBlank() ? motsCles : "Objectif libre";

        String selectExisting = """
                SELECT id
                FROM objectifs_hebdomadaires
                WHERE user_id = ? AND week_number = ? AND year = ?
                LIMIT 1
                """;
        try (PreparedStatement ps = c.prepareStatement(selectExisting)) {
            ps.setInt(1, userId);
            ps.setInt(2, week);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int existingId = rs.getInt("id");
                    String updateSql = """
                            UPDATE objectifs_hebdomadaires
                            SET objectifs = ?,
                                objectif_principal = ?,
                                message_ia = ?,
                                updated_at = NOW()
                            WHERE id = ?
                            """;
                    try (PreparedStatement up = c.prepareStatement(updateSql)) {
                        up.setString(1, texteObjectif);
                        up.setString(2, objectifPrincipal);
                        up.setString(3, reponseIa);
                        up.setInt(4, existingId);
                        up.executeUpdate();
                    }
                    return existingId;
                }
            }
        }

        String sql = """
                INSERT INTO objectifs_hebdomadaires
                (week_number, year, objectifs, objectif_principal, taches_prevues, taches_realisees,
                 taux_realisation, statut, message_ia, message_encadrant, efforts_valides,
                 message_encadrant_date, created_at, updated_at, user_id)
                VALUES (?,?,?,?,0,0,0,'EN_COURS',?,?,NULL,NULL,NOW(),NOW(),?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, week);
            ps.setInt(2, year);
            ps.setString(3, texteObjectif);
            ps.setString(4, objectifPrincipal);
            ps.setString(5, reponseIa);
            ps.setString(6, null);
            ps.setInt(7, userId);
            ps.executeUpdate();
        }

        String selectLast = """
                SELECT id
                FROM objectifs_hebdomadaires
                WHERE user_id = ?
                ORDER BY COALESCE(updated_at, created_at) DESC, id DESC
                LIMIT 1
                """;
        try (PreparedStatement ps = c.prepareStatement(selectLast)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        throw new SQLException("Insertion objectif hebdomadaire sans id relu.");
    }

    public void updateIntervention(int objectifId, String interventionEncadrant) throws SQLException {
        Connection c = conn();
        if (c == null) {
            throw new SQLException("Connexion base indisponible.");
        }
        String sql = """
                UPDATE objectifs_hebdomadaires
                SET message_encadrant = ?, message_encadrant_date = NOW(), updated_at = NOW()
                WHERE id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, interventionEncadrant);
            ps.setInt(2, objectifId);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new SQLException("Aucune ligne objectifs_hebdomadaires pour id=" + objectifId);
            }
        }
    }

    /**
     * Met à jour l’intervention sur le dernier objectif enregistré pour l’utilisateur.
     */
    public void updateInterventionOnLatestForUser(int userId, String interventionEncadrant) throws SQLException {
        Optional<ObjectifClientRow> latest = findLatestByUserId(userId);
        if (latest.isEmpty()) {
            throw new SQLException("Aucun objectif libre enregistré pour ce client.");
        }
        updateIntervention(latest.get().id(), interventionEncadrant);
    }

    public List<ObjectifClientAdminRow> listRecentWithUsers(int limit) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return List.of();
        }
        int lim = Math.min(Math.max(limit, 1), 300);
        String sql = """
                SELECT o.id, o.user_id,
                       o.objectifs AS texte_objectif,
                       o.message_ia AS reponse_ia,
                       o.message_encadrant AS intervention_encadrant,
                       COALESCE(o.updated_at, o.created_at) AS date_enregistrement,
                       u.first_name_user, u.last_name_user, u.email_user
                FROM objectifs_hebdomadaires o
                INNER JOIN users u ON u.id_user = o.user_id
                ORDER BY COALESCE(o.updated_at, o.created_at) DESC, o.id DESC
                LIMIT ?
                """;
        List<ObjectifClientAdminRow> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, lim);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String fn = rs.getString("first_name_user");
                    String ln = rs.getString("last_name_user");
                    String name = ((fn != null ? fn : "") + " " + (ln != null ? ln : "")).trim();
                    if (name.isEmpty()) {
                        name = "—";
                    }
                    out.add(new ObjectifClientAdminRow(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            name,
                            rs.getString("email_user") != null ? rs.getString("email_user") : "",
                            rs.getString("texte_objectif"),
                            rs.getString("reponse_ia"),
                            "",
                            "",
                            "",
                            toLdt(rs.getTimestamp("date_enregistrement")),
                            rs.getString("intervention_encadrant")));
                }
            }
        }
        return out;
    }

    private static ObjectifClientRow mapRow(ResultSet rs) throws SQLException {
        return new ObjectifClientRow(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("texte_objectif"),
                rs.getString("reponse_ia"),
                "",
                "",
                toLdt(rs.getTimestamp("date_enregistrement")),
                rs.getString("intervention_encadrant"));
    }

    private static LocalDateTime toLdt(Timestamp ts) {
        if (ts == null) {
            return LocalDateTime.now();
        }
        return ts.toLocalDateTime();
    }

    private static Connection conn() throws SQLException {
        Connection c = MyDataBase.getInstance().getConnection();
        if (c == null || c.isClosed()) {
            return null;
        }
        return c;
    }
}
