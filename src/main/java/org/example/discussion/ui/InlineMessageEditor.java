package org.example.discussion.ui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.entities.MessageRow;

import java.util.function.Consumer;

/**
 * Édition de message inline (même emplacement qu’une bulle « envoyée »), sans fenêtre modale.
 */
public final class InlineMessageEditor {

    private InlineMessageEditor() {
    }

    public static Node createMineRow(
            MessageRow m,
            javafx.beans.value.ObservableDoubleValue threadWidth,
            Consumer<String> onSave,
            Runnable onCancel) {
        String initial = m.contenu() == null ? "" : m.contenu();

        TextArea ta = new TextArea(initial);
        ta.setWrapText(true);
        ta.setPrefRowCount(3);
        ta.setMinHeight(72);
        ta.getStyleClass().add("msg-inline-edit-field");
        ta.maxWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(220, threadWidth.get() * 0.58),
                threadWidth));

        Label hint = new Label("Modifications visibles par tous les participants.");
        hint.getStyleClass().add("msg-inline-edit-hint");
        hint.setWrapText(true);
        hint.maxWidthProperty().bind(ta.maxWidthProperty());

        Button cancel = new Button("Annuler");
        cancel.getStyleClass().addAll("msg-inline-edit-btn", "msg-inline-edit-btn--ghost");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> onCancel.run());

        Button save = new Button("Enregistrer");
        save.getStyleClass().addAll("msg-inline-edit-btn", "msg-inline-edit-btn--primary");
        save.setDefaultButton(true);
        save.setOnAction(e -> {
            String t = ta.getText() == null ? "" : ta.getText().trim();
            if (t.isEmpty()) {
                return;
            }
            onSave.accept(t);
        });

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        actions.getChildren().addAll(grow, cancel, save);

        VBox card = new VBox(10, hint, ta, actions);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("msg-inline-edit-card");
        card.setMaxWidth(Region.USE_PREF_SIZE);

        VBox stack = new VBox(6);
        stack.setAlignment(Pos.CENTER_RIGHT);
        stack.setMaxWidth(Region.USE_PREF_SIZE);
        stack.getChildren().add(card);

        Region leftGrow = new Region();
        HBox.setHgrow(leftGrow, Priority.ALWAYS);
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(8, 0, 8, 0));
        row.getStyleClass().add("msg-bubble-row msg-bubble-row--me msg-bubble-row--editing");
        row.getChildren().addAll(leftGrow, stack);

        Platform.runLater(() -> {
            ta.requestFocus();
            ta.positionCaret(ta.getText().length());
        });

        return row;
    }
}
