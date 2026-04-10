package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.entities.Evenement;
import org.example.services.EvenementServices;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class AfficherEvenementsController implements Initializable {

    @FXML
    private TableView<EvenementRow> eventsTable;

    @FXML
    private TableColumn<EvenementRow, String> titreColumn;

    @FXML
    private TableColumn<EvenementRow, String> descriptionColumn;

    @FXML
    private TableColumn<EvenementRow, String> dateDebutColumn;

    @FXML
    private TableColumn<EvenementRow, String> dateFinColumn;

    @FXML
    private TableColumn<EvenementRow, String> lieuColumn;

    @FXML
    private TableColumn<EvenementRow, String> villeColumn;

    @FXML
    private TableColumn<EvenementRow, Integer> placesColumn;

    @FXML
    private TableColumn<EvenementRow, String> statutColumn;

    @FXML
    private Button ajouterBtn;

    private EvenementServices evenementServices = new EvenementServices();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        loadEvents();
    }

    private void setupTableColumns() {
        titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dateDebutColumn.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        dateFinColumn.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        lieuColumn.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        villeColumn.setCellValueFactory(new PropertyValueFactory<>("ville"));
        placesColumn.setCellValueFactory(new PropertyValueFactory<>("placesMax"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
    }

    private void loadEvents() {
        try {
            List<Evenement> evenements = evenementServices.afficher();
            ObservableList<EvenementRow> rows = FXCollections.observableArrayList();

            for (Evenement e : evenements) {
                rows.add(new EvenementRow(
                        e.getTitre(),
                        e.getDescription(),
                        dateFormat.format(e.getDateDebut()),
                        dateFormat.format(e.getDateFin()),
                        e.getLieu(),
                        e.getVille(),
                        e.getPlacesMax(),
                        e.getStatut()
                ));
            }

            eventsTable.setItems(rows);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openAddEventForm(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/AjouterEvenements.fxml"));
            Stage addEventStage = new Stage();
            addEventStage.setTitle("Ajouter un Nouvel Événement");
            addEventStage.setScene(new Scene(loader.load(), 700, 850));
            addEventStage.initModality(Modality.APPLICATION_MODAL);
            addEventStage.showAndWait();

            // Refresh table after adding event
            loadEvents();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper class to represent a row in the events table
     */
    public static class EvenementRow {
        private String titre;
        private String description;
        private String dateDebut;
        private String dateFin;
        private String lieu;
        private String ville;
        private int placesMax;
        private String statut;

        public EvenementRow(String titre, String description, String dateDebut, String dateFin,
                           String lieu, String ville, int placesMax, String statut) {
            this.titre = titre;
            this.description = description;
            this.dateDebut = dateDebut;
            this.dateFin = dateFin;
            this.lieu = lieu;
            this.ville = ville;
            this.placesMax = placesMax;
            this.statut = statut;
        }

        // Getters
        public String getTitre() { return titre; }
        public String getDescription() { return description; }
        public String getDateDebut() { return dateDebut; }
        public String getDateFin() { return dateFin; }
        public String getLieu() { return lieu; }
        public String getVille() { return ville; }
        public int getPlacesMax() { return placesMax; }
        public String getStatut() { return statut; }
    }
}

