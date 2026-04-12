package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.ResourceBundle;

public class EncadrantPlanningController implements Initializable {

    @FXML private TableView<?> planningTable;
    @FXML private TableColumn<?, ?> titreColumn;
    @FXML private TableColumn<?, ?> dateColumn;
    @FXML private TableColumn<?, ?> lieuColumn;
    @FXML private TableColumn<?, ?> statutColumn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TODO: charger le planning depuis EvenementServices
    }

    @FXML
    private void handleRefresh() {
        // TODO: recharger les données
    }
}
