package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestGemini {
    public static void main(String[] args) throws Exception {
        String apiKey = "AIzaSyAQHvSW-EokJy-X6wuS8YPQhVXY2abDX_E";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;
        
        String body = "{\"contents\":[{\"parts\":[{\"text\":\"Dis bonjour en français\"}]}]}";
        
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        
        HttpResponse<String> res = HttpClient.newHttpClient()
            .send(req, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("STATUS: " + res.statusCode());
        System.out.println("BODY: " + res.body());
        
        // Test - Lister les modèles disponibles
        System.out.println("\n=== LISTE DES MODÈLES DISPONIBLES ===");
        String listUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;
        
        HttpRequest listReq = HttpRequest.newBuilder()
            .uri(URI.create(listUrl))
            .GET()
            .build();
        
        HttpResponse<String> listRes = HttpClient.newHttpClient()
            .send(listReq, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("LIST STATUS: " + listRes.statusCode());
        System.out.println("LIST BODY: " + listRes.body());
    }
}
