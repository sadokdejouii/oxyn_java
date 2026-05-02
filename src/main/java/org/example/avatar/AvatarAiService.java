package org.example.avatar;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Génère un avatar anime/cartoon depuis une photo via HuggingFace (img2img).
 */
public class AvatarAiService {

    private static final String HF_TOKEN = resolveHfToken();

    private static String resolveHfToken() {
        String env = System.getenv("HF_TOKEN");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        try {
            String p = org.example.utils.ConfigManager.getInstance().getHfToken();
            return p != null ? p.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static final String SUBMIT_URL = "https://router.huggingface.co/wavespeed/api/v3/wavespeed-ai/qwen-image/edit-plus-lora";
    private static final String PROMPT     = "Convert this portrait into anime style";
    private static final String SDXL_URL   = "https://router.huggingface.co/hf-inference/models/stabilityai/stable-diffusion-xl-base-1.0";

    /**
     * Génère un avatar anime en envoyant l'image (bytes JPEG/PNG) au modèle HuggingFace.
     * @param imageBytes Bytes de l'image source
     * @return Bytes de l'image générée (JPEG), ou null en cas d'erreur
     */
    public byte[] generateAnimeAvatar(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) return null;
        try {
            String b64    = Base64.getEncoder().encodeToString(imageBytes);
            String dataUrl = "data:image/jpeg;base64," + b64;

            String json = "{"
                    + "\"images\":[\"" + dataUrl + "\"],"
                    + "\"prompt\":\"" + PROMPT + "\","
                    + "\"loras\":[],"
                    + "\"seed\":-1,"
                    + "\"output_format\":\"jpeg\","
                    + "\"enable_sync_mode\":true"
                    + "}";

            return callHuggingFace(SUBMIT_URL, json.getBytes(StandardCharsets.UTF_8), "application/json");
        } catch (Exception e) {
            System.err.println("[AvatarAI] Erreur img2img: " + e.getMessage());
            return null;
        }
    }

    /**
     * Génère un avatar depuis un prompt texte uniquement (SDXL text-to-image).
     */
    public byte[] generateFromPrompt(String customPrompt) {
        String prompt = (customPrompt != null && !customPrompt.isBlank()) ? customPrompt : PROMPT;
        String json   = "{\"inputs\":\"" + prompt.replace("\"", "\\\"") + "\"}";
        try {
            return callHuggingFace(SDXL_URL, json.getBytes(StandardCharsets.UTF_8), "application/json");
        } catch (Exception e) {
            System.err.println("[AvatarAI] Erreur prompt: " + e.getMessage());
            return null;
        }
    }

    private byte[] callHuggingFace(String urlStr, byte[] body, String contentType) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(150_000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + HF_TOKEN);
        conn.setRequestProperty("Content-Type", contentType);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }

        int code = conn.getResponseCode();
        if (code == 200) {
            try (InputStream is = conn.getInputStream()) {
                byte[] responseBytes = is.readAllBytes();
                String responseStr   = new String(responseBytes, StandardCharsets.UTF_8);

                if (responseStr.trim().startsWith("{")) {
                    int start = responseStr.indexOf("data:image/jpeg;base64,");
                    if (start != -1) {
                        int end = responseStr.indexOf("\"", start);
                        String b64Data = responseStr.substring(start + "data:image/jpeg;base64,".length(), end);
                        return Base64.getDecoder().decode(b64Data);
                    }
                    int urlStart = responseStr.indexOf("http");
                    if (urlStart != -1) {
                        int urlEnd = responseStr.indexOf("\"", urlStart);
                        return downloadImage(responseStr.substring(urlStart, urlEnd));
                    }
                }
                return responseBytes;
            }
        }

        InputStream err = conn.getErrorStream();
        if (err != null) {
            System.err.println("[AvatarAI] HTTP " + code + ": " + new String(err.readAllBytes(), StandardCharsets.UTF_8));
        }
        return null;
    }

    private byte[] downloadImage(String urlStr) throws Exception {
        try (InputStream is = new URL(urlStr).openStream()) {
            return is.readAllBytes();
        }
    }
}

