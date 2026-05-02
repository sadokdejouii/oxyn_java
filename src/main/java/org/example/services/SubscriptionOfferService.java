package org.example.services;

import org.example.entities.SubscriptionOffer;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service complet pour la gestion des offres d'abonnement
 */
public class SubscriptionOfferService {

    /**
     * ✅ Récupérer toutes les offres (tous gymnases)
     */
    public List<SubscriptionOffer> getAll() {
        List<SubscriptionOffer> list = new ArrayList<>();
        String sql = "SELECT * FROM gym_subscription_offers ORDER BY created_at DESC";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                SubscriptionOffer offer = mapRow(rs);
                list.add(offer);
            }
            
        } catch (SQLException ex) {
            System.err.println("❌ getAll(): " + ex.getMessage());
        }
        
        return list;
    }

    /**
     * ✅ Récupérer les offres par gymnase
     */
    public List<SubscriptionOffer> getByGym(int gymnasiumId) {
        List<SubscriptionOffer> list = new ArrayList<>();
        String sql = "SELECT * FROM gym_subscription_offers WHERE gymnasium_id = ? AND is_active = 1 ORDER BY created_at DESC";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, gymnasiumId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                SubscriptionOffer offer = mapRow(rs);
                list.add(offer);
            }
            
        } catch (SQLException ex) {
            System.err.println("❌ getByGym(): " + ex.getMessage());
        }
        
        return list;
    }

    /**
     * ✅ Ajouter une offre (INSERT) - sans throws SQLException
     */
    public void add(SubscriptionOffer o) {
        String sql = """
            INSERT INTO gym_subscription_offers 
            (gymnasium_id, name, duration_months, price, description, is_active, created_at)
            VALUES (?, ?, ?, ?, ?, 1, NOW())
            """;
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, o.getGymnasiumId());
            stmt.setString(2, o.getName());
            stmt.setInt(3, o.getDurationMonths());
            stmt.setDouble(4, o.getPrice());
            stmt.setString(5, o.getDescription() != null ? o.getDescription() : "");
            
            stmt.executeUpdate();
            
            // Récupérer l'ID généré
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    o.setId(generatedKeys.getInt(1));
                }
            }
            
            System.out.println("✅ Offre ajoutée en BD avec ID: " + o.getId());
            
        } catch (SQLException ex) {
            System.err.println("❌ add(): " + ex.getMessage());
        }
    }

    /**
     * ✅ Modifier une offre (UPDATE)
     */
    public void update(SubscriptionOffer o) {
        String sql = """
            UPDATE gym_subscription_offers 
            SET name = ?, duration_months = ?, price = ?, description = ?, gymnasium_id = ?
            WHERE id = ?
            """;
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, o.getName());
            stmt.setInt(2, o.getDurationMonths());
            stmt.setDouble(3, o.getPrice());
            stmt.setString(4, o.getDescription() != null ? o.getDescription() : "");
            stmt.setInt(5, o.getGymnasiumId());
            stmt.setInt(6, o.getId());
            
            stmt.executeUpdate();
            System.out.println("✅ Offre modifiée en BD (ID: " + o.getId() + ")");
            
        } catch (SQLException ex) {
            System.err.println("❌ update(): " + ex.getMessage());
        }
    }

    /**
     * ✅ Supprimer une offre (DELETE)
     */
    public void delete(int id) {
        String sql = "DELETE FROM gym_subscription_offers WHERE id = ?";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
            System.out.println("✅ Offre supprimée en BD (ID: " + id + ")");
            
        } catch (SQLException ex) {
            System.err.println("❌ delete(): " + ex.getMessage());
        }
    }

    /**
     * ✅ Activer/Désactiver une offre (UPDATE is_active)
     */
    public void toggleActive(int id, boolean active) {
        String sql = "UPDATE gym_subscription_offers SET is_active = ? WHERE id = ?";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, active ? 1 : 0);
            stmt.setInt(2, id);
            
            stmt.executeUpdate();
            System.out.println("✅ Offre " + (active ? "activée" : "désactivée") + " en BD (ID: " + id + ")");
            
        } catch (SQLException ex) {
            System.err.println("❌ toggleActive(): " + ex.getMessage());
        }
    }

    /**
     * ✅ Récupérer les commandes pour une offre
     */
    public List<String[]> getOrdersByOffer(int offerId) {
        // TODO: Implémenter quand la gestion des commandes sera prête
        // Pour l'instant, retourne une liste vide
        System.out.println("📋 getOrdersByOffer() non implémenté pour l'offre ID: " + offerId);
        return new ArrayList<>();
    }

    /**
     * ✅ Compter les offres par salle
     */
    public int countParSalle(int gymnasiumId) {
        String sql = "SELECT COUNT(*) FROM gym_subscription_offers WHERE gymnasium_id = ? AND is_active = 1";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, gymnasiumId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException ex) {
            System.err.println("❌ countParSalle(): " + ex.getMessage());
        }
        
        return 0;
    }

    /**
     * ✅ Mapper un ResultSet vers SubscriptionOffer
     */
    private SubscriptionOffer mapRow(ResultSet rs) throws SQLException {
        SubscriptionOffer offer = new SubscriptionOffer();
        offer.setId(rs.getInt("id"));
        offer.setGymnasiumId(rs.getInt("gymnasium_id"));
        offer.setName(rs.getString("name"));
        offer.setDurationMonths(rs.getInt("duration_months"));
        offer.setPrice(rs.getDouble("price"));
        offer.setDescription(rs.getString("description"));
        offer.setActive(rs.getInt("is_active") == 1);
        offer.setCreatedAt(rs.getTimestamp("created_at"));
        return offer;
    }
}
