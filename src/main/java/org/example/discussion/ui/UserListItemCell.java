package org.example.discussion.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.entities.ConversationInboxItem;

import java.time.format.DateTimeFormatter;

/**
 * Cellule de liste conversations (avatar, nom, aperçu, heure, présence).
 */
public final class UserListItemCell extends ListCell<ConversationInboxItem> {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final HBox root = new HBox(12);
    private final StackPane avatar = new StackPane();
    private final Label avText = new Label();
    private final VBox textCol = new VBox(4);
    private final Label name = new Label();
    private final Label preview = new Label();
    private final HBox metaRow = new HBox(8);
    private final Label time = new Label();
    private final Label presence = new Label();

    public UserListItemCell() {
        root.setAlignment(Pos.CENTER_LEFT);
        root.getStyleClass().add("msg-user-item");
        avatar.setMinSize(44, 44);
        avatar.setMaxSize(44, 44);
        avatar.getStyleClass().add("msg-user-item-avatar");
        avText.getStyleClass().add("msg-user-item-avatar-text");
        avatar.getChildren().add(avText);
        name.getStyleClass().add("msg-user-item-name");
        preview.getStyleClass().add("msg-user-item-preview");
        preview.setWrapText(true);
        preview.setTextOverrun(OverrunStyle.ELLIPSIS);
        preview.setMaxWidth(200);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setMaxWidth(200);
        time.getStyleClass().add("msg-user-item-time");
        presence.getStyleClass().add("msg-user-item-presence");
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.getChildren().addAll(time, presence);
        textCol.getChildren().addAll(name, preview, metaRow);
        HBox.setHgrow(textCol, Priority.ALWAYS);
        root.getChildren().addAll(avatar, textCol);
    }

    @Override
    protected void updateItem(ConversationInboxItem item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            return;
        }
        avText.setText(initials(item.clientName()));
        name.setText(item.clientName());
        preview.setText(item.lastMessagePreview() != null ? item.lastMessagePreview() : "");
        time.setText(item.lastMessageAt() != null ? TIME.format(item.lastMessageAt()) : "");
        presence.setText(item.presenceLabel());
        presence.getStyleClass().setAll(
                "msg-user-item-presence",
                "En ligne".equals(item.presenceLabel()) ? "msg-user-item-presence--on" : "msg-user-item-presence--off");
        setGraphic(root);
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) {
            return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase();
        }
        return (p[0].substring(0, 1) + p[p.length - 1].substring(0, 1)).toUpperCase();
    }
}
