package org.example.discussion.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.entities.MessageRow;

/**
 * Une ligne de message type Messenger : bulle gauche (reçu) ou droite (envoyé).
 */
public final class ChatBubble {

    private ChatBubble() {
    }

    public static Node createRow(MessageRow m, boolean mine, String timeStr, String peerInitials,
                                 javafx.beans.value.ObservableDoubleValue threadWidth) {
        String text = m.contenu() == null ? "" : m.contenu();

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(Double.MAX_VALUE);
        bubble.getStyleClass().add(mine ? "msg-bubble msg-bubble--me" : "msg-bubble msg-bubble--them");
        bubble.setPadding(new Insets(10, 15, 10, 15));

        Label time = new Label(timeStr == null ? "" : timeStr);
        time.getStyleClass().add(mine ? "msg-bubble-time--me" : "msg-bubble-time--them");

        VBox bubbleStack = new VBox(4);
        bubbleStack.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubbleStack.setMaxWidth(Double.MAX_VALUE);
        bubbleStack.getChildren().addAll(bubble, time);
        bubbleStack.maxWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(120, threadWidth.get() * 0.6),
                threadWidth));

        if (mine) {
            Region grow = new Region();
            HBox.setHgrow(grow, Priority.ALWAYS);
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_RIGHT);
            row.setPadding(new Insets(8, 0, 8, 0));
            row.getStyleClass().add("msg-bubble-row msg-bubble-row--me");
            row.getChildren().addAll(grow, bubbleStack);
            return row;
        }

        StackPane av = new StackPane();
        av.setMinSize(36, 36);
        av.setMaxSize(36, 36);
        av.getStyleClass().add("msg-bubble-peer-avatar");
        Label ini = new Label(peerInitials == null || peerInitials.isBlank() ? "?" : peerInitials);
        ini.getStyleClass().add("msg-bubble-peer-avatar-text");
        av.getChildren().add(ini);

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 0, 8, 0));
        row.getStyleClass().add("msg-bubble-row msg-bubble-row--them");
        row.getChildren().addAll(av, bubbleStack);
        return row;
    }
}
