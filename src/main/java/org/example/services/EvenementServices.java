package org.example.services;

import org.example.entities.Evenement;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementServices implements ICrud<Evenement> {

    Connection con;

    public EvenementServices() {
        con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Evenement e) throws SQLException {
        String sql = "INSERT INTO evenements (" +
                "titre_evenement, description_evenement, date_debut_evenement, date_fin_evenement, " +
                "lieu_evenement, ville_evenement, places_max_evenement, statut_evenement, " +
                "created_at_evenement, created_by_evenement) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql);
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
            e.setDateDebut(rs.getTimestamp("date_debut_evenement"));
            e.setDateFin(rs.getTimestamp("date_fin_evenement"));
            e.setLieu(rs.getString("lieu_evenement"));
            e.setVille(rs.getString("ville_evenement"));
            e.setPlacesMax(rs.getInt("places_max_evenement"));
            e.setStatut(rs.getString("statut_evenement"));
            e.setCreatedAt(rs.getTimestamp("created_at_evenement"));
            e.setCreatedBy(rs.getInt("created_by_evenement"));

            evenements.add(e);
        }

        return evenements;
    }

    @Override
    public void modifier(int id) throws SQLException {
        String sql = "UPDATE evenements SET " +
                "titre_evenement = ?, " +
                "description_evenement = ?, " +
                "date_debut_evenement = ?, " +
                "date_fin_evenement = ?, " +
                "lieu_evenement = ?, " +
                "ville_evenement = ?, " +
                "places_max_evenement = ?, " +
                "statut_evenement = ?, " +
                "created_at_evenement = ?, " +
                "created_by_evenement = ? " +
                "WHERE id_evenement = ?";

        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, "Evenement modifié");
        ps.setString(2, "Description modifiée");
        ps.setTimestamp(3, Timestamp.valueOf("2026-04-10 10:00:00"));
        ps.setTimestamp(4, Timestamp.valueOf("2026-04-10 18:00:00"));
        ps.setString(5, "Tunis");
        ps.setString(6, "Tunis");
        ps.setInt(7, 150);
        ps.setString(8, "ACTIF");
        ps.setTimestamp(9, Timestamp.valueOf("2026-04-04 12:00:00"));
        ps.setInt(10, 1);
        ps.setInt(11, id);

        ps.executeUpdate();
        System.out.println("Evenement modifié avec succès !");
    }
}