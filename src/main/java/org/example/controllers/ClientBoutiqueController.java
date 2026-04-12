package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.example.entities.PanierSession;
import org.example.entities.produits;
import org.example.services.ProduitsService;
import org.example.utils.ProductImageStorage;
import org.example.utils.TexteRecherche;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ClientBoutiqueController {

    @FXML
    private TilePane produitsContainer;
    @FXML
    private TextField rechercheProduitsClient;
    @FXML
    private ComboBox<String> triProduitsClient;

    private final ProduitsService produitsService = new ProduitsService();
    private final List<produits> tousLesProduits = new ArrayList<>();
    private MainLayoutController mainLayoutController;
    private final PanierSession panier = PanierSession.getInstance();

    public ClientBoutiqueController() {
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    @FXML
    public void initialize() {
        if (triProduitsClient != null) {
            triProduitsClient.getItems().setAll(
                    "Nom (A → Z)",
                    "Nom (Z → A)",
                    "Prix (croissant)",
                    "Prix (décroissant)",
                    "Stock (croissant)",
                    "Stock (décroissant)");
            triProduitsClient.getSelectionModel().selectFirst();
        }
        if (rechercheProduitsClient != null) {
            rechercheProduitsClient.textProperty().addListener((o, a, b) -> appliquerFiltreEtTri());
        }
        chargerProduitsDepuisBase();
    }

    private void chargerProduitsDepuisBase() {
        try {
            tousLesProduits.clear();
            tousLesProduits.addAll(produitsService.afficher());
            if (triProduitsClient != null && triProduitsClient.getSelectionModel().getSelectedIndex() < 0) {
                triProduitsClient.getSelectionModel().selectFirst();
            }
            appliquerFiltreEtTri();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les produits: " + e.getMessage());
        }
    }

    @FXML
    private void handleRechercheOuTriClient() {
        appliquerFiltreEtTri();
    }

    private void appliquerFiltreEtTri() {
        String q = rechercheProduitsClient != null ? rechercheProduitsClient.getText() : "";
        String tri = triProduitsClient != null && triProduitsClient.getValue() != null
                ? triProduitsClient.getValue()
                : "Nom (A → Z)";

        List<produits> filtered = new ArrayList<>();
        for (produits p : tousLesProduits) {
            if (produitCorrespond(p, q)) {
                filtered.add(p);
            }
        }
        filtered.sort(comparateurTri(tri));

        produitsContainer.getChildren().clear();
        for (produits produit : filtered) {
            produitsContainer.getChildren().add(createProductCard(produit));
        }
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

    private static Comparator<produits> comparateurTri(String libelle) {
        if (libelle == null) {
            return Comparator.comparing(
                    (produits p) -> p.getNom_produit() != null ? p.getNom_produit() : "",
                    String.CASE_INSENSITIVE_ORDER);
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

    private VBox createProductCard(produits produit) {
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(250);
        imageView.setFitHeight(150);
        imageView.getStyleClass().add("product-image");
        ProductImageStorage.applyToImageView(imageView, produit.getImage_produit());

        Label brandLabel = new Label("OXYN");
        brandLabel.getStyleClass().add("product-brand");

        Label nameLabel = new Label(produit.getNom_produit());
        nameLabel.getStyleClass().add("product-name");

        Label ratingLabel = new Label("***** (24 avis)");
        ratingLabel.getStyleClass().add("product-rating");

        Label priceLabel = new Label(String.format("%.2f TND", produit.getPrix_produit()));
        priceLabel.getStyleClass().add("product-price");

        boolean enStock = produit.getQuantite_stock_produit() > 0;
        Label stockLabel = new Label(enStock ? "En stock" : "Rupture de stock");
        stockLabel.getStyleClass().addAll("product-stock", enStock ? "stock-available" : "stock-out");

        AnchorPane cardContent = new AnchorPane();

        if (!enStock) {
            Label ruptureBadge = new Label("RUPTURE");
            ruptureBadge.getStyleClass().add("rupture-badge");
            AnchorPane.setTopAnchor(ruptureBadge, 5.0);
            AnchorPane.setRightAnchor(ruptureBadge, 5.0);
            cardContent.getChildren().add(ruptureBadge);
        }

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
        if (rechercheProduitsClient != null) {
            rechercheProduitsClient.clear();
        }
        chargerProduitsDepuisBase();
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
    }

    private void handleAjouterPanier(produits produit) {
        panier.ajouterProduit(produit);
        showAlert("Panier", "« " + produit.getNom_produit() + " » a été ajouté à votre panier.");
    }

    private void handleRupture(produits produit) {
        showAlert("Rupture de stock", "Ce produit est actuellement en rupture de stock: " + produit.getNom_produit());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
