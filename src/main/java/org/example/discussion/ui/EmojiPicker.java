package org.example.discussion.ui;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Sélecteur d'emojis Unicode façon Messenger : popup catégorisée à attacher
 * à un bouton « 😀 ». Le caractère choisi est inséré via le {@link Consumer}
 * fourni au constructeur.
 *
 * <p>100 % JavaFX, aucun asset externe ; rendu correct sur les polices
 * système modernes (Segoe UI Emoji sur Windows, Apple Color Emoji sur macOS,
 * Noto Color Emoji sur Linux).</p>
 */
public final class EmojiPicker {

    private static final Map<String, String[]> CATEGORIES = new LinkedHashMap<>();

    static {
        CATEGORIES.put("Visages", new String[]{
                "😀", "😃", "😄", "😁", "😆", "🥹", "😅", "😂",
                "🤣", "😊", "🙂", "🙃", "😉", "😍", "🥰", "😘",
                "😎", "🤩", "😇", "🥳", "🤗", "🤔", "😴", "😌",
                "🙄", "😏", "😬", "😢", "😭", "😤", "🤯", "🥺"
        });
        CATEGORIES.put("Mains & Cœurs", new String[]{
                "👍", "👎", "👏", "🙌", "🙏", "✌️", "🤞", "🤟",
                "🤘", "👌", "✋", "💪", "🫶", "🤝", "🤙", "👋",
                "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
                "💔", "💖", "💝", "💯", "✨", "🔥", "⭐", "🌟"
        });
        CATEGORIES.put("Activité", new String[]{
                "🎉", "🎊", "🎁", "🎂", "🍰", "☕", "🍵", "🍔",
                "🍕", "🥗", "🥑", "🍎", "🍌", "🍇", "🍓", "🍉",
                "🏋️", "🚴", "🏃", "🏊", "🧘", "⚽", "🏀", "🎾"
        });
        CATEGORIES.put("Nature & Météo", new String[]{
                "🌞", "🌜", "⛅", "🌧️", "❄️", "🌈", "🌊", "🌸",
                "🌹", "🌻", "🌳", "🌲", "🍀", "🌍", "🐶", "🐱",
                "🦋", "🐝", "🐢", "🐬", "🦄", "🐧", "🦁", "🐼"
        });
        CATEGORIES.put("Symboles", new String[]{
                "✅", "✔️", "❌", "⚠️", "❗", "❓", "💬", "💡",
                "📌", "📎", "🔗", "🔒", "🔑", "📅", "🕐", "⏰",
                "🎯", "🚀", "🛡️", "💎", "🎵", "🎶", "📷", "📱"
        });
    }

    private final Popup popup = new Popup();
    private final Consumer<String> onPick;

    public EmojiPicker(Consumer<String> onPick) {
        this.onPick = onPick;
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        popup.getContent().add(buildContent());
    }

    /**
     * Affiche le picker juste au-dessus du bouton anchor.
     */
    public void showFor(Node anchor) {
        if (popup.isShowing()) {
            popup.hide();
            return;
        }
        Window owner = anchor.getScene() != null ? anchor.getScene().getWindow() : null;
        if (owner == null) {
            return;
        }
        Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
        if (b == null) {
            return;
        }
        popup.show(owner, b.getMinX() - 220, b.getMinY() - 320);
    }

    public void hide() {
        popup.hide();
    }

    private Node buildContent() {
        VBox card = new VBox(10);
        card.getStyleClass().add("msg-emoji-card");
        card.setPadding(new Insets(14));
        card.setMinWidth(360);
        card.setPrefWidth(360);

        Label title = new Label("Insérer un emoji");
        title.getStyleClass().add("msg-emoji-title");

        VBox grids = new VBox(14);
        for (Map.Entry<String, String[]> entry : CATEGORIES.entrySet()) {
            Label cat = new Label(entry.getKey());
            cat.getStyleClass().add("msg-emoji-cat");
            GridPane gp = buildGrid(entry.getValue());
            VBox section = new VBox(6, cat, gp);
            grids.getChildren().add(section);
        }

        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(grids);
        scroll.getStyleClass().add("msg-emoji-scroll");
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(280);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);

        card.getChildren().addAll(title, scroll);
        card.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) popup.hide();
        });
        return card;
    }

    private GridPane buildGrid(String[] emojis) {
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        int cols = 8;
        for (int i = 0; i < emojis.length; i++) {
            String e = emojis[i];
            Button b = new Button(e);
            b.getStyleClass().add("msg-emoji-btn");
            b.setFocusTraversable(false);
            b.setOnAction(ev -> {
                if (onPick != null) onPick.accept(e);
            });
            HBox.setHgrow(b, Priority.ALWAYS);
            grid.add(b, i % cols, i / cols);
        }
        return grid;
    }

}
