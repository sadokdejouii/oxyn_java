package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.example.entities.Salle;
import org.example.entities.SubscriptionOffer;
import org.example.services.SubscriptionOfferService;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur complet pour la gestion des abonnements
 */
public class AbonnementController implements Initializable {

    @FXML private TableView<SubscriptionOffer> tableViewOffres; // TableView des offres d'abonnement
    @FXML private TableColumn<SubscriptionOffer, String> colNom;
    @FXML private TableColumn<SubscriptionOffer, Double> colPrix;
    @FXML private TableColumn<SubscriptionOffer, String> colActions;
    
    @FXML private TextField fieldName;
    @FXML private TextField fieldPrice;
    @FXML private Button btnAjouter;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnAnnuler;
    
    private Salle salleActuelle;
    private final SubscriptionOfferService service = new SubscriptionOfferService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableView();
    }

    /**
     * ✅ Configurer la TableView
     */
    private void setupTableView() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("price"));
        
        // Colonne Actions avec boutons Modifier/Supprimer
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnModifier = new Button("Modifier");
            private final Button btnSupprimer = new Button("Supprimer");
            private final HBox hbox = new HBox(5, btnModifier, btnSupprimer);

            {
                btnModifier.getStyleClass().add("button-modifier");
                btnSupprimer.getStyleClass().add("button-supprimer");
                
                btnModifier.setOnAction(e -> handleModifier(getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(e -> handleSupprimer(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hbox);
                }
            }
        });
    }

    /**
     * ✅ Passer la salle actuelle depuis la fenêtre parent
     */
    public void setSalle(Salle salle) {
        this.salleActuelle = salle;
        System.out.println("📍 Salle reçue: " + salle.getName() + " (ID: " + salle.getId() + ")");
        loadData();
    }

    /**
     * ✅ Charger les données depuis la BD
     */
    private void loadData() {
        if (salleActuelle == null) {
            System.out.println("❌ Aucune salle sélectionnée");
            return;
        }

        List<SubscriptionOffer> offers = service.getByGym(salleActuelle.getId());
        tableViewOffres.setItems(FXCollections.observableArrayList(offers));
        
        System.out.println("📊 " + offers.size() + " offres chargées pour la salle " + salleActuelle.getName());
    }

    /**
     * ✅ Ajouter une offre - BOUTON CLIQUÉ
     */
    @FXML
    private void handleAjouter() {
        System.out.println("🔍 BOUTON CLIQUÉ - salle: " + salleActuelle.getId());
        
        String nom = fieldName.getText().trim();
        String prixStr = fieldPrice.getText().trim();

        // Validation
        if (nom.isEmpty()) {
            showAlert("Erreur", "Le nom de l'offre est obligatoire");
            return;
        }

        if (prixStr.isEmpty()) {
            showAlert("Erreur", "Le prix est obligatoire");
            return;
        }

        double prix;
        try {
            prix = Double.parseDouble(prixStr.replace(",", "."));
            if (prix <= 0) {
                showAlert("Erreur", "Le prix doit être positif");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Le prix est invalide");
            return;
        }

        // Créer et ajouter l'offre
        SubscriptionOffer offre = new SubscriptionOffer(
            salleActuelle.getId(),
            nom,
            1, // duration_months par défaut
            prix,
            ""
        );

        service.add(offre);
        System.out.println("✅ Offre ajoutée en BD !");
        
        // Vider les champs
        fieldName.clear();
        fieldPrice.clear();
        
        // Refresh
        loadData();
        
        showAlert("Succès", "Offre ajoutée avec succès !");
    }

    /**
     * ✅ Modifier une offre
     */
    private void handleModifier(SubscriptionOffer offer) {
        // Pour l'instant, simple édition inline
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Modifier l'offre");
        dialog.setHeaderText("Modifier \"" + offer.getName() + "\" ?");
        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                // Ici vous pourriez ouvrir une fenêtre d'édition
                // Pour l'instant, on garde l'offre telle quelle
                System.out.println("🔍 Modification de l'offre ID: " + offer.getId());
            }
        });
    }

    /**
     * ✅ Supprimer une offre
     */
    private void handleSupprimer(SubscriptionOffer offer) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer \"" + offer.getName() + "\" ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                service.delete(offer.getId());
                loadData();
                showAlert("Succès", "Offre supprimée !");
            }
        });
    }

    /**
     * ✅ Enregistrer - ferme la fenêtre
     */
    @FXML
    private void handleEnregistrer() {
        System.out.println("🔍 Fermeture de la fenêtre");
        // Fermer la fenêtre
        btnEnregistrer.getScene().getWindow().hide();
    }

    /**
     * ✅ Annuler - ferme la fenêtre
     */
    @FXML
    private void handleAnnuler() {
        System.out.println("🔍 Annulation - fermeture de la fenêtre");
        // Fermer la fenêtre
        btnAnnuler.getScene().getWindow().hide();
    }

    /**
     * ✅ Afficher une alerte
     */
    private void showAlert(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * ✅ Obtenir la salle actuelle (pour AbonnementIntegration)
     */
    public Salle getSalleActuelle() {
        return salleActuelle;
    }

    /**
     * ✅ Obtenir la liste des offres (pour AbonnementIntegration)
     */
    public List<SubscriptionOffer> getListeOffres() {
        if (tableViewOffres != null && tableViewOffres.getItems() != null) {
            return new ArrayList<>(tableViewOffres.getItems());
        }
        return new ArrayList<>();
    }
}
