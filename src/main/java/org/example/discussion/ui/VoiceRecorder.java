package org.example.discussion.ui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Enregistreur de message vocal — backend purement {@code javax.sound.sampled}
 * (aucune dépendance externe). Capture en mémoire puis écrit un WAV final
 * dans {@link VoiceStore}.
 *
 * <p>Format : PCM mono 16 bits 16 kHz — bon compromis voix / taille.</p>
 *
 * <p>Pensée pour une UI de type Messenger : appelle {@code onLevel} (RMS
 * normalisé entre 0 et 1) ~30 fois par seconde pour piloter une animation
 * de waveform, et {@code onTick} avec la durée écoulée en secondes.</p>
 */
public final class VoiceRecorder {

    /** Limite haute pour éviter de produire des fichiers trop lourds. */
    public static final double MAX_DURATION_SECONDS = 60d;

    private static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            16_000f, 16, 1, 2, 16_000f, false);

    private final ReadOnlyDoubleWrapper levelProperty = new ReadOnlyDoubleWrapper(0d);
    private final ReadOnlyDoubleWrapper durationProperty = new ReadOnlyDoubleWrapper(0d);

    private TargetDataLine line;
    private Thread captureThread;
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private ByteArrayOutputStream buffer;
    private long startNanos;

    private Consumer<Double> onLevel;
    private Consumer<Double> onTick;
    private Consumer<Throwable> onError;

    public ReadOnlyDoubleProperty levelProperty() {
        return levelProperty.getReadOnlyProperty();
    }

    public ReadOnlyDoubleProperty durationProperty() {
        return durationProperty.getReadOnlyProperty();
    }

    public boolean isRecording() {
        return recording.get();
    }

    public void setOnLevel(Consumer<Double> handler) {
        this.onLevel = handler;
    }

    public void setOnTick(Consumer<Double> handler) {
        this.onTick = handler;
    }

    public void setOnError(Consumer<Throwable> handler) {
        this.onError = handler;
    }

    /**
     * Démarre la capture. Lève {@link IllegalStateException} si la ligne
     * audio est déjà active. {@link LineUnavailableException} est
     * encapsulée et redirigée vers {@code onError}.
     */
    public synchronized void start() {
        if (recording.get()) {
            throw new IllegalStateException("Capture déjà en cours");
        }
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        if (!AudioSystem.isLineSupported(info)) {
            fireError(new LineUnavailableException("Aucun micro disponible"));
            return;
        }
        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(FORMAT);
        } catch (LineUnavailableException e) {
            fireError(e);
            return;
        }
        buffer = new ByteArrayOutputStream();
        line.start();
        recording.set(true);
        startNanos = System.nanoTime();

        captureThread = new Thread(this::captureLoop, "voice-recorder");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    private void captureLoop() {
        byte[] tmp = new byte[1024];
        long lastTickMs = 0L;
        try {
            while (recording.get()) {
                int n = line.read(tmp, 0, tmp.length);
                if (n <= 0) {
                    break;
                }
                buffer.write(tmp, 0, n);

                // RMS normalisé pour piloter la waveform
                double rms = computeRms(tmp, n);
                fireLevel(rms);

                long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
                double elapsedSec = elapsedMs / 1000d;
                if (elapsedMs - lastTickMs > 80L) {
                    lastTickMs = elapsedMs;
                    fireTick(elapsedSec);
                }
                if (elapsedSec >= MAX_DURATION_SECONDS) {
                    Platform.runLater(this::stopAndDiscardOverflow);
                    return;
                }
            }
        } catch (RuntimeException e) {
            fireError(e);
        }
    }

    private void stopAndDiscardOverflow() {
        // Stoppe sans déclencher d'erreur — la limite haute est silencieuse.
        try {
            stop();
        } catch (Exception ignored) {
        }
    }

    /**
     * Arrête la capture et écrit le résultat dans un WAV.
     *
     * @return id du fichier vocal stocké, ou null si la capture n'a rien produit.
     */
    public synchronized RecordedVoice stop() {
        if (!recording.get()) {
            return null;
        }
        recording.set(false);
        try {
            if (captureThread != null) {
                captureThread.join(500);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        if (line != null) {
            try {
                line.stop();
                line.close();
            } catch (RuntimeException ignored) {
            }
            line = null;
        }
        double duration = (System.nanoTime() - startNanos) / 1_000_000_000d;
        byte[] data = buffer == null ? new byte[0] : buffer.toByteArray();
        buffer = null;
        if (data.length == 0 || duration < 0.5d) {
            return null;
        }
        String id = VoiceStore.newId();
        File out = VoiceStore.fileFor(id);
        try (AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(data), FORMAT, data.length / FORMAT.getFrameSize())) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out);
        } catch (IOException e) {
            fireError(e);
            return null;
        }
        return new RecordedVoice(id, duration);
    }

    /**
     * Annule la capture en cours sans écrire de fichier.
     */
    public synchronized void cancel() {
        if (!recording.get()) {
            return;
        }
        recording.set(false);
        try {
            if (captureThread != null) {
                captureThread.join(500);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        if (line != null) {
            try {
                line.stop();
                line.close();
            } catch (RuntimeException ignored) {
            }
            line = null;
        }
        buffer = null;
    }

    private static double computeRms(byte[] data, int len) {
        // PCM 16 bits little-endian signé
        long sum = 0L;
        int samples = len / 2;
        for (int i = 0; i + 1 < len; i += 2) {
            int low = data[i] & 0xff;
            int high = data[i + 1];
            int sample = (high << 8) | low;
            sum += (long) sample * sample;
        }
        if (samples == 0) {
            return 0d;
        }
        double rms = Math.sqrt(sum / (double) samples);
        return Math.min(1d, rms / 12_000d);
    }

    private void fireLevel(double level) {
        Platform.runLater(() -> {
            levelProperty.set(level);
            if (onLevel != null) {
                onLevel.accept(level);
            }
        });
    }

    private void fireTick(double seconds) {
        Platform.runLater(() -> {
            durationProperty.set(seconds);
            if (onTick != null) {
                onTick.accept(seconds);
            }
        });
    }

    private void fireError(Throwable t) {
        Platform.runLater(() -> {
            if (onError != null) {
                onError.accept(t);
            }
        });
    }

    /** Résultat d'un enregistrement réussi. */
    public record RecordedVoice(String voiceId, double durationSec) {

        public RecordedVoice {
            Objects.requireNonNull(voiceId);
        }
    }
}
