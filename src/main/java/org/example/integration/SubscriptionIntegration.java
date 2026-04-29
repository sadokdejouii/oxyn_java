package org.example.integration;

import org.example.scheduler.SubscriptionScheduler;
import org.example.webhook.FlouciWebhookServer;

/**
 * Point d'intégration pour démarrer les services d'abonnement
 * À appeler dans le main() de votre application JavaFX
 */
public class SubscriptionIntegration {

    private static SubscriptionScheduler scheduler;
    private static boolean servicesStarted = false;

    /**
     * Démarrer tous les services d'abonnement
     */
    public static void startServices() {
        if (servicesStarted) {
            System.out.println("⚠️ Services d'abonnement déjà démarrés");
            return;
        }

        try {
            // 1. Démarrer le webhook server
            FlouciWebhookServer.start();
            System.out.println("✅ Webhook Flouci démarré");

            // ❌ Scheduler désactivé — structure Symfony sans date_fin/payment_id
            // scheduler = SubscriptionScheduler.getInstance();
            // scheduler.start();
            // System.out.println("✅ Scheduler d'abonnements démarré");
            System.out.println("ℹ️ Scheduler désactivé (structure Symfony sans date_fin/payment_id)");

            servicesStarted = true;
            System.out.println("🚀 Services d'abonnement sont opérationnels");

        } catch (Exception e) {
            System.err.println("❌ Erreur démarrage services: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Arrêter proprement tous les services
     */
    public static void stopServices() {
        if (!servicesStarted) {
            return;
        }

        try {
            // ❌ Scheduler désactivé — plus besoin de l'arrêter
            // if (scheduler != null) {
            //     scheduler.stop();
            //     System.out.println("🛑 Scheduler d'abonnements arrêté");
            // }

            // 1. Arrêter le webhook server
            FlouciWebhookServer.stop();
            System.out.println("🛑 Webhook Flouci arrêté");

            servicesStarted = false;
            System.out.println("✅ Services d'abonnement arrêtés proprement");

        } catch (Exception e) {
            System.err.println("❌ Erreur arrêt services: " + e.getMessage());
        }
    }

    /**
     * Vérifier si les services sont démarrés
     */
    public static boolean areServicesRunning() {
        return servicesStarted && 
               FlouciWebhookServer.isRunning();
    }

    /**
     * Obtenir le statut des services
     */
    public static String getServicesStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== STATUT SERVICES ABONNEMENT ===\n");
        status.append("Services démarrés: ").append(servicesStarted ? "OUI" : "NON").append("\n");
        
        // ❌ Scheduler désactivé
        // if (scheduler != null) {
        //     status.append("Scheduler: ").append(scheduler.isRunning() ? "ACTIF" : "INACTIF").append("\n");
        // }
        status.append("Scheduler: ").append("DÉSACTIVÉ").append("\n");
        
        status.append("Webhook: ").append(FlouciWebhookServer.isRunning() ? "ACTIF" : "INACTIF").append("\n");
        status.append("================================");
        
        return status.toString();
    }

    /**
     * Exécuter manuellement les tâches de maintenance
     */
    public static void runMaintenanceTasks() {
        // ❌ Scheduler désactivé — plus de tâches automatiques
        System.out.println("ℹ️ Scheduler désactivé - pas de tâches de maintenance automatiques");
        System.out.println("ℹ️ Structure Symfony sans date_fin/payment_id");
    }
}
