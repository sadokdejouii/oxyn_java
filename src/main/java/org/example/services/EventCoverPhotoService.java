package org.example.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class EventCoverPhotoService {

    private static final String API_URL = "https://api.pexels.com/v1/search";

    private final HttpClient httpClient;
    private final ConcurrentHashMap<String, CompletableFuture<Optional<byte[]>>> imageCache = new ConcurrentHashMap<>();

    public EventCoverPhotoService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public CompletableFuture<Optional<byte[]>> resolveCoverImageAsync(String visualKey, String title, String description) {
        String searchQuery = buildSearchQuery(visualKey, title, description);
        String cacheKey = visualKey + "::" + searchQuery;
        return imageCache.computeIfAbsent(cacheKey, ignored ->
                CompletableFuture.supplyAsync(() -> fetchCoverImage(searchQuery))
                        .whenComplete((result, error) -> {
                            if (error != null || result == null || result.isEmpty()) {
                                imageCache.remove(cacheKey);
                            }
                        }));
    }

    private Optional<byte[]> fetchCoverImage(String query) {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URI searchUri = URI.create(API_URL + "?query=" + encodedQuery + "&per_page=1&orientation=landscape&size=medium");

            HttpRequest searchRequest = HttpRequest.newBuilder()
                    .uri(searchUri)
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> searchResponse = httpClient.send(searchRequest, HttpResponse.BodyHandlers.ofString());
            if (searchResponse.statusCode() < 200 || searchResponse.statusCode() >= 300) {
                return Optional.empty();
            }

            String imageUrl = extractImageUrl(searchResponse.body());
            if (imageUrl == null || imageUrl.isBlank()) {
                return Optional.empty();
            }

            HttpRequest imageRequest = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<byte[]> imageResponse = httpClient.send(imageRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (imageResponse.statusCode() < 200 || imageResponse.statusCode() >= 300 || imageResponse.body().length == 0) {
                return Optional.empty();
            }

            return Optional.of(imageResponse.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    private String extractImageUrl(String responseBody) {
        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray photos = root.getAsJsonArray("photos");
        if (photos == null || photos.isEmpty()) {
            return null;
        }

        JsonObject photo = photos.get(0).getAsJsonObject();
        if (!photo.has("src")) {
            return null;
        }

        JsonObject src = photo.getAsJsonObject("src");
        if (src.has("landscape")) {
            return src.get("landscape").getAsString();
        }
        if (src.has("large")) {
            return src.get("large").getAsString();
        }
        if (src.has("medium")) {
            return src.get("medium").getAsString();
        }
        return src.has("original") ? src.get("original").getAsString() : null;
    }

    private String buildSearchQuery(String visualKey, String title, String description) {
        String combined = (safe(title) + " " + safe(description)).toLowerCase();

        if (combined.contains("box") || combined.contains("boxing")) {
            return "boxing ring training";
        }
        if (combined.contains("crossfit") || combined.contains("musculation") || combined.contains("workout") || combined.contains("gym")) {
            return "gym workout training";
        }
        if (combined.contains("cardio") || combined.contains("fitness")) {
            return "cardio fitness workout";
        }
        if (combined.contains("circuit") || combined.contains("course") || combined.contains("urban")) {
            return "circuit training race";
        }

        return switch (safe(visualKey).toLowerCase()) {
            case "yoga" -> "yoga wellness class";
            case "football" -> "football match stadium";
            case "basket", "basketball" -> "basketball game court";
            case "tennis" -> "tennis training court";
            case "running" -> "running race fitness";
            case "swimming" -> "swimming pool training";
            case "music" -> "concert crowd stage";
            default -> buildGenericQuery(title, description);
        };
    }

    private String buildGenericQuery(String title, String description) {
        StringBuilder query = new StringBuilder();
        appendIfPresent(query, title);
        appendIfPresent(query, description);
        if (query.length() == 0) {
            return "fitness event people";
        }
        return query + " sports event";
    }

    private void appendIfPresent(StringBuilder query, String value) {
        String cleaned = safe(value)
                .replaceAll("[^\\p{L}\\p{N} ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.isEmpty()) {
            return;
        }

        String[] parts = cleaned.split(" ");
        int added = 0;
        for (String part : parts) {
            if (part.length() < 3) {
                continue;
            }
            if (query.length() > 0) {
                query.append(' ');
            }
            query.append(part);
            added++;
            if (added >= 5) {
                break;
            }
        }
    }

    private String resolveApiKey() {
        String envKey = System.getenv("PEXELS_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey;
        }

        String fallbackEnvKey = System.getenv("OXYN_PEXELS_API_KEY");
        if (fallbackEnvKey != null && !fallbackEnvKey.isBlank()) {
            return fallbackEnvKey;
        }

        String propertyKey = System.getProperty("pexels.api.key");
        return propertyKey == null || propertyKey.isBlank() ? null : propertyKey;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}