package org.example.controllers;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import org.example.entities.LignePanier;
import org.example.entities.PanierSession;
import org.example.entities.commandes;
import org.example.services.CommandesService;
import org.example.services.CurrencyExchangeService;
import org.example.services.SessionContext;
import org.example.services.StripePaymentService;
import org.example.services.StripePaymentSession;
import org.example.services.UserRole;
import org.example.utils.AdresseCommandeValidator;
import org.example.utils.CommandeClientResolver;
import org.example.utils.ProductImageStorage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PanierController {

    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    private VBox panierLignesContainer;
    @FXML
    private Label panierCountLabel;
    @FXML
    private TextArea adresseField;
    @FXML
    private ComboBox<String> paiementCombo;
    @FXML
    private ComboBox<String> devisePanierCombo;
    @FXML
    private Label totalLabel;
    @FXML
    private Button validerBtn;

    private MainLayoutController mainLayoutController;

    private final PanierSession panier = PanierSession.getInstance();
    private final CurrencyExchangeService currencyExchangeService = new CurrencyExchangeService();

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    @FXML
    public void initialize() {
        paiementCombo.setItems(FXCollections.observableArrayList("En ligne", "En espèces"));
        paiementCombo.getSelectionModel().selectFirst();
        if (devisePanierCombo != null) {
            devisePanierCombo.setItems(FXCollections.observableArrayList("TND", "EUR", "USD"));
            devisePanierCombo.getSelectionModel().select("TND");
        }
        AdresseCommandeValidator.appliquerLimiteLongueur(adresseField);
        rafraichirPanier();
    }

    @FXML
    private void handleRefreshCurrency() {
        rafraichirPanier();
    }

    @FXML
    private void handleRetourBoutique() {
        if (mainLayoutController != null) {
            mainLayoutController.navigate("/FXML/pages/ClientBoutique.fxml", "Boutique", null);
        }
    }

    @FXML
    private void handleValiderCommande() {
        validerCommande();
    }

    @FXML
    private void handleViderPanier() {
        if (panier.estVide()) {
            showAlert(Alert.AlertType.INFORMATION, "Panier", "Votre panier est déjà vide.");
            return;
        }
        panier.viderPanier();
        adresseField.clear();
        rafraichirPanier();
        showAlert(Alert.AlertType.INFORMATION, "Panier", "Le panier a été vidé.");
    }

    private void rafraichirPanier() {
        List<LignePanier> lignes = panier.getLignes();
        panierLignesContainer.getChildren().clear();

        int n = lignes.size();
        if (panierCountLabel != null) {
            panierCountLabel.setText(n == 0 ? "Panier vide" : (n == 1 ? "1 article" : n + " articles"));
        }

        if (lignes.isEmpty()) {
            panierLignesContainer.setAlignment(Pos.CENTER);
            Label empty = new Label("Aucun article pour le moment.\nAjoutez des produits depuis la boutique.");
            empty.getStyleClass().add("panier-vide-placeholder");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            panierLignesContainer.getChildren().add(empty);
        } else {
            panierLignesContainer.setAlignment(Pos.TOP_LEFT);
            for (LignePanier ligne : lignes) {
                panierLignesContainer.getChildren().add(creerCarteLigne(ligne));
            }
        }

        totalLabel.setText(formatFromTnd(panier.getTotal()));
        validerBtn.setDisable(lignes.isEmpty());
    }

    private HBox creerCarteLigne(LignePanier ligne) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("shop-client-panier-row");

        VBox thumbWrap = new VBox();
        thumbWrap.setAlignment(Pos.CENTER);
        thumbWrap.getStyleClass().add("shop-client-thumb-wrap");

        ImageView thumb = new ImageView();
        thumb.getStyleClass().add("shop-client-thumb");
        thumb.setFitWidth(62);
        thumb.setFitHeight(62);
        thumb.setPreserveRatio(true);
        thumb.setSmooth(true);
        if (ligne.getProduit() != null) {
            ProductImageStorage.applyToImageView(thumb, ligne.getProduit().getImage_produit());
        }
        thumbWrap.getChildren().add(thumb);

        VBox infos = new VBox(6);
        HBox.setHgrow(infos, Priority.ALWAYS);

        Label nom = new Label(ligne.getNomProduit());
        nom.getStyleClass().add("shop-client-line-nom");
        nom.setWrapText(true);

        HBox meta = new HBox(10);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.getStyleClass().add("shop-client-line-meta");

        Label qteBadge = new Label("Qté " + ligne.getQuantite());
        qteBadge.getStyleClass().add("shop-client-qte");

        Label detail = new Label("×  " + formatFromTnd(ligne.getPrixUnitaire()) + "  l’unité");
        detail.getStyleClass().add("shop-client-unit-price");

        meta.getChildren().addAll(qteBadge, detail);
        infos.getChildren().addAll(nom, meta);

        VBox prixCol = new VBox(2);
        prixCol.setAlignment(Pos.CENTER_RIGHT);
        Label sous = new Label(formatFromTnd(ligne.getSousTotal()));
        sous.getStyleClass().add("shop-client-sous-total");
        Label hint = new Label("Sous-total");
        hint.getStyleClass().add("shop-client-sous-hint");
        prixCol.getChildren().addAll(sous, hint);

        card.getChildren().addAll(thumbWrap, infos, prixCol);
        return card;
    }

    private void validerCommande() {
        String rawAdresse = adresseField.getText();
        String erreurAdresse = AdresseCommandeValidator.valider(rawAdresse);
        String modePaiement = paiementCombo.getValue();
        if (modePaiement == null) {
            showAlert(Alert.AlertType.WARNING, "Champs requis", "Veuillez choisir un mode de paiement.");
            return;
        }
        if (erreurAdresse != null) {
            showAlert(Alert.AlertType.WARNING, "Adresse de livraison", erreurAdresse);
            adresseField.requestFocus();
            return;
        }
        String adresse = AdresseCommandeValidator.formaterPourEnregistrement(rawAdresse);
        if (panier.estVide()) {
            showAlert(Alert.AlertType.INFORMATION, "Panier vide", "Ajoutez des produits depuis la boutique avant de valider.");
            return;
        }
        SessionContext session = SessionContext.getInstance();
        if (session.getRole() != UserRole.CLIENT) {
            showAlert(Alert.AlertType.WARNING, "Compte non autorisé", "Seuls les clients peuvent valider une commande depuis le panier.");
            return;
        }
        int idClient = CommandeClientResolver.idClientConnecte();
        if (idClient <= 0) {
            showAlert(Alert.AlertType.ERROR, "Session", "Identifiant client introuvable. Déconnectez-vous puis reconnectez-vous.");
            return;
        }
        commandes commande = new commandes(
                LocalDateTime.now().format(DATE_HEURE),
                panier.getTotal(),
                modePaiement.equalsIgnoreCase("En ligne") ? "en attente" : "validée",
                modePaiement,
                idClient,
                adresse
        );
        CommandesService commandesService = new CommandesService();
        boolean success = commandesService.ajouterCommande(commande, panier.getLignes());
        if (success) {
            if (modePaiement.equalsIgnoreCase("En ligne")) {
                ouvrirPaiementStripe(commande, idClient);
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Commande enregistrée", "Votre commande a été validée. Merci pour votre achat !");
                panier.viderPanier();
                rafraichirPanier();
                adresseField.clear();
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Commande impossible",
                    "L’enregistrement a échoué : stock insuffisant pour un ou plusieurs produits, ou erreur de base de données. Vérifiez les quantités disponibles.");
        }
    }

    private void ouvrirPaiementStripe(commandes commande, int idClient) {
        StripePaymentService stripePaymentService = new StripePaymentService();
        try {
            StripePaymentService.PaymentIntentData paymentIntentData =
                    stripePaymentService.createPaymentIntentForCommande(commande.getId_commande(), commande.getTotal_commande());
            StripePaymentSession.getInstance().start(
                    commande.getId_commande(),
                    idClient,
                    commande.getTotal_commande(),
                    paymentIntentData.clientSecret(),
                    paymentIntentData.publishableKey()
            );
            panier.viderPanier();
            rafraichirPanier();
            adresseField.clear();
            if (mainLayoutController != null) {
                mainLayoutController.navigate("/FXML/pages/PaiementEnLigne.fxml", "Paiement en ligne", null);
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.WARNING,
                    "Commande en attente",
                    "La commande a été enregistrée en attente de paiement, mais l'initialisation Stripe a échoué : "
                            + ex.getMessage()
                            + ". Vous pourrez relancer le paiement plus tard.");
        }
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String selectedCurrency() {
        if (devisePanierCombo == null || devisePanierCombo.getValue() == null) {
            return "TND";
        }
        return devisePanierCombo.getValue();
    }

    private String formatFromTnd(double amountTnd) {
        return currencyExchangeService.formatFromTnd(amountTnd, selectedCurrency());
    }
}
