package org.example.services;

import org.example.entities.InscriptionEvenement;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InscriptionEvenementServices implements ICrud<InscriptionEvenement> {

    Connection con;

    public InscriptionEvenementServices() {
        con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(InscriptionEvenement i) throws SQLException {
        String sql = "INSERT INTO inscription_evenement (" +
                "date_inscription_evenemen, statut_inscription_evenemen, " +
                "id_evenement_inscription_evenemen_id, id_user_inscription_evenemen_id) " +
                "VALUES (?, ?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setTimestamp(1, new Timestamp(i.getDateInscription().getTime()));
        ps.setString(2, i.getStatut());
        ps.setInt(3, i.getIdEvenement());
        ps.setInt(4, i.getIdUser());

        ps.executeUpdate();
        System.out.println("Inscription ajoutée avec succès !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM inscription_evenement WHERE id_inscription_inscription_evenement = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();

        System.out.println("Inscription supprimée avec succès !");
    }

    @Override
    public List<InscriptionEvenement> afficher() throws SQLException {
        List<InscriptionEvenement> inscriptions = new ArrayList<>();

        String sql = "SELECT * FROM inscription_evenement";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            InscriptionEvenement i = new InscriptionEvenement();
            i.setId(rs.getInt("id_inscription_inscription_evenement"));
            i.setDateInscription(rs.getTimestamp("date_inscription_evenemen"));
            i.setStatut(rs.getString("statut_inscription_evenemen"));
            i.setIdEvenement(rs.getInt("id_evenement_inscription_evenemen_id"));
            i.setIdUser(rs.getInt("id_user_inscription_evenemen_id"));

            inscriptions.add(i);
        }

        return inscriptions;
    }

    @Override
    public void modifier(int id) throws SQLException {
        String sql = "UPDATE inscription_evenement SET " +
                "date_inscription_evenemen = ?, " +
                "statut_inscription_evenemen = ?, " +
                "id_evenement_inscription_evenemen_id = ?, " +
                "id_user_inscription_evenemen_id = ? " +
                "WHERE id_inscription_inscription_evenement = ?";

        PreparedStatement ps = con.prepareStatement(sql);

        ps.setTimestamp(1, Timestamp.valueOf("2026-04-05 10:00:00"));
        ps.setString(2, "confirmée");
        ps.setInt(3, 1);
        ps.setInt(4, 1);
        ps.setInt(5, id);

        ps.executeUpdate();
        System.out.println("Inscription modifiée avec succès !");
    }
}