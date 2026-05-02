package org.example.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Utility class for translating text using MyMemory Translation API
 * with automatic language detection and proper error handling.
 */
public class TranslationUtil {

    private static final String MYMEMORY_API_URL = "https://api.mymemory.translated.net/get";
    private static final int MAX_TEXT_LENGTH = 500;

    /**
     * Detects the language of the given text using character pattern analysis.
     * Returns ISO 639-1 language code (e.g., "en", "fr", "es", "de", "ar").
     */
    public static String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "en";
        }

        String normalized = text.toLowerCase();

        // Check for Arabic characters
        if (Pattern.compile("[\\u0600-\\u06FF]").matcher(normalized).find()) {
            return "ar";
        }

        // Check for Chinese characters
        if (Pattern.compile("[\\u4E00-\\u9FFF]").matcher(normalized).find()) {
            return "zh";
        }

        // Check for Cyrillic characters (Russian, etc.)
        if (Pattern.compile("[\\u0400-\\u04FF]").matcher(normalized).find()) {
            return "ru";
        }

        // Check for Greek characters
        if (Pattern.compile("[\\u0370-\\u03FF]").matcher(normalized).find()) {
            return "el";
        }

        // Check for Italian-specific patterns (before French to avoid false positives)
        if (normalized.contains("ciao") || normalized.contains("grazie") || normalized.contains("prego") ||
            normalized.contains("scusi") || normalized.contains("bene") || normalized.contains("buongiorno") ||
            normalized.contains("buonasera") || normalized.contains("arrivederci") || normalized.contains("perché") ||
            normalized.contains("questo") || normalized.contains("quello") || normalized.contains("questa") ||
            normalized.contains("quella") || normalized.contains("tutto") || normalized.contains("tutta") ||
            normalized.contains("niente") || normalized.contains("nulla") || normalized.contains("sì") ||
            normalized.contains("noi") || normalized.contains("voi") || normalized.contains("loro") ||
            normalized.contains("mio") || normalized.contains("tuo") || normalized.contains("suo") ||
            normalized.contains("nostro") || normalized.contains("vostro") || normalized.contains("loro")) {
            return "it";
        }

        // Check for French-specific characters and patterns
        if (normalized.contains("bonjour") || normalized.contains("merci") || normalized.contains("salut") ||
            normalized.contains("s'il") || normalized.contains("s'il vous plaît") || normalized.contains("au revoir") ||
            normalized.contains("é") || normalized.contains("è") || normalized.contains("ê") ||
            normalized.contains("à") || normalized.contains("â") || normalized.contains("û") ||
            normalized.contains("ô") || normalized.contains("ï") || normalized.contains("ü") ||
            normalized.contains("ç") || normalized.contains("œ") || normalized.contains("æ") ||
            normalized.contains("le ") || normalized.contains("la ") || normalized.contains("les ") ||
            normalized.contains("un ") || normalized.contains("une ") || normalized.contains("des ") ||
            normalized.contains("et ") || normalized.contains("ou ") || normalized.contains("est ") ||
            normalized.contains("dans ") || normalized.contains("pour ") || normalized.contains("avec ") ||
            normalized.contains("ceci") || normalized.contains("cela") || normalized.contains("tout") ||
            normalized.contains("rien") || normalized.contains("oui") || normalized.contains("non")) {
            return "fr";
        }

        // Check for Spanish-specific patterns
        if (normalized.contains("ñ") || normalized.contains("¿") || normalized.contains("¡") ||
            normalized.contains("hola") || normalized.contains("gracias") || normalized.contains("por favor") ||
            normalized.contains("el ") || normalized.contains("la ") || normalized.contains("los ") ||
            normalized.contains("las ") || normalized.contains("un ") || normalized.contains("una ") ||
            normalized.contains("y ") || normalized.contains("o ") || normalized.contains("es ") ||
            normalized.contains("en ") || normalized.contains("para ") || normalized.contains("con ") ||
            normalized.contains("esto") || normalized.contains("eso") || normalized.contains("todo") ||
            normalized.contains("nada") || normalized.contains("sí") || normalized.contains("no")) {
            return "es";
        }

        // Check for German-specific patterns
        if (normalized.contains("ä") || normalized.contains("ö") || normalized.contains("ü") ||
            normalized.contains("ß") || normalized.contains("hallo") || normalized.contains("danke") ||
            normalized.contains("bitte") || normalized.contains("der ") || normalized.contains("die ") ||
            normalized.contains("das ") || normalized.contains("ein ") || normalized.contains("eine ") ||
            normalized.contains("und ") || normalized.contains("oder ") || normalized.contains("ist ") ||
            normalized.contains("mit ") || normalized.contains("für ") || normalized.contains("von ") ||
            normalized.contains("dies") || normalized.contains("das") || normalized.contains("alles") ||
            normalized.contains("nichts") || normalized.contains("ja") || normalized.contains("nein")) {
            return "de";
        }

        // Check for Portuguese-specific patterns
        if (normalized.contains("ã") || normalized.contains("õ") || normalized.contains("ç") ||
            normalized.contains("o ") || normalized.contains("a ") || normalized.contains("os ") ||
            normalized.contains("as ") || normalized.contains("um ") || normalized.contains("uma ") ||
            normalized.contains("e ") || normalized.contains("ou ") || normalized.contains("é ") ||
            normalized.contains("em ") || normalized.contains("para ") || normalized.contains("com ")) {
            return "pt";
        }

        // Default to English
        return "en";
    }

    /**
     * Translates text to English using MyMemory API.
     * Automatically detects source language.
     * 
     * @param text The text to translate
     * @return Translated text, or original text if translation fails
     */
    public static String translateToEnglish(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        // Truncate text if too long
        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH);
        }

        try {
            String sourceLang = detectLanguage(text);
            
            // If already English, return as-is
            if ("en".equals(sourceLang)) {
                return text;
            }

            return translate(text, sourceLang, "en");
        } catch (Exception e) {
            System.err.println("Translation error: " + e.getMessage());
            return text; // Return original text on error
        }
    }

    /**
     * Translates text from source language to target language using MyMemory API.
     * 
     * @param text The text to translate
     * @param sourceLang Source language code (e.g., "en", "fr", "es")
     * @param targetLang Target language code (e.g., "en", "fr", "es")
     * @return Translated text, or original text if translation fails
     */
    public static String translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        if (sourceLang == null || sourceLang.isEmpty()) {
            sourceLang = "en";
        }

        if (targetLang == null || targetLang.isEmpty()) {
            targetLang = "en";
        }

        // If source and target are the same, return as-is
        if (sourceLang.equals(targetLang)) {
            return text;
        }

        try {
            // Truncate text if too long
            if (text.length() > MAX_TEXT_LENGTH) {
                text = text.substring(0, MAX_TEXT_LENGTH);
            }

            // Build API URL with proper langpair format (source|target)
            String langPair = sourceLang + "|" + targetLang;
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String apiUrl = MYMEMORY_API_URL + "?q=" + encodedText + "&langpair=" + langPair;

            // Make HTTP request
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Translation API returned status: " + responseCode);
                return text;
            }

            // Read response
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse JSON response to extract translated text
            String jsonResponse = response.toString();
            String translatedText = extractTranslatedText(jsonResponse);

            if (translatedText != null && !translatedText.isEmpty()) {
                return translatedText;
            } else {
                System.err.println("Could not extract translated text from response");
                return text;
            }

        } catch (Exception e) {
            System.err.println("Translation error: " + e.getMessage());
            e.printStackTrace();
            return text; // Return original text on error
        }
    }

    /**
     * Extracts the translated text from MyMemory API JSON response.
     * Simple JSON parsing without external dependencies.
     */
    private static String extractTranslatedText(String jsonResponse) {
        try {
            // Look for "translatedText":"..." pattern
            int startIndex = jsonResponse.indexOf("\"translatedText\":\"");
            if (startIndex == -1) {
                return null;
            }
            startIndex += "\"translatedText\":\"".length();
            
            int endIndex = startIndex;
            boolean escaped = false;
            while (endIndex < jsonResponse.length()) {
                char c = jsonResponse.charAt(endIndex);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    break;
                }
                endIndex++;
            }
            
            String translatedText = jsonResponse.substring(startIndex, endIndex);
            // Unescape common JSON characters
            translatedText = translatedText.replace("\\n", "\n")
                                           .replace("\\r", "\r")
                                           .replace("\\t", "\t")
                                           .replace("\\\"", "\"")
                                           .replace("\\\\", "\\");
            
            return translatedText;
        } catch (Exception e) {
            System.err.println("Error parsing translation response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks if the given text is likely in English.
     */
    public static boolean isEnglish(String text) {
        return "en".equals(detectLanguage(text));
    }

    /**
     * Gets the language name from ISO code.
     */
    public static String getLanguageName(String langCode) {
        if (langCode == null) return "Unknown";
        switch (langCode.toLowerCase()) {
            case "en": return "English";
            case "fr": return "French";
            case "es": return "Spanish";
            case "de": return "German";
            case "it": return "Italian";
            case "pt": return "Portuguese";
            case "ar": return "Arabic";
            case "zh": return "Chinese";
            case "ru": return "Russian";
            case "el": return "Greek";
            default: return "Unknown";
        }
    }
}
