package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.entities.produits;
import org.example.services.ProduitsService;
import org.example.utils.TexteRecherche;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BoutiqueController {

    @FXML
    private ListView<produits> produitsListView;
    @FXML
    private TextField rechercheProduits;
    @FXML
    private ComboBox<String> triProduits;

    private final ProduitsService produitsService = new ProduitsService();
    private final List<produits> tousLesProduits = new ArrayList<>();
    private MainLayoutController mainLayoutController;

    public BoutiqueController() {
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    @FXML
    public void initialize() {
        if (triProduits != null) {
            triProduits.getItems().setAll(
                    "Nom (A → Z)",
                    "Nom (Z → A)",
                    "Prix (croissant)",
                    "Prix (décroissant)",
                    "Stock (croissant)",
                    "Stock (décroissant)");
            triProduits.getSelectionModel().selectFirst();
        }
        if (rechercheProduits != null) {
            rechercheProduits.textProperty().addListener((o, a, b) -> appliquerFiltreEtTriProduits());
        }
        setupListViewCellFactory();
        chargerProduitsDepuisBase();
    }

    private void setupListViewCellFactory() {
        produitsListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(produits produit, boolean empty) {
                super.updateItem(produit, empty);
                if (empty || produit == null) {
                    setGraphic(null);
                } else {
                    VBox content = new VBox(8);
                    content.getStyleClass().add("boutique-pro-list-row");

                    Label nomLabel = new Label("Nom : " + produit.getNom_produit());
                    nomLabel.getStyleClass().add("boutique-pro-name");

                    Label descriptionLabel = new Label("Description : " + produit.getDescription_produit());
                    descriptionLabel.getStyleClass().add("boutique-pro-desc");
                    descriptionLabel.setWrapText(true);

                    Label prixLabel = new Label("Prix : " + produit.getPrix_produit() + " TND");
                    prixLabel.getStyleClass().add("boutique-pro-price");

                    Label stockLabel = new Label("Stock : " + produit.getQuantite_stock_produit());
                    stockLabel.getStyleClass().add(
                            produit.getQuantite_stock_produit() > 5 ? "boutique-pro-stock-ok" : "boutique-pro-stock-low");

                    Label statutLabel = new Label("Statut : " + produit.getStatut_produit());
                    statutLabel.getStyleClass().add("boutique-pro-statut");

                    HBox buttonsBox = new HBox(10);
                    buttonsBox.getStyleClass().add("boutique-pro-actions");

                    Button modifierBtn = new Button("Modifier");
                    modifierBtn.getStyleClass().add("boutique-pro-btn-edit");
                    modifierBtn.setOnAction(e -> handleModifier(produit));

                    Button supprimerBtn = new Button("Supprimer");
                    supprimerBtn.getStyleClass().add("boutique-pro-btn-del");
                    supprimerBtn.setOnAction(e -> handleSupprimer(produit));

                    buttonsBox.getChildren().addAll(modifierBtn, supprimerBtn);

                    content.getChildren().addAll(nomLabel, descriptionLabel, prixLabel, stockLabel, statutLabel, buttonsBox);
                    setGraphic(content);
                }
            }
        });
    }

    private void chargerProduitsDepuisBase() {
        try {
            tousLesProduits.clear();
            tousLesProduits.addAll(produitsService.afficher());
            if (triProduits != null && triProduits.getSelectionModel().getSelectedIndex() < 0) {
                triProduits.getSelectionModel().selectFirst();
            }
            appliquerFiltreEtTriProduits();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les produits: " + e.getMessage());
        }
    }

    @FXML
    private void handleRechercheOuTriProduits() {
        appliquerFiltreEtTriProduits();
    }

    private void appliquerFiltreEtTriProduits() {
        String q = rechercheProduits != null ? rechercheProduits.getText() : "";
        String tri = triProduits != null && triProduits.getValue() != null
                ? triProduits.getValue()
                : "Nom (A → Z)";

        List<produits> filtered = new ArrayList<>();
        for (produits p : tousLesProduits) {
            if (produitCorrespond(p, q)) {
                filtered.add(p);
            }
        }

        Comparator<produits> cmp = comparateurTriProduits(tri);
        filtered.sort(cmp);

        ObservableList<produits> obs = FXCollections.observableArrayList(filtered);
        produitsListView.setItems(obs);
    }

    private static boolean produitCorrespond(produits p, String q) {
        String bloc = String.join(" ",
                p.getNom_produit() != null ? p.getNom_produit() : "",
                p.getDescription_produit() != null ? p.getDescription_produit() : "",
                p.getStatut_produit() != null ? p.getStatut_produit() : "",
                String.valueOf(p.getPrix_produit()),
                String.valueOf(p.getQuantite_stock_produit()));
        return TexteRecherche.correspond(bloc, q);
    }

    private static Comparator<produits> comparateurTriProduits(String libelle) {
        if (libelle == null) {
            return Comparator.comparing(produits::getNom_produit, String.CASE_INSENSITIVE_ORDER);
        }
        return switch (libelle) {
            case "Nom (Z → A)" -> Comparator.comparing(
                    (produits p) -> p.getNom_produit() != null ? p.getNom_produit() : "",
                    String.CASE_INSENSITIVE_ORDER).reversed();
            case "Prix (croissant)" -> Comparator.comparingDouble(produits::getPrix_produit);
            case "Prix (décroissant)" -> Comparator.comparingDouble(produits::getPrix_produit).reversed();
            case "Stock (croissant)" -> Comparator.comparingInt(produits::getQuantite_stock_produit);
            case "Stock (décroissant)" -> Comparator.comparingInt(produits::getQuantite_stock_produit).reversed();
            default -> Comparator.comparing(
                    (produits p) -> p.getNom_produit() != null ? p.getNom_produit() : "",
                    String.CASE_INSENSITIVE_ORDER);
        };
    }

    @FXML
    private void handleAjouterProduit() {
        try {
            if (mainLayoutController != null) {
                mainLayoutController.navigate("/FXML/pages/AjouterProduitPage.fxml", "Ajouter un Produit", null);
            } else {
                showAlert("Erreur", "MainLayoutController non disponible");
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir la page d'ajout: " + e.getMessage());
        }
    }

    private void handleModifier(produits produit) {
        try {
            if (mainLayoutController != null) {
                ModifierProduitPageController.setProduitTemporaire(produit);
                mainLayoutController.navigate("/FXML/pages/ModifierProduitPage.fxml", "Modifier un Produit", null);
            } else {
                showAlert("Erreur", "MainLayoutController non disponible");
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir la page de modification: " + e.getMessage());
        }
    }

    private void handleSupprimer(produits produit) {
        try {
            produitsService.supprimer(produit.getId_produit());
            showAlert("Succès", "Produit supprimé avec succès");
            chargerProduitsDepuisBase();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de supprimer le produit: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        if (rechercheProduits != null) {
            rechercheProduits.clear();
        }
        chargerProduitsDepuisBase();
    }

    @FXML
    private void handleVoirCommandes() {
        if (mainLayoutController != null) {
            mainLayoutController.navigateToAdminCommandes();
        } else {
            showAlert("Erreur", "Navigation indisponible.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
