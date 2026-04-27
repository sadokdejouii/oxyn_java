package org.example.notifications;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class SendGridConfig {

    public record Cfg(String apiKey, String fromEmail, boolean debug) {
    }

    private static final String BOM = "\uFEFF";
    // UTF-8 BOM when Properties.load() decodes as ISO-8859-1 (ï»¿)
    private static final String UTF8_BOM_MOJIBAKE = "\u00EF\u00BB\u00BF";

    private SendGridConfig() {
    }

    public static Cfg load() {
        // 1) env
        String apiKey = trim(System.getenv("SENDGRID_API_KEY"));
        String from = trim(System.getenv("SENDGRID_FROM_EMAIL"));
        boolean debug = "1".equals(trim(System.getenv("SENDGRID_DEBUG")));

        // 2) JVM system properties (mvn -Dsendgrid.apiKey=... -Dsendgrid.from=...)
        if (apiKey == null) apiKey = trim(System.getProperty("sendgrid.apiKey"));
        if (from == null) from = trim(System.getProperty("sendgrid.from"));
        if (!debug) debug = "1".equals(trim(System.getProperty("sendgrid.debug")));

        // 3) user home file: ~/.oxyn/sendgrid.properties
        Path propsPath = userHomePropsPath();
        Properties p = (apiKey == null || from == null || debug) ? readProps(propsPath) : new Properties();
        if (apiKey == null) apiKey = trim(getProp(p, "sendgrid.apiKey"));
        if (from == null) from = trim(getProp(p, "sendgrid.from"));
        if (!debug) debug = "1".equals(trim(getProp(p, "sendgrid.debug")));

        if (debug) {
            String src = (trim(System.getenv("SENDGRID_API_KEY")) != null || trim(System.getenv("SENDGRID_FROM_EMAIL")) != null)
                    ? "env"
                    : (trim(System.getProperty("sendgrid.apiKey")) != null || trim(System.getProperty("sendgrid.from")) != null)
                    ? "system-properties"
                    : "file:" + propsPath;
            System.out.println("[SendGrid] config source=" + src
                    + " apiKey=" + (apiKey != null ? "set" : "missing")
                    + " from=" + (from != null ? from : "missing"));
        }

        return new Cfg(apiKey, from, debug);
    }

    public static Path userHomePropsPath() {
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            return Paths.get("sendgrid.properties");
        }
        return Paths.get(home, ".oxyn", "sendgrid.properties");
    }

    private static Properties readProps(Path path) {
        Properties p = new Properties();
        if (path == null || !Files.exists(path)) {
            return p;
        }
        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
        } catch (IOException ignored) {
        }
        return p;
    }

    private static String getProp(Properties p, String key) {
        if (p == null) return null;
        String v = p.getProperty(key);
        if (v != null) return v;
        // Cas courant: fichier UTF-8 avec BOM → la 1ère clé devient "\uFEFFsendgrid.apiKey"
        v = p.getProperty(BOM + key);
        if (v != null) return v;
        // Cas fréquent sous Windows: BOM UTF-8 lu en ISO-8859-1 → "ï»¿sendgrid.apiKey"
        return p.getProperty(UTF8_BOM_MOJIBAKE + key);
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

