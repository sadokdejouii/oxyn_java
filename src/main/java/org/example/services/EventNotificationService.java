package org.example.services;

import org.example.entities.Evenement;
import org.example.entities.EventNotification;
import org.example.entities.InscriptionEvenement;
import org.example.utils.MyDataBase;
import org.example.utils.SqlDateReaders;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EventNotificationService {

    private final Connection con;
    private final InscriptionEvenementServices inscriptionEvenementServices;

    public EventNotificationService() {
        this.con = MyDataBase.getInstance().getConnection();
        this.inscriptionEvenementServices = new InscriptionEvenementServices();
        ensureTable();
    }

    private void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS event_notifications ("
                + "id_notification INT AUTO_INCREMENT PRIMARY KEY, "
                + "id_user_notification INT NOT NULL, "
                + "id_evenement_notification INT NOT NULL, "
                + "titre_notification VARCHAR(255) NOT NULL, "
                + "message_notification VARCHAR(1000) NOT NULL, "
                + "lu_notification TINYINT(1) NOT NULL DEFAULT 0, "
                + "created_at_notification TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "INDEX idx_event_notifications_user (id_user_notification), "
                + "INDEX idx_event_notifications_event (id_evenement_notification), "
                + "INDEX idx_event_notifications_unread (id_user_notification, lu_notification), "
                + "CONSTRAINT fk_event_notifications_event FOREIGN KEY (id_evenement_notification) "
                + "REFERENCES evenements(id_evenement) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci";

        try (Statement st = con.createStatement()) {
            st.execute(sql);
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible d'initialiser la table des notifications d'événement.", ex);
        }
    }

    public void createCancellationNotifications(Evenement event) throws SQLException {
        if (event == null || event.getId() <= 0) {
            return;
        }

        Set<Integer> recipientIds = new LinkedHashSet<>();
        for (InscriptionEvenement inscription : inscriptionEvenementServices.afficher()) {
            if (inscription.getIdEvenement() == event.getId() && isActiveInscription(inscription.getStatut())) {
                recipientIds.add(inscription.getIdUser());
            }
        }

        if (recipientIds.isEmpty()) {
            return;
        }

        String title = "Événement annulé";
        String eventTitle = event.getTitre() == null || event.getTitre().isBlank() ? "cet événement" : event.getTitre().trim();
        String message = "L'événement \"" + eventTitle + "\" a été annulé. Veuillez consulter les détails avant de vous déplacer.";
        String sql = "INSERT INTO event_notifications (id_user_notification, id_evenement_notification, titre_notification, message_notification, lu_notification) VALUES (?, ?, ?, ?, 0)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Integer recipientId : recipientIds) {
                ps.setInt(1, recipientId);
                ps.setInt(2, event.getId());
                ps.setString(3, title);
                ps.setString(4, message);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<EventNotification> findByUser(int userId, int limit) throws SQLException {
        List<EventNotification> notifications = new ArrayList<>();
        if (userId <= 0) {
            return notifications;
        }

        String sql = "SELECT id_notification, id_user_notification, id_evenement_notification, titre_notification, message_notification, lu_notification, created_at_notification "
                + "FROM event_notifications WHERE id_user_notification = ? ORDER BY created_at_notification DESC LIMIT ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EventNotification notification = new EventNotification();
                    notification.setId(rs.getInt("id_notification"));
                    notification.setUserId(rs.getInt("id_user_notification"));
                    notification.setEventId(rs.getInt("id_evenement_notification"));
                    notification.setTitle(rs.getString("titre_notification"));
                    notification.setMessage(rs.getString("message_notification"));
                    notification.setRead(rs.getBoolean("lu_notification"));
                    notification.setCreatedAt(SqlDateReaders.readTimestampOrNull(rs, "created_at_notification"));
                    notifications.add(notification);
                }
            }
        }
        return notifications;
    }

    public int countUnreadByUser(int userId) throws SQLException {
        if (userId <= 0) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM event_notifications WHERE id_user_notification = ? AND lu_notification = 0";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void markAllAsRead(int userId) throws SQLException {
        if (userId <= 0) {
            return;
        }

        String sql = "UPDATE event_notifications SET lu_notification = 1 WHERE id_user_notification = ? AND lu_notification = 0";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public boolean deleteNotification(int notificationId, int userId) throws SQLException {
        if (notificationId <= 0 || userId <= 0) {
            return false;
        }

        String sql = "DELETE FROM event_notifications WHERE id_notification = ? AND id_user_notification = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private boolean isActiveInscription(String statut) {
        String normalized = normalize(statut);
        return normalized.isBlank()
                || !(normalized.contains("annul")
                || normalized.contains("cancel")
                || normalized.contains("rejet")
                || normalized.contains("refus"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}