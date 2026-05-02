package org.example.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conversion de devise depuis TND vers TND/EUR/USD.
 * Les taux sont récupérés en ligne et mis en cache pour limiter les appels réseau.
 */
public final class CurrencyExchangeService {

    private static final String BASE_CURRENCY = "TND";
    private static final String RATES_ENDPOINT = "https://open.er-api.com/v6/latest/" + BASE_CURRENCY;
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final Set<String> SUPPORTED = Set.of("TND", "EUR", "USD");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private final Map<String, Double> rates = new ConcurrentHashMap<>();
    private volatile Instant lastFetch = Instant.EPOCH;

    public CurrencyExchangeService() {
        rates.put(BASE_CURRENCY, 1.0);
    }

    public Set<String> supportedCurrencies() {
        return SUPPORTED;
    }

    public double convertFromTnd(double amountTnd, String targetCurrency) {
        String target = normalize(targetCurrency);
        if (BASE_CURRENCY.equals(target)) {
            return amountTnd;
        }
        ensureRatesLoaded();
        Double rate = rates.get(target);
        if (rate == null || rate <= 0) {
            return amountTnd;
        }
        return amountTnd * rate;
    }

    public String formatFromTnd(double amountTnd, String targetCurrency) {
        String currency = normalize(targetCurrency);
        double converted = convertFromTnd(amountTnd, currency);
        return String.format(Locale.FRANCE, "%.2f %s", converted, currency);
    }

    private void ensureRatesLoaded() {
        Instant now = Instant.now();
        if (Duration.between(lastFetch, now).compareTo(CACHE_TTL) < 0) {
            return;
        }
        synchronized (this) {
            now = Instant.now();
            if (Duration.between(lastFetch, now).compareTo(CACHE_TTL) < 0) {
                return;
            }
            try {
                refreshRatesFromApi();
                lastFetch = now;
            } catch (Exception ignored) {
                // Fallback silencieux: on garde les derniers taux connus (ou 1.0 pour TND).
            }
        }
    }

    private void refreshRatesFromApi() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RATES_ENDPOINT))
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode());
        }
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonObject ratesObj = root.getAsJsonObject("rates");
        if (ratesObj == null) {
            throw new IOException("Réponse taux invalide");
        }
        for (String currency : SUPPORTED) {
            if (BASE_CURRENCY.equals(currency)) {
                rates.put(currency, 1.0);
                continue;
            }
            if (ratesObj.has(currency) && ratesObj.get(currency).isJsonPrimitive()) {
                rates.put(currency, ratesObj.get(currency).getAsDouble());
            }
        }
    }

    private static String normalize(String currency) {
        if (currency == null || currency.isBlank()) {
            return BASE_CURRENCY;
        }
        String c = currency.trim().toUpperCase(Locale.ROOT);
        return SUPPORTED.contains(c) ? c : BASE_CURRENCY;
    }
}
