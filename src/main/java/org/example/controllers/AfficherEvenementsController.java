package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
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
    private TableColumn<EvenementRow, Integer> actionsColumn;

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
        
        // Setup actions column with custom cells
        actionsColumn.setCellFactory(param -> new ActionTableCell());
    }

    private void loadEvents() {
        try {
            List<Evenement> evenements = evenementServices.afficher();
            ObservableList<EvenementRow> rows = FXCollections.observableArrayList();

            for (Evenement e : evenements) {
                rows.add(new EvenementRow(
                        e.getId(),
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
        private int id;
        private String titre;
        private String description;
        private String dateDebut;
        private String dateFin;
        private String lieu;
        private String ville;
        private int placesMax;
        private String statut;

        public EvenementRow(int id, String titre, String description, String dateDebut, String dateFin,
                           String lieu, String ville, int placesMax, String statut) {
            this.id = id;
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
        public int getId() { return id; }
        public String getTitre() { return titre; }
        public String getDescription() { return description; }
        public String getDateDebut() { return dateDebut; }
        public String getDateFin() { return dateFin; }
        public String getLieu() { return lieu; }
        public String getVille() { return ville; }
        public int getPlacesMax() { return placesMax; }
        public String getStatut() { return statut; }
    }

    /**
     * Custom table cell for action buttons (Modifier and Supprimer)
     */
    private class ActionTableCell extends TableCell<EvenementRow, Integer> {
        private HBox container;
        private Button modifierBtn;
        private Button supprimerBtn;

        public ActionTableCell() {
            // Create buttons
            modifierBtn = new Button("✏️ Modifier");
            modifierBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 10;");
            modifierBtn.setOnAction(e -> {
                EvenementRow row = getTableView().getItems().get(getIndex());
                openModifyEventForm(row.getId(), row.getTitre());
            });

            supprimerBtn = new Button("🗑️ Supprimer");
            supprimerBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 10;");
            supprimerBtn.setOnAction(e -> {
                EvenementRow row = getTableView().getItems().get(getIndex());
                deleteEventWithConfirmation(row.getId(), row.getTitre());
            });

            // Container for buttons
            container = new HBox(8);
            container.setStyle("-fx-alignment: CENTER_LEFT;");
            container.getChildren().addAll(modifierBtn, supprimerBtn);
        }

        @Override
        protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                setGraphic(container);
            }
        }
    }

    /**
     * Opens the modification form for an event
     */
    private void openModifyEventForm(int eventId, String eventTitle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/ModifierEvenements.fxml"));
            ModifierEvenementController controller = new ModifierEvenementController();
            loader.setController(controller);
            
            Stage modifyStage = new Stage();
            modifyStage.setTitle("Modifier l'Événement: " + eventTitle);
            modifyStage.setScene(new Scene(loader.load(), 700, 850));
            modifyStage.initModality(Modality.APPLICATION_MODAL);
            
            // Load the event data
            controller.setEventData(eventId);
            
            modifyStage.showAndWait();
            
            // Refresh table after modifying event
            loadEvents();
        } catch (IOException e) {
            e.printStackTrace();
            showStyledError("Erreur", "Impossible d'ouvrir le formulaire de modification.");
        }
    }

    /**
     * Deletes an event with a styled confirmation dialog
     */
    private void deleteEventWithConfirmation(int eventId, String eventTitle) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation de suppression");
        confirmDialog.setHeaderText("Supprimer l'événement?");
        confirmDialog.setContentText("Êtes-vous sûr de vouloir supprimer l'événement '" + eventTitle + "'?");
        
        stylizeAlert(confirmDialog, "error");
        
        if (confirmDialog.showAndWait().isPresent()) {
            try {
                evenementServices.supprimer(eventId);
                showStyledSuccess("Succès", "L'événement a été supprimé avec succès.");
                loadEvents();
            } catch (Exception e) {
                e.printStackTrace();
                showStyledError("Erreur", "Impossible de supprimer l'événement.");
            }
        }
    }

    /**
     * Stylizes an alert dialog with custom styling
     */
    private void stylizeAlert(Alert alert, String type) {
        try {
            var dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());
            
            if ("error".equals(type)) {
                dialogPane.setStyle("-fx-border-color: rgba(244, 67, 54, 0.3);");
            }
            
            dialogPane.getButtonTypes().forEach(button -> {
                var buttonNode = dialogPane.lookupButton(button);
                buttonNode.setStyle("-fx-font-size: 12px; -fx-padding: 8 20;");
                // Style OK buttons with red background for delete confirmation
                if (button == javafx.scene.control.ButtonType.OK) {
                    buttonNode.setStyle("-fx-font-size: 12px; -fx-padding: 8 20; -fx-base: rgba(244, 67, 54, 0.8);");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows a styled success dialog
     */
    private void showStyledSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.setHeaderText(null);
        stylizeAlert(alert, "success");
        alert.showAndWait();
    }

    /**
     * Shows a styled error dialog
     */
    private void showStyledError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.setHeaderText(null);
        stylizeAlert(alert, "error");
        alert.showAndWait();
    }
}

