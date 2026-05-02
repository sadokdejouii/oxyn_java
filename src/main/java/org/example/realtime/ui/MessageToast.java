package org.example.realtime.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Toast de notification façon Messenger : carte ancrée en bas-droite de
 * l'écran, slide-in depuis la droite, auto-hide après {@value #VISIBLE_MS} ms,
 * cliquable pour ouvrir la conversation associée.
 *
 * <p>Style « système » : fenêtre {@link StageStyle#TRANSPARENT} sans
 * décoration. Plusieurs toasts peuvent s'empiler verticalement (le service
 * gestionnaire les espace).</p>
 */
public final class MessageToast {

    private static final long VISIBLE_MS = 5_000;
    private static final double WIDTH = 360;
    private static final double SLIDE_OFFSET = 24;

    private final Stage stage;
    private final StackPane root;
    private final Runnable onClick;
    private final Runnable onClosed;
    private Timeline hideTimer;
    private boolean closed;

    public MessageToast(Window owner,
                        String title,
                        String preview,
                        String initials,
                        Runnable onClick,
                        Runnable onClosed) {
        this.onClick = onClick;
        this.onClosed = onClosed;

        // ----- Avatar (cercle initiales) ---------------------------------
        StackPane avatar = new StackPane();
        avatar.setMinSize(44, 44);
        avatar.setPrefSize(44, 44);
        avatar.setMaxSize(44, 44);
        avatar.getStyleClass().add("toast-avatar");
        Label avatarLabel = new Label(initials == null || initials.isBlank() ? "?" : initials);
        avatarLabel.getStyleClass().add("toast-avatar-text");
        avatar.getChildren().add(avatarLabel);

        // ----- Texte ------------------------------------------------------
        Label titleLabel = new Label(title == null ? "Nouveau message" : title);
        titleLabel.getStyleClass().add("toast-title");
        Label previewLabel = new Label(preview == null || preview.isBlank() ? "Vous avez reçu un nouveau message" : preview);
        previewLabel.getStyleClass().add("toast-preview");
        previewLabel.setWrapText(true);
        previewLabel.setMaxWidth(WIDTH - 100);

        VBox text = new VBox(2, titleLabel, previewLabel);
        text.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);

        // ----- Bouton fermer ---------------------------------------------
        Label closeBtn = new Label("\u2715");
        closeBtn.getStyleClass().add("toast-close");
        closeBtn.setOnMouseClicked(e -> {
            e.consume();
            close();
        });

        // ----- Carte ------------------------------------------------------
        HBox card = new HBox(12, avatar, text, closeBtn);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.getStyleClass().add("toast-card");
        card.setMinWidth(WIDTH);
        card.setMaxWidth(WIDTH);

        card.setOnMouseClicked(this::handleCardClicked);

        root = new StackPane(card);
        root.getStyleClass().add("toast-root");
        root.setPadding(new Insets(0));

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        // Charge le CSS principal qui contient les classes toast-* (planning-notifications.css)
        try {
            String css = MessageToast.class.getResource("/css/planning-notifications.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ignored) {
            // Pas bloquant si manquant : la carte restera visible avec les styles inline.
        }

        stage = new Stage(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        if (owner != null && owner instanceof Stage) {
            stage.initOwner(owner);
        }
        stage.setScene(scene);
    }

    /**
     * Affiche le toast à la position {@code yOffsetFromBottom} (px depuis le bas).
     * Anime un slide-in depuis la droite et un fade-in.
     */
    public void show(double yOffsetFromBottom) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double targetX = bounds.getMaxX() - WIDTH - 24;
        double targetY = bounds.getMaxY() - yOffsetFromBottom - 88;
        stage.setX(targetX + SLIDE_OFFSET);
        stage.setY(targetY);
        stage.setOpacity(0);
        stage.show();

        // Slide-in horizontal : on anime stage.x manuellement (xProperty est read-only),
        // combiné à un fade-in sur opacityProperty (writable).
        Timeline slideIn = new Timeline();
        int frames = 12;
        for (int i = 0; i <= frames; i++) {
            double t = i / (double) frames;
            // ease-out cubique
            double eased = 1 - Math.pow(1 - t, 3);
            double x = targetX + SLIDE_OFFSET * (1 - eased);
            double opacity = eased;
            slideIn.getKeyFrames().add(new KeyFrame(
                    Duration.millis(220 * t),
                    ev -> { stage.setX(x); stage.setOpacity(opacity); }
            ));
        }
        slideIn.play();

        hideTimer = new Timeline(new KeyFrame(Duration.millis(VISIBLE_MS), e -> close()));
        hideTimer.play();
    }

    /** Ferme proprement le toast (fade-out + onClosed). */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (hideTimer != null) {
            hideTimer.stop();
        }
        // Fade-out manuel (stage.opacityProperty() est writable via setOpacity ; on évite
        // KeyValue car un Timeline classique ne s'applique qu'aux Property d'objets Node).
        Timeline fadeOut = new Timeline();
        int frames = 8;
        double startOpacity = stage.getOpacity();
        for (int i = 0; i <= frames; i++) {
            double t = i / (double) frames;
            double opacity = startOpacity * (1 - t);
            fadeOut.getKeyFrames().add(new KeyFrame(
                    Duration.millis(180 * t),
                    ev -> stage.setOpacity(opacity)
            ));
        }
        fadeOut.setOnFinished(ev -> {
            try { stage.hide(); } catch (Exception ignored) { }
            if (onClosed != null) onClosed.run();
        });
        fadeOut.play();
    }

    private void handleCardClicked(MouseEvent e) {
        if (e.getButton().name().equals("PRIMARY")) {
            try {
                if (onClick != null) onClick.run();
            } finally {
                close();
            }
        }
    }

    /** Hauteur estimée pour empiler les toasts. */
    public static double stackedHeight() {
        return 88; // hauteur carte + marge
    }

    /** Largeur fixe pour positionnement. */
    public static double width() {
        return WIDTH;
    }

    /** Permet à des classes utilitaires (ex. tests) de récupérer la racine. */
    public Region nodeForTests() {
        return root;
    }
}
