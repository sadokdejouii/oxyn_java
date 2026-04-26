package org.example.services;

import org.example.entities.Evenement;
import org.example.utils.MyDataBase;
import org.example.utils.SqlDateReaders;

import java.sql.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;

public class EvenementServices implements ICrud<Evenement> {

    Connection con;
    private final EventNotificationService eventNotificationService;

    public EvenementServices() {
        con = MyDataBase.getInstance().getConnection();
        eventNotificationService = new EventNotificationService();
    }

    @Override
    public void ajouter(Evenement e) throws SQLException {
        e.setStatut(resolveStatusFromDates(e.getStatut(), e.getDateDebut(), e.getDateFin()));

        String sql = "INSERT INTO evenements (" +
                "titre_evenement, description_evenement, date_debut_evenement, date_fin_evenement, " +
                "lieu_evenement, ville_evenement, places_max_evenement, statut_evenement, " +
                "created_at_evenement, created_by_evenement) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, e.getTitre());
        ps.setString(2, e.getDescription());
        ps.setTimestamp(3, new Timestamp(e.getDateDebut().getTime()));
        ps.setTimestamp(4, new Timestamp(e.getDateFin().getTime()));
        ps.setString(5, e.getLieu());
        ps.setString(6, e.getVille());
        ps.setInt(7, e.getPlacesMax());
        ps.setString(8, e.getStatut());
        ps.setTimestamp(9, new Timestamp(e.getCreatedAt().getTime()));
        ps.setInt(10, e.getCreatedBy());

        ps.executeUpdate();
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                e.setId(keys.getInt(1));
            }
        }
        System.out.println("Evenement ajouté avec succès !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM evenements WHERE id_evenement = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();

        System.out.println("Evenement supprimé avec succès !");
    }

    @Override
    public List<Evenement> afficher() throws SQLException {
        synchronizeStatusesFromDates();

        List<Evenement> evenements = new ArrayList<>();

        String sql = "SELECT * FROM evenements";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Evenement e = new Evenement();
            e.setId(rs.getInt("id_evenement"));
            e.setTitre(rs.getString("titre_evenement"));
            e.setDescription(rs.getString("description_evenement"));
            e.setDateDebut(SqlDateReaders.readTimestampOrNull(rs, "date_debut_evenement"));
            e.setDateFin(SqlDateReaders.readTimestampOrNull(rs, "date_fin_evenement"));
            e.setLieu(rs.getString("lieu_evenement"));
            e.setVille(rs.getString("ville_evenement"));
            e.setPlacesMax(rs.getInt("places_max_evenement"));
            e.setStatut(rs.getString("statut_evenement"));
            e.setCreatedAt(SqlDateReaders.readTimestampOrNull(rs, "created_at_evenement"));
            e.setCreatedBy(rs.getInt("created_by_evenement"));

            evenements.add(e);
        }

        return evenements;
    }

    @Override
    public void modifier(int id) throws SQLException {
        // This overload is for the interface - use the Evenement version instead
    }

    public void modifier(Evenement e) throws SQLException {
        boolean previousAutoCommit = con.getAutoCommit();
        String previousStatus = null;
        e.setStatut(resolveStatusFromDates(e.getStatut(), e.getDateDebut(), e.getDateFin()));
        String sql = "UPDATE evenements SET " +
                "titre_evenement = ?, " +
                "description_evenement = ?, " +
                "date_debut_evenement = ?, " +
                "date_fin_evenement = ?, " +
                "lieu_evenement = ?, " +
                "ville_evenement = ?, " +
                "places_max_evenement = ?, " +
                "statut_evenement = ? " +
                "WHERE id_evenement = ?";

        try {
            con.setAutoCommit(false);
            previousStatus = fetchCurrentStatus(e.getId());

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, e.getTitre());
                ps.setString(2, e.getDescription());
                ps.setTimestamp(3, new Timestamp(e.getDateDebut().getTime()));
                ps.setTimestamp(4, new Timestamp(e.getDateFin().getTime()));
                ps.setString(5, e.getLieu());
                ps.setString(6, e.getVille());
                ps.setInt(7, e.getPlacesMax());
                ps.setString(8, e.getStatut());
                ps.setInt(9, e.getId());
                ps.executeUpdate();
            }

            if (transitionedToCancelled(previousStatus, e.getStatut())) {
                eventNotificationService.createCancellationNotifications(e);
            }

            con.commit();
            System.out.println("Evenement modifié avec succès !");
        } catch (SQLException ex) {
            try {
                con.rollback();
            } catch (SQLException rollbackEx) {
                ex.addSuppressed(rollbackEx);
            }
            throw ex;
        } finally {
            con.setAutoCommit(previousAutoCommit);
        }
    }

    public Evenement afficherById(int id) throws SQLException {
        synchronizeStatusesFromDates();

        String sql = "SELECT * FROM evenements WHERE id_evenement = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Evenement e = new Evenement();
            e.setId(rs.getInt("id_evenement"));
            e.setTitre(rs.getString("titre_evenement"));
            e.setDescription(rs.getString("description_evenement"));
            e.setDateDebut(SqlDateReaders.readTimestampOrNull(rs, "date_debut_evenement"));
            e.setDateFin(SqlDateReaders.readTimestampOrNull(rs, "date_fin_evenement"));
            e.setLieu(rs.getString("lieu_evenement"));
            e.setVille(rs.getString("ville_evenement"));
            e.setPlacesMax(rs.getInt("places_max_evenement"));
            e.setStatut(rs.getString("statut_evenement"));
            e.setCreatedAt(SqlDateReaders.readTimestampOrNull(rs, "created_at_evenement"));
            e.setCreatedBy(rs.getInt("created_by_evenement"));

            return e;
        }

        return null;
    }

    private String fetchCurrentStatus(int eventId) throws SQLException {
        String sql = "SELECT statut_evenement FROM evenements WHERE id_evenement = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("statut_evenement");
                }
            }
        }
        return null;
    }

    private boolean transitionedToCancelled(String previousStatus, String nextStatus) {
        return !isCancelledStatus(previousStatus) && isCancelledStatus(nextStatus);
    }

    private boolean isCancelledStatus(String status) {
        String normalized = Normalizer.normalize(status == null ? "" : status, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .trim();
        return normalized.contains("annul") || normalized.contains("cancel") || normalized.contains("close");
    }

    private void synchronizeStatusesFromDates() throws SQLException {
        String selectSql = "SELECT id_evenement, statut_evenement, date_debut_evenement, date_fin_evenement FROM evenements";
        String updateSql = "UPDATE evenements SET statut_evenement = ? WHERE id_evenement = ?";

        List<StatusUpdate> updates = new ArrayList<>();

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(selectSql)) {
            while (rs.next()) {
                int eventId = rs.getInt("id_evenement");
                String currentStatus = rs.getString("statut_evenement");
                Date startDate = SqlDateReaders.readTimestampOrNull(rs, "date_debut_evenement");
                Date endDate = SqlDateReaders.readTimestampOrNull(rs, "date_fin_evenement");
                String resolvedStatus = resolveStatusFromDates(currentStatus, startDate, endDate);

                if (!resolvedStatus.equals(currentStatus)) {
                    updates.add(new StatusUpdate(eventId, resolvedStatus));
                }
            }
        }

        if (updates.isEmpty()) {
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(updateSql)) {
            for (StatusUpdate update : updates) {
                ps.setString(1, update.status());
                ps.setInt(2, update.eventId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private String resolveStatusFromDates(String currentStatus, Date startDate, Date endDate) {
        if (isCancelledStatus(currentStatus)) {
            return currentStatus == null || currentStatus.isBlank() ? "Annulée" : currentStatus;
        }

        LocalDate today = LocalDate.now();
        LocalDate start = toLocalDate(startDate);
        LocalDate end = toLocalDate(endDate);

        if (end != null && today.isAfter(end)) {
            return "Terminée";
        }
        if (start != null && (!today.isBefore(start)) && (end == null || !today.isAfter(end))) {
            return "En cours";
        }
        if (start != null && today.isBefore(start)) {
            return "À venir";
        }
        return currentStatus == null || currentStatus.isBlank() ? "À venir" : currentStatus;
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private record StatusUpdate(int eventId, String status) {
    }
}