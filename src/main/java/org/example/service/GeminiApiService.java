package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GeminiApiService {

    private static final String API_KEY = "AIzaSyBb74mBN7Il3E_vsYFO1nqVwtBK7U2FuGA";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;
    private static final String SYSTEM_PROMPT = "Tu es l'assistant OXYN salle de sport Tunisie. Salles : 1.California Gym Tunis 2.BLUE Hammamet 3.Factory Sfax. Séances : Box Anglaise, Yoga, Aérobique, Pilates. Réponds en français.";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<ObjectNode> history = new ArrayList<>();

    public CompletableFuture<String> sendMessage(String userMessage) {
        final String msgCopy = userMessage;
        return CompletableFuture.supplyAsync(new java.util.function.Supplier<String>() {
            @Override
            public String get() {
                try {
                    history.add(buildTurn("user", msgCopy));
                    while (history.size() > 20) { history.remove(0); }

                    ObjectNode body = mapper.createObjectNode();

                    ObjectNode sys = mapper.createObjectNode();
                    ObjectNode sysPart = mapper.createObjectNode();
                    sysPart.put("text", SYSTEM_PROMPT);
                    ArrayNode sysParts = mapper.createArrayNode();
                    sysParts.add(sysPart);
                    sys.set("parts", sysParts);
                    body.set("systemInstruction", sys);

                    ArrayNode contents = mapper.createArrayNode();
                    for (ObjectNode h : history) { contents.add(h); }
                    body.set("contents", contents);

                    ObjectNode cfg = mapper.createObjectNode();
                    cfg.put("maxOutputTokens", 1024);
                    body.set("generationConfig", cfg);

                    HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                        .build();

                    HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                    System.out.println("GEMINI STATUS: " + res.statusCode());

                    if (res.statusCode() == 429) {
                        // Too Many Requests - attendre et réessayer
                        System.out.println("⏳ Rate limit atteint, attente de 5 secondes...");
                        Thread.sleep(5000);
                        
                        // Réessayer une seule fois
                        res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                        System.out.println("GEMINI RETRY STATUS: " + res.statusCode());
                        
                        if (res.statusCode() == 429) {
                            return "⏳ Désolé, trop de demandes. Veuillez réessayer dans quelques instants.";
                        }
                    }
                    
                    if (res.statusCode() != 200) return "❌ Erreur " + res.statusCode();

                    String reply = mapper.readTree(res.body())
                        .path("candidates").get(0)
                        .path("content").path("parts").get(0)
                        .path("text").asText("Pas de réponse.");

                    history.add(buildTurn("model", reply));
                    return reply;

                } catch (Exception e) {
                    return "❌ Erreur: " + e.getMessage();
                }
            }
        });
    }

    public void resetConversation() { history.clear(); }
    public boolean isApiKeyConfigured() { return !API_KEY.equals("AIzaSyAQHvSW-EokJy-X6wuS8YPQhVXY2abDX_E"); }

    private ObjectNode buildTurn(String role, String text) {
        ObjectNode t = mapper.createObjectNode();
        t.put("role", role);
        ObjectNode p = mapper.createObjectNode();
        p.put("text", text);
        ArrayNode ps = mapper.createArrayNode();
        ps.add(p);
        t.set("parts", ps);
        return t;
    }
}