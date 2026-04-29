package org.example.services;

import org.example.entities.produits;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitsService implements ICrud<produits>{
    Connection con;

    public ProduitsService() {
        con = MyDataBase.getConnection();
    }

    @Override
    public void ajouter(produits p) throws SQLException {
        String sql = "INSERT INTO produits (nom_produit, description_produit, prix_produit, quantite_stock_produit, image_produit, date_creation_produit, statut_produit) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNom_produit());
            ps.setString(2, p.getDescription_produit());
            ps.setDouble(3, p.getPrix_produit());
            ps.setInt(4, p.getQuantite_stock_produit());
            ps.setString(5, p.getImage_produit());
            ps.setString(6, p.getDate_creation_produit());
            ps.setString(7, p.getStatut_produit());
            ps.executeUpdate();
        }
        System.out.println("Produit ajouté avec succès");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM `produits` WHERE `id_produit`=?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
        System.out.println("personne supprime");
    }

    @Override
    public List<produits> afficher() throws SQLException {
        List<produits> produits = new ArrayList<>();
        String sql = "SELECT * FROM produits";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()) {
            produits prod = new produits();
            prod.setId_produit(rs.getInt("id_produit"));
            prod.setNom_produit(rs.getString("nom_produit"));
            prod.setDescription_produit(rs.getString("description_produit"));
            prod.setPrix_produit(rs.getDouble("prix_produit"));
            prod.setQuantite_stock_produit(rs.getInt("quantite_stock_produit"));
            prod.setImage_produit(rs.getString("image_produit"));
            prod.setDate_creation_produit(rs.getString("date_creation_produit"));
            prod.setStatut_produit(rs.getString("statut_produit"));
            produits.add(prod);
        }
        return produits;
    }

    @Override
    public void modifier(int id) throws SQLException {
        // Pour l'instant, cette méthode est vide car nous avons besoin d'un objet produit pour la modification
        // Nous allons créer une surcharge de cette méthode qui prend un produit en paramètre
        System.out.println("Méthode modifier appelée avec ID: " + id);
    }
    
    public void modifier(produits p) throws SQLException {
        String sql = "UPDATE `produits` SET `nom_produit`=?,`description_produit`=?,`prix_produit`=?,`quantite_stock_produit`=?,`image_produit`=?,`statut_produit`=? WHERE `id_produit`=?";
        PreparedStatement ps = con.prepareStatement(sql);
        
        ps.setString(1, p.getNom_produit());
        ps.setString(2, p.getDescription_produit());
        ps.setDouble(3, p.getPrix_produit());
        ps.setInt(4, p.getQuantite_stock_produit());
        ps.setString(5, p.getImage_produit());
        ps.setString(6, p.getStatut_produit());
        ps.setInt(7, p.getId_produit());
        
        ps.executeUpdate();
        System.out.println("Produit modifié avec succès !");
    }
}
