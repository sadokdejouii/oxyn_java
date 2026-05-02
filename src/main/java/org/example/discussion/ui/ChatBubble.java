package org.example.discussion.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableDoubleValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.entities.MessageRow;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

/**
 * Ligne de message moderne (Messenger / Twitter) :
 * <ul>
 *   <li>avatar circulaire 32px côté reçu ;</li>
 *   <li>bulle adaptative texte (max 65% de la largeur) ou bulle vocale
 *       avec lecteur intégré ({@link VoicePlayer}) ;</li>
 *   <li>actions via bouton ⋮ et feuille flottante {@link MessageActionSheet} ;</li>
 *   <li>traduction inline sous la bulle (toggle) ;</li>
 *   <li>clic droit → même feuille d’actions.</li>
 * </ul>
 */
public final class ChatBubble {

    private ChatBubble() {
    }

    /** Description fonctionnelle d'une rangée de message à rendre. */
    public static final class Row {

        private final MessageRow message;
        private final boolean mine;
        private final String timeStr;
        private final String peerInitials;
        private final ObservableDoubleValue threadWidth;
        private final boolean canEditDelete;

        // callbacks (peuvent être null)
        private Runnable onEdit;
        private Runnable onDelete;
        private Consumer<TranslationToggle> onTranslate;

        public Row(MessageRow message,
                   boolean mine,
                   String timeStr,
                   String peerInitials,
                   ObservableDoubleValue threadWidth,
                   boolean canEditDelete) {
            this.message = message;
            this.mine = mine;
            this.timeStr = timeStr;
            this.peerInitials = peerInitials;
            this.threadWidth = threadWidth;
            this.canEditDelete = canEditDelete;
        }

        public Row onEdit(Runnable r) { this.onEdit = r; return this; }
        public Row onDelete(Runnable r) { this.onDelete = r; return this; }
        public Row onTranslate(Consumer<TranslationToggle> r) { this.onTranslate = r; return this; }
    }

    /**
     * Handle pour piloter la zone "Traduction" sous une bulle : on l'utilise
     * pour afficher d'abord un état « ⏳ Traduction… », puis le résultat.
     */
    public interface TranslationToggle {
        boolean isVisible();
        void showLoading();
        void showResult(String arabicText);
        void hide();
    }

    /**
     * Crée la ligne de message complète. Préférer cette méthode au lieu
     * d'instancier {@link Row} séparément, c'est l'unique point d'entrée.
     */
    public static Node createRow(Row r) {
        MessageContent.Parsed parsed = MessageContent.parse(r.message.contenu());
        Node body = parsed.isVoice()
                ? VoicePlayer.create(parsed.voiceId(), parsed.voiceDurationSec(), r.mine)
                : buildTextBubble(parsed.text(), r);

        VBox translationBox = new VBox(0);
        translationBox.setManaged(false);
        translationBox.setVisible(false);
        translationBox.getStyleClass().add(r.mine ? "msg-translation msg-translation--me" : "msg-translation msg-translation--them");
        translationBox.setMaxWidth(Region.USE_PREF_SIZE);
        translationBox.maxWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(140d, r.threadWidth.get() * 0.62),
                r.threadWidth));

        TranslationToggleImpl toggle = new TranslationToggleImpl(translationBox, r.mine);

        Node actionTrigger;
        if (parsed.isVoice()) {
            if (r.canEditDelete && r.onDelete != null) {
                actionTrigger = buildActionTrigger(r, toggle, false, false);
            } else {
                actionTrigger = null;
            }
        } else {
            actionTrigger = buildActionTrigger(r, toggle, r.canEditDelete, true);
        }

        return assemble(r, parsed, body, actionTrigger, translationBox, toggle);
    }

    private static Node buildTextBubble(String text, Row r) {
        Label label = new Label(text == null ? "" : text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.maxWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(160d, r.threadWidth.get() * 0.62),
                r.threadWidth));
        label.getStyleClass().add(r.mine ? "msg-bubble msg-bubble--me" : "msg-bubble msg-bubble--them");
        label.setPadding(new Insets(11, 16, 11, 16));
        return label;
    }

    private static Node assemble(Row r,
                                 MessageContent.Parsed parsed,
                                 Node body,
                                 Node actionTrigger,
                                 VBox translationBox,
                                 TranslationToggle toggle) {
        Node bubbleNode;
        if (actionTrigger != null) {
            HBox wrap = new HBox(8);
            wrap.setAlignment(r.mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            wrap.getStyleClass().add("msg-bubble-wrap");
            wrap.setMaxWidth(Region.USE_PREF_SIZE);
            wrap.getChildren().addAll(body, actionTrigger);
            bubbleNode = wrap;
        } else {
            bubbleNode = body;
        }

        if (!parsed.isVoice() && (r.onEdit != null || r.onDelete != null || r.onTranslate != null)) {
            attachContextMenu(bubbleNode, r, toggle, false);
        } else if (parsed.isVoice() && r.onDelete != null) {
            attachContextMenu(bubbleNode, r, toggle, true);
        }

        Label time = new Label(r.timeStr == null ? "" : r.timeStr);
        time.getStyleClass().add(r.mine ? "msg-bubble-time--me" : "msg-bubble-time--them");

        VBox bubbleStack = new VBox(4);
        bubbleStack.setAlignment(r.mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubbleStack.setMaxWidth(Region.USE_PREF_SIZE);
        bubbleStack.getChildren().addAll(bubbleNode, time, translationBox);

        if (r.mine) {
            Region grow = new Region();
            HBox.setHgrow(grow, Priority.ALWAYS);
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_RIGHT);
            row.setPadding(new Insets(4, 0, 4, 0));
            row.getStyleClass().add("msg-bubble-row msg-bubble-row--me");
            row.getChildren().addAll(grow, bubbleStack);
            return row;
        }

        StackPane av = new StackPane();
        av.setMinSize(32, 32);
        av.setMaxSize(32, 32);
        av.getStyleClass().add("msg-bubble-peer-avatar");
        Label ini = new Label(r.peerInitials == null || r.peerInitials.isBlank() ? "?" : r.peerInitials);
        ini.getStyleClass().add("msg-bubble-peer-avatar-text");
        av.getChildren().add(ini);

        VBox avCol = new VBox(av);
        avCol.setAlignment(Pos.TOP_LEFT);
        avCol.setPadding(new Insets(2, 0, 0, 0));

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));
        row.getStyleClass().add("msg-bubble-row msg-bubble-row--them");
        row.getChildren().addAll(avCol, bubbleStack);
        return row;
    }

    private static Button buildActionTrigger(Row r,
                                             TranslationToggle toggle,
                                             boolean allowEdit,
                                             boolean allowTranslate) {
        Button btn = new Button();
        btn.setFocusTraversable(false);
        btn.setMnemonicParsing(false);
        btn.getStyleClass().addAll(
                "msg-actions-trigger",
                r.mine ? "msg-actions-trigger--me" : "msg-actions-trigger--them");
        FontIcon icon = new FontIcon("fas-ellipsis-v");
        icon.setIconSize(14);
        btn.setGraphic(icon);

        boolean canTr = allowTranslate && r.onTranslate != null;
        boolean canEd = allowEdit && r.onEdit != null;
        boolean canDel = r.onDelete != null;
        boolean translateOnly = canTr && !canEd && !canDel;
        if (!r.mine && translateOnly) {
            // Ensure translation is explicitly reachable for received text messages.
            btn.getStyleClass().add("msg-actions-trigger--always-visible");
        }

        btn.setOnAction(e -> {
            e.consume();
            MessageActionSheet.showBelow(btn, r.mine, canTr, canEd, canDel,
                    toggle, r.onTranslate, r.onEdit, r.onDelete);
        });
        return btn;
    }

    private static void attachContextMenu(Node target, Row r, TranslationToggle toggle, boolean voice) {
        boolean canTr = !voice && r.onTranslate != null;
        boolean canEd = !voice && r.onEdit != null;
        boolean canDel = r.onDelete != null;

        target.setOnContextMenuRequested(e -> {
            e.consume();
            if (!canTr && !canEd && !canDel) {
                return;
            }
            if (target.getScene() == null) {
                return;
            }
            MessageActionSheet.showAtScreenCoords(e.getScreenX(), e.getScreenY(), target.getScene(),
                    canTr, canEd, canDel, toggle, r.onTranslate, r.onEdit, r.onDelete);
        });
    }

    private static final class TranslationToggleImpl implements TranslationToggle {

        private final VBox container;
        private final boolean mine;
        private Label header;
        private Label body;

        TranslationToggleImpl(VBox container, boolean mine) {
            this.container = container;
            this.mine = mine;
        }

        @Override
        public boolean isVisible() {
            return container.isVisible();
        }

        @Override
        public void showLoading() {
            ensureBuilt();
            header.setText("Traduction…");
            body.setText("");
            body.setManaged(false);
            body.setVisible(false);
            container.setManaged(true);
            container.setVisible(true);
        }

        @Override
        public void showResult(String arabicText) {
            ensureBuilt();
            header.setText("Arabe");
            body.setText(arabicText == null ? "" : arabicText);
            body.setManaged(true);
            body.setVisible(true);
            container.setManaged(true);
            container.setVisible(true);
        }

        @Override
        public void hide() {
            container.setManaged(false);
            container.setVisible(false);
            container.getChildren().clear();
        }

        private void ensureBuilt() {
            if (!container.getChildren().isEmpty()) return;
            container.setSpacing(2);
            container.setPadding(new Insets(8, 12, 10, 12));

            header = new Label("Traduction…");
            header.getStyleClass().add(mine ? "msg-translation-header--me" : "msg-translation-header--them");

            body = new Label("");
            body.setWrapText(true);
            body.getStyleClass().add(mine ? "msg-translation-body--me" : "msg-translation-body--them");
            body.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);

            container.getChildren().addAll(header, body);
        }
    }
}
