package org.example.config;

import java.util.Properties;

/**
 * Configuration sécurisée pour YouTube API
 * Protège la clé API et gère les variables d'environnement
 */
public class YouTubeConfig {
    
    private static final String ENV_API_KEY = "YOUTUBE_API_KEY";
    private static final String CONFIG_FILE = "/youtube-config.properties";
    
    private static String apiKey;
    private static boolean initialized = false;
    
    /**
     * Initialise la configuration avec priorité :
     * 1. Variable d'environnement
     * 2. Fichier properties
     * 3. Valeur par défaut (développement uniquement)
     */
    private static void initialize() {
        if (initialized) return;
        
        // 1. Variable d'environnement (priorité haute)
        String envKey = System.getenv(ENV_API_KEY);
        if (envKey != null && !envKey.trim().isEmpty()) {
            apiKey = envKey.trim();
            System.out.println("✅ Clé API chargée depuis variable d'environnement");
        } 
        // 2. Fichier properties
        else {
            try {
                Properties props = new Properties();
                props.load(YouTubeConfig.class.getResourceAsStream(CONFIG_FILE));
                apiKey = props.getProperty("youtube.api.key");
                if (apiKey != null && !apiKey.trim().isEmpty()) {
                    System.out.println("✅ Clé API chargée depuis fichier properties");
                }
            } catch (Exception e) {
                System.out.println("⚠️ Fichier de configuration non trouvé");
            }
        }
        
        // 3. Valeur par défaut (développement uniquement)
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = "AIzaSyDUYjvXGH5nNQyO7nQnCjXQxQxQxQxQxQx"; // Remplacer en production
            System.out.println("⚠️ Clé API par défaut utilisée (développement uniquement)");
        }
        
        initialized = true;
    }
    
    /**
     * Récupère la clé API YouTube de manière sécurisée
     * @return clé API ou null si non configurée
     */
    public static String getApiKey() {
        initialize();
        return apiKey;
    }
    
    /**
     * Vérifie si la configuration est valide
     * @return true si clé API configurée
     */
    public static boolean isConfigured() {
        initialize();
        return apiKey != null && !apiKey.trim().isEmpty() && 
               !apiKey.equals("AIzaSyDUYjvXGH5nNQyO7nQnCjXQxQxQxQxQxQx");
    }
    
    /**
     * Affiche le statut de configuration
     */
    public static void printStatus() {
        initialize();
        System.out.println("🔑 Configuration YouTube API :");
        System.out.println("   Variable d'environnement: " + 
            (System.getenv(ENV_API_KEY) != null ? "✅" : "❌"));
        System.out.println("   Fichier properties: " + 
            (YouTubeConfig.class.getResource(CONFIG_FILE) != null ? "✅" : "❌"));
        System.out.println("   Clé configurée: " + (isConfigured() ? "✅" : "❌"));
    }
}
