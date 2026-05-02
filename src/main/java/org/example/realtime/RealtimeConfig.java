package org.example.realtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton de configuration du module temps réel.
 *
 * <p>Trois sources, par ordre de priorité :</p>
 * <ol>
 *   <li>variable d'environnement (ex. {@code MERCURE_URL}) ;</li>
 *   <li>propriété {@code realtime.properties} embarquée dans le classpath ;</li>
 *   <li>valeur par défaut codée en dur (dev local).</li>
 * </ol>
 *
 * <p>Les défauts sont alignés avec {@code docker/docker-compose.mercure.yml} pour
 * que l'application fonctionne immédiatement après {@code docker compose up}.</p>
 */
public final class RealtimeConfig {

    private static final RealtimeConfig INSTANCE = new RealtimeConfig();

    private static final String PROPERTIES_PATH = "/realtime.properties";

    private final String publishUrl;
    private final String subscribeUrl;
    private final String jwtSecret;
    private final boolean enabled;
    private final int reconnectInitialDelaySeconds;
    private final int reconnectMaxDelaySeconds;
    private final int httpConnectTimeoutSeconds;
    private final int fallbackPollSeconds;

    private RealtimeConfig() {
        Properties props = loadProperties();
        this.publishUrl = resolve("MERCURE_URL", props, "realtime.mercure.publishUrl",
                "http://localhost:3000/.well-known/mercure");
        this.subscribeUrl = resolve("MERCURE_PUBLIC_URL", props, "realtime.mercure.subscribeUrl",
                this.publishUrl);
        this.jwtSecret = resolve("MERCURE_JWT_SECRET", props, "realtime.mercure.jwtSecret",
                "oxyn-dev-secret-change-me-please-use-32+chars");
        this.enabled = Boolean.parseBoolean(resolve("OXYN_REALTIME_ENABLED", props,
                "realtime.enabled", "true"));
        this.reconnectInitialDelaySeconds = parseInt(resolve("OXYN_REALTIME_RECONNECT_INITIAL", props,
                "realtime.reconnect.initialDelaySeconds", "2"), 2);
        this.reconnectMaxDelaySeconds = parseInt(resolve("OXYN_REALTIME_RECONNECT_MAX", props,
                "realtime.reconnect.maxDelaySeconds", "30"), 30);
        this.httpConnectTimeoutSeconds = parseInt(resolve("OXYN_REALTIME_HTTP_TIMEOUT", props,
                "realtime.http.connectTimeoutSeconds", "5"), 5);
        this.fallbackPollSeconds = parseInt(resolve("OXYN_REALTIME_FALLBACK_POLL", props,
                "realtime.fallback.pollSeconds", "5"), 5);
    }

    public static RealtimeConfig getInstance() {
        return INSTANCE;
    }

    public String publishUrl() {
        return publishUrl;
    }

    public String subscribeUrl() {
        return subscribeUrl;
    }

    public String jwtSecret() {
        return jwtSecret;
    }

    public boolean enabled() {
        return enabled;
    }

    public int reconnectInitialDelaySeconds() {
        return reconnectInitialDelaySeconds;
    }

    public int reconnectMaxDelaySeconds() {
        return reconnectMaxDelaySeconds;
    }

    public int httpConnectTimeoutSeconds() {
        return httpConnectTimeoutSeconds;
    }

    public int fallbackPollSeconds() {
        return fallbackPollSeconds;
    }

    // ---------------------------------------------------------------------

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = RealtimeConfig.class.getResourceAsStream(PROPERTIES_PATH)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // Fichier optionnel — on s'appuie sur les défauts.
        }
        return props;
    }

    private static String resolve(String envKey, Properties props, String propKey, String defaultValue) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String prop = props.getProperty(propKey);
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }
        return defaultValue;
    }

    private static int parseInt(String raw, int defaultValue) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
