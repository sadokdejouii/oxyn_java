package org.example.discussion.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Barre de saisie fixe en bas : champ arrondi, pièce jointe (décoratif), envoi, actualiser.
 */
public final class MessageInput extends HBox {

    private final TextField textField;
    private final Button sendButton;
    private final Button refreshButton;

    public MessageInput() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setPadding(new Insets(14, 20, 14, 20));
        getStyleClass().add("msg-input-bar");

        textField = new TextField();
        textField.setPromptText("Écrire un message…");
        textField.getStyleClass().add("msg-input-field");
        HBox.setHgrow(textField, Priority.ALWAYS);

        Button attach = new Button();
        attach.setFocusTraversable(false);
        attach.getStyleClass().add("msg-input-icon-btn");
        FontIcon clip = new FontIcon();
        clip.setIconLiteral("fas-paperclip");
        clip.setIconSize(16);
        attach.setGraphic(clip);
        attach.setOnAction(e -> { /* décoratif */ });

        sendButton = new Button("Envoyer");
        sendButton.getStyleClass().add("msg-input-send");
        sendButton.setDefaultButton(true);

        refreshButton = new Button("Actualiser");
        refreshButton.getStyleClass().add("msg-input-refresh");

        getChildren().addAll(textField, attach, sendButton, refreshButton);
    }

    public TextField textField() {
        return textField;
    }

    public Button sendButton() {
        return sendButton;
    }

    public Button refreshButton() {
        return refreshButton;
    }
}
