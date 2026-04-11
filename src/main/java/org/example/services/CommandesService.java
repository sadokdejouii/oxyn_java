package org.example.services;

import org.example.entities.commandes;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandesService implements ICrud<commandes> {
    Connection con;

    public CommandesService() {
        con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(commandes c) throws SQLException {
        String sql = "INSERT INTO `commandes`(`date_commande`, `total_commande`, `statut_commande`, `mode_paiement_commande`, `id_client_commande`, `Adresse_commande`) VALUES ('"
                + c.getDate_commande() + "',"
                + c.getTotal_commande() + ",'"
                + c.getStatut_commande() + "','"
                + c.getMode_paiement_commande() + "',"
                + c.getId_client_commande() + ",'"
                + c.getAdresse_commande() + "')";
        Statement statement = con.createStatement();
        statement.executeUpdate(sql);
        System.out.println("Commande ajoutée avec succès");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM `commandes` WHERE `id_commande`=?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
        System.out.println("Commande supprimée");
    }

    @Override
    public List<commandes> afficher() throws SQLException {
        List<commandes> listeCommandes = new ArrayList<>();
        String sql = "SELECT * FROM commandes";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()) {
            commandes c = new commandes();
            c.setId_commande(rs.getInt("id_commande"));
            c.setDate_commande(rs.getString("date_commande"));
            c.setTotal_commande(rs.getDouble("total_commande"));
            c.setStatut_commande(rs.getString("statut_commande"));
            c.setMode_paiement_commande(rs.getString("mode_paiement_commande"));
            c.setId_client_commande(rs.getInt("id_client_commande"));
            c.setAdresse_commande(rs.getString("Adresse_commande"));
            listeCommandes.add(c);
        }
        return listeCommandes;
    }

    @Override
    public void modifier(int id) throws SQLException {
//        String sql = "UPDATE `commandes` SET `date_commande`=?, `total_commande`=?, `statut_commande`=?, `mode_paiement_commande`=?, `id_client_commande`=?, `Adresse_commande`=? WHERE `id_commande`=?";
//        PreparedStatement ps = con.prepareStatement(sql);
//
//        ps.setString(1, c.getDate_commande());
//        ps.setDouble(2, c.getTotal_commande());
//        ps.setString(3, c.getStatut_commande());
//        ps.setString(4, c.getMode_paiement_commande());
//        ps.setInt(5, c.getId_client_commande());
//        ps.setString(6, c.getAdresse_commande());
//        ps.setInt(7, c.getId_commande());
//
//        ps.executeUpdate();
//
//        System.out.println("Commande modifiée !");
    }
}