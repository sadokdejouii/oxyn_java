package org.example.discussion.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * En-tête conversation : avatar, nom, détail (ex. e-mail), statut présence.
 */
public final class ChatHeader extends HBox {

    private final Label initialsLabel;
    private final Label nameLabel;
    private final Label detailLabel;
    private final Label statusLabel;

    public ChatHeader() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(14);
        setPadding(new Insets(0, 20, 0, 20));
        getStyleClass().add("msg-chat-header");

        StackPane avatar = new StackPane();
        avatar.setMinSize(48, 48);
        avatar.setMaxSize(48, 48);
        avatar.getStyleClass().add("msg-chat-header-avatar");
        initialsLabel = new Label("");
        initialsLabel.setAlignment(Pos.CENTER);
        initialsLabel.getStyleClass().add("msg-chat-header-avatar-text");
        avatar.getChildren().add(initialsLabel);

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
        text.getChildren().addAll(nameLabel, detailLabel, statusLabel);

        getChildren().addAll(avatar, text);
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
}
