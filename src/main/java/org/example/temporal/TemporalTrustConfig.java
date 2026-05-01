package org.example.temporal;

import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;

/**
 * Configuration des durées "Temporal Trust".
 *
 * Surcharge facile pour tests via :
 * - variables d'environnement (ex: OXYN_TTL_COACH_GROUP=PT1M)
 * - propriétés JVM (ex: -Doxyn.ttl.coach.group=PT1M)
 * - fichier ~/.oxyn/temporal-trust.properties (optionnel)
 *
 * Format: ISO-8601 Duration (ex: PT24H, PT15M, P90D)
 */
public final class TemporalTrustConfig {

    public record Cfg(Duration coachGroupAccess,
                      Duration tempAdmin,
                      Duration notifyBeforeExpiry) {}

    private TemporalTrustConfig() {}

    public static Cfg load() {
        Properties p = new Properties();

        // (Optionnel) classpath default
        try (InputStream is = TemporalTrustConfig.class.getResourceAsStream("/temporal-trust.properties")) {
            if (is != null) {
                p.load(is);
            }
        } catch (Exception ignored) {
        }

        // (Optionnel) user home override: ~/.oxyn/temporal-trust.properties
        try {
            java.nio.file.Path file = java.nio.file.Path.of(System.getProperty("user.home"), ".oxyn", "temporal-trust.properties");
            if (java.nio.file.Files.exists(file)) {
                try (InputStream is = java.nio.file.Files.newInputStream(file)) {
                    Properties fp = new Properties();
                    fp.load(is);
                    p.putAll(fp);
                }
            }
        } catch (Exception ignored) {
        }
                            //a changerrr
        Duration coachGroup = duration("oxyn.ttl.coach.group", "OXYN_TTL_COACH_GROUP", p, Duration.ofDays(90));
        Duration tempAdmin  = duration("oxyn.ttl.admin.temp", "OXYN_TTL_ADMIN_TEMP", p, Duration.ofMinutes(1));
        Duration notifyBefore = duration("oxyn.ttl.notify.before", "OXYN_TTL_NOTIFY_BEFORE", p, Duration.ofDays(3));

        return new Cfg(coachGroup, tempAdmin, notifyBefore);
    }

    private static Duration duration(String sysKey, String envKey, Properties fileProps, Duration def) {
        String v = System.getProperty(sysKey);
        if (v == null || v.isBlank()) {
            v = System.getenv(envKey);
        }
        if (v == null || v.isBlank()) {
            v = Objects.toString(fileProps.getProperty(sysKey), "").trim();
        }
        if (v == null || v.isBlank()) {
            return def;
        }
        try {
            return Duration.parse(v.trim());
        } catch (Exception e) {
            System.err.println("[TemporalTrust] invalid duration " + sysKey + "=" + v + " (using default " + def + ")");
            return def;
        }
    }
}

