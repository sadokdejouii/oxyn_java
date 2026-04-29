package org.example.services;

import okhttp3.*;
import org.json.JSONObject;
import org.example.utils.ConfigManager;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service Flouci amélioré avec gestion des erreurs et annulations
 */
public class EnhancedFlouciService {

    private final ConfigManager config;
    private final OkHttpClient client;
    private final SubscriptionOrderService orderService;

    public EnhancedFlouciService() {
        this.config = ConfigManager.getInstance();
        this.client = new OkHttpClient.Builder()
            .connectTimeout(config.getFlouciTimeout(), java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(config.getFlouciTimeout(), java.util.concurrent.TimeUnit.SECONDS)
            .build();
        this.orderService = new SubscriptionOrderService();
    }

    /**
     * Créer un paiement Flouci avec configuration dynamique
     */
    public FlouciPayment createPayment(double amount, String description) throws Exception {
        if (config.isTest()) {
            return createSimulationPayment(amount);
        }

        String baseUrl = config.getFlouciBaseUrl();
        String authHeader = getAuthHeader();

        JSONObject body = new JSONObject();
        body.put("amount", (int)(amount * 1000));
        body.put("accept_card", true);
        body.put("session_timeout_secs", 1200);
        body.put("success_link", "http://localhost:" + getWebhookPort() + "/flouci/webhook");
        body.put("fail_link", "http://localhost:" + getWebhookPort() + "/flouci/webhook");
        body.put("developer_tracking_id", "oxyn_" + System.currentTimeMillis());

        Request request = new Request.Builder()
            .url(baseUrl + "/v2/generate_payment")
            .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", authHeader)
            .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            System.out.println("Flouci response: " + responseBody);

            if (!response.isSuccessful()) {
                throw new Exception("Flouci API error: " + response.code() + " - " + responseBody);
            }

            JSONObject json = new JSONObject(responseBody);

            FlouciPayment payment = new FlouciPayment();
            payment.paymentId = json.getString("payment_id");
            payment.link = json.getString("link");
            payment.success = true;

            return payment;
        }
    }

    /**
     * Vérifier le statut du paiement avec retry automatique
     */
    public PaymentVerificationResult verifyPaymentWithRetry(String paymentId, int maxRetries) {
        AtomicInteger attempts = new AtomicInteger(0);
        
        while (attempts.get() < maxRetries) {
            attempts.incrementAndGet();
            
            try {
                boolean success = verifyPayment(paymentId);
                
                if (success) {
                    return new PaymentVerificationResult(true, "Paiement confirmé", attempts.get());
                } else {
                    // Attendre avant la prochaine tentative
                    if (attempts.get() < maxRetries) {
                        Thread.sleep(2000); // 2 secondes d'attente
                    }
                }
                
            } catch (Exception e) {
                System.err.println("❌ Tentative " + attempts.get() + " échouée: " + e.getMessage());
                
                if (attempts.get() >= maxRetries) {
                    return new PaymentVerificationResult(false, 
                        "Échec après " + maxRetries + " tentatives: " + e.getMessage(), 
                        attempts.get());
                }
                
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new PaymentVerificationResult(false, "Vérification interrompue", attempts.get());
                }
            }
        }
        
        return new PaymentVerificationResult(false, "Échec de vérification", maxRetries);
    }

    /**
     * Annuler un paiement côté Flouci si possible
     */
    public boolean cancelPayment(String paymentId) {
        try {
            if (config.isTest()) {
                System.out.println("🚫 [SIM] Annulation paiement: " + paymentId);
                return true;
            }

            String baseUrl = config.getFlouciBaseUrl();
            String authHeader = getAuthHeader();

            // Note: Adapter selon la documentation Flouci v2 pour l'annulation
            JSONObject body = new JSONObject();
            body.put("payment_id", paymentId);

            Request request = new Request.Builder()
                .url(baseUrl + "/v2/cancel_payment")
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", authHeader)
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    System.out.println("✅ Paiement annulé: " + paymentId);
                    return true;
                } else {
                    System.err.println("❌ Échec annulation: " + response.code());
                    return false;
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur annulation paiement: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gérer l'annulation depuis l'interface
     */
    public void handleUserCancellation(String paymentId) {
        try {
            // 1. Tenter d'annuler côté Flouci
            boolean flouciCancelled = cancelPayment(paymentId);
            
            // 2. Mettre à jour la BD dans tous les cas
            String status = flouciCancelled ? "ANNULE_FLOUCI" : "ANNULE_CLIENT";
            orderService.updateOrderStatus(paymentId, status);
            
            System.out.println("🚫 Gestion annulation: " + paymentId + " → " + status);
            
        } catch (Exception e) {
            System.err.println("❌ Erreur handleUserCancellation: " + e.getMessage());
            try {
                orderService.updateOrderStatus(paymentId, "ANNULE_ERREUR");
            } catch (Exception ex) {
                System.err.println("❌ Erreur mise à jour BD: " + ex.getMessage());
            }
        }
    }

    /**
     * Gérer l'échec de vérification après retries
     */
    public void handleVerificationFailure(String paymentId, String errorMessage) {
        try {
            orderService.updateOrderStatus(paymentId, "ECHEC_VERIFICATION");
            
            // Logger pour investigation manuelle
            System.err.println("🔍 ÉCHEC VERIFICATION - À investiguer manuellement:");
            System.err.println("   Payment ID: " + paymentId);
            System.err.println("   Erreur: " + errorMessage);
            System.err.println("   Timestamp: " + java.time.LocalDateTime.now());
            
        } catch (Exception e) {
            System.err.println("❌ Erreur handleVerificationFailure: " + e.getMessage());
        }
    }

    private boolean verifyPayment(String paymentId) throws Exception {
        if (config.isTest()) {
            return Math.random() > 0.3; // 70% de succès en simulation
        }

        String baseUrl = config.getFlouciBaseUrl();
        String authHeader = getAuthHeader();

        Request request = new Request.Builder()
            .url(baseUrl + "/v2/verify_payment/" + paymentId)
            .get()
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", authHeader)
            .build();

        try (Response response = client.newCall(request).execute()) {
            JSONObject json = new JSONObject(response.body().string());
            System.out.println("Verify response: " + json);
            
            return "SUCCESS".equals(json.optString("result"));
        }
    }

    private FlouciPayment createSimulationPayment(double amount) {
        FlouciPayment payment = new FlouciPayment();
        payment.paymentId = "SIM_" + System.currentTimeMillis();
        payment.link = "https://app.flouci.com";
        payment.success = true;
        System.out.println("💳 [SIM] Paiement créé: " + amount + " TND");
        return payment;
    }

    private String getAuthHeader() {
        return "Bearer " + config.getFlouciPublicKey() + ":" + config.getFlouciPrivateKey();
    }

    private int getWebhookPort() {
        return 8080; // Port du webhook server
    }

    /**
     * Classe résultat pour la vérification avec retry
     */
    public static class PaymentVerificationResult {
        public final boolean success;
        public final String message;
        public final int attempts;

        public PaymentVerificationResult(boolean success, String message, int attempts) {
            this.success = success;
            this.message = message;
            this.attempts = attempts;
        }
    }

    /**
     * Classe paiement
     */
    public static class FlouciPayment {
        public String paymentId;
        public String link;
        public boolean success;
    }
}
