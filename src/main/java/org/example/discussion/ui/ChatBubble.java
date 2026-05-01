package org.example.discussion.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.entities.MessageRow;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Ligne de message type Messenger : bulle, horodatage, menu ⋯ sur vos messages (modifier / supprimer).
 */
public final class ChatBubble {

    private ChatBubble() {
    }

    public static Node createRow(
            MessageRow m,
            boolean mine,
            String timeStr,
            String peerInitials,
            javafx.beans.value.ObservableDoubleValue threadWidth,
            boolean showActionMenu,
            Runnable onEdit,
            Runnable onDelete) {
        String text = m.contenu() == null ? "" : m.contenu();

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(Double.MAX_VALUE);
        bubble.maxWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(140, threadWidth.get() * 0.58),
                threadWidth));
        bubble.getStyleClass().add(mine ? "msg-bubble msg-bubble--me" : "msg-bubble msg-bubble--them");
        bubble.setPadding(new Insets(12, 16, 12, 16));

        Label time = new Label(timeStr == null ? "" : timeStr);
        time.getStyleClass().add(mine ? "msg-bubble-time--me" : "msg-bubble-time--them");

        Node bubbleBlock;
        if (mine && showActionMenu && onEdit != null && onDelete != null) {
            MenuButton more = buildMoreMenu(onEdit, onDelete);
            StackPane wrap = new StackPane();
            wrap.getStyleClass().add("msg-bubble-wrap");
            wrap.setMaxWidth(Region.USE_PREF_SIZE);
            wrap.getChildren().addAll(bubble, more);
            StackPane.setAlignment(more, Pos.TOP_RIGHT);
            more.setTranslateX(-4);
            more.setTranslateY(4);
            bubbleBlock = wrap;
        } else {
            bubbleBlock = bubble;
        }

        VBox bubbleStack = new VBox(6);
        bubbleStack.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubbleStack.setMaxWidth(Region.USE_PREF_SIZE);
        bubbleStack.getChildren().addAll(bubbleBlock, time);

        if (mine) {
            Region grow = new Region();
            HBox.setHgrow(grow, Priority.ALWAYS);
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_RIGHT);
            row.setPadding(new Insets(6, 0, 6, 0));
            row.getStyleClass().add("msg-bubble-row msg-bubble-row--me");
            row.getChildren().addAll(grow, bubbleStack);
            return row;
        }

        StackPane av = new StackPane();
        av.setMinSize(40, 40);
        av.setMaxSize(40, 40);
        av.getStyleClass().add("msg-bubble-peer-avatar");
        Label ini = new Label(peerInitials == null || peerInitials.isBlank() ? "?" : peerInitials);
        ini.getStyleClass().add("msg-bubble-peer-avatar-text");
        av.getChildren().add(ini);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 0, 6, 0));
        row.getStyleClass().add("msg-bubble-row msg-bubble-row--them");
        row.getChildren().addAll(av, bubbleStack);
        return row;
    }

    private static MenuButton buildMoreMenu(Runnable onEdit, Runnable onDelete) {
        MenuButton mb = new MenuButton();
        mb.setFocusTraversable(false);
        mb.setPopupSide(javafx.geometry.Side.BOTTOM);
        mb.getStyleClass().add("msg-bubble-more");
        FontIcon icon = new FontIcon("fas-ellipsis-h");
        icon.setIconSize(12);
        mb.setGraphic(icon);
        mb.setMnemonicParsing(false);

        MenuItem edit = new MenuItem("Modifier");
        edit.getStyleClass().add("msg-bubble-menu-item");
        edit.setOnAction(e -> {
            mb.hide();
            onEdit.run();
        });
        MenuItem del = new MenuItem("Supprimer…");
        del.getStyleClass().add("msg-bubble-menu-item--danger");
        del.setOnAction(e -> {
            mb.hide();
            onDelete.run();
        });
        mb.getItems().addAll(edit, del);
        return mb;
    }
}
