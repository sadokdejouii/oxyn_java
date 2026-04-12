package org.example.controllers;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.example.entities.CommandeAdminRow;
import org.example.entities.commandes;
import org.example.services.CommandesService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

public class AdminCommandesController {

    private static final int JOURS_RETENTION = 7;
    private static final DateTimeFormatter AFFICHAGE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);
    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    private TableView<CommandeAdminRow> tableCommandes;
    @FXML
    private TableColumn<CommandeAdminRow, String> colDate;
    @FXML
    private TableColumn<CommandeAdminRow, String> colClient;
    @FXML
    private TableColumn<CommandeAdminRow, String> colTotal;
    @FXML
    private TableColumn<CommandeAdminRow, String> colStatut;
    @FXML
    private TableColumn<CommandeAdminRow, String> colPaiement;
    @FXML
    private TableColumn<CommandeAdminRow, String> colAdresse;
    @FXML
    private TableColumn<CommandeAdminRow, Void> colAction;
    @FXML
    private Label purgeInfoLabel;
    @FXML
    private Label countLabel;

    private final CommandesService commandesService = new CommandesService();

    @FXML
    public void initialize() {
        tableCommandes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colDate.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(
                formaterDate(data.getValue().getCommande().getDate_commande())));
        colClient.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getLibelleClient()));

        colTotal.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(
                formaterMontant(data.getValue().getCommande().getTotal_commande())));

        colStatut.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(
                data.getValue().getCommande().getStatut_commande() != null
                        ? data.getValue().getCommande().getStatut_commande()
                        : "—"));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label chip = new Label(item);
                chip.setMaxWidth(Double.MAX_VALUE);
                chip.getStyleClass().setAll("admin-order-chip");
                String s = item.toLowerCase(Locale.ROOT);
                if (s.contains("annul")) {
                    chip.getStyleClass().add("admin-order-chip-annule");
                } else if (s.contains("valid")) {
                    chip.getStyleClass().add("admin-order-chip-valide");
                } else {
                    chip.getStyleClass().add("admin-order-chip-neutre");
                }
                HBox wrap = new HBox(chip);
                HBox.setHgrow(chip, Priority.ALWAYS);
                setGraphic(wrap);
            }
        });

        colPaiement.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(
                data.getValue().getCommande().getMode_paiement_commande() != null
                        ? data.getValue().getCommande().getMode_paiement_commande()
                        : "—"));

        colAdresse.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(tronquer(
                data.getValue().getCommande().getAdresse_commande() != null
                        ? data.getValue().getCommande().getAdresse_commande()
                        : "—", 72)));
        colAdresse.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label l = new Label(item);
                l.setWrapText(true);
                l.setMaxWidth(Double.MAX_VALUE);
                l.getStyleClass().add("admin-order-address");
                setGraphic(l);
            }
        });

        colAction.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(null));
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Supprimer");
            {
                btn.getStyleClass().add("danger-toolbar-btn");
                btn.setMinWidth(Region.USE_PREF_SIZE);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setGraphic(null);
                    return;
                }
                CommandeAdminRow row = getTableRow().getItem();
                if (row == null) {
                    setGraphic(null);
                    return;
                }
                String st = row.getCommande().getStatut_commande();
                if (st != null && st.toLowerCase(Locale.ROOT).contains("annul")) {
                    btn.setOnAction(e -> confirmerSuppression(row.getCommande()));
                    setGraphic(btn);
                } else {
                    Label dash = new Label("—");
                    dash.getStyleClass().add("saas-stat-hint");
                    setGraphic(dash);
                }
            }
        });

        chargerTable();
    }

    @FXML
    private void handleRefresh() {
        chargerTable();
    }

    private void chargerTable() {
        try {
            int purges = commandesService.purgerCommandesAnnuleesPlusAnciennesQue(JOURS_RETENTION);
            if (purgeInfoLabel != null) {
                if (purges > 0) {
                    purgeInfoLabel.setText(purges + " commande(s) annulée(s) depuis plus de "
                            + JOURS_RETENTION + " jour(s) ont été supprimées automatiquement.");
                    purgeInfoLabel.setVisible(true);
                    purgeInfoLabel.setManaged(true);
                } else {
                    purgeInfoLabel.setVisible(false);
                    purgeInfoLabel.setManaged(false);
                }
            }

            ObservableList<CommandeAdminRow> rows =
                    FXCollections.observableArrayList(commandesService.afficherPourAdmin());
            tableCommandes.setItems(rows);
            if (countLabel != null) {
                countLabel.setText(String.valueOf(rows.size()));
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
        }
    }

    private void confirmerSuppression(commandes c) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la commande");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Supprimer définitivement cette commande annulée ? Cette action est irréversible.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }
        try {
            if (commandesService.supprimerCommandeAnnuleeParAdmin(c.getId_commande())) {
                new Alert(Alert.AlertType.INFORMATION, "Commande supprimée.").showAndWait();
                chargerTable();
            } else {
                new Alert(Alert.AlertType.WARNING,
                        "Suppression impossible (commande introuvable ou statut non annulé).").showAndWait();
            }
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private static String formaterDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return "—";
        }
        String s = raw.trim();
        try {
            return LocalDateTime.parse(s, ISO).format(AFFICHAGE);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(s).format(AFFICHAGE);
        } catch (DateTimeParseException ignored) {
        }
        return s;
    }

    private static String formaterMontant(double total) {
        return String.format(Locale.FRENCH, "%.2f TND", total);
    }

    private static String tronquer(String s, int max) {
        if (s == null) {
            return "—";
        }
        String t = s.trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max - 1) + "…";
    }
}
