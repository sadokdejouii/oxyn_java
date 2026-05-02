package org.example.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.utils.ConfigManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Client HTTP minimal pour l'API Groq (OpenAI-compatible).
 * Clé : variable d'environnement {@code GROQ_API_KEY} ou propriété {@code GROQ_API_KEY} dans {@code config.properties}.
 */
public class GroqService {

    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;

    public GroqService() {
        String key = System.getenv("GROQ_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getProperty("GROQ_API_KEY", "");
        }
        if (key.isBlank()) {
            key = ConfigManager.getInstance().getGroqApiKey();
        }
        this.apiKey = key != null ? key.trim() : "";
    }

    public CompletableFuture<String> askFitnessQuestionAsync(String question) {
        final String q = question == null ? "" : question.trim();
        if (apiKey.isBlank()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("GROQ_API_KEY manquante (env ou config.properties)"));
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                ObjectNode body = mapper.createObjectNode();
                body.put("model", MODEL);
                ArrayNode messages = body.putArray("messages");
                ObjectNode sys = messages.addObject();
                sys.put("role", "system");
                sys.put("content",
                        "Tu es le coach fitness OXYN (Tunisie). Reponses utiles et concises en francais.");
                ObjectNode user = messages.addObject();
                user.put("role", "user");
                user.put("content", q);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ENDPOINT))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                        .build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                    throw new IllegalStateException("Groq HTTP " + resp.statusCode() + ": " + resp.body());
                }
                JsonNode root = mapper.readTree(resp.body());
                return root.path("choices").path(0).path("message").path("content").asText("").trim();
            } catch (Exception e) {
                if (e instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Appel léger pour vérifier la clé et l'accessibilité de l'API (bloque le thread appelant).
     */
    public boolean testConnection() {
        try {
            if (apiKey.isBlank()) {
                return false;
            }
            String r = askFitnessQuestionAsync("Reponds uniquement par le mot: OK").join();
            return r != null && !r.isBlank();
        } catch (Exception e) {
            return false;
        }
    }
}
