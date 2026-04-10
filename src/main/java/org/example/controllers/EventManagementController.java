package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.entities.Evenement;
import org.example.services.EvenementServices;

import java.net.URL;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class EventManagementController implements Initializable {

    public static final class EventRow {
        private final String title;
        private final String ville;
        private final String debut;
        private final String fin;
        private final String statut;
        private final int id;

        public EventRow(int id, String title, String ville, String debut, String fin, String statut) {
            this.id = id;
            this.title = title;
            this.ville = ville;
            this.debut = debut;
            this.fin = fin;
            this.statut = statut;
        }

        public String getTitle() {
            return title;
        }

        public String getVille() {
            return ville;
        }

        public String getDebut() {
            return debut;
        }

        public String getFin() {
            return fin;
        }

        public String getStatut() {
            return statut;
        }

        public int getId() {
            return id;
        }
    }

    @FXML
    private TableView<EventRow> eventsTable;

    @FXML
    private TableColumn<EventRow, String> titleColumn;

    @FXML
    private TableColumn<EventRow, String> villeColumn;

    @FXML
    private TableColumn<EventRow, String> debutColumn;

    @FXML
    private TableColumn<EventRow, String> finColumn;

    @FXML
    private TableColumn<EventRow, String> statutColumn;

    private final ObservableList<EventRow> rows = FXCollections.observableArrayList();
    private final EvenementServices evenementServices = new EvenementServices();
    private final DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        villeColumn.setCellValueFactory(new PropertyValueFactory<>("ville"));
        debutColumn.setCellValueFactory(new PropertyValueFactory<>("debut"));
        finColumn.setCellValueFactory(new PropertyValueFactory<>("fin"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        eventsTable.setItems(rows);
        reload();
    }

    private void reload() {
        rows.clear();
        try {
            List<Evenement> list = evenementServices.afficher();
            for (Evenement e : list) {
                Date d0 = e.getDateDebut();
                Date d1 = e.getDateFin();
                rows.add(new EventRow(
                        e.getId(),
                        safe(e.getTitre()),
                        safe(e.getVille()),
                        d0 == null ? "—" : fmt.format(d0),
                        d1 == null ? "—" : fmt.format(d1),
                        safe(e.getStatut())
                ));
            }
        } catch (SQLException ex) {
            rows.add(new EventRow(-1, "Database unavailable", "—", "—", "—", "Error"));
        }
        if (rows.isEmpty()) {
            rows.add(new EventRow(-1, "No events yet", "—", "—", "—", "Draft"));
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @FXML
    private void handleAddEvent() {
        try {
            Stage owner = (Stage) eventsTable.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/AjouterEvenements.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Add event");
            stage.setScene(new Scene(root, 640, 520));
            stage.setOnHidden(ev -> reload());
            stage.show();
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText("Could not open the form: " + e.getMessage());
            a.showAndWait();
        }
    }

    @FXML
    private void handleRefresh() {
        reload();
    }

    @FXML
    private void handleDelete() {
        EventRow sel = eventsTable.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getId() < 0) {
            info("Delete", "Select a persisted event to delete.");
            return;
        }
        try {
            evenementServices.supprimer(sel.getId());
            reload();
        } catch (SQLException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }

    private static void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
