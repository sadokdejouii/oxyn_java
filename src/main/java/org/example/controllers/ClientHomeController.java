package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.services.DashboardDataService;

import java.net.URL;
import java.text.NumberFormat;
import java.util.ResourceBundle;

public class ClientHomeController implements Initializable {

    @FXML
    private Label miniEventsLabel;

    @FXML
    private Label miniPlansLabel;

    @FXML
    private Label miniNotifLabel;

    @FXML
    private LineChart<String, Number> sparkLineChart;

    @FXML
    private VBox clientHighlights;

    private final DashboardDataService data = new DashboardDataService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        DashboardDataService.KpiSnapshot kpi = data.loadKpis();
        miniEventsLabel.setText(NumberFormat.getIntegerInstance().format(Math.max(3, kpi.totalEvents() / 3)));
        miniPlansLabel.setText("5");
        miniNotifLabel.setText("2");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("You");
        for (DashboardDataService.ActivityPoint p : data.activitySeriesLastWeeks()) {
            series.getData().add(new XYChart.Data<>(p.label(), Math.max(1, p.value() - 5 + (p.label().hashCode() % 4))));
        }
        sparkLineChart.getData().add(series);

        for (DashboardDataService.ActivityRow row : data.recentActivity()) {
            VBox rowBox = new VBox(4);
            rowBox.getStyleClass().add("activity-row");
            Label title = new Label(row.title());
            title.getStyleClass().add("activity-title");
            Label sub = new Label(row.subtitle());
            sub.getStyleClass().add("activity-sub");
            Label time = new Label(row.timeLabel());
            time.getStyleClass().add("activity-time");
            HBox top = new HBox(12);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            top.getChildren().addAll(title, time);
            rowBox.getChildren().addAll(top, sub);
            clientHighlights.getChildren().add(rowBox);
        }
    }
}
