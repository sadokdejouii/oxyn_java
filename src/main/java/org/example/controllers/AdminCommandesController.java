package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.entities.CommandeAdminRow;
import org.example.entities.commandes;
import org.example.services.CommandesService;
import org.example.utils.TexteRecherche;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AdminCommandesController {

    private static final int JOURS_RETENTION = 7;
    private static final DateTimeFormatter AFFICHAGE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);
    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    private ListView<CommandeAdminRow> listCommandes;
    @FXML
    private Label purgeInfoLabel;
    @FXML
    private Label countLabel;
    @FXML
    private TextField rechercheCommandes;
    @FXML
    private ComboBox<String> triCommandes;

    private final CommandesService commandesService = new CommandesService();
    private final List<CommandeAdminRow> toutesLesLignes = new ArrayList<>();

    @FXML
    public void initialize() {
        if (listCommandes != null) {
            listCommandes.setStyle("-fx-background-color: #0D1B3E; -fx-control-inner-background: #0D1B3E;");
        }

        if (triCommandes != null) {
            triCommandes.getItems().setAll(
                    "Date (récent d’abord)",
                    "Date (ancien d’abord)",
                    "Montant (croissant)",
                    "Montant (décroissant)",
                    "Client (A → Z)",
                    "Statut (A → Z)");
            triCommandes.getSelectionModel().selectFirst();
        }

        if (rechercheCommandes != null) {
            rechercheCommandes.textProperty().addListener((obs, a, b) -> appliquerFiltreEtTri());
        }

        listCommandes.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CommandeAdminRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                commandes c = row.getCommande();
                VBox card = new VBox(10);
                card.getStyleClass().add("admin-order-list-card");
                card.setPadding(new Insets(14, 16, 14, 16));

                HBox top = new HBox(12);
                top.setAlignment(Pos.CENTER_LEFT);
                Label dateLbl = new Label(formaterDate(c.getDate_commande()));
                dateLbl.getStyleClass().add("saas-stat-value");
                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);
                Label chip = new Label(c.getStatut_commande() != null ? c.getStatut_commande() : "—");
                chip.getStyleClass().setAll("admin-order-chip");
                String st = c.getStatut_commande() != null ? c.getStatut_commande().toLowerCase(Locale.ROOT) : "";
                if (st.contains("annul")) {
                    chip.getStyleClass().add("admin-order-chip-annule");
                } else if (st.contains("valid")) {
                    chip.getStyleClass().add("admin-order-chip-valide");
                } else {
                    chip.getStyleClass().add("admin-order-chip-neutre");
                }
                top.getChildren().addAll(dateLbl, sp, chip);

                GridPane grid = new GridPane();
                grid.setHgap(12);
                grid.setVgap(6);
                int r = 0;
                grid.add(labelHint("Client"), 0, r);
                grid.add(valueLabel(row.getLibelleClient()), 1, r++);
                grid.add(labelHint("Montant"), 0, r);
                grid.add(valueLabel(formaterMontant(c.getTotal_commande())), 1, r++);
                grid.add(labelHint("Paiement"), 0, r);
                grid.add(valueLabel(c.getMode_paiement_commande() != null ? c.getMode_paiement_commande() : "—"), 1, r++);
                grid.add(labelHint("Livraison"), 0, r);
                Label addr = valueLabel(c.getAdresse_commande() != null ? c.getAdresse_commande() : "—");
                addr.setWrapText(true);
                addr.setMaxWidth(Double.MAX_VALUE);
                addr.getStyleClass().add("admin-order-address");
                GridPane.setHgrow(addr, Priority.ALWAYS);
                grid.add(addr, 1, r);

                HBox actions = new HBox(10);
                actions.setAlignment(Pos.CENTER_RIGHT);
                actions.setPadding(new Insets(6, 0, 0, 0));
                if (st.contains("annul")) {
                    Button sup = new Button("Supprimer");
                    sup.getStyleClass().add("danger-toolbar-btn");
                    sup.setOnAction(e -> confirmerSuppression(c));
                    actions.getChildren().add(sup);
                } else {
                    Label dash = new Label("—");
                    dash.getStyleClass().add("saas-stat-hint");
                    actions.getChildren().add(dash);
                }

                card.getChildren().addAll(top, grid, actions);
                setGraphic(card);
            }
        });

        chargerTable();
    }

    private static Label labelHint(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("saas-stat-label");
        return l;
    }

    private static Label valueLabel(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("saas-stat-hint");
        l.setWrapText(true);
        return l;
    }

    @FXML
    private void handleRefresh() {
        chargerTable();
    }

    @FXML
    private void handleRechercheOuTri() {
        appliquerFiltreEtTri();
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

            toutesLesLignes.clear();
            toutesLesLignes.addAll(commandesService.afficherPourAdmin());
            if (triCommandes != null && triCommandes.getSelectionModel().getSelectedIndex() < 0) {
                triCommandes.getSelectionModel().selectFirst();
            }
            appliquerFiltreEtTri();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
        }
    }

    private void appliquerFiltreEtTri() {
        String q = rechercheCommandes != null ? rechercheCommandes.getText() : "";
        String tri = triCommandes != null && triCommandes.getValue() != null
                ? triCommandes.getValue()
                : "Date (récent d’abord)";

        List<CommandeAdminRow> filtered = new ArrayList<>();
        for (CommandeAdminRow row : toutesLesLignes) {
            if (ligneCorrespondRecherche(row, q)) {
                filtered.add(row);
            }
        }

        Comparator<CommandeAdminRow> cmp = comparateurTri(tri);
        filtered.sort(cmp);

        ObservableList<CommandeAdminRow> obs = FXCollections.observableArrayList(filtered);
        listCommandes.setItems(obs);

        if (countLabel != null) {
            if (filtered.size() == toutesLesLignes.size()) {
                countLabel.setText(String.valueOf(filtered.size()));
            } else {
                countLabel.setText(filtered.size() + " / " + toutesLesLignes.size());
            }
        }
    }

    private static boolean ligneCorrespondRecherche(CommandeAdminRow row, String q) {
        commandes c = row.getCommande();
        String bloc = String.join(" ",
                row.getLibelleClient(),
                c.getStatut_commande() != null ? c.getStatut_commande() : "",
                c.getMode_paiement_commande() != null ? c.getMode_paiement_commande() : "",
                c.getAdresse_commande() != null ? c.getAdresse_commande() : "",
                c.getDate_commande() != null ? c.getDate_commande() : "",
                String.format(Locale.FRENCH, "%.2f", c.getTotal_commande()));
        return TexteRecherche.correspond(bloc, q);
    }

    private Comparator<CommandeAdminRow> comparateurTri(String libelleTri) {
        Comparator<CommandeAdminRow> parDateDesc = Comparator.comparing(
                (CommandeAdminRow r) -> parseDate(r.getCommande())).reversed();
        Comparator<CommandeAdminRow> parDateAsc = Comparator.comparing(
                (CommandeAdminRow r) -> parseDate(r.getCommande()));

        if (libelleTri == null) {
            return parDateDesc;
        }
        return switch (libelleTri) {
            case "Date (ancien d’abord)" -> parDateAsc;
            case "Montant (croissant)" -> Comparator.comparingDouble(r -> r.getCommande().getTotal_commande());
            case "Montant (décroissant)" -> Comparator.comparingDouble(
                    (CommandeAdminRow r) -> r.getCommande().getTotal_commande()).reversed();
            case "Client (A → Z)" -> Comparator.comparing(
                    r -> r.getLibelleClient() != null ? r.getLibelleClient().toLowerCase(Locale.ROOT) : "",
                    String.CASE_INSENSITIVE_ORDER);
            case "Statut (A → Z)" -> Comparator.comparing(
                    r -> r.getCommande().getStatut_commande() != null
                            ? r.getCommande().getStatut_commande().toLowerCase(Locale.ROOT)
                            : "",
                    String.CASE_INSENSITIVE_ORDER);
            default -> parDateDesc;
        };
    }

    private static LocalDateTime parseDate(commandes c) {
        String raw = c.getDate_commande();
        if (raw == null || raw.isBlank()) {
            return LocalDateTime.MIN;
        }
        String s = raw.trim();
        try {
            return LocalDateTime.parse(s, ISO);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(s).atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }
        return LocalDateTime.MIN;
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
}
