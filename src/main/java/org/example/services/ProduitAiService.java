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
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Génération de description produit via Pollinations (OpenAI-compatible).
 * Docs: https://enter.pollinations.ai/api/docs
 *
 * Notes:
 * - L'API de Pollinations est compatible avec /v1/chat/completions.
 * - Une clé Pollinations (pk_... ou sk_...) est requise pour générer (selon leur politique).
 */
public final class ProduitAiService {

    private static final String API_URL = "https://gen.pollinations.ai/v1/chat/completions";
    private static final String DEFAULT_MODEL = "openai";
    // TODO: Remplacer par votre clé Pollinations (pk_... ou sk_...) depuis https://enter.pollinations.ai/
    private static final String POLLINATIONS_API_KEY = "sk_qjnHubDKSIZYJElDJ0xgu2JrIupYSaMV";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    public GenerationResult genererDescriptionProduit(String nomProduit, double prix, String statut) {
        String apiKey = POLLINATIONS_API_KEY;
        if (apiKey == null || apiKey.isBlank() || apiKey.endsWith("_replace_me") || apiKey.contains("xxxx")) {
            return new GenerationResult(
                    genererLocalFallback(nomProduit, prix, statut),
                    false,
                    "Clé Pollinations absente/invalide dans le code. Description générée en fallback local.");
        }
        try {
            return new GenerationResult(
                    appelerOpenAi(apiKey, nomProduit, prix, statut),
                    true,
                    null);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "";
            if (msg.contains("HTTP 429")) {
                return new GenerationResult(
                        genererLocalFallback(nomProduit, prix, statut),
                        false,
                        "Pollinations refuse temporairement (HTTP 429: trop de requêtes / rate limit). "
                                + "Attends 30–60s et réessaie. "
                                + "Fallback local utilisé.");
            }
            if (msg.contains("HTTP 402")) {
                return new GenerationResult(
                        genererLocalFallback(nomProduit, prix, statut),
                        false,
                        "Pollinations indique un solde insuffisant (HTTP 402). "
                                + "Essaie avec une clé publishable (pk_) ou attends un refill, puis réessaie. "
                                + "Fallback local utilisé.");
            }
            return new GenerationResult(
                    genererLocalFallback(nomProduit, prix, statut),
                    false,
                    "Échec API Pollinations (" + ex.getMessage() + "). Fallback local utilisé.");
        }
    }

    private String appelerOpenAi(String apiKey, String nomProduit, double prix, String statut)
            throws IOException, InterruptedException {
        String model = lireModele();
        String angle = choisirAngleRedaction();
        String nom = safe(nomProduit);
        String statutSafe = safe(statut);

        String userPrompt = "Génère une description produit e-commerce en français, naturelle et unique.\n"
                + "Contraintes:\n"
                + "- 70 à 120 mots\n"
                + "- Ton vendeur premium mais crédible\n"
                + "- Pas de phrases génériques répétitives\n"
                + "- Inclure le prix sans exagération marketing\n"
                + "- Angle de rédaction: " + angle + "\n\n"
                + "Données produit:\n"
                + "- Nom: " + nom + "\n"
                + "- Prix: " + String.format(Locale.FRANCE, "%.2f TND", prix) + "\n"
                + "- Statut: " + (statutSafe.isBlank() ? "Disponible" : statutSafe);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", 0.95);

        JsonArray messages = new JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content",
                "Tu es un copywriter e-commerce senior. Tu rédiges des descriptions distinctes pour chaque produit.");
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userPrompt);
        messages.add(sys);
        messages.add(user);
        body.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = sendWithRetryOn429(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IOException("Réponse OpenAI vide.");
        }
        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        if (message == null || !message.has("content")) {
            throw new IOException("Format de réponse inattendu.");
        }
        String content = message.get("content").getAsString().trim();
        if (content.isBlank()) {
            throw new IOException("Contenu de description vide.");
        }
        return content;
    }

    private HttpResponse<String> sendWithRetryOn429(HttpRequest request) throws IOException, InterruptedException {
        int attempts = 0;
        long backoffMs = 900;
        while (true) {
            attempts++;
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 429) {
                return response;
            }
            if (attempts >= 3) {
                return response;
            }
            long waitMs = retryAfterMs(response);
            if (waitMs <= 0) {
                waitMs = backoffMs;
                backoffMs = Math.min(4000, backoffMs * 2);
            }
            Thread.sleep(waitMs);
        }
    }

    private static long retryAfterMs(HttpResponse<?> response) {
        try {
            return response.headers()
                    .firstValue("retry-after")
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(s -> (long) (Double.parseDouble(s) * 1000))
                    .orElse(0L);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private String lireModele() {
        String model = System.getenv("POLLINATIONS_MODEL");
        return model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    private String choisirAngleRedaction() {
        String[] angles = {
                "bénéfices concrets pour usage quotidien",
                "qualité matière et durabilité",
                "confort d'utilisation et simplicité",
                "positionnement rapport qualité/prix",
                "contexte cadeau et usage événementiel"
        };
        return angles[ThreadLocalRandom.current().nextInt(angles.length)];
    }

    private String genererLocalFallback(String nomProduit, double prix, String statut) {
        // Ce fallback reste varié pour éviter des descriptions quasi identiques.
        String[] intros = {
                "%s se distingue par son équilibre entre praticité et style.",
                "Pensé pour les clients exigeants, %s combine simplicité et efficacité.",
                "%s apporte une réponse fiable pour un usage régulier.",
                "Avec %s, vous optez pour un produit orienté confort d'utilisation."
        };
        String[] suites = {
                "Son tarif de %s le place comme un choix judicieux pour un achat durable.",
                "Proposé à %s, il vise un excellent ratio qualité/prix.",
                "Affiché à %s, il reste accessible tout en gardant une finition soignée.",
                "À %s, ce produit conserve un positionnement cohérent avec ses bénéfices."
        };
        String[] fins = {
                "Il convient aussi bien à une utilisation personnelle qu'à un besoin professionnel.",
                "Son usage est intuitif, ce qui facilite son adoption dès la première prise en main.",
                "Le rendu final reste constant, même dans un usage fréquent.",
                "Il s'intègre facilement dans différents contextes d'utilisation."
        };
        int i = ThreadLocalRandom.current().nextInt(intros.length);
        int j = ThreadLocalRandom.current().nextInt(suites.length);
        int k = ThreadLocalRandom.current().nextInt(fins.length);

        String base = String.format(intros[i], safe(nomProduit)) + " "
                + String.format(suites[j], String.format(Locale.FRANCE, "%.2f TND", prix)) + " "
                + fins[k];
        String statutSafe = safe(statut);
        if (!statutSafe.isBlank()) {
            base += " Statut: " + statutSafe + ".";
        }
        return base;
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }

    public record GenerationResult(String description, boolean fromExternalApi, String warning) {
    }
}
