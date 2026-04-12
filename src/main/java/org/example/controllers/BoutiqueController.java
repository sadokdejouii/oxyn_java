package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entities.produits;
import org.example.services.ProduitsService;

import java.sql.SQLException;
import java.util.List;

public class BoutiqueController {

    @FXML
    private ListView<produits> produitsListView;

    private ProduitsService produitsService;
    private MainLayoutController mainLayoutController;

    public BoutiqueController() {
        this.produitsService = new ProduitsService();
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    @FXML
    public void initialize() {
        loadProduits();
        setupListViewCellFactory();
    }

    private void loadProduits() {
        try {
            List<produits> produitsList = produitsService.afficher();
            produitsListView.getItems().clear();
            produitsListView.getItems().addAll(produitsList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les produits: " + e.getMessage());
        }
    }

    private void setupListViewCellFactory() {
        produitsListView.setCellFactory(param -> new ListCell<produits>() {
            @Override
            protected void updateItem(produits produit, boolean empty) {
                super.updateItem(produit, empty);
                if (empty || produit == null) {
                    setGraphic(null);
                } else {
                    VBox content = new VBox(5);
                    content.setStyle("-fx-padding: 10;");
                    
                    // Informations du produit
                    Label nomLabel = new Label("Nom: " + produit.getNom_produit());
                    nomLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #FFFFFF;");
                    
                    Label descriptionLabel = new Label("Description: " + produit.getDescription_produit());
                    descriptionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #E0E0E0; -fx-wrap-text: true;");
                    
                    Label prixLabel = new Label("Prix: " + produit.getPrix_produit() + " TND");
                    prixLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4FC3F7; -fx-font-weight: bold;");
                    
                    Label stockLabel = new Label("Stock: " + produit.getQuantite_stock_produit());
                    stockLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + 
                        (produit.getQuantite_stock_produit() > 5 ? "#81C784" : "#FF8A80") + "; -fx-font-weight: bold;");
                    
                    Label statutLabel = new Label("Statut: " + produit.getStatut_produit());
                    statutLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #B0BEC5;");
                    
                    // Boutons d'action
                    HBox buttonsBox = new HBox(10);
                    buttonsBox.setStyle("-fx-alignment: center-left;");
                    
                    Button modifierBtn = new Button("Modifier");
                    modifierBtn.getStyleClass().add("modifier-btn");
                    modifierBtn.setStyle("-fx-pref-width: 80; -fx-pref-height: 30;");
                    modifierBtn.setOnAction(e -> handleModifier(produit));
                    
                    Button supprimerBtn = new Button("Supprimer");
                    supprimerBtn.getStyleClass().add("supprimer-btn");
                    supprimerBtn.setStyle("-fx-pref-width: 80; -fx-pref-height: 30;");
                    supprimerBtn.setOnAction(e -> handleSupprimer(produit));
                    
                    buttonsBox.getChildren().addAll(modifierBtn, supprimerBtn);
                    
                    content.getChildren().addAll(nomLabel, descriptionLabel, prixLabel, stockLabel, statutLabel, buttonsBox);
                    setGraphic(content);
                }
            }
        });
    }

    @FXML
    private void handleAjouterProduit() {
        try {
            // Naviguer vers la page d'ajout
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
            // Naviguer vers la page de modification
            if (mainLayoutController != null) {
                // Stocker le produit à modifier dans une variable statique temporaire
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
            loadProduits();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de supprimer le produit: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadProduits();
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
