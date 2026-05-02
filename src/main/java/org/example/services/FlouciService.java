package org.example.services;

public class FlouciService {

    public static class FlouciPayment {
        public boolean success;
        public String  paymentId;
        public String  link;
    }

    public FlouciPayment createPayment(double amount, String description) {
        System.out.println("⚠️ MODE DEMO: paiement simulé de " + amount + " TND");
        FlouciPayment result = new FlouciPayment();
        result.success   = true;
        result.paymentId = "DEMO-" + System.currentTimeMillis();
        result.link      = "https://flouci.com/pay/demo";
        return result;
    }

    public boolean verifyPayment(String paymentId) {
        System.out.println("⚠️ MODE DEMO: vérification simulée pour " + paymentId);
        return true;
    }
}
