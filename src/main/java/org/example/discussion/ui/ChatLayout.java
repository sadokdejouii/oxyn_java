package org.example.discussion.ui;

import javafx.scene.layout.HBox;

/**
 * Conteneur horizontal principal : liste des conversations | zone chat (style Messenger).
 */
public final class ChatLayout extends HBox {

    public ChatLayout() {
        setSpacing(0);
        getStyleClass().add("msg-chat-layout");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }
}
