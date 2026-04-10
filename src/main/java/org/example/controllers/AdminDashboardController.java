package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.services.DashboardDataService;

import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML
    private Label kpiStockLabel;

    @FXML
    private Label kpiEventsLabel;

    @FXML
    private Label kpiUsersLabel;

    @FXML
    private Label kpiRevenueLabel;

    @FXML
    private LineChart<String, Number> activityLineChart;

    @FXML
    private PieChart mixPieChart;

    @FXML
    private VBox activityList;

    private final DashboardDataService data = new DashboardDataService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        DashboardDataService.KpiSnapshot kpi = data.loadKpis();
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        kpiStockLabel.setText(NumberFormat.getIntegerInstance().format(kpi.totalStockUnits()));
        kpiEventsLabel.setText(NumberFormat.getIntegerInstance().format(kpi.totalEvents()));
        kpiUsersLabel.setText(NumberFormat.getIntegerInstance().format(kpi.totalUsers()));
        kpiRevenueLabel.setText(currency.format(kpi.revenue()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Activity");
        for (DashboardDataService.ActivityPoint p : data.activitySeriesLastWeeks()) {
            series.getData().add(new XYChart.Data<>(p.label(), p.value()));
        }
        activityLineChart.getData().add(series);
        activityLineChart.setCreateSymbols(true);

        for (DashboardDataService.DistributionSlice slice : data.distributionSlices()) {
            mixPieChart.getData().add(new PieChart.Data(slice.name(), slice.value()));
        }

        for (DashboardDataService.ActivityRow row : data.recentActivity()) {
            VBox rowBox = new VBox(4);
            rowBox.getStyleClass().add("activity-row");
            Label title = new Label(row.title());
            title.getStyleClass().add("activity-title");
            Label sub = new Label(row.subtitle());
            sub.getStyleClass().add("activity-sub");
            Label time = new Label(row.timeLabel());
            time.getStyleClass().add("activity-time");
            HBox top = new HBox();
            top.setSpacing(12);
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            top.getChildren().addAll(title, time);
            rowBox.getChildren().addAll(top, sub);
            activityList.getChildren().add(rowBox);
        }

        NumberAxis yAxis = (NumberAxis) activityLineChart.getYAxis();
        yAxis.setForceZeroInRange(true);
    }
}
