package org.example.webhook;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.example.services.SubscriptionOrderService;
import org.example.services.SubscriptionLifecycleService;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Serveur Webhook simple pour écouter les notifications Flouci
 */
public class FlouciWebhookServer {

    private static final int[] PORTS = {8080, 8081, 8082, 8083};
    private static HttpServer server;
    private static SubscriptionOrderService orderService;
    private static SubscriptionLifecycleService lifecycleService;
    private static boolean isRunning = false;
    private static int currentPort = -1;

    public static void start() throws IOException {
        if (isRunning) {
            System.out.println("⚠ Webhook déjà actif, skip.");
            return;
        }
        
        orderService = new SubscriptionOrderService();
        lifecycleService = new SubscriptionLifecycleService();
        
        for (int port : PORTS) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                server.createContext("/flouci/webhook", new FlouciWebhookHandler());
                server.createContext("/health", new HealthHandler());
                server.setExecutor(Executors.newCachedThreadPool());
                server.start();
                isRunning = true;
                currentPort = port;
                System.out.println("✅ Webhook démarré port " + port);
                System.out.println("📡 Endpoint: http://localhost:" + port + "/flouci/webhook");
                System.out.println("📡 Health: http://localhost:" + port + "/health");
                return;
            } catch (java.net.BindException e) {
                System.out.println("⚠ Port " + port + " occupé, essai suivant...");
            }
        }
        
        System.err.println("❌ Webhook non démarré — tous les ports occupés");
        System.err.println("❌ Ports testés: " + java.util.Arrays.toString(PORTS));
        throw new IOException("Impossible de démarrer le webhook sur aucun port disponible");
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;  // Réinitialiser pour permettre redémarrage
            isRunning = false;  // Réinitialiser l'état
            System.out.println("🛑 Webhook server arrêté");
        }
    }

    /**
     * Vérifier si le webhook server est en cours d'exécution
     */
    public static boolean isRunning() {
        return isRunning && server != null;
    }

    /**
     * Handler pour les webhooks Flouci
     */
    static class FlouciWebhookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method Not Allowed");
                    return;
                }

                // Lire le corps de la requête
                String requestBody = readRequestBody(exchange);
                System.out.println("📥 Webhook reçu: " + requestBody);

                // Parser le JSON
                JSONObject webhookData = new JSONObject(requestBody);
                String paymentId = webhookData.optString("payment_id", "");
                String status = webhookData.optString("status", "");

                if (paymentId.isEmpty()) {
                    System.err.println("❌ payment_id manquant dans webhook");
                    sendResponse(exchange, 400, "payment_id manquant");
                    return;
                }

                // Traiter le webhook selon le statut
                if ("SUCCESS".equals(status)) {
                    handleSuccessPayment(webhookData);
                    sendResponse(exchange, 200, "Paiement SUCCESS traité");
                } else if ("FAILED".equals(status) || "CANCELLED".equals(status)) {
                    handleFailedPayment(webhookData);
                    sendResponse(exchange, 200, "Paiement FAILED/CANCELLED traité");
                } else {
                    System.out.println("ℹ️ Statut inconnu: " + status + ", ignoré");
                    sendResponse(exchange, 200, "Statut inconnu ignoré");
                }

            } catch (Exception e) {
                System.err.println("❌ Erreur traitement webhook: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Erreur interne");
            }
        }

        private void handleSuccessPayment(JSONObject webhookData) {
            try {
                String paymentId = webhookData.getString("payment_id");
                double amount = webhookData.optDouble("amount", 0.0) / 1000.0; // Convertir de millimes à TND
                
                // Flux : PENDING → PAYÉ → ACTIVE
                lifecycleService.onFlouciConfirmed(paymentId);
                
                System.out.println("✅ Paiement SUCCESS traité: " + paymentId + " (montant: " + amount + " TND)");
                
            } catch (Exception e) {
                System.err.println("❌ Erreur handleSuccessPayment: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void handleFailedPayment(JSONObject webhookData) {
            try {
                String paymentId = webhookData.getString("payment_id");
                orderService.updateOrderStatus(paymentId, "ECHEC");
                System.out.println("❌ Paiement FAILED traité: " + paymentId);
                
            } catch (Exception e) {
                System.err.println("❌ Erreur handleFailedPayment: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private String readRequestBody(HttpExchange exchange) throws IOException {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    body.append(line);
                }
                return body.toString();
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    /**
     * Handler pour health check
     */
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"status\":\"ok\",\"service\":\"flouci-webhook\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, 200, response);
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    // Méthode main pour test standalone
    public static void main(String[] args) {
        try {
            System.out.println("🚀 Démarrage du serveur webhook Flouci...");
            start();
            System.out.println("⏳ Serveur en écoute. Ctrl+C pour arrêter.");
            
            // Garder le serveur en vie
            Thread.sleep(Long.MAX_VALUE);
            
        } catch (Exception e) {
            System.err.println("❌ Erreur démarrage webhook: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
