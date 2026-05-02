package org.example.services;

public final class StripePaymentSession {

    private static final StripePaymentSession INSTANCE = new StripePaymentSession();

    private int idCommande;
    private int idClient;
    private double totalCommande;
    private String clientSecret;
    private String publishableKey;

    private StripePaymentSession() {
    }

    public static StripePaymentSession getInstance() {
        return INSTANCE;
    }

    public void start(int idCommande, int idClient, double totalCommande, String clientSecret, String publishableKey) {
        this.idCommande = idCommande;
        this.idClient = idClient;
        this.totalCommande = totalCommande;
        this.clientSecret = clientSecret;
        this.publishableKey = publishableKey;
    }

    public boolean hasActiveSession() {
        return idCommande > 0
                && idClient > 0
                && totalCommande > 0
                && clientSecret != null
                && !clientSecret.isBlank()
                && publishableKey != null
                && !publishableKey.isBlank();
    }

    public int getIdCommande() {
        return idCommande;
    }

    public int getIdClient() {
        return idClient;
    }

    public double getTotalCommande() {
        return totalCommande;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public void clear() {
        this.idCommande = 0;
        this.idClient = 0;
        this.totalCommande = 0d;
        this.clientSecret = null;
        this.publishableKey = null;
    }
}
