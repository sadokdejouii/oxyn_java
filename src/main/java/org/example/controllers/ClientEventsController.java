package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.entities.Evenement;
import org.example.services.EvenementServices;

import java.net.URL;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Read-only events view for client users (no CRUD).
 */
public class ClientEventsController implements Initializable {

    public static final class EventSummaryRow {
        private final String title;
        private final String ville;
        private final String debut;
        private final String statut;

        public EventSummaryRow(String title, String ville, String debut, String statut) {
            this.title = title;
            this.ville = ville;
            this.debut = debut;
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

        public String getStatut() {
            return statut;
        }
    }

    @FXML
    private TableView<EventSummaryRow> eventsTable;

    @FXML
    private TableColumn<EventSummaryRow, String> titleColumn;

    @FXML
    private TableColumn<EventSummaryRow, String> villeColumn;

    @FXML
    private TableColumn<EventSummaryRow, String> debutColumn;

    @FXML
    private TableColumn<EventSummaryRow, String> statutColumn;

    private final ObservableList<EventSummaryRow> rows = FXCollections.observableArrayList();
    private final EvenementServices evenementServices = new EvenementServices();
    private final DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        villeColumn.setCellValueFactory(new PropertyValueFactory<>("ville"));
        debutColumn.setCellValueFactory(new PropertyValueFactory<>("debut"));
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
                rows.add(new EventSummaryRow(
                        safe(e.getTitre()),
                        safe(e.getVille()),
                        d0 == null ? "—" : fmt.format(d0),
                        safe(e.getStatut())
                ));
            }
        } catch (SQLException ex) {
            rows.add(new EventSummaryRow("Unable to load events", "—", "—", "Error"));
        }
        if (rows.isEmpty()) {
            rows.add(new EventSummaryRow("No published events", "—", "—", "—"));
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @FXML
    private void handleRefresh() {
        reload();
    }

    @FXML
    private void handleDetails() {
        EventSummaryRow sel = eventsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            info("Details", "Select an event first.");
            return;
        }
        info(sel.getTitle(), "City: " + sel.getVille() + "\nStarts: " + sel.getDebut() + "\nStatus: " + sel.getStatut());
    }

    private static void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
