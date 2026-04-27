package org.example.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.example.entities.PanierSession;
import org.example.entities.produits;
import org.example.services.ProduitRecommendationService;
import org.example.services.ProduitsService;
import org.example.utils.CommandeClientResolver;
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
    @FXML
    private TilePane recommandationsContainer;
    @FXML
    private Label recoHintLabel;

    private final ProduitsService produitsService = new ProduitsService();
    private final ProduitRecommendationService recommendationService = new ProduitRecommendationService();
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
            chargerRecommandations();
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
        chargerRecommandations();
    }

    private void chargerRecommandations() {
        if (recommandationsContainer == null) {
            return;
        }
        recommandationsContainer.getChildren().clear();
        int clientId = CommandeClientResolver.idClientConnecte();
        if (clientId <= 0) {
            if (recoHintLabel != null) {
                recoHintLabel.setText("Connectez-vous en compte client pour des recommandations personnalisées.");
            }
            return;
        }

        String q = rechercheProduitsClient != null ? rechercheProduitsClient.getText() : "";
        try {
            List<produits> recos = recommendationService.recommanderPourClient(clientId, q, 3);
            if (recos.isEmpty()) {
                if (recoHintLabel != null) {
                    recoHintLabel.setText("Aucune recommandation disponible pour le moment.");
                }
                return;
            }
            if (recoHintLabel != null) {
                recoHintLabel.setText("Sélection IA basée sur vos commandes précédentes et vos mots-clés de recherche.");
            }
            for (produits p : recos) {
                recommandationsContainer.getChildren().add(createProductCard(p));
            }
        } catch (SQLException e) {
            if (recoHintLabel != null) {
                recoHintLabel.setText("Recommandations indisponibles: " + e.getMessage());
            }
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
        VBox card = new VBox(0);
        card.getStyleClass().add("shop-client-card");

        boolean enStock = produit.getQuantite_stock_produit() > 0;

        StackPane imgWrap = new StackPane();
        imgWrap.getStyleClass().add("shop-client-img-wrap");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(252);
        imageView.setFitHeight(148);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("shop-client-img");
        ProductImageStorage.applyToImageView(imageView, produit.getImage_produit());
        imgWrap.getChildren().add(imageView);

        if (!enStock) {
            Label ruptureBadge = new Label("RUPTURE");
            ruptureBadge.getStyleClass().add("shop-client-rupture-badge");
            StackPane.setAlignment(ruptureBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(ruptureBadge, new Insets(6, 10, 0, 0));
            imgWrap.getChildren().add(ruptureBadge);
        }

        VBox body = new VBox(6);
        body.getStyleClass().add("shop-client-body");

        Label brandLabel = new Label("OXYN");
        brandLabel.getStyleClass().add("shop-client-brand");

        Label nameLabel = new Label(produit.getNom_produit() != null ? produit.getNom_produit() : "—");
        nameLabel.getStyleClass().add("shop-client-name");
        nameLabel.setWrapText(true);

        Label ratingLabel = new Label("★★★★★  (24 avis)");
        ratingLabel.getStyleClass().add("shop-client-rating");

        Label priceLabel = new Label(String.format("%.2f TND", produit.getPrix_produit()));
        priceLabel.getStyleClass().add("shop-client-price");

        Label stockLabel = new Label(enStock ? "En stock" : "Rupture de stock");
        stockLabel.getStyleClass().addAll("shop-client-stock-pill", enStock ? "shop-client-stock-ok" : "shop-client-stock-out");

        body.getChildren().addAll(brandLabel, nameLabel, ratingLabel, priceLabel, stockLabel);

        HBox actionsBox = new HBox(10);
        actionsBox.getStyleClass().add("shop-client-actions");

        Button voirBtn = new Button("Voir");
        voirBtn.getStyleClass().addAll("shop-client-btn", "shop-client-btn-view");
        voirBtn.setOnAction(e -> handleVoirProduit(produit));

        if (enStock) {
            Button addBtn = new Button("Ajouter");
            addBtn.getStyleClass().addAll("shop-client-btn", "shop-client-btn-add");
            addBtn.setOnAction(e -> handleAjouterPanier(produit));
            actionsBox.getChildren().addAll(voirBtn, addBtn);
        } else {
            Button ruptureBtn = new Button("Indisponible");
            ruptureBtn.getStyleClass().addAll("shop-client-btn", "shop-client-btn-rupture");
            ruptureBtn.setOnAction(e -> handleRupture(produit));
            actionsBox.getChildren().addAll(voirBtn, ruptureBtn);
        }

        card.getChildren().addAll(imgWrap, body, actionsBox);
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
