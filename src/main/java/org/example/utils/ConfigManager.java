package org.example.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gestionnaire de configuration externe pour l'application
 */
public class ConfigManager {
    private static ConfigManager instance;
    private Properties properties;

    private ConfigManager() {
        loadConfig();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private void loadConfig() {
        properties = new Properties();
        
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("Fichier config.properties non trouvé dans classpath");
                // Valeurs par défaut
                setDefaultValues();
                return;
            }
            
            properties.load(input);
            System.out.println("✅ Configuration chargée depuis config.properties");
            
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de config.properties: " + e.getMessage());
            setDefaultValues();
        }
    }

    private void setDefaultValues() {
        properties.setProperty("FLOUCI_ENV", "test");
        properties.setProperty("FLOUCI_TEST_URL", "https://developers.flouci.com/api");
        properties.setProperty("FLOUCI_TEST_PUBLIC_KEY", "TEST_KEY");
        properties.setProperty("FLOUCI_TEST_PRIVATE_KEY", "TEST_SECRET");
        properties.setProperty("FLOUCI_PROD_URL", "https://api.flouci.com/api");
        properties.setProperty("FLOUCI_PROD_PUBLIC_KEY", "PROD_KEY");
        properties.setProperty("FLOUCI_PROD_PRIVATE_KEY", "PROD_SECRET");
        properties.setProperty("FLOUCI_TIMEOUT", "30");
        properties.setProperty("FLOUCI_MAX_RETRIES", "3");
        properties.setProperty("GROQ_API_KEY", "");
        properties.setProperty("HF_TOKEN", "");
    }

    public String getGroqApiKey() {
        return properties.getProperty("GROQ_API_KEY", "");
    }

    public String getHfToken() {
        return properties.getProperty("HF_TOKEN", "");
    }

    public String getFlouciEnvironment() {
        return properties.getProperty("FLOUCI_ENV", "test");
    }

    public String getFlouciBaseUrl() {
        String env = getFlouciEnvironment();
        if ("prod".equals(env)) {
            return properties.getProperty("FLOUCI_PROD_URL");
        }
        return properties.getProperty("FLOUCI_TEST_URL");
    }

    public String getFlouciPublicKey() {
        String env = getFlouciEnvironment();
        if ("prod".equals(env)) {
            return properties.getProperty("FLOUCI_PROD_PUBLIC_KEY");
        }
        return properties.getProperty("FLOUCI_TEST_PUBLIC_KEY");
    }

    public String getFlouciPrivateKey() {
        String env = getFlouciEnvironment();
        if ("prod".equals(env)) {
            return properties.getProperty("FLOUCI_PROD_PRIVATE_KEY");
        }
        return properties.getProperty("FLOUCI_TEST_PRIVATE_KEY");
    }

    public int getFlouciTimeout() {
        return Integer.parseInt(properties.getProperty("FLOUCI_TIMEOUT", "30"));
    }

    public int getFlouciMaxRetries() {
        return Integer.parseInt(properties.getProperty("FLOUCI_MAX_RETRIES", "3"));
    }

    public boolean isProduction() {
        return "prod".equals(getFlouciEnvironment());
    }

    public boolean isTest() {
        return "test".equals(getFlouciEnvironment());
    }

    public void reload() {
        loadConfig();
    }
}
