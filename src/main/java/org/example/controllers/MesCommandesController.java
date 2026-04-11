package org.example.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.entities.LigneCommandeAffichage;
import org.example.entities.commandes;
import org.example.services.CommandesService;
import org.example.services.SessionContext;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class MesCommandesController {

    @FXML
    private VBox ordersContainer;

    private MainLayoutController mainLayoutController;

    private final CommandesService commandesService = new CommandesService();

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    @FXML
    public void initialize() {
        refresh();
    }

    @FXML
    private void handleRetourBoutique() {
        if (mainLayoutController != null) {
            mainLayoutController.navigate("/FXML/pages/ClientBoutique.fxml", "Boutique", null);
        }
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    private void refresh() {
        ordersContainer.getChildren().clear();
        int clientId = SessionContext.getInstance().getClientDatabaseId();
        try {
            List<commandes> liste = commandesService.afficherParClient(clientId);
            if (liste.isEmpty()) {
                Label empty = new Label("Vous n’avez pas encore passé de commande.");
                empty.getStyleClass().add("page-hero-sub");
                empty.setWrapText(true);
                ordersContainer.getChildren().add(empty);
                return;
            }
            for (commandes c : liste) {
                try {
                    ordersContainer.getChildren().add(buildOrderCard(c));
                } catch (SQLException ex) {
                    error("Erreur lors du chargement d’une commande : " + ex.getMessage());
                    break;
                }
            }
        } catch (SQLException e) {
            Label err = new Label("Impossible de charger les commandes : " + e.getMessage());
            err.setWrapText(true);
            err.setStyle("-fx-text-fill: #ef9a9a;");
            ordersContainer.getChildren().add(err);
        }
    }

    private VBox buildOrderCard(commandes c) throws SQLException {
        VBox card = new VBox(12);
        card.getStyleClass().add("saas-chart-card");
        card.setPadding(new Insets(16, 18, 18, 18));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label idLbl = new Label("Commande #" + c.getId_commande());
        idLbl.getStyleClass().add("saas-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statutLbl = new Label(c.getStatut_commande() != null ? c.getStatut_commande() : "—");
        if (c.getStatut_commande() != null && c.getStatut_commande().toLowerCase().contains("valid")) {
            statutLbl.setStyle("-fx-background-color: rgba(46, 125, 50, 0.35); -fx-text-fill: #c8e6c9; -fx-padding: 4 12; -fx-background-radius: 8px;");
        } else if (c.getStatut_commande() != null && c.getStatut_commande().toLowerCase().contains("annul")) {
            statutLbl.setStyle("-fx-background-color: rgba(198, 40, 40, 0.35); -fx-text-fill: #ffcdd2; -fx-padding: 4 12; -fx-background-radius: 8px;");
        } else {
            statutLbl.setStyle("-fx-background-color: rgba(0, 191, 255, 0.15); -fx-text-fill: #b3e5fc; -fx-padding: 4 12; -fx-background-radius: 8px;");
        }

        header.getChildren().addAll(idLbl, spacer, statutLbl);

        Label meta = new Label(String.format("Date : %s  ·  Paiement : %s",
                c.getDate_commande() != null ? c.getDate_commande() : "—",
                c.getMode_paiement_commande() != null ? c.getMode_paiement_commande() : "—"));
        meta.getStyleClass().add("saas-stat-hint");
        meta.setWrapText(true);

        Label total = new Label(String.format("Total : %.2f TND", c.getTotal_commande()));
        total.getStyleClass().add("saas-stat-value");

        Label addrTitle = new Label("Adresse de livraison");
        addrTitle.getStyleClass().add("saas-stat-label");
        Label addr = new Label(c.getAdresse_commande() != null ? c.getAdresse_commande() : "—");
        addr.getStyleClass().add("page-hero-sub");
        addr.setWrapText(true);

        Separator sep = new Separator();

        Label lignesTitle = new Label("Détail des articles");
        lignesTitle.getStyleClass().add("saas-stat-label");

        VBox lignesBox = new VBox(6);
        List<LigneCommandeAffichage> lignes = commandesService.getLignesPourCommande(c.getId_commande());
        if (lignes.isEmpty()) {
            lignesBox.getChildren().add(new Label("(Aucune ligne enregistrée)"));
        } else {
            for (LigneCommandeAffichage l : lignes) {
                Label line = new Label(String.format("· %s  × %d  →  %.2f TND",
                        l.getNomProduit(), l.getQuantite(), l.getSousTotal()));
                line.getStyleClass().add("saas-stat-hint");
                line.setWrapText(true);
                lignesBox.getChildren().add(line);
            }
        }

        card.getChildren().addAll(header, meta, total, addrTitle, addr, sep, lignesTitle, lignesBox);

        if (commandesService.peutAnnuler(c)) {
            Button annuler = new Button("Annuler la commande");
            annuler.getStyleClass().add("danger-toolbar-btn");
            annuler.setOnAction(e -> confirmAnnuler(c.getId_commande()));
            HBox actions = new HBox(annuler);
            actions.setAlignment(Pos.CENTER_RIGHT);
            card.getChildren().add(actions);
        }

        return card;
    }

    private static void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void confirmAnnuler(int idCommande) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annulation");
        confirm.setHeaderText(null);
        confirm.setContentText("Confirmer l’annulation de cette commande ? Cette action est autorisée uniquement dans les 24 heures suivant la validation.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }
        int clientId = SessionContext.getInstance().getClientDatabaseId();
        try {
            boolean ok = commandesService.annulerCommande(idCommande, clientId);
            if (ok) {
                info("La commande a été annulée.");
                refresh();
            } else {
                warn("Annulation impossible (délai dépassé, statut déjà modifié, ou commande introuvable).");
            }
        } catch (SQLException ex) {
            error("Erreur : " + ex.getMessage());
        }
    }
}
