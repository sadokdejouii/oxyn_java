package org.example.services;

import org.example.entities.LignePanier;
import org.example.entities.produits;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistance du panier en base par client.
 * Table: panier_ligne (id_client, id_produit, quantite)
 */
public final class PanierPersistenceService {

    private final Connection con;

    public PanierPersistenceService() {
        con = MyDataBase.getInstance().getConnection();
    }

    public void ensureTables() throws SQLException {
        String ddl = "CREATE TABLE IF NOT EXISTS panier_ligne ("
                + "id_client INT NOT NULL, "
                + "id_produit INT NOT NULL, "
                + "quantite INT NOT NULL, "
                + "date_maj TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "PRIMARY KEY (id_client, id_produit)"
                + ")";
        try (Statement st = con.createStatement()) {
            st.executeUpdate(ddl);
        }
    }

    public Map<Integer, LignePanier> loadPanier(int idClient) throws SQLException {
        ensureTables();
        Map<Integer, LignePanier> map = new LinkedHashMap<>();
        String sql = "SELECT p.id_produit, p.nom_produit, p.description_produit, p.prix_produit, "
                + "p.quantite_stock_produit, p.image_produit, p.date_creation_produit, p.statut_produit, "
                + "pl.quantite "
                + "FROM panier_ligne pl "
                + "INNER JOIN produits p ON p.id_produit = pl.id_produit "
                + "WHERE pl.id_client = ? "
                + "ORDER BY pl.date_maj DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    produits p = new produits();
                    p.setId_produit(rs.getInt("id_produit"));
                    p.setNom_produit(rs.getString("nom_produit"));
                    p.setDescription_produit(rs.getString("description_produit"));
                    p.setPrix_produit(rs.getDouble("prix_produit"));
                    p.setQuantite_stock_produit(rs.getInt("quantite_stock_produit"));
                    p.setImage_produit(rs.getString("image_produit"));
                    p.setDate_creation_produit(rs.getString("date_creation_produit"));
                    p.setStatut_produit(rs.getString("statut_produit"));
                    int qte = Math.max(1, rs.getInt("quantite"));
                    map.put(p.getId_produit(), new LignePanier(p, qte));
                }
            }
        }
        return map;
    }

    public void upsertQuantite(int idClient, int idProduit, int quantite) throws SQLException {
        ensureTables();
        int q = Math.max(1, quantite);
        String sql = "INSERT INTO panier_ligne (id_client, id_produit, quantite) VALUES (?,?,?) "
                + "ON DUPLICATE KEY UPDATE quantite = VALUES(quantite)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            ps.setInt(2, idProduit);
            ps.setInt(3, q);
            ps.executeUpdate();
        }
    }

    public void deleteProduit(int idClient, int idProduit) throws SQLException {
        ensureTables();
        String sql = "DELETE FROM panier_ligne WHERE id_client = ? AND id_produit = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            ps.setInt(2, idProduit);
            ps.executeUpdate();
        }
    }

    public void clearPanier(int idClient) throws SQLException {
        ensureTables();
        String sql = "DELETE FROM panier_ligne WHERE id_client = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            ps.executeUpdate();
        }
    }
}

