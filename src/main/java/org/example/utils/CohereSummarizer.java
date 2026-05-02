package org.example.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for summarizing text using the Cohere AI API (v2 with v1 fallback).
 * Uses HttpURLConnection without external dependencies.
 */
public class CohereSummarizer {

    private static final Logger LOGGER = Logger.getLogger(CohereSummarizer.class.getName());

    // API Configuration
    private static final String API_KEY     = "77hxudBQaNzIwtgizELsVUDkcnzhxhYxC1I7Ewcu";
    private static final String API_URL_V2  = "https://api.cohere.com/v2/chat";
    private static final String API_URL_V1  = "https://api.cohere.com/v1/chat"; // fallback
    private static final String MODEL       = "command-r-08-2024";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS    = 30_000;
    private static final int MAX_RETRIES        = 3;
    private static final int RETRY_DELAY_MS     = 1_000;

    // ------------------------------------------------------------------ //
    //  Public entry point
    // ------------------------------------------------------------------ //

    public static String summarize(String text) {
        if (text == null || text.trim().isEmpty()) return text;
        if (text.trim().length() < 50)             return text;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                String summary = sendRequest(text);
                if (summary != null && !summary.trim().isEmpty()) return summary;
            } catch (IOException e) {
                LOGGER.log(Level.WARNING,
                        "Summarization attempt " + (attempt + 1) + " failed: " + e.getMessage());
                if (attempt < MAX_RETRIES - 1) {
                    try { Thread.sleep(RETRY_DELAY_MS); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }

        LOGGER.log(Level.WARNING, "All summarization attempts failed, returning original text");
        return text;
    }

    // ------------------------------------------------------------------ //
    //  Network – tries v2, falls back to v1 automatically on 404
    // ------------------------------------------------------------------ //

    private static String sendRequest(String text) throws IOException {
        try {
            return sendToEndpoint(API_URL_V2, buildV2Body(text));
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("HTTP_404")) {
                LOGGER.log(Level.WARNING, "v2 returned 404 – retrying with v1 endpoint");
                return sendToEndpoint(API_URL_V1, buildV1Body(text));
            }
            throw e;
        }
    }

    private static String sendToEndpoint(String apiUrl, String jsonBody) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();

            // Retryable server errors
            if (code == 429 || (code >= 500 && code < 600)) {
                throw new IOException("Retryable error: HTTP " + code);
            }

            if (code != 200) {
                // Read error body for diagnosis
                String errBody = "";
                try { errBody = readStream(conn, true); } catch (Exception ignored) {}
                LOGGER.log(Level.WARNING,
                        "HTTP " + code + " from " + apiUrl + " | error body: " + errBody);
                throw new IOException("HTTP_" + code + ": " + errBody);
            }

            String responseBody = readStream(conn, false);
            LOGGER.log(Level.FINE, "Raw response from " + apiUrl + ": " + responseBody);
            return extractText(responseBody);

        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ------------------------------------------------------------------ //
    //  Request body builders
    // ------------------------------------------------------------------ //

    /** v2 API: messages array format */
    private static String buildV2Body(String text) {
        String prompt = escapeJson(
                "Summarize the following text in 2-3 concise sentences: " + text);
        return String.format(
                "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
                MODEL, prompt);
    }

    /** v1 API: flat 'message' string format */
    private static String buildV1Body(String text) {
        String prompt = escapeJson(
                "Summarize the following text in 2-3 concise sentences: " + text);
        return String.format(
                "{\"model\":\"%s\",\"message\":\"%s\"}",
                MODEL, prompt);
    }

    // ------------------------------------------------------------------ //
    //  Response parsing  (handles v2 and v1 shapes)
    // ------------------------------------------------------------------ //

    /**
     * v2 response shape:
     *   { "message": { "content": [ { "type": "text", "text": "THE SUMMARY" } ] } }
     *
     * v1 response shape:
     *   { "text": "THE SUMMARY", ... }
     */
    private static String extractText(String json) {
        if (json == null || json.isEmpty()) return null;

        // --- v2: message -> content[] -> first object whose "text" is long ---
        int msgIdx = json.indexOf("\"message\"");
        if (msgIdx != -1) {
            int contentIdx = json.indexOf("\"content\"", msgIdx);
            if (contentIdx != -1) {
                int searchFrom = contentIdx;
                while (true) {
                    int textIdx = json.indexOf("\"text\"", searchFrom);
                    if (textIdx == -1) break;
                    String value = extractStringValue(json, textIdx);
                    if (value != null && value.length() > 5) return value; // skip "text" type markers
                    searchFrom = textIdx + 6;
                }
            }
        }

        // --- v1 / generic fallback: top-level "text" field ---
        int textIdx = json.indexOf("\"text\"");
        if (textIdx != -1) {
            String value = extractStringValue(json, textIdx);
            if (value != null && !value.trim().isEmpty()) return value;
        }

        LOGGER.log(Level.WARNING, "Could not parse text from response: " + json);
        return null;
    }

    // ------------------------------------------------------------------ //
    //  JSON helpers
    // ------------------------------------------------------------------ //

    private static String extractStringValue(String json, int keyIndex) {
        int colon = json.indexOf(':', keyIndex);
        if (colon == -1) return null;
        int openQuote = json.indexOf('"', colon + 1);
        if (openQuote == -1) return null;

        int i = openQuote + 1;
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/');  break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'u':
                        if (i + 4 < json.length()) {
                            try {
                                sb.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                                i += 4;
                            } catch (NumberFormatException ignore) { sb.append(c); }
                        }
                        break;
                    default: sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < ' ') sb.append(String.format("\\u%04x", (int) c));
                    else         sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String readStream(HttpURLConnection conn, boolean errorStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                errorStream ? conn.getErrorStream() : conn.getInputStream(),
                StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }
}