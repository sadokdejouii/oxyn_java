package org.example.webhook;

/**
 * Test simple pour vérifier le démarrage du webhook server
 */
public class WebhookTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("🧪 Test du webhook server Flouci...");
            
            // Démarrer le serveur
            FlouciWebhookServer.start();
            
            System.out.println("✅ Serveur démarré avec succès !");
            System.out.println("📡 Testez les endpoints:");
            System.out.println("   - Webhook: http://localhost:8080/flouci/webhook");
            System.out.println("   - Health:  http://localhost:8080/health");
            System.out.println();
            System.out.println("📋 Commandes curl pour tester:");
            System.out.println("curl -X POST http://localhost:8080/flouci/webhook \\");
            System.out.println("  -H 'Content-Type: application/json' \\");
            System.out.println("  -d '{\"payment_id\":\"test_123\",\"status\":\"SUCCESS\"}'");
            System.out.println();
            System.out.println("curl http://localhost:8080/health");
            System.out.println();
            System.out.println("⏳ Serveur en écoute. Ctrl+C pour arrêter.");
            
            // Garder le serveur en vie
            Thread.sleep(Long.MAX_VALUE);
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
