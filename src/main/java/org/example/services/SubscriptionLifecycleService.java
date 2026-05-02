package org.example.services;

import org.example.utils.MyDataBase;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service pour gérer le cycle de vie complet des abonnements
 * Flux : PENDING → PAYÉ → ACTIVE → EXPIRÉ
 */
public class SubscriptionLifecycleService {

    private final SubscriptionOrderService orderService;

    public SubscriptionLifecycleService() {
        this.orderService = new SubscriptionOrderService();
    }

    /**
     * Étape 1 : Flouci confirme le paiement
     * Passe le statut de PENDING → PAYÉ
     */
    public void onFlouciConfirmed(String paymentId) {
        try {
            // Statut = 'payé' → paiement reçu par Flouci
            orderService.updateOrderStatus(paymentId, "payé");
            
            // Étape 2 : Activer l'abonnement
            activateSubscription(paymentId);
            
            System.out.println("✅ Flux PENDING → PAYÉ → ACTIVE complété pour: " + paymentId);
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur onFlouciConfirmed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Étape 2 : Activer l'abonnement
     * Passe le statut de PAYÉ → ACTIVE
     */
    public void activateSubscription(String paymentId) throws SQLException {
        // ✅ try-with-resources → ferme automatiquement
        try (Connection conn = MyDataBase.getConnection()) {
            
            // Récupérer les détails de la commande
            try (PreparedStatement selectStmt = conn.prepareStatement("SELECT offre_id, montant FROM gym_subscription_orders WHERE payment_id = ?")) {
                selectStmt.setString(1, paymentId);
                ResultSet rs = selectStmt.executeQuery();
                
                if (!rs.next()) {
                    throw new SQLException("Commande non trouvée pour payment_id: " + paymentId);
                }
                
                int offreId = rs.getInt("offre_id");
                double montant = rs.getDouble("montant");
                rs.close();
                
                // Récupérer la durée de l'offre
                try (PreparedStatement dureeStmt = conn.prepareStatement("SELECT duration_months FROM gym_subscription_offers WHERE id = ?")) {
                    dureeStmt.setInt(1, offreId);
                    ResultSet dureeRs = dureeStmt.executeQuery();
                    
                    if (!dureeRs.next()) {
                        throw new SQLException("Offre non trouvée pour offre_id: " + offreId);
                    }
                    
                    int dureeMois = dureeRs.getInt("duration_months");
                    dureeRs.close();
                    
                    // Calculer les dates
                    LocalDate dateDebut = LocalDate.now();
                    LocalDate dateFin = dateDebut.plusMonths(dureeMois);
                    
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    
                    // Mettre à jour la commande avec statut ACTIVE et les dates
                    try (PreparedStatement stmt = conn.prepareStatement("UPDATE gym_subscription_orders SET " +
                                      "statut = 'active', " +
                                      "date_debut = ?, " +
                                      "date_fin = ?, " +
                                      "updated_at = NOW() " +
                                      "WHERE payment_id = ?")) {
                        
                        stmt.setString(1, dateDebut.format(formatter));
                        stmt.setString(2, dateFin.format(formatter));
                        stmt.setString(3, paymentId);
                        
                        int result = stmt.executeUpdate();
                        
                        if (result > 0) {
                            System.out.println("✅ Abonnement activé: " + paymentId + 
                                             " (du " + dateDebut + " au " + dateFin + ")");
                        } else {
                            throw new SQLException("Échec de l'activation de l'abonnement");
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur activateSubscription: " + e.getMessage());
            throw e;
        }
        // connexion fermée automatiquement ici ✅
    }

    /**
     * Vérifier les abonnements actifs d'un utilisateur
     */
    public boolean hasActiveSubscription(int userId) throws SQLException {
        // ✅ try-with-resources → ferme automatiquement
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) as count FROM gym_subscription_orders " +
                          "WHERE user_id = ? AND status = 'active' AND date_fin >= CURDATE()");
             ResultSet rs = stmt.executeQuery()) {
            
            stmt.setInt(1, userId);
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
            
            return false;
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur hasActiveSubscription: " + e.getMessage());
            throw e;
        }
        // connexion et ressources fermées automatiquement ici ✅
    }

    /**
     * Obtenir les détails de l'abonnement actif d'un utilisateur
     */
    public SubscriptionInfo getActiveSubscription(int userId) throws SQLException {
        // ✅ try-with-resources → ferme automatiquement
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT o.*, g.name as offre_name, g.duration_months " +
                          "FROM gym_subscription_orders o " +
                          "JOIN gym_subscription_offers g ON o.offre_id = g.id " +
                          "WHERE o.user_id = ? AND o.status = 'active' AND o.date_fin >= CURDATE() " +
                          "ORDER BY o.created_at DESC LIMIT 1");
             ResultSet rs = stmt.executeQuery()) {
            
            stmt.setInt(1, userId);
            
            if (rs.next()) {
                SubscriptionInfo info = new SubscriptionInfo();
                info.paymentId = rs.getString("payment_id");
                info.offreName = rs.getString("offre_name");
                info.dateDebut = rs.getDate("date_debut");
                info.dateFin = rs.getDate("date_fin");
                info.montant = rs.getDouble("montant");
                info.dureeMois = rs.getInt("duration_months");
                info.statut = rs.getString("statut");
                return info;
            }
            
            return null;
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur getActiveSubscription: " + e.getMessage());
            throw e;
        }
        // connexion et ressources fermées automatiquement ici ✅
    }

    /**
     * Vérifier les paiements confirmés non encore activés
     */
    public void processPendingActivations() {
        // ✅ try-with-resources → ferme automatiquement
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT payment_id FROM gym_subscription_orders WHERE status = 'payé'");
             ResultSet rs = stmt.executeQuery()) {
            
            int processed = 0;
            while (rs.next()) {
                String paymentId = rs.getString("payment_id");
                try {
                    activateSubscription(paymentId);
                    processed++;
                } catch (Exception e) {
                    System.err.println("❌ Erreur activation " + paymentId + ": " + e.getMessage());
                }
            }
            
            if (processed > 0) {
                System.out.println("✅ " + processed + " abonnements activés automatiquement");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur processPendingActivations: " + e.getMessage());
        }
        // connexion et ressources fermées automatiquement ici ✅
    }

    /**
     * Expirer les abonnements automatiquement
     */
    public void expireSubscriptions() {
        // ✅ try-with-resources → ferme automatiquement
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE gym_subscription_orders " +
                          "SET status = 'expiré', updated_at = NOW() " +
                          "WHERE status = 'active' AND date_fin < CURDATE()")) {
            
            int expired = stmt.executeUpdate();
            
            if (expired > 0) {
                System.out.println("🕐 " + expired + " abonnements expirés automatiquement");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur expireSubscriptions: " + e.getMessage());
        }
        // connexion et ressources fermées automatiquement ici ✅
    }

    /**
     * Classe pour stocker les informations d'abonnement
     */
    public static class SubscriptionInfo {
        public String paymentId;
        public String offreName;
        public java.sql.Date dateDebut;
        public java.sql.Date dateFin;
        public double montant;
        public int dureeMois;
        public String statut;
        
        @Override
        public String toString() {
            return "SubscriptionInfo{" +
                   "paymentId='" + paymentId + '\'' +
                   ", offreName='" + offreName + '\'' +
                   ", dateDebut=" + dateDebut +
                   ", dateFin=" + dateFin +
                   ", montant=" + montant +
                   ", dureeMois=" + dureeMois +
                   ", statut='" + statut + '\'' +
                   '}';
        }
    }
}
