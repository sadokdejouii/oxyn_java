package org.example.services;

import org.example.entities.Evenement;
import org.example.utils.MyDataBase;
import org.example.utils.SqlDateReaders;

import java.sql.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EvenementServices implements ICrud<Evenement> {

    Connection con;
    private final EventNotificationService eventNotificationService;

    public EvenementServices() {
        con = MyDataBase.getInstance().getConnection();
        eventNotificationService = new EventNotificationService();
    }

    @Override
    public void ajouter(Evenement e) throws SQLException {
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
}