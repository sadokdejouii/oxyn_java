package org.example.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Couche d'integration n8n "pro" mais sans dependance forte.
 * - mode simulation configurable
 * - timeout court
 * - fallback local via retour null
 */
public final class N8nService {

    private static final String PROPERTIES_PATH = "/realtime.properties";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(2);

    private final boolean simulation;
    private final String workflowUrl;
    private final HttpClient httpClient;

    public N8nService() {
        Properties props = loadProperties();
        this.simulation = Boolean.parseBoolean(resolve(props, "n8n.simulation", "true"));
        this.workflowUrl = resolve(props, "n8n.url", "http://localhost:5678/webhook/ia-workflow");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .build();
    }

    public boolean isSimulationEnabled() {
        return simulation;
    }

    /**
     * Simule ou tente un appel n8n.
     * @return JSON de reponse n8n; null si echec reel (pour fallback IA locale).
     */
    public String callN8nWorkflow(String objectif, String ficheSanteJson) {
        System.out.println("[N8N] Sending data to workflow...");

        if (simulation) {
            simulateExternalDelay();
            System.out.println("[N8N] Response received");
            return buildMockResponseJson(objectif);
        }

        try {
            String payload = buildPayload(objectif, ficheSanteJson);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(workflowUrl))
                    .timeout(HTTP_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300
                    && response.body() != null && !response.body().isBlank()) {
                simulateExternalDelay();
                System.out.println("[N8N] Response received");
                return response.body();
            }
        } catch (Exception ignored) {
            // fallback local managed by caller
        }

        System.out.println("[N8N] Fallback to local AI");
        return null;
    }

    public N8nResponse parseResponse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String analysis = root.has("analysis") && !root.get("analysis").isJsonNull()
                    ? root.get("analysis").getAsString() : "";
            String recommendation = root.has("recommendation") && !root.get("recommendation").isJsonNull()
                    ? root.get("recommendation").getAsString() : "";
            List<N8nResponse.ProductDTO> products = new ArrayList<>();
            if (root.has("products") && root.get("products").isJsonArray()) {
                JsonArray arr = root.getAsJsonArray("products");
                arr.forEach(el -> {
                    if (!el.isJsonObject()) {
                        return;
                    }
                    JsonObject p = el.getAsJsonObject();
                    String name = p.has("name") && !p.get("name").isJsonNull()
                            ? p.get("name").getAsString() : "";
                    double price = p.has("price") && !p.get("price").isJsonNull()
                            ? p.get("price").getAsDouble() : 0d;
                    products.add(new N8nResponse.ProductDTO(name, price));
                });
            }
            return new N8nResponse(analysis, recommendation, products);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String buildPayload(String objectif, String ficheSanteJson) {
        JsonObject root = new JsonObject();
        root.addProperty("objectif", objectif == null ? "" : objectif);
        if (ficheSanteJson == null || ficheSanteJson.isBlank()) {
            root.add("ficheSante", new JsonObject());
        } else {
            try {
                root.add("ficheSante", JsonParser.parseString(ficheSanteJson));
            } catch (Exception ignored) {
                root.addProperty("ficheSanteRaw", ficheSanteJson);
            }
        }
        return root.toString();
    }

    private static String buildMockResponseJson(String objectif) {
        String normalized = objectif == null ? "" : objectif.toLowerCase();
        String analysis = normalized.contains("genou")
                ? "Analyse de votre objectif : douleur au genou detectee"
                : "Analyse de votre objectif : point de vigilance detecte";
        JsonObject root = new JsonObject();
        root.addProperty("analysis", analysis);
        root.addProperty("recommendation", "Reduire les impacts et privilegier des exercices doux");

        JsonArray products = new JsonArray();
        JsonObject p1 = new JsonObject();
        p1.addProperty("name", "Genouillere de maintien");
        p1.addProperty("price", 59.9);
        products.add(p1);

        JsonObject p2 = new JsonObject();
        p2.addProperty("name", "Bande de compression");
        p2.addProperty("price", 25.0);
        products.add(p2);
        root.add("products", products);
        return root.toString();
    }

    private static void simulateExternalDelay() {
        long delayMs = ThreadLocalRandom.current().nextLong(800, 1501);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = N8nService.class.getResourceAsStream(PROPERTIES_PATH)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
        }
        return props;
    }

    private static String resolve(Properties props, String key, String defaultValue) {
        String envKey = key.toUpperCase().replace('.', '_');
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String prop = props.getProperty(key);
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }
        return defaultValue;
    }
}
