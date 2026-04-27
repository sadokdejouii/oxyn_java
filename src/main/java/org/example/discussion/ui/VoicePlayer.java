package org.example.discussion.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lecteur compact pour les bulles vocales : pastille play/pause + barre de
 * progression + waveform pseudo-animée + durée.
 *
 * <p>Adossé à {@link Clip} (JDK) — pas de dépendance externe.</p>
 */
public final class VoicePlayer {

    /** Construit le bloc UI à insérer à la place du label texte de la bulle. */
    public static Region create(String voiceId, double durationSec, boolean mine) {
        VoicePlayer player = new VoicePlayer();
        return player.build(voiceId, durationSec, mine);
    }

    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(0d);
    private Clip clip;
    private Timeline ticker;
    private Button toggle;
    private FontIcon icon;
    private Label timeLabel;
    private double durationSec;
    private final List<Rectangle> waveformBars = new ArrayList<>();

    private Region build(String voiceId, double duration, boolean mine) {
        this.durationSec = Math.max(1d, duration);

        toggle = new Button();
        toggle.setFocusTraversable(false);
        toggle.getStyleClass().addAll("msg-voice-play", mine ? "msg-voice-play--me" : "msg-voice-play--them");
        icon = new FontIcon("fas-play");
        icon.setIconSize(13);
        toggle.setGraphic(icon);
        toggle.setOnAction(e -> togglePlay(voiceId));

        HBox waveform = new HBox(2);
        waveform.setAlignment(Pos.CENTER_LEFT);
        waveform.getStyleClass().add("msg-voice-waveform");
        for (int i = 0; i < 22; i++) {
            Rectangle bar = new Rectangle(3, 6 + (i % 5) * 4);
            bar.setArcWidth(3);
            bar.setArcHeight(3);
            bar.getStyleClass().add(mine ? "msg-voice-bar msg-voice-bar--me" : "msg-voice-bar msg-voice-bar--them");
            waveform.getChildren().add(bar);
            waveformBars.add(bar);
        }
        HBox.setHgrow(waveform, Priority.ALWAYS);

        ProgressBar bar = new ProgressBar(0);
        bar.getStyleClass().add(mine ? "msg-voice-progress msg-voice-progress--me" : "msg-voice-progress msg-voice-progress--them");
        bar.progressProperty().bind(progress);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.prefWidthProperty().bind(waveform.widthProperty());

        StackPane visual = new StackPane(waveform, bar);
        visual.setAlignment(Pos.CENTER_LEFT);
        StackPane.setMargin(bar, new Insets(0, 0, 0, 0));
        bar.setOpacity(0); // visible seulement pendant la lecture pour l'effet
        progress.addListener((obs, o, n) -> bar.setOpacity(n.doubleValue() > 0 ? 0.55 : 0));

        timeLabel = new Label(formatTime(this.durationSec));
        timeLabel.getStyleClass().add(mine ? "msg-voice-time msg-voice-time--me" : "msg-voice-time msg-voice-time--them");

        HBox row = new HBox(10, toggle, visual, timeLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 4, 2, 0));
        row.getStyleClass().add("msg-voice-row");

        VBox shell = new VBox(row);
        shell.setMaxWidth(Region.USE_PREF_SIZE);
        if (!VoiceStore.exists(voiceId)) {
            Label fallback = new Label("Audio disponible localement uniquement.");
            fallback.getStyleClass().add(mine ? "msg-voice-fallback--me" : "msg-voice-fallback--them");
            fallback.setWrapText(true);
            fallback.maxWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.max(160d, row.getWidth()), row.widthProperty()));
            shell.getChildren().add(fallback);
            toggle.setDisable(true);
        }
        return shell;
    }

    private void togglePlay(String voiceId) {
        try {
            if (clip != null && clip.isRunning()) {
                pause();
                return;
            }
            if (clip == null) {
                File f = VoiceStore.fileFor(voiceId);
                if (!f.isFile()) {
                    return;
                }
                AudioInputStream ais = AudioSystem.getAudioInputStream(f);
                clip = AudioSystem.getClip();
                clip.open(ais);
                clip.addLineListener(this::onClipEvent);
            }
            clip.start();
            startTicker();
            icon.setIconLiteral("fas-pause");
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            // best effort — on désactive simplement le bouton
            toggle.setDisable(true);
        }
    }

    private void pause() {
        if (clip != null) {
            clip.stop();
        }
        stopTicker();
        icon.setIconLiteral("fas-play");
    }

    private void onClipEvent(LineEvent ev) {
        if (ev.getType() == LineEvent.Type.STOP) {
            Platform.runLater(() -> {
                stopTicker();
                if (clip != null && clip.getMicrosecondPosition() >= clip.getMicrosecondLength() - 50_000L) {
                    progress.set(0d);
                    if (clip != null) {
                        clip.setMicrosecondPosition(0L);
                    }
                    if (timeLabel != null) {
                        timeLabel.setText(formatTime(durationSec));
                    }
                }
                if (icon != null) {
                    icon.setIconLiteral("fas-play");
                }
            });
        }
    }

    private void startTicker() {
        stopTicker();
        ticker = new Timeline(new KeyFrame(Duration.millis(60), e -> updateProgress()));
        ticker.setCycleCount(Animation.INDEFINITE);
        ticker.play();
    }

    private void stopTicker() {
        if (ticker != null) {
            ticker.stop();
            ticker = null;
        }
    }

    private void updateProgress() {
        if (clip == null) {
            return;
        }
        double pos = clip.getMicrosecondPosition() / 1_000_000d;
        double total = Math.max(durationSec, clip.getMicrosecondLength() / 1_000_000d);
        progress.set(Math.min(1d, pos / Math.max(0.1d, total)));
        timeLabel.setText(formatTime(Math.max(0d, total - pos)));
        // Anime barres waveform proportionnellement à la position
        for (int i = 0; i < waveformBars.size(); i++) {
            double localPhase = (pos * 6d + i) % waveformBars.size();
            double scale = 0.6 + 0.4 * Math.abs(Math.sin(localPhase));
            waveformBars.get(i).setScaleY(scale);
        }
    }

    private static String formatTime(double seconds) {
        long s = (long) Math.max(0d, Math.round(seconds));
        long mm = s / 60;
        long ss = s % 60;
        return String.format("%d:%02d", mm, ss);
    }

    private VoicePlayer() {
    }
}
