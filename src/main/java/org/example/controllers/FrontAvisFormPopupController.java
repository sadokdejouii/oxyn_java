package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.entities.AvisEvenement;

public class FrontAvisFormPopupController {

    @FXML
    private Label formTitleLabel;

    @FXML
    private ComboBox<Integer> noteCombo;

    @FXML
    private TextArea commentTextArea;

    @FXML
    private Label feedbackLabel;

    private boolean confirmed;

    @FXML
    private void initialize() {
        noteCombo.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        noteCombo.setButtonCell(createStarCell());
        noteCombo.setCellFactory(listView -> createStarCell());
    }

    public void setData(String title, AvisEvenement avis) {
        formTitleLabel.setText(title);
        feedbackLabel.setText("");

        if (avis != null) {
            noteCombo.setValue(Math.max(1, Math.min(5, avis.getNote())));
            commentTextArea.setText(avis.getCommentaire() == null ? "" : avis.getCommentaire());
        } else {
            noteCombo.setValue(5);
            commentTextArea.clear();
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public int getSelectedNote() {
        return noteCombo.getValue() == null ? 5 : noteCombo.getValue();
    }

    public String getComment() {
        String text = commentTextArea.getText();
        return text == null ? "" : text.trim();
    }

    @FXML
    private void handleSave() {
        if (noteCombo.getValue() == null) {
            feedbackLabel.setText("Choisissez une note entre 1 et 5.");
            return;
        }

        confirmed = true;
        closeStage();
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) formTitleLabel.getScene().getWindow();
        stage.close();
    }

    private ListCell<Integer> createStarCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(buildStars(item) + " (" + item + "/5)");
                }
            }
        };
    }

    private String buildStars(int note) {
        int value = Math.max(1, Math.min(5, note));
        return "★".repeat(value) + "☆".repeat(5 - value);
    }
}
