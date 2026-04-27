package org.example.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.entities.commandes;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class StripePaymentService {

    private static final String PAYMENT_INTENT_ENDPOINT = "https://api.stripe.com/v1/payment_intents";
    private static final String DEFAULT_CURRENCY = "eur";
    private static final String STRIPE_SECRET_KEY = "sk_test_51QUbRfJtir63S780PJgPYYdOb1HByaXvKAmTkEbLvDDgfbLEgt10PbjHeIvrEWKO3oMIOKxZDTOqajr1D9FlM4K000jAhS07yK";
    private static final String STRIPE_PUBLISHABLE_KEY = "pk_test_51QUbRfJtir63S780sr3tNZW1FLBD0Pqc2VfAxmvH4m53zQhgesKwYlvDKThZyTheiaTFy8C4Y0xG9CEyRALMMZRO00C9hWb004";
    private static final String STRIPE_CURRENCY = DEFAULT_CURRENCY;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public PaymentIntentData createPaymentIntentForCommande(int idCommande, double totalCommande) throws IOException, InterruptedException {
        String secretKey = getSecretKey();
        String publishableKey = getPublishableKey();
        String currency = getCurrency();
        long amountMinor = toMinorUnit(totalCommande);

        String body = "amount=" + amountMinor
                + "&currency=" + encode(currency)
                + "&description=" + encode("Commande OXYN #" + idCommande)
                + "&metadata[id_commande]=" + idCommande
                + "&automatic_payment_methods[enabled]=true";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PAYMENT_INTENT_ENDPOINT))
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Stripe API a retourné " + response.statusCode() + " : " + response.body());
        }
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!json.has("client_secret")) {
            throw new IOException("Réponse Stripe invalide: client_secret absent.");
        }
        String clientSecret = json.get("client_secret").getAsString();
        return new PaymentIntentData(clientSecret, publishableKey);
    }

    public PaymentIntentData createPaymentIntentForCommande(commandes commande) throws IOException, InterruptedException {
        if (commande == null || commande.getId_commande() <= 0 || commande.getTotal_commande() <= 0) {
            throw new IllegalArgumentException("Commande invalide pour initialiser Stripe.");
        }
        return createPaymentIntentForCommande(commande.getId_commande(), commande.getTotal_commande());
    }

    private static long toMinorUnit(double amount) {
        return Math.round(amount * 100.0d);
    }

    private static String getSecretKey() {
        return STRIPE_SECRET_KEY;
    }

    private static String getPublishableKey() {
        return STRIPE_PUBLISHABLE_KEY;
    }

    private static String getCurrency() {
        return STRIPE_CURRENCY.trim().toLowerCase();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record PaymentIntentData(String clientSecret, String publishableKey) {
    }
}
