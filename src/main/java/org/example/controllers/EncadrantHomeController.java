package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.example.services.EvenementServices;

import java.net.URL;
import java.util.ResourceBundle;

public class EncadrantHomeController implements Initializable {

    @FXML private Label encEventsLabel;
    @FXML private Label encParticipantsLabel;
    @FXML private Label encPlanningsLabel;
    @FXML private BarChart<String, Number> participationChart;
    @FXML private VBox encActivityList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadStats();
        loadChart();
    }

    private void loadStats() {
        try {
            int totalEvents = new EvenementServices().afficher().size();
            encEventsLabel.setText(String.valueOf(totalEvents));
        } catch (Exception e) {
            encEventsLabel.setText("0");
        }
        encParticipantsLabel.setText("—");
        encPlanningsLabel.setText("—");
    }

    private void loadChart() {
        try {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            new EvenementServices().afficher().stream().limit(6).forEach(ev ->
                series.getData().add(new XYChart.Data<>(
                    ev.getTitre() != null ? ev.getTitre() : "?", 0))
            );
            participationChart.getData().add(series);
        } catch (Exception ignored) {
        }
    }
}
