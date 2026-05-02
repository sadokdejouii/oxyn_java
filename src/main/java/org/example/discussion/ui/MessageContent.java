package org.example.discussion.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encodage / décodage du champ {@code messages.contenu} pour gérer des
 * messages enrichis (voix, traduction) sans modifier le schéma de base.
 *
 * <p>Markers supportés (texte plat dans la colonne {@code contenu}) :</p>
 * <ul>
 *   <li>Voix : {@code [[VOICE:{uuid}:{durationSeconds}]]} — fichier audio
 *       stocké localement dans {@link VoiceStore#voiceDir()}.</li>
 * </ul>
 *
 * <p>Tout le reste est traité comme un message texte standard.</p>
 */
public final class MessageContent {

    /** Préfixe stable d'un message vocal. */
    public static final String VOICE_PREFIX = "[[VOICE:";

    private static final Pattern VOICE_PATTERN =
            Pattern.compile("^\\s*\\[\\[VOICE:([A-Za-z0-9_\\-]+):(\\d+(?:\\.\\d+)?)]]\\s*$");

    public enum Kind { TEXT, VOICE }

    public record Parsed(Kind kind, String text, String voiceId, double voiceDurationSec) {

        public static Parsed text(String body) {
            return new Parsed(Kind.TEXT, body == null ? "" : body, null, 0d);
        }

        public static Parsed voice(String id, double duration) {
            return new Parsed(Kind.VOICE, "", id, Math.max(0d, duration));
        }

        public boolean isVoice() {
            return kind == Kind.VOICE;
        }
    }

    private MessageContent() {
    }

    /**
     * Parse un contenu de message en provenance de la DB.
     */
    public static Parsed parse(String raw) {
        if (raw == null) {
            return Parsed.text("");
        }
        Matcher m = VOICE_PATTERN.matcher(raw);
        if (m.matches()) {
            try {
                double dur = Double.parseDouble(m.group(2));
                return Parsed.voice(m.group(1), dur);
            } catch (NumberFormatException ignored) {
                // marker mal formé : on retombe sur le texte brut
            }
        }
        return Parsed.text(raw);
    }

    /**
     * Encode un message vocal pour persistance dans {@code messages.contenu}.
     */
    public static String encodeVoice(String voiceId, double durationSec) {
        if (voiceId == null || voiceId.isBlank()) {
            throw new IllegalArgumentException("voiceId requis");
        }
        long durRounded = Math.max(1L, Math.round(durationSec));
        return VOICE_PREFIX + voiceId + ":" + durRounded + "]]";
    }

    /**
     * Aperçu court pour la liste de conversations (utilisé pour la sidebar).
     */
    public static String preview(String raw) {
        Parsed p = parse(raw);
        if (p.isVoice()) {
            return "🎙️ Message vocal";
        }
        String t = p.text();
        if (t == null) {
            return "";
        }
        return t.length() > 120 ? t.substring(0, 117) + "…" : t;
    }
}
