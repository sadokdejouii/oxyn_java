package org.example.realtime;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Génère les JWT HS256 attendus par Mercure pour publier ou souscrire.
 *
 * <p>Format minimal du payload Mercure :</p>
 * <pre>
 * {
 *   "mercure": {
 *     "publish":   ["topic1", "topic2"],   // URI ou * pour tout
 *     "subscribe": ["topic1", "topic2"]    // URI ou * pour tout
 *   }
 * }
 * </pre>
 *
 * <p>Implémenté à la main (Base64URL + HmacSHA256) pour éviter d'ajouter
 * une dépendance JWT supplémentaire — Gson est déjà au classpath et suffit
 * à sérialiser le payload.</p>
 */
public final class MercureJwt {

    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String HEADER_B64 =
            Base64.getUrlEncoder().withoutPadding().encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));

    private MercureJwt() {
    }

    /**
     * Construit un JWT autorisant la publication sur la liste de topics donnée.
     */
    public static String publisherToken(List<String> publishTopics, String secret) {
        return token(publishTopics, List.of(), secret);
    }

    /**
     * Construit un JWT autorisant la souscription à la liste de topics donnée.
     */
    public static String subscriberToken(List<String> subscribeTopics, String secret) {
        return token(List.of(), subscribeTopics, secret);
    }

    /**
     * Construit un JWT mixte publish + subscribe.
     */
    public static String token(List<String> publishTopics, List<String> subscribeTopics, String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Mercure JWT secret is empty");
        }
        String payloadJson = buildPayload(publishTopics, subscribeTopics);
        String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = HEADER_B64 + "." + payloadB64;
        String signature = sign(signingInput, secret);
        return signingInput + "." + signature;
    }

    private static String buildPayload(List<String> publishTopics, List<String> subscribeTopics) {
        StringBuilder sb = new StringBuilder(64);
        sb.append("{\"mercure\":{");
        sb.append("\"publish\":").append(arrayJson(publishTopics));
        sb.append(",\"subscribe\":").append(arrayJson(subscribeTopics));
        sb.append("}}");
        return sb.toString();
    }

    private static String arrayJson(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(values.size() * 32);
        sb.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(escapeJson(values.get(i))).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String escapeJson(String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private static String sign(String input, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign Mercure JWT", e);
        }
    }
}
