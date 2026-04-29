package org.example.services;

import org.example.utils.MyDataBase;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service pour la gestion des ordres d'abonnement avec intégration Flouci
 */
public class SubscriptionOrderService {

    /**
     * Sauvegarde une commande d'abonnement simple dans la base de données
     * @param userId ID de l'utilisateur
     * @param offreId ID de l'offre d'abonnement
     * @param montant Montant payé en TND
     */
    public void saveFlouciOrder(int userId, int offreId, double montant) {
        String sql = """
            INSERT INTO gym_subscription_orders 
            (user_id, offer_id, quantity, unit_price, total_price, status, created_at)
            VALUES (?, ?, 1, ?, ?, 'active', NOW())
            """;
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, offreId);
            stmt.setDouble(3, montant);      // unit_price
            stmt.setDouble(4, montant);      // total_price (quantity=1)
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                System.out.println("✅ Abonnement enregistré: user_id=" + userId + 
                                 ", offer_id=" + offreId + ", montant=" + montant + " TND");
            } else {
                System.err.println("❌ Échec de l'enregistrement de l'abonnement");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur saveFlouciOrder: " + e.getMessage());
            e.printStackTrace();
        }
        // connexion fermée automatiquement ici ✅
    }

    /**
     * Mettre à jour le statut d'une commande
     */
    public void updateOrderStatus(String paymentId, String statut) throws SQLException {
        // ✅ try-with-resources → ferme automatiquement
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE gym_subscription_orders SET statut = ?, updated_at = NOW() " +
                          "WHERE payment_id = ?")) {
            
            stmt.setString(1, statut);
            stmt.setString(2, paymentId);
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                System.out.println("✅ Statut mis à jour: payment_id=" + paymentId + " → " + statut);
            } else {
                System.err.println("⚠️ Aucune commande trouvée pour payment_id=" + paymentId);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur updateOrderStatus: " + e.getMessage());
            throw e;
        }
        // connexion fermée automatiquement ici ✅
    }

    /**
     * Récupérer les détails d'une commande par payment_id
     */
    public String getOrderStatus(String paymentId) throws SQLException {
        // ✅ try-with-resources → ferme automatiquement
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT statut FROM gym_subscription_orders WHERE payment_id = ?");
             ResultSet rs = stmt.executeQuery()) {
            
            stmt.setString(1, paymentId);
            
            if (rs.next()) {
                return rs.getString("statut");
            } else {
                return null;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur getOrderStatus: " + e.getMessage());
            throw e;
        }
        // connexion et ressources fermées automatiquement ici ✅
    }
}
