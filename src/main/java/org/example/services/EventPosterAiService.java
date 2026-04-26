package org.example.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

public class EventPosterAiService {

    private static final String API_URL = "https://api.openai.com/v1/images/generations";
    private static final String DEFAULT_MODEL = "gpt-image-1";

    private final HttpClient httpClient;

    public EventPosterAiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public byte[] generatePosterBackground(PosterRequest posterRequest) throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "La variable d'environnement OPENAI_API_KEY est manquante. "
                            + "Ajoutez votre cle API OpenAI puis relancez l'application."
            );
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("model", resolveModel());
        payload.addProperty("size", "1024x1536");
        payload.addProperty("prompt", buildPrompt(posterRequest));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Erreur OpenAI (" + response.statusCode() + "): " + extractErrorMessage(response.body()));
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray data = root.getAsJsonArray("data");
        if (data == null || data.isEmpty()) {
            throw new IOException("OpenAI n'a retourne aucune image.");
        }

        JsonObject firstItem = data.get(0).getAsJsonObject();
        if (firstItem.has("b64_json")) {
            return Base64.getDecoder().decode(firstItem.get("b64_json").getAsString());
        }

        if (firstItem.has("url")) {
            HttpRequest imageRequest = HttpRequest.newBuilder()
                    .uri(URI.create(firstItem.get("url").getAsString()))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();
            HttpResponse<byte[]> imageResponse = httpClient.send(imageRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (imageResponse.statusCode() >= 200 && imageResponse.statusCode() < 300) {
                return imageResponse.body();
            }
        }

        throw new IOException("Reponse OpenAI invalide: aucune image exploitable n'a ete retournee.");
    }

    private String resolveModel() {
        String configuredModel = System.getenv("OXYN_OPENAI_IMAGE_MODEL");
        return configuredModel == null || configuredModel.isBlank() ? DEFAULT_MODEL : configuredModel;
    }

    private String buildPrompt(PosterRequest posterRequest) {
        String description = clean(posterRequest.description(), "Fitness event");
        String lieu = clean(posterRequest.lieu(), "OXYN venue");
        String ville = clean(posterRequest.ville(), "Tunisia");

        return "Create a premium Facebook event poster background for a wellness and fitness brand. "
                + "Style: cinematic, elegant, modern, dark navy atmosphere, electric blue highlights, subtle gold accents, luxurious sports club vibe. "
                + "Do not render any words, letters, logos, numbers, or watermark. Leave clean negative space for text overlay. "
                + "Event title inspiration: " + clean(posterRequest.titre(), "OXYN event") + ". "
                + "Event description inspiration: " + description + ". "
                + "Location inspiration: " + lieu + ", " + ville + ". "
                + "Poster should feel beautiful, amazing, premium, and suitable for social media upload.";
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            if (root.has("error")) {
                JsonObject error = root.getAsJsonObject("error");
                if (error.has("message")) {
                    return error.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
            // Fall back to raw response body below.
        }
        return responseBody;
    }

    private String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.replace('\n', ' ').trim();
    }

    public record PosterRequest(
            String titre,
            String description,
            String lieu,
            String ville,
            String debut,
            String fin,
            int placesMax
    ) {
    }
}