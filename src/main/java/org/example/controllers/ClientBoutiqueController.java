package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.example.entities.PanierSession;
import org.example.entities.produits;
import org.example.services.ProduitsService;
import org.example.utils.ProductImageStorage;

import java.sql.SQLException;
import java.util.List;

public class ClientBoutiqueController {

    @FXML
    private TilePane produitsContainer;

    private ProduitsService produitsService;
    private MainLayoutController mainLayoutController;
    private final PanierSession panier = PanierSession.getInstance();

    public ClientBoutiqueController() {
        this.produitsService = new ProduitsService();
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    @FXML
    public void initialize() {
        loadProduits();
    }

    private void loadProduits() {
        try {
            List<produits> produitsList = produitsService.afficher();
            produitsContainer.getChildren().clear();
            
            for (produits produit : produitsList) {
                produitsContainer.getChildren().add(createProductCard(produit));
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les produits: " + e.getMessage());
        }
    }

    private VBox createProductCard(produits produit) {
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card");
        
        ImageView imageView = new ImageView();
        imageView.setFitWidth(250);
        imageView.setFitHeight(150);
        imageView.getStyleClass().add("product-image");
        ProductImageStorage.applyToImageView(imageView, produit.getImage_produit());
        
        // Marque et nom
        Label brandLabel = new Label("OXYN");
        brandLabel.getStyleClass().add("product-brand");
        
        Label nameLabel = new Label(produit.getNom_produit());
        nameLabel.getStyleClass().add("product-name");
        
        // Rating (placeholder)
        Label ratingLabel = new Label("***** (24 avis)");
        ratingLabel.getStyleClass().add("product-rating");
        
        // Prix
        Label priceLabel = new Label(String.format("%.2f TND", produit.getPrix_produit()));
        priceLabel.getStyleClass().add("product-price");
        
        // Stock
        boolean enStock = produit.getQuantite_stock_produit() > 0;
        Label stockLabel = new Label(enStock ? "En stock" : "Rupture de stock");
        stockLabel.getStyleClass().addAll("product-stock", enStock ? "stock-available" : "stock-out");
        
        // Badge rupture si nécessaire
        AnchorPane cardContent = new AnchorPane();
        
        if (!enStock) {
            Label ruptureBadge = new Label("RUPTURE");
            ruptureBadge.getStyleClass().add("rupture-badge");
            AnchorPane.setTopAnchor(ruptureBadge, 5.0);
            AnchorPane.setRightAnchor(ruptureBadge, 5.0);
            cardContent.getChildren().add(ruptureBadge);
        }
        
        // Positionner les éléments
        AnchorPane.setTopAnchor(imageView, 10.0);
        AnchorPane.setLeftAnchor(imageView, 15.0);
        AnchorPane.setTopAnchor(brandLabel, 170.0);
        AnchorPane.setLeftAnchor(brandLabel, 15.0);
        AnchorPane.setTopAnchor(nameLabel, 190.0);
        AnchorPane.setLeftAnchor(nameLabel, 15.0);
        AnchorPane.setTopAnchor(ratingLabel, 220.0);
        AnchorPane.setLeftAnchor(ratingLabel, 15.0);
        AnchorPane.setTopAnchor(priceLabel, 245.0);
        AnchorPane.setLeftAnchor(priceLabel, 15.0);
        AnchorPane.setTopAnchor(stockLabel, 275.0);
        AnchorPane.setLeftAnchor(stockLabel, 15.0);
        
        cardContent.getChildren().addAll(imageView, brandLabel, nameLabel, ratingLabel, priceLabel, stockLabel);
        
        // Boutons d'action
        HBox actionsBox = new HBox(8);
        actionsBox.getStyleClass().add("product-actions");
        actionsBox.setStyle("-fx-alignment: center;");
        
        Button voirBtn = new Button("VOIR");
        voirBtn.getStyleClass().addAll("product-btn", "btn-view");
        voirBtn.setStyle("-fx-pref-width: 70; -fx-pref-height: 28;");
        voirBtn.setOnAction(e -> handleVoirProduit(produit));
        
        if (enStock) {
            Button addBtn = new Button("AJOUTER");
            addBtn.getStyleClass().addAll("product-btn", "btn-add");
            addBtn.setStyle("-fx-pref-width: 100; -fx-pref-height: 28;");
            addBtn.setOnAction(e -> handleAjouterPanier(produit));
            actionsBox.getChildren().addAll(voirBtn, addBtn);
        } else {
            Button ruptureBtn = new Button("RUPTURE");
            ruptureBtn.getStyleClass().addAll("product-btn", "btn-rupture");
            ruptureBtn.setStyle("-fx-pref-width: 100; -fx-pref-height: 28;");
            ruptureBtn.setOnAction(e -> handleRupture(produit));
            actionsBox.getChildren().addAll(voirBtn, ruptureBtn);
        }
        
        AnchorPane.setBottomAnchor(actionsBox, -40.0);
        AnchorPane.setLeftAnchor(actionsBox, 15.0);
        AnchorPane.setRightAnchor(actionsBox, 15.0);
        cardContent.getChildren().add(actionsBox);
        
        card.getChildren().add(cardContent);
        
        return card;
    }

    @FXML
    private void handleRefresh() {
        loadProduits();
    }

    @FXML
    private void handlePanier() {
        if (mainLayoutController == null) {
            return;
        }
        mainLayoutController.navigate("/FXML/pages/Panier.fxml", "Mon panier", null);
    }

    @FXML
    private void handleMesCommandes() {
        if (mainLayoutController == null) {
            return;
        }
        mainLayoutController.navigate("/FXML/pages/MesCommandes.fxml", "Mes commandes", null);
    }
    
    private void handleVoirProduit(produits produit) {
        showAlert("Détails du produit", "Fonctionnalité de détails à implémenter pour: " + produit.getNom_produit());
        // TODO: Implémenter la page de détails
    }
    
    private void handleAjouterPanier(produits produit) {
        panier.ajouterProduit(produit);
        showAlert("Panier", "« " + produit.getNom_produit() + " » a été ajouté à votre panier.");
    }
    
    private void handleRupture(produits produit) {
        showAlert("Rupture de stock", "Ce produit est actuellement en rupture de stock: " + produit.getNom_produit());
        // TODO: Implémenter les notifications de rupture
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
