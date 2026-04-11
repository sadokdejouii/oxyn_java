package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.example.entities.produits;
import org.example.services.ProduitsService;

import java.sql.SQLException;

public class ModifierProduitPageController {

    private static produits produitTemporaire;

    @FXML
    private TextField nomField;
    
    @FXML
    private TextArea descriptionField;
    
    @FXML
    private TextField prixField;
    
    @FXML
    private TextField quantiteField;
    
    @FXML
    private TextField imageField;
    
    @FXML
    private TextField statutField;
    
    @FXML
    private TextField dateField;
    
    private ProduitsService produitsService;
    private MainLayoutController mainLayoutController;
    private produits produitActuel;
    private String[] valeursInitiales;

    public ModifierProduitPageController() {
        this.produitsService = new ProduitsService();
    }

    public static void setProduitTemporaire(produits produit) {
        produitTemporaire = produit;
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    public void setProduit(produits produit) {
        this.produitActuel = produit;
        if (produit != null) {
            populateFields();
            saveInitialValues();
        }
    }

    @FXML
    public void initialize() {
        // Utiliser le produit temporaire s'il existe
        if (produitTemporaire != null) {
            this.produitActuel = produitTemporaire;
            populateFields();
            saveInitialValues();
            produitTemporaire = null; // Nettoyer après utilisation
        }
    }

    private void populateFields() {
        if (produitActuel != null) {
            nomField.setText(produitActuel.getNom_produit());
            descriptionField.setText(produitActuel.getDescription_produit());
            prixField.setText(String.valueOf(produitActuel.getPrix_produit()));
            quantiteField.setText(String.valueOf(produitActuel.getQuantite_stock_produit()));
            imageField.setText(produitActuel.getImage_produit());
            statutField.setText(produitActuel.getStatut_produit());
            dateField.setText(produitActuel.getDate_creation_produit());
        }
    }

    private void saveInitialValues() {
        if (produitActuel != null) {
            valeursInitiales = new String[]{
                nomField.getText(),
                descriptionField.getText(),
                prixField.getText(),
                quantiteField.getText(),
                imageField.getText(),
                statutField.getText()
            };
        }
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) {
            return;
        }

        try {
            // Mettre à jour le produit avec les nouvelles valeurs
            produitActuel.setNom_produit(nomField.getText().trim());
            produitActuel.setDescription_produit(descriptionField.getText().trim());
            produitActuel.setPrix_produit(Double.parseDouble(prixField.getText().trim()));
            produitActuel.setQuantite_stock_produit(Integer.parseInt(quantiteField.getText().trim()));
            produitActuel.setImage_produit(imageField.getText().trim().isEmpty() ? "default.jpg" : imageField.getText().trim());
            produitActuel.setStatut_produit(statutField.getText().trim());

            // Mettre à jour dans la base de données
            produitsService.modifier(produitActuel);
            
            showAlert("Succès", "Produit modifié avec succès !");
            
            // Retourner à la page boutique
            if (mainLayoutController != null) {
                mainLayoutController.navigate("/FXML/pages/BoutiquePage.fxml", "Boutique", null);
            }
            
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Veuillez vérifier les champs numériques (prix et quantité)");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la modification du produit: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        // Retourner à la page boutique
        if (mainLayoutController != null) {
            mainLayoutController.navigate("/FXML/pages/BoutiquePage.fxml", "Boutique", null);
        }
    }

    @FXML
    private void handleReset() {
        // Restaurer les valeurs initiales
        if (valeursInitiales != null) {
            nomField.setText(valeursInitiales[0]);
            descriptionField.setText(valeursInitiales[1]);
            prixField.setText(valeursInitiales[2]);
            quantiteField.setText(valeursInitiales[3]);
            imageField.setText(valeursInitiales[4]);
            statutField.setText(valeursInitiales[5]);
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (nomField.getText().trim().isEmpty()) {
            errors.append("- Le nom du produit est obligatoire\n");
        }

        if (descriptionField.getText().trim().isEmpty()) {
            errors.append("- La description est obligatoire\n");
        }

        try {
            double prix = Double.parseDouble(prixField.getText().trim());
            if (prix < 0) {
                errors.append("- Le prix doit être positif\n");
            }
        } catch (NumberFormatException e) {
            errors.append("- Le prix doit être un nombre valide\n");
        }

        try {
            int quantite = Integer.parseInt(quantiteField.getText().trim());
            if (quantite < 0) {
                errors.append("- La quantité doit être positive\n");
            }
        } catch (NumberFormatException e) {
            errors.append("- La quantité doit être un nombre entier valide\n");
        }

        if (statutField.getText().trim().isEmpty()) {
            errors.append("- Le statut est obligatoire\n");
        }

        if (errors.length() > 0) {
            showAlert("Erreurs de validation", errors.toString());
            return false;
        }

        return true;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
