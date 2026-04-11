package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class FrontEventPopupController {

    @FXML
    private Label popupTitleLabel;

    @FXML
    private Label popupSubtitleLabel;

    @FXML
    private VBox popupItemsBox;

    @FXML
    private HBox popupActionsBox;

    @FXML
    private Button primaryActionButton;

    @FXML
    private Button secondaryActionButton;

    private Runnable primaryAction;
    private Runnable secondaryAction;

    public void setData(String title, String subtitle, List<String> rows, String emptyMessage) {
        popupTitleLabel.setText(title);
        popupSubtitleLabel.setText(subtitle);
        popupItemsBox.getChildren().clear();

        if (rows == null || rows.isEmpty()) {
            Label emptyLabel = new Label(emptyMessage);
            emptyLabel.setWrapText(true);
            emptyLabel.getStyleClass().add("front-popup-empty");
            popupItemsBox.getChildren().add(emptyLabel);
            return;
        }

        for (String row : rows) {
            Label itemLabel = new Label(row);
            itemLabel.setWrapText(true);
            itemLabel.setMaxWidth(Double.MAX_VALUE);
            itemLabel.getStyleClass().add("front-popup-item");
            popupItemsBox.getChildren().add(itemLabel);
        }
    }

    public void configureActions(String primaryText,
                                 Runnable primaryAction,
                                 boolean showPrimary,
                                 boolean disablePrimary,
                                 String secondaryText,
                                 Runnable secondaryAction,
                                 boolean showSecondary,
                                 boolean disableSecondary) {
        this.primaryAction = primaryAction;
        this.secondaryAction = secondaryAction;

        primaryActionButton.setText(primaryText == null ? "Action" : primaryText);
        primaryActionButton.setVisible(showPrimary);
        primaryActionButton.setManaged(showPrimary);
        primaryActionButton.setDisable(disablePrimary);

        secondaryActionButton.setText(secondaryText == null ? "Action" : secondaryText);
        secondaryActionButton.setVisible(showSecondary);
        secondaryActionButton.setManaged(showSecondary);
        secondaryActionButton.setDisable(disableSecondary);

        boolean showActions = showPrimary || showSecondary;
        popupActionsBox.setVisible(showActions);
        popupActionsBox.setManaged(showActions);
    }

    @FXML
    private void handlePrimaryAction() {
        Runnable action = primaryAction;
        closeStage();
        if (action != null) {
            Platform.runLater(() -> {
                try {
                    action.run();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            });
        }
    }

    @FXML
    private void handleSecondaryAction() {
        Runnable action = secondaryAction;
        closeStage();
        if (action != null) {
            Platform.runLater(() -> {
                try {
                    action.run();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            });
        }
    }

    @FXML
    private void handleClose() {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) popupItemsBox.getScene().getWindow();
        stage.close();
    }
}
