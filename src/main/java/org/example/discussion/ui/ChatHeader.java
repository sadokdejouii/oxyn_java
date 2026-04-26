package org.example.discussion.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * En-tête conversation façon Messenger :
 * <ul>
 *   <li>avatar circulaire avec pastille présence ancrée bas-droit (halo
 *       pulsant en vert quand l'utilisateur est en ligne) ;</li>
 *   <li>nom + libellé statut (« En ligne » / « Hors ligne depuis X min ») ;</li>
 *   <li>bulle "typing" animée à 3 points qui s'affiche et disparaît
 *       automatiquement, lue par {@code PresenceService} et
 *       {@code TypingService} (les deux côtés client + encadrant).</li>
 * </ul>
 */
public final class ChatHeader extends HBox {

    private final Label initialsLabel;
    private final Label nameLabel;
    private final Label detailLabel;
    private final Label statusLabel;

    /** Pastille présence ancrée en bas-droite de l'avatar. */
    private final Circle presenceDot;
    /** Halo qui pulse quand l'utilisateur est en ligne. */
    private final Circle presenceHalo;
    private Timeline presencePulseTimeline;

    /** Bulle "typing" : « X est en train d'écrire… » + 3 points animés. */
    private final HBox typingBubble;
    private final Label typingText;
    private final Circle typingDot1;
    private final Circle typingDot2;
    private final Circle typingDot3;
    private Timeline typingDotsTimeline;

    public ChatHeader() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(14);
        setPadding(new Insets(0, 20, 0, 20));
        getStyleClass().add("msg-chat-header");

        // ----- Avatar (avec pastille présence ancrée + halo) ----------------
        StackPane avatar = new StackPane();
        avatar.setMinSize(48, 48);
        avatar.setMaxSize(48, 48);
        avatar.getStyleClass().add("msg-chat-header-avatar");
        initialsLabel = new Label("");
        initialsLabel.setAlignment(Pos.CENTER);
        initialsLabel.getStyleClass().add("msg-chat-header-avatar-text");
        avatar.getChildren().add(initialsLabel);

        presenceHalo = new Circle(11);
        presenceHalo.getStyleClass().add("msg-presence-halo");
        presenceHalo.setManaged(false);
        presenceHalo.setVisible(false);
        presenceHalo.setMouseTransparent(true);

        presenceDot = new Circle(7);
        presenceDot.getStyleClass().addAll("msg-presence-dot", "msg-presence-dot--offline");
        presenceDot.setManaged(false);
        presenceDot.setVisible(false);
        presenceDot.setMouseTransparent(true);

        // Empilement : avatar (carré 48x48) + halo + dot positionnés bas-droite
        StackPane avatarStack = new StackPane();
        avatarStack.setMinSize(54, 54);
        avatarStack.setPrefSize(54, 54);
        avatarStack.setMaxSize(54, 54);
        avatarStack.setAlignment(Pos.BOTTOM_RIGHT);
        avatarStack.getChildren().addAll(avatar, presenceHalo, presenceDot);
        StackPane.setAlignment(avatar, Pos.CENTER);
        // Décaler dot/halo vers l'extérieur bas-droite de l'avatar
        presenceHalo.setTranslateX(-2);
        presenceHalo.setTranslateY(-2);
        presenceDot.setTranslateX(-2);
        presenceDot.setTranslateY(-2);

        // ----- Bloc texte ---------------------------------------------------
        VBox text = new VBox(2);
        HBox.setHgrow(text, Priority.ALWAYS);
        nameLabel = new Label("");
        nameLabel.getStyleClass().add("msg-chat-header-name");
        nameLabel.setWrapText(true);
        detailLabel = new Label("");
        detailLabel.getStyleClass().add("msg-chat-header-detail");
        detailLabel.setWrapText(true);
        detailLabel.setManaged(false);
        detailLabel.setVisible(false);

        statusLabel = new Label("");
        statusLabel.getStyleClass().add("msg-chat-header-status");
        statusLabel.setWrapText(true);

        // ----- Bulle typing (3 points animés style Messenger) ---------------
        typingBubble = new HBox(8);
        typingBubble.setAlignment(Pos.CENTER_LEFT);
        typingBubble.getStyleClass().add("msg-typing-bubble");
        typingBubble.setManaged(false);
        typingBubble.setVisible(false);

        typingText = new Label("");
        typingText.getStyleClass().add("msg-typing-text");

        HBox dotsBox = new HBox(3);
        dotsBox.setAlignment(Pos.CENTER);
        dotsBox.getStyleClass().add("msg-typing-dots");
        typingDot1 = new Circle(2.6);
        typingDot1.getStyleClass().add("msg-typing-dot");
        typingDot2 = new Circle(2.6);
        typingDot2.getStyleClass().add("msg-typing-dot");
        typingDot3 = new Circle(2.6);
        typingDot3.getStyleClass().add("msg-typing-dot");
        dotsBox.getChildren().addAll(typingDot1, typingDot2, typingDot3);

        typingBubble.getChildren().addAll(dotsBox, typingText);

        text.getChildren().addAll(nameLabel, detailLabel, statusLabel, typingBubble);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(avatarStack, text, spacer);
    }

    public void update(String displayName, String detailLine, String statusLine, String initials) {
        nameLabel.setText(displayName == null ? "" : displayName);
        boolean hasDetail = detailLine != null && !detailLine.isBlank();
        detailLabel.setText(hasDetail ? detailLine : "");
        detailLabel.setManaged(hasDetail);
        detailLabel.setVisible(hasDetail);
        statusLabel.setText(statusLine == null ? "" : statusLine);
        statusLabel.getStyleClass().removeAll("msg-chat-header-status--online", "msg-chat-header-status--offline");
        if ("En ligne".equalsIgnoreCase(statusLine != null ? statusLine.trim() : "")) {
            statusLabel.getStyleClass().add("msg-chat-header-status--online");
        } else if (statusLine != null && !statusLine.isBlank()) {
            statusLabel.getStyleClass().add("msg-chat-header-status--offline");
        }
        initialsLabel.setText(initials == null || initials.isBlank() ? "?" : initials);
    }

    /**
     * Met à jour la pastille présence ancrée à l'avatar + le libellé associé.
     * Démarre / arrête le halo pulsant selon l'état.
     */
    public void updatePresence(boolean online, String label) {
        presenceDot.setManaged(true);
        presenceDot.setVisible(true);
        presenceDot.getStyleClass().removeAll("msg-presence-dot--online", "msg-presence-dot--offline");
        presenceDot.getStyleClass().add(online ? "msg-presence-dot--online" : "msg-presence-dot--offline");

        presenceHalo.setManaged(false); // toujours décoratif (pas dans la mise en page)
        presenceHalo.setVisible(online);

        if (online) {
            startPresencePulse();
        } else {
            stopPresencePulse();
        }

        if (label != null && !label.isBlank()) {
            statusLabel.setText(label);
            statusLabel.getStyleClass().removeAll("msg-chat-header-status--online", "msg-chat-header-status--offline");
            statusLabel.getStyleClass().add(online ? "msg-chat-header-status--online" : "msg-chat-header-status--offline");
        }
    }

    /**
     * Cache la pastille présence (ex. quand on n'a pas encore de conversation
     * ou qu'on n'a pas pu identifier le pair).
     */
    public void clearPresence() {
        presenceDot.setManaged(false);
        presenceDot.setVisible(false);
        presenceHalo.setVisible(false);
        stopPresencePulse();
    }

    /**
     * Affiche / cache la bulle "typing" façon Messenger.
     *
     * @param active true pour afficher la bulle et démarrer l'animation
     * @param label  texte à afficher (ex. « Sami est en train d'écrire… »).
     *               Si null/blank, on affiche uniquement les 3 points animés.
     */
    public void setTypingActive(boolean active, String label) {
        if (active) {
            String text = label != null ? label.trim() : "";
            typingText.setText(text);
            typingText.setManaged(!text.isEmpty());
            typingText.setVisible(!text.isEmpty());
            typingBubble.setManaged(true);
            typingBubble.setVisible(true);
            startTypingDotsAnimation();
        } else {
            typingBubble.setManaged(false);
            typingBubble.setVisible(false);
            stopTypingDotsAnimation();
        }
    }

    // ------------------------------------------------------------------
    // Animations
    // ------------------------------------------------------------------

    private void startPresencePulse() {
        if (presencePulseTimeline != null) {
            return;
        }
        presenceHalo.setOpacity(0.55);
        presenceHalo.setScaleX(1.0);
        presenceHalo.setScaleY(1.0);
        presencePulseTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(presenceHalo.opacityProperty(), 0.55, Interpolator.EASE_OUT),
                        new KeyValue(presenceHalo.scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(presenceHalo.scaleYProperty(), 1.0, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(1400),
                        new KeyValue(presenceHalo.opacityProperty(), 0.0, Interpolator.EASE_OUT),
                        new KeyValue(presenceHalo.scaleXProperty(), 1.85, Interpolator.EASE_OUT),
                        new KeyValue(presenceHalo.scaleYProperty(), 1.85, Interpolator.EASE_OUT))
        );
        presencePulseTimeline.setCycleCount(Timeline.INDEFINITE);
        presencePulseTimeline.play();
    }

    private void stopPresencePulse() {
        if (presencePulseTimeline != null) {
            presencePulseTimeline.stop();
            presencePulseTimeline = null;
        }
        presenceHalo.setOpacity(0);
        presenceHalo.setScaleX(1.0);
        presenceHalo.setScaleY(1.0);
    }

    private void startTypingDotsAnimation() {
        if (typingDotsTimeline != null) {
            return;
        }
        // 3 points qui font un "bounce" séquentiel — style Messenger
        typingDotsTimeline = new Timeline(
                kvOpacity(typingDot1, 0.30, 0),
                kvOpacity(typingDot2, 0.30, 0),
                kvOpacity(typingDot3, 0.30, 0),

                kvOpacity(typingDot1, 1.0, 200),
                kvTranslateY(typingDot1, -3, 200),

                kvOpacity(typingDot1, 0.30, 400),
                kvTranslateY(typingDot1, 0, 400),
                kvOpacity(typingDot2, 1.0, 400),
                kvTranslateY(typingDot2, -3, 400),

                kvOpacity(typingDot2, 0.30, 600),
                kvTranslateY(typingDot2, 0, 600),
                kvOpacity(typingDot3, 1.0, 600),
                kvTranslateY(typingDot3, -3, 600),

                kvOpacity(typingDot3, 0.30, 800),
                kvTranslateY(typingDot3, 0, 800)
        );
        typingDotsTimeline.setCycleCount(Timeline.INDEFINITE);
        typingDotsTimeline.play();
    }

    private void stopTypingDotsAnimation() {
        if (typingDotsTimeline != null) {
            typingDotsTimeline.stop();
            typingDotsTimeline = null;
        }
        typingDot1.setOpacity(1);
        typingDot2.setOpacity(1);
        typingDot3.setOpacity(1);
        typingDot1.setTranslateY(0);
        typingDot2.setTranslateY(0);
        typingDot3.setTranslateY(0);
    }

    private static KeyFrame kvOpacity(Circle dot, double opacity, double atMs) {
        return new KeyFrame(Duration.millis(atMs),
                new KeyValue(dot.opacityProperty(), opacity, Interpolator.EASE_BOTH));
    }

    private static KeyFrame kvTranslateY(Circle dot, double y, double atMs) {
        return new KeyFrame(Duration.millis(atMs),
                new KeyValue(dot.translateYProperty(), y, Interpolator.EASE_BOTH));
    }
}
