package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class PlanningPageController implements Initializable {

    @FXML
    private VBox agendaList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        addRow("Mon", "Soil sampling — North parcel", "09:30");
        addRow("Wed", "Supplier call (irrigation parts)", "14:00");
        addRow("Fri", "Team sync & sprint review", "10:00");
    }

    private void addRow(String day, String title, String time) {
        VBox row = new VBox(4);
        row.getStyleClass().add("activity-row");
        HBox top = new HBox(12);
        Label dayLbl = new Label(day);
        dayLbl.getStyleClass().add("activity-time");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label timeLbl = new Label(time);
        timeLbl.getStyleClass().add("activity-time");
        top.getChildren().addAll(dayLbl, spacer, timeLbl);
        Label t = new Label(title);
        t.getStyleClass().add("activity-title");
        Label sub = new Label("Workspace calendar · OXYN");
        sub.getStyleClass().add("activity-sub");
        row.getChildren().addAll(top, t, sub);
        agendaList.getChildren().add(row);
    }
}
