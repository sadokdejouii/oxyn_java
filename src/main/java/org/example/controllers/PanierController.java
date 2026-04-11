package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.entities.LignePanier;
import org.example.entities.PanierSession;
import org.example.entities.commandes;
import org.example.services.CommandesService;
import org.example.services.SessionContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PanierController {

    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    private TableView<LignePanier> tablePanier;
    @FXML
    private TableColumn<LignePanier, String> colNom;
    @FXML
    private TableColumn<LignePanier, Double> colPrix;
    @FXML
    private TableColumn<LignePanier, Integer> colQte;
    @FXML
    private TableColumn<LignePanier, Double> colSousTotal;
    @FXML
    private TextArea adresseField;
    @FXML
    private ComboBox<String> paiementCombo;
    @FXML
    private Label totalLabel;
    @FXML
    private Button validerBtn;

    private MainLayoutController mainLayoutController;

    private final PanierSession panier = PanierSession.getInstance();

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nomProduit"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        colQte.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colSousTotal.setCellValueFactory(new PropertyValueFactory<>("sousTotal"));
        paiementCombo.setItems(FXCollections.observableArrayList("En ligne", "En espèces"));
        paiementCombo.getSelectionModel().selectFirst();
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

    private void rafraichirPanier() {
        List<LignePanier> lignes = panier.getLignes();
        ObservableList<LignePanier> observableList = FXCollections.observableArrayList(lignes);
        tablePanier.setItems(observableList);
        totalLabel.setText(String.format("%.2f TND", panier.getTotal()));
        validerBtn.setDisable(lignes.isEmpty());
    }

    private void validerCommande() {
        String adresse = adresseField.getText() != null ? adresseField.getText().trim() : "";
        String modePaiement = paiementCombo.getValue();
        if (adresse.isEmpty() || modePaiement == null) {
            showAlert(Alert.AlertType.WARNING, "Champs requis", "Veuillez renseigner l’adresse et choisir un mode de paiement.");
            return;
        }
        if (panier.estVide()) {
            showAlert(Alert.AlertType.INFORMATION, "Panier vide", "Ajoutez des produits depuis la boutique avant de valider.");
            return;
        }
        int idClient = SessionContext.getInstance().getClientDatabaseId();
        commandes commande = new commandes(
                LocalDateTime.now().format(DATE_HEURE),
                panier.getTotal(),
                "validée",
                modePaiement,
                idClient,
                adresse
        );
        CommandesService commandesService = new CommandesService();
        boolean success = commandesService.ajouterCommande(commande, panier.getLignes());
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Commande enregistrée", "Votre commande a été validée. Merci pour votre achat !");
            panier.viderPanier();
            rafraichirPanier();
            adresseField.clear();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d’enregistrer la commande. Vérifiez la base de données (table ligne_commande, contraintes).");
        }
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
