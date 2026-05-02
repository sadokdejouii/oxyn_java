package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.entities.Evenement;
import org.example.services.EvenementServices;
import org.example.services.WeatherService;

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

        private Button meteoBtn;

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

            meteoBtn = new Button("🌤️ Météo");
            meteoBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 10; -fx-background-color: #3a8fc3; -fx-text-fill: white; -fx-background-radius: 6;");
            meteoBtn.setOnAction(e -> {
                EvenementRow row = getTableView().getItems().get(getIndex());
                showWeatherPopup(row.getVille(), row.getDateDebut(), row.getTitre());
            });

            // Container for buttons
            container = new HBox(8);
            container.setStyle("-fx-alignment: CENTER_LEFT;");
            container.getChildren().addAll(modifierBtn, supprimerBtn, meteoBtn);
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
     * Shows a weather popup for the given event city and date
     */
    private void showWeatherPopup(String ville, String dateDebut, String eventTitre) {
        Stage weatherStage = new Stage();
        weatherStage.initModality(Modality.APPLICATION_MODAL);
        weatherStage.setTitle("Météo – " + eventTitre);
        weatherStage.setResizable(false);

        // --- Header ---
        Label titleLabel = new Label("🌤️ Météo de l'Événement");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Label eventLabel = new Label(eventTitre);
        eventLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");

        Label villeLabel = new Label("📍 " + ville + "   📅 " + dateDebut);
        villeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #777;");

        VBox header = new VBox(4, titleLabel, eventLabel, villeLabel);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: linear-gradient(to bottom, #e8f4fc, #f8fbff); -fx-padding: 20 24 16 24; -fx-border-radius: 10 10 0 0; -fx-background-radius: 10 10 0 0;");

        // --- Loading state ---
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(40, 40);
        Label loadingLabel = new Label("Chargement des données météo...");
        loadingLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");
        VBox loadingBox = new VBox(12, spinner, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(30));

        // --- Content area (replaced once data loaded) ---
        VBox contentArea = new VBox(loadingBox);
        contentArea.setAlignment(Pos.CENTER);
        contentArea.setStyle("-fx-padding: 10 24 10 24;");

        // --- Close button ---
        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-font-size: 12px; -fx-padding: 8 24; -fx-background-color: #3a8fc3; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        closeBtn.setOnAction(ev -> weatherStage.close());
        HBox footer = new HBox(closeBtn);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(10, 24, 20, 24));

        VBox root = new VBox(header, new Separator(), contentArea, footer);
        root.setStyle("-fx-background-color: #f8fbff; -fx-background-radius: 10;");

        Scene scene = new Scene(root, 420, 380);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());
        } catch (Exception ignored) {}

        weatherStage.setScene(scene);
        weatherStage.show();

        // Fetch weather in background thread
        WeatherService weatherService = new WeatherService();
        new Thread(() -> {
            try {
                WeatherService.WeatherResult result = weatherService.getWeather(ville);
                Platform.runLater(() -> {
                    // Build weather details grid
                    GridPane grid = new GridPane();
                    grid.setHgap(16);
                    grid.setVgap(10);
                    grid.setPadding(new Insets(10, 0, 10, 0));

                    Label emojiLabel = new Label(result.getWeatherEmoji());
                    emojiLabel.setStyle("-fx-font-size: 48px;");
                    Label descLabel = new Label(result.description);
                    descLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");
                    Label tempBigLabel = new Label(String.format("%.1f°C", result.temperature));
                    tempBigLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #e67e22;");

                    VBox topWeather = new VBox(4, emojiLabel, tempBigLabel, descLabel);
                    topWeather.setAlignment(Pos.CENTER);
                    topWeather.setPadding(new Insets(0, 0, 12, 0));

                    // Details rows
                    addWeatherRow(grid, 0, "🌡️ Ressenti", String.format("%.1f°C", result.feelsLike));
                    addWeatherRow(grid, 1, "🔼 Max / 🔽 Min", String.format("%.1f°C / %.1f°C", result.tempMax, result.tempMin));
                    addWeatherRow(grid, 2, "💧 Humidité", result.humidity + "%");
                    addWeatherRow(grid, 3, "💨 Vent", String.format("%.1f m/s (%s)", result.windSpeed, result.getWindDirection()));
                    addWeatherRow(grid, 4, "☁️ Nuages", result.cloudiness + "%");
                    int rowIndex = 5;
                    if (result.rainVolume > 0) {
                        addWeatherRow(grid, rowIndex++, "🌧️ Pluie (1h)", String.format("%.1f mm", result.rainVolume));
                    }
                    if (result.snowVolume > 0) {
                        addWeatherRow(grid, rowIndex++, "❄️ Neige (1h)", String.format("%.1f mm", result.snowVolume));
                    }
                    if (result.visibility > 0) {
                        addWeatherRow(grid, rowIndex, "👁️ Visibilité", (result.visibility / 1000.0) + " km");
                    }

                    Label noteLabel = new Label("ℹ️ Données météo actuelles pour " + result.ville + ", " + result.pays);
                    noteLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999; -fx-wrap-text: true;");
                    noteLabel.setMaxWidth(370);

                    VBox weatherContent = new VBox(10, topWeather, new Separator(), grid, noteLabel);
                    weatherContent.setAlignment(Pos.TOP_CENTER);

                    contentArea.getChildren().setAll(weatherContent);
                    weatherStage.setHeight(500);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Label errLabel = new Label("❌ " + ex.getMessage());
                    errLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #c0392b; -fx-wrap-text: true;");
                    errLabel.setMaxWidth(370);
                    VBox errBox = new VBox(12, errLabel);
                    errBox.setAlignment(Pos.CENTER);
                    errBox.setPadding(new Insets(20));
                    contentArea.getChildren().setAll(errBox);
                });
            }
        }).start();
    }

    private void addWeatherRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
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

