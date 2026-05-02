package org.example.digitalwill;

import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;

/**
 * Configuration Digital Will (comptes inactifs / succession).
 *
 * Pour TESTS en minutes :
 * - Env: OXYN_DIGITAL_WILL_INACTIVE_AFTER=PT2M
 * - JVM: -Doxyn.digitalWill.inactiveAfter=PT2M
 *
 * Format: ISO-8601 Duration (PT2M, PT1H, P30D...).
 */
public final class DigitalWillConfig {

    public enum Mode {
        ARCHIVE, TRANSFER, DELETE, LEGACY
    }

    public record Cfg(Duration inactiveAfter,
                      Mode mode,
                      Duration notifyBeforeDelete) {}

    private DigitalWillConfig() {}

    public static Cfg load() {
        Properties p = new Properties();

        try (InputStream is = DigitalWillConfig.class.getResourceAsStream("/digital-will.properties")) {
            if (is != null) p.load(is);
        } catch (Exception ignored) {
        }

        try {
            java.nio.file.Path file = java.nio.file.Path.of(System.getProperty("user.home"), ".oxyn", "digital-will.properties");
            if (java.nio.file.Files.exists(file)) {
                try (InputStream is = java.nio.file.Files.newInputStream(file)) {
                    Properties fp = new Properties();
                    fp.load(is);
                    p.putAll(fp);
                }
            }
        } catch (Exception ignored) {
        }

        Duration inactiveAfter = duration("oxyn.digitalWill.inactiveAfter", "OXYN_DIGITAL_WILL_INACTIVE_AFTER", p, Duration.ofDays(90));
        Duration notifyBeforeDelete = duration("oxyn.digitalWill.notifyBeforeDelete", "OXYN_DIGITAL_WILL_NOTIFY_BEFORE_DELETE", p, Duration.ofDays(7));

        Mode mode = mode("oxyn.digitalWill.mode", "OXYN_DIGITAL_WILL_MODE", p, Mode.ARCHIVE);
        return new Cfg(inactiveAfter, mode, notifyBeforeDelete);
    }

    private static Duration duration(String sysKey, String envKey, Properties fileProps, Duration def) {
        String v = System.getProperty(sysKey);
        if (v == null || v.isBlank()) v = System.getenv(envKey);
        if (v == null || v.isBlank()) v = Objects.toString(fileProps.getProperty(sysKey), "").trim();
        if (v == null || v.isBlank()) return def;
        try {
            return Duration.parse(v.trim());
        } catch (Exception e) {
            System.err.println("[DigitalWill] invalid duration " + sysKey + "=" + v + " (using default " + def + ")");
            return def;
        }
    }

    private static Mode mode(String sysKey, String envKey, Properties fileProps, Mode def) {
        String v = System.getProperty(sysKey);
        if (v == null || v.isBlank()) v = System.getenv(envKey);
        if (v == null || v.isBlank()) v = Objects.toString(fileProps.getProperty(sysKey), "").trim();
        if (v == null || v.isBlank()) return def;
        try {
            return Mode.valueOf(v.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Exception e) {
            System.err.println("[DigitalWill] invalid mode " + sysKey + "=" + v + " (using default " + def + ")");
            return def;
        }
    }
}

