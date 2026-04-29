package org.example.scheduler;

import org.example.services.SubscriptionLifecycleService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Planificateur pour les tâches automatiques d'abonnement
 * - Expiration des abonnements
 * - Activation des paiements en attente
 */
public class SubscriptionScheduler {

    private static SubscriptionScheduler instance;
    private ScheduledExecutorService scheduler;
    private final SubscriptionLifecycleService lifecycleService;

    private SubscriptionScheduler() {
        this.lifecycleService = new SubscriptionLifecycleService();
    }

    public static synchronized SubscriptionScheduler getInstance() {
        if (instance == null) {
            instance = new SubscriptionScheduler();
        }
        return instance;
    }

    /**
     * Démarrer les tâches planifiées
     */
    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            System.out.println("⚠️ Scheduler déjà démarré");
            return;
        }

        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true); // ← indispensable
            t.setName("subscription-scheduler-" + System.currentTimeMillis());
            return t;
        });

        // Tâche 1 : Expirer les abonnements toutes les heures
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("🕐 Vérification des abonnements expirés...");
                lifecycleService.expireSubscriptions();
            } catch (Exception e) {
                System.err.println("❌ Erreur tâche expiration: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.HOURS);

        // Tâche 2 : Activer les paiements en attente toutes les 5 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("⏳ Vérification des paiements en attente...");
                lifecycleService.processPendingActivations();
            } catch (Exception e) {
                System.err.println("❌ Erreur tâche activation: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.MINUTES);

        System.out.println("🚀 Scheduler d'abonnements démarré");
        System.out.println("   - Expiration: toutes les heures");
        System.out.println("   - Activation: toutes les 5 minutes");
    }

    /**
     * Arrêter les tâches planifiées
     */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("🛑 Scheduler d'abonnements arrêté");
        }
    }

    /**
     * Exécuter manuellement la tâche d'expiration
     */
    public void expireNow() {
        try {
            System.out.println("🕐 Expiration manuelle des abonnements...");
            lifecycleService.expireSubscriptions();
        } catch (Exception e) {
            System.err.println("❌ Erreur expiration manuelle: " + e.getMessage());
        }
    }

    /**
     * Exécuter manuellement la tâche d'activation
     */
    public void activateNow() {
        try {
            System.out.println("⏳ Activation manuelle des paiements en attente...");
            lifecycleService.processPendingActivations();
        } catch (Exception e) {
            System.err.println("❌ Erreur activation manuelle: " + e.getMessage());
        }
    }

    /**
     * Vérifier si le scheduler est en cours d'exécution
     */
    public boolean isRunning() {
        return scheduler != null && !scheduler.isShutdown();
    }
}
