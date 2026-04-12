package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.ResourceBundle;

public class EncadrantGroupeController implements Initializable {

    @FXML private TableView<?> groupeTable;
    @FXML private TableColumn<?, ?> nomColumn;
    @FXML private TableColumn<?, ?> evenementColumn;
    @FXML private TableColumn<?, ?> dateInscriptionColumn;
    @FXML private TableColumn<?, ?> statutColumn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TODO: charger les participants du groupe depuis InscriptionEvenementServices
    }

    @FXML
    private void handleRefresh() {
        // TODO: recharger les données
    }
}
