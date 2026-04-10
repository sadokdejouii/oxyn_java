package org.example.controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class StockManagementController implements Initializable {

    public static final class StockRow {
        private final SimpleStringProperty sku;
        private final SimpleStringProperty name;
        private final SimpleIntegerProperty qty;
        private final SimpleStringProperty site;
        private final SimpleStringProperty status;

        public StockRow(String sku, String name, int qty, String site, String status) {
            this.sku = new SimpleStringProperty(sku);
            this.name = new SimpleStringProperty(name);
            this.qty = new SimpleIntegerProperty(qty);
            this.site = new SimpleStringProperty(site);
            this.status = new SimpleStringProperty(status);
        }

        public String getSku() {
            return sku.get();
        }

        public String getName() {
            return name.get();
        }

        public int getQty() {
            return qty.get();
        }

        public String getSite() {
            return site.get();
        }

        public String getStatus() {
            return status.get();
        }
    }

    @FXML
    private TableView<StockRow> stockTable;

    @FXML
    private TableColumn<StockRow, String> skuColumn;

    @FXML
    private TableColumn<StockRow, String> nameColumn;

    @FXML
    private TableColumn<StockRow, Integer> qtyColumn;

    @FXML
    private TableColumn<StockRow, String> siteColumn;

    @FXML
    private TableColumn<StockRow, String> statusColumn;

    private final ObservableList<StockRow> rows = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        skuColumn.setCellValueFactory(new PropertyValueFactory<>("sku"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        qtyColumn.setCellValueFactory(new PropertyValueFactory<>("qty"));
        siteColumn.setCellValueFactory(new PropertyValueFactory<>("site"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        loadDemo();
        stockTable.setItems(rows);
    }

    private void loadDemo() {
        rows.setAll(
                new StockRow("FD-GRN-01", "Winter wheat seeds (25kg)", 420, "North silo", "In stock"),
                new StockRow("CH-MAP-12", "Organic fertilizer MAP", 118, "Chemical bay", "Low"),
                new StockRow("IR-PPE-03", "Field PPE kit", 64, "Safety", "In stock"),
                new StockRow("TL-DRP-88", "Drip irrigation tape", 210, "Workshed", "In stock")
        );
    }

    @FXML
    private void handleAdd() {
        info("Add SKU", "Hook this action to your inventory dialog or service.");
    }

    @FXML
    private void handleEdit() {
        StockRow selected = stockTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            info("Edit", "Select a row first.");
            return;
        }
        info("Edit SKU", "Selected: " + selected.getSku());
    }

    @FXML
    private void handleDelete() {
        StockRow selected = stockTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            info("Delete", "Select a row first.");
            return;
        }
        rows.remove(selected);
    }

    @FXML
    private void handleRefresh() {
        loadDemo();
    }

    private static void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
