package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entities.AvisEvenement;
import org.example.services.AvisAiModerationService;

public class FrontAvisFormPopupController {

    @FXML
    private Label formTitleLabel;

    @FXML
    private ComboBox<Integer> noteCombo;

    @FXML
    private TextArea commentTextArea;

    @FXML
    private Label feedbackLabel;

    @FXML
    private VBox moderationAlertBox;

    @FXML
    private Label moderationAlertTitleLabel;

    @FXML
    private Label moderationAlertBodyLabel;

    @FXML
    private Label moderationAlertMetaLabel;

    private boolean confirmed;

    @FXML
    private void initialize() {
        noteCombo.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        noteCombo.setButtonCell(createStarCell());
        noteCombo.setCellFactory(listView -> createStarCell());
        commentTextArea.textProperty().addListener((obs, oldValue, newValue) -> clearModerationAlert());
    }

    public void setData(String title, AvisEvenement avis) {
        formTitleLabel.setText(title);
        feedbackLabel.setText("");
        clearModerationAlert();

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

        String normalizedComment = AvisAiModerationService.normalizeAcceptedComment(getComment());
        AvisAiModerationService.ModerationResult result = AvisAiModerationService.analyze(normalizedComment);
        if (result.blocked()) {
            showModerationAlert(result);
            return;
        }

        commentTextArea.setText(result.cleanedComment());
        feedbackLabel.setText("");
        clearModerationAlert();

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

    private void showModerationAlert(AvisAiModerationService.ModerationResult result) {
        moderationAlertTitleLabel.setText("Commentaire refuse");
        moderationAlertBodyLabel.setText(result.userMessage());

        StringBuilder meta = new StringBuilder();
        if (!result.reasons().isEmpty()) {
            meta.append("Signaux : ").append(String.join(", ", result.reasons()));
        }

        if (!result.recommendation().isBlank()) {
            if (meta.length() > 0) {
                meta.append("\n");
            }
            meta.append("Suggestion : ").append(result.recommendation());
        }

        moderationAlertMetaLabel.setText(meta.toString());
        moderationAlertMetaLabel.setManaged(meta.length() > 0);
        moderationAlertMetaLabel.setVisible(meta.length() > 0);
        moderationAlertBox.setManaged(true);
        moderationAlertBox.setVisible(true);
        feedbackLabel.setText("");
        if (!commentTextArea.getStyleClass().contains("front-modal-textarea-danger")) {
            commentTextArea.getStyleClass().add("front-modal-textarea-danger");
        }
    }

    private void clearModerationAlert() {
        moderationAlertBox.setManaged(false);
        moderationAlertBox.setVisible(false);
        moderationAlertTitleLabel.setText("");
        moderationAlertBodyLabel.setText("");
        moderationAlertMetaLabel.setText("");
        moderationAlertMetaLabel.setManaged(false);
        moderationAlertMetaLabel.setVisible(false);
        commentTextArea.getStyleClass().remove("front-modal-textarea-danger");
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
