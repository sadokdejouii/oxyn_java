package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import org.example.services.DashboardDataService;

import java.net.URL;
import java.util.ResourceBundle;

public class ReportsStatisticsController implements Initializable {

    @FXML
    private BarChart<String, Number> barChart;

    @FXML
    private PieChart pieChart;

    private final DashboardDataService data = new DashboardDataService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        XYChart.Series<String, Number> inbound = new XYChart.Series<>();
        inbound.setName("Inbound");
        inbound.getData().add(new XYChart.Data<>("Retail", 42));
        inbound.getData().add(new XYChart.Data<>("B2B", 28));
        inbound.getData().add(new XYChart.Data<>("Exports", 19));
        inbound.getData().add(new XYChart.Data<>("Direct", 11));
        barChart.getData().add(inbound);

        for (DashboardDataService.DistributionSlice slice : data.distributionSlices()) {
            pieChart.getData().add(new PieChart.Data(slice.name(), slice.value()));
        }
    }
}
