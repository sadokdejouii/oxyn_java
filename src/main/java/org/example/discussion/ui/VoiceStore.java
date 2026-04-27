package org.example.discussion.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Cache local des messages vocaux : {@code ~/.oxyn/voice/{uuid}.wav}.
 * Centralise la résolution d'un identifiant en fichier {@link File}.
 */
public final class VoiceStore {

    private static final String DIR_NAME = ".oxyn/voice";
    private static volatile Path cachedDir;

    private VoiceStore() {
    }

    /**
     * Renvoie (et crée si besoin) le dossier de stockage des fichiers WAV.
     */
    public static synchronized Path voiceDir() {
        if (cachedDir != null) {
            return cachedDir;
        }
        String home = System.getProperty("user.home", System.getProperty("java.io.tmpdir", "."));
        Path p = Paths.get(home, DIR_NAME);
        try {
            Files.createDirectories(p);
        } catch (IOException ignored) {
            // En dernier recours : tmpdir
            p = Paths.get(System.getProperty("java.io.tmpdir", "."), DIR_NAME);
            try {
                Files.createDirectories(p);
            } catch (IOException ignored2) {
                // tant pis : retourne le chemin même s'il n'existe pas
            }
        }
        cachedDir = p;
        return p;
    }

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static File fileFor(String voiceId) {
        return voiceDir().resolve(voiceId + ".wav").toFile();
    }

    public static boolean exists(String voiceId) {
        return voiceId != null && fileFor(voiceId).isFile();
    }

    public static void delete(String voiceId) {
        if (voiceId == null) return;
        File f = fileFor(voiceId);
        if (f.isFile()) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }
}
