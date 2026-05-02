package org.example.discussion.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Feuille d’actions flottante type Messenger / WhatsApp : carte blanche, ombre
 * douce, ouverte sous le bouton ⋮ (ou au point du clic droit), avec entrée
 * fade + scale. Fermeture par clic extérieur ou Échap.
 */
public final class MessageActionSheet {

    private static final double CARD_WIDTH = 160;
    private static final Duration ANIM_IN = Duration.millis(175);

    private MessageActionSheet() {
    }

    /**
     * Ouvre la feuille juste sous le bouton déclencheur (bord bas du bouton + 4 px).
     *
     * @param alignTrailingEdge si {@code true}, le bord droit de la carte est aligné
     *                          sur le bord droit du bouton (messages envoyés à droite).
     */
    public static void showBelow(Button anchor,
                                 boolean alignTrailingEdge,
                                 boolean allowTranslate,
                                 boolean allowEdit,
                                 boolean showDelete,
                                 ChatBubble.TranslationToggle toggle,
                                 Consumer<ChatBubble.TranslationToggle> onTranslate,
                                 Runnable onEdit,
                                 Runnable onDelete) {
        Objects.requireNonNull(anchor, "anchor");
        Scene scene = anchor.getScene();
        if (scene == null) {
            return;
        }
        Window owner = scene.getWindow();
        if (owner == null) {
            return;
        }
        anchor.applyCss();
        anchor.layout();
        Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
        if (b == null) {
            return;
        }
        double topY = b.getMaxY() + 4;
        double leftX = b.getMinX();
        double trailX = b.getMaxX();
        open(owner, leftX, topY, alignTrailingEdge, trailX,
                allowTranslate, allowEdit, showDelete, toggle, onTranslate, onEdit, onDelete);
    }

    /** Clic droit / menu contextuel : ouvre près du pointeur écran. */
    public static void showAtScreenCoords(double screenX,
                                          double screenY,
                                          Scene scene,
                                          boolean allowTranslate,
                                          boolean allowEdit,
                                          boolean showDelete,
                                          ChatBubble.TranslationToggle toggle,
                                          Consumer<ChatBubble.TranslationToggle> onTranslate,
                                          Runnable onEdit,
                                          Runnable onDelete) {
        if (scene == null) {
            return;
        }
        Window owner = scene.getWindow();
        if (owner == null) {
            return;
        }
        open(owner, screenX, screenY + 2, false, screenX,
                allowTranslate, allowEdit, showDelete, toggle, onTranslate, onEdit, onDelete);
    }

    private static void open(Window owner,
                             double anchorLeftX,
                             double anchorTopY,
                             boolean alignTrailingEdge,
                             double trailingRefX,
                             boolean allowTranslate,
                             boolean allowEdit,
                             boolean showDelete,
                             ChatBubble.TranslationToggle toggle,
                             Consumer<ChatBubble.TranslationToggle> onTranslate,
                             Runnable onEdit,
                             Runnable onDelete) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        VBox card = new VBox(2);
        card.getStyleClass().add("msg-actions-sheet");
        card.setMinWidth(CARD_WIDTH);
        card.setPrefWidth(CARD_WIDTH);
        card.setMaxWidth(CARD_WIDTH);
        URL css = MessageActionSheet.class.getResource("/css/planning-discussion-page.css");
        if (css != null) {
            card.getStylesheets().add(css.toExternalForm());
        }

        if (allowTranslate && onTranslate != null && toggle != null) {
            card.getChildren().add(row("fas-language", "Traduire", false, () -> {
                popup.hide();
                onTranslate.accept(toggle);
            }));
        }
        if (allowEdit && onEdit != null) {
            card.getChildren().add(row("fas-pen", "Modifier", false, () -> {
                popup.hide();
                onEdit.run();
            }));
        }
        if (showDelete && onDelete != null) {
            card.getChildren().add(row("fas-trash-alt", "Supprimer", true, () -> {
                popup.hide();
                onDelete.run();
            }));
        }

        if (card.getChildren().isEmpty()) {
            return;
        }

        popup.getContent().add(card);
        card.setOpacity(0);
        card.setScaleX(0.94);
        card.setScaleY(0.94);

        double showX = alignTrailingEdge ? (trailingRefX - CARD_WIDTH) : anchorLeftX;
        popup.show(owner, showX, anchorTopY);

        FadeTransition fade = new FadeTransition(ANIM_IN, card);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        ScaleTransition scale = new ScaleTransition(ANIM_IN, card);
        scale.setFromX(0.94);
        scale.setFromY(0.94);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, scale).play();
    }

    private static HBox row(String iconLiteral, String text, boolean danger, Runnable action) {
        FontIcon ic = new FontIcon(iconLiteral);
        ic.setIconSize(14);
        ic.getStyleClass().add(danger ? "msg-actions-row-icon--danger" : "msg-actions-row-icon");
        Label lbl = new Label(text);
        lbl.getStyleClass().add(danger ? "msg-actions-row-label--danger" : "msg-actions-row-label");
        lbl.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(lbl, Priority.ALWAYS);

        HBox row = new HBox(10, ic, lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.getStyleClass().add("msg-actions-row");
        if (danger) {
            row.getStyleClass().add("msg-actions-row--danger");
        }
        row.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                action.run();
            }
        });
        return row;
    }
}
