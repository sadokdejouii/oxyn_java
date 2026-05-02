package org.example.discussion.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * Barre de saisie style Messenger / Twitter :
 * <ul>
 *   <li>champ arrondi avec auto-grow visuel ;</li>
 *   <li>bouton emoji (popup {@link EmojiPicker}) ;</li>
 *   <li>bouton micro qui bascule vers une UI d'enregistrement avec
 *       compteur, vumètre et bouton « envoyer / annuler » ;</li>
 *   <li>bouton envoyer avec dégradé bleu et animation de pulsation ;</li>
 *   <li>bouton actualiser discret (préserve la logique existante).</li>
 * </ul>
 *
 * <p>Le contrôleur reste responsable du wiring DB / realtime — cette classe
 * n'a aucune dépendance vers les services métiers.</p>
 */
public final class MessageInput extends VBox {

    /** Mode courant de la barre : saisie texte vs enregistrement vocal. */
    public enum Mode { TEXT, RECORDING }

    private final HBox textBar = new HBox();
    private final HBox recordBar = new HBox();

    private final TextField textField = new TextField();
    private final Button emojiButton = new Button();
    private final Button micButton = new Button();
    private final Button sendButton = new Button();
    private final Button refreshButton = new Button();

    // Recording UI
    private final Button cancelRecordBtn = new Button();
    private final Button confirmRecordBtn = new Button();
    private final Label recordTimer = new Label("0:00");
    private final List<Rectangle> levelBars = new ArrayList<>();
    private Timeline blinkTimeline;

    private final EmojiPicker emojiPicker = new EmojiPicker(this::insertEmojiAtCaret);

    private Mode mode = Mode.TEXT;

    private Runnable onMicStart;
    private Runnable onMicCancel;
    private Runnable onMicConfirm;

    public MessageInput() {
        getStyleClass().add("msg-input-dock");
        setSpacing(0);

        buildTextBar();
        buildRecordBar();

        getChildren().add(textBar);
    }

    // ------------------------------------------------------------------ build
    private void buildTextBar() {
        textBar.setAlignment(Pos.CENTER_LEFT);
        textBar.setSpacing(10);
        textBar.setPadding(new Insets(14, 18, 14, 18));
        textBar.getStyleClass().add("msg-input-bar");

        textField.setPromptText("Écrire un message…");
        textField.getStyleClass().add("msg-input-field");
        HBox.setHgrow(textField, Priority.ALWAYS);

        emojiButton.setFocusTraversable(false);
        emojiButton.getStyleClass().add("msg-input-icon-btn");
        emojiButton.setGraphic(icon("far-smile", 16));
        emojiButton.setTooltip(new javafx.scene.control.Tooltip("Insérer un emoji"));
        emojiButton.setOnAction(e -> emojiPicker.showFor(emojiButton));

        micButton.setFocusTraversable(false);
        micButton.getStyleClass().add("msg-input-icon-btn");
        micButton.setGraphic(icon("fas-microphone", 15));
        micButton.setTooltip(new javafx.scene.control.Tooltip("Enregistrer un message vocal"));
        micButton.setOnAction(e -> {
            if (onMicStart != null) onMicStart.run();
        });

        sendButton.getStyleClass().add("msg-input-send");
        sendButton.setDefaultButton(true);
        sendButton.setGraphic(icon("fas-paper-plane", 14));
        sendButton.setText("");
        sendButton.setTooltip(new javafx.scene.control.Tooltip("Envoyer (Entrée)"));

        refreshButton.getStyleClass().add("msg-input-icon-btn--ghost");
        refreshButton.setGraphic(icon("fas-sync-alt", 13));
        refreshButton.setTooltip(new javafx.scene.control.Tooltip("Actualiser"));
        refreshButton.setFocusTraversable(false);

        textBar.getChildren().addAll(refreshButton, emojiButton, textField, micButton, sendButton);
    }

    private void buildRecordBar() {
        recordBar.setAlignment(Pos.CENTER_LEFT);
        recordBar.setSpacing(12);
        recordBar.setPadding(new Insets(14, 18, 14, 18));
        recordBar.getStyleClass().add("msg-input-record-bar");

        cancelRecordBtn.getStyleClass().add("msg-record-btn--cancel");
        cancelRecordBtn.setGraphic(icon("fas-trash-alt", 14));
        cancelRecordBtn.setTooltip(new javafx.scene.control.Tooltip("Annuler l'enregistrement"));
        cancelRecordBtn.setOnAction(e -> {
            if (onMicCancel != null) onMicCancel.run();
        });

        StackPane recDot = new StackPane();
        recDot.setMinSize(10, 10);
        recDot.setMaxSize(10, 10);
        recDot.getStyleClass().add("msg-record-dot");

        Label header = new Label("Enregistrement…");
        header.getStyleClass().add("msg-record-header");

        recordTimer.getStyleClass().add("msg-record-timer");

        HBox levelBox = new HBox(2);
        levelBox.setAlignment(Pos.CENTER_LEFT);
        levelBox.getStyleClass().add("msg-record-level");
        for (int i = 0; i < 28; i++) {
            Rectangle bar = new Rectangle(3, 8);
            bar.setArcWidth(2);
            bar.setArcHeight(2);
            bar.getStyleClass().add("msg-record-level-bar");
            levelBox.getChildren().add(bar);
            levelBars.add(bar);
        }
        HBox.setHgrow(levelBox, Priority.ALWAYS);

        confirmRecordBtn.getStyleClass().add("msg-record-btn--send");
        confirmRecordBtn.setGraphic(icon("fas-paper-plane", 14));
        confirmRecordBtn.setTooltip(new javafx.scene.control.Tooltip("Envoyer le message vocal"));
        confirmRecordBtn.setOnAction(e -> {
            if (onMicConfirm != null) onMicConfirm.run();
        });

        VBox metaCol = new VBox(2, header, recordTimer);
        metaCol.setAlignment(Pos.CENTER_LEFT);

        recordBar.getChildren().addAll(cancelRecordBtn, recDot, metaCol, levelBox, confirmRecordBtn);
        startBlinkAnimation(recDot);
        // attached / detached à la demande dans switchMode
    }

    // ----------------------------------------------------------------- public
    public TextField textField() { return textField; }
    public Button sendButton() { return sendButton; }
    public Button refreshButton() { return refreshButton; }
    public Button micButton() { return micButton; }
    public Button emojiButton() { return emojiButton; }

    public void setOnMicStart(Runnable r) { this.onMicStart = r; }
    public void setOnMicCancel(Runnable r) { this.onMicCancel = r; }
    public void setOnMicConfirm(Runnable r) { this.onMicConfirm = r; }

    public Mode mode() { return mode; }

    /** Bascule la barre en mode enregistrement (UI rouge avec timer). */
    public void enterRecordingMode() {
        if (mode == Mode.RECORDING) return;
        mode = Mode.RECORDING;
        recordTimer.setText("0:00");
        for (Rectangle r : levelBars) {
            r.setHeight(8);
        }
        getChildren().setAll(recordBar);
    }

    /** Revient à la barre texte. */
    public void exitRecordingMode() {
        if (mode == Mode.TEXT) return;
        mode = Mode.TEXT;
        getChildren().setAll(textBar);
        Platform.runLater(textField::requestFocus);
    }

    /**
     * Met à jour le vumètre + timer pendant la capture.
     *
     * @param level  0..1 niveau RMS courant
     * @param secs   durée écoulée en secondes
     */
    public void updateRecordingFeedback(double level, double secs) {
        long s = (long) Math.max(0d, Math.round(secs));
        recordTimer.setText(String.format("%d:%02d", s / 60, s % 60));
        // Anime les barres : pic centré + atténuation latérale
        int n = levelBars.size();
        for (int i = 0; i < n; i++) {
            double dist = Math.abs(i - n / 2.0) / (n / 2.0);
            double envelope = Math.max(0.15, 1.0 - dist * 0.85);
            double phase = (System.currentTimeMillis() / 90.0 + i) % 6.0;
            double wobble = 0.45 + 0.55 * Math.abs(Math.sin(phase));
            double h = 4 + 30 * Math.max(0.05, level) * envelope * wobble;
            levelBars.get(i).setHeight(h);
        }
    }

    private void insertEmojiAtCaret(String emoji) {
        if (emoji == null || emoji.isEmpty()) {
            return;
        }
        String current = textField.getText() == null ? "" : textField.getText();
        int caret = textField.getCaretPosition();
        if (caret < 0 || caret > current.length()) {
            caret = current.length();
        }
        String updated = current.substring(0, caret) + emoji + current.substring(caret);
        textField.setText(updated);
        textField.positionCaret(caret + emoji.length());
        textField.requestFocus();
    }

    private static FontIcon icon(String literal, int size) {
        FontIcon i = new FontIcon();
        i.setIconLiteral(literal);
        i.setIconSize(size);
        return i;
    }

    private void startBlinkAnimation(Region dot) {
        if (blinkTimeline != null) return;
        dot.setOpacity(1);
        blinkTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> dot.setOpacity(1)),
                new KeyFrame(Duration.millis(600), e -> dot.setOpacity(0.25)),
                new KeyFrame(Duration.millis(1200), e -> dot.setOpacity(1))
        );
        blinkTimeline.setCycleCount(Animation.INDEFINITE);
        blinkTimeline.play();
    }
}
