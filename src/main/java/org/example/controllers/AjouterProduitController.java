package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.entities.produits;
import org.example.services.ProduitsService;

import java.sql.SQLException;
import java.time.LocalDate;

public class AjouterProduitController {

    @FXML
    private TextField nomField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TextField prixField;

    @FXML
    private TextField quantiteField;

    @FXML
    private TextField imageField;

    @FXML
    private TextField statutField;

    private ProduitsService produitsService;

    public AjouterProduitController() {
        this.produitsService = new ProduitsService();
    }

    @FXML
    private void handleAjouter() {
        if (validerFormulaire()) {
            try {
                produits produit = creerProduit();
                produitsService.ajouter(produit);
                
                showAlert("Succès", "Produit ajouté avec succès!");
                fermerFenetre();
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible d'ajouter le produit: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleAnnuler() {
        fermerFenetre();
    }

    private boolean validerFormulaire() {
        StringBuilder erreurs = new StringBuilder();

        if (nomField.getText().trim().isEmpty()) {
            erreurs.append("Le nom du produit est obligatoire.\n");
        }

        if (descriptionArea.getText().trim().isEmpty()) {
            erreurs.append("La description est obligatoire.\n");
        }

        if (prixField.getText().trim().isEmpty()) {
            erreurs.append("Le prix est obligatoire.\n");
        } else {
            try {
                double prix = Double.parseDouble(prixField.getText().trim());
                if (prix <= 0) {
                    erreurs.append("Le prix doit être supérieur à 0.\n");
                }
            } catch (NumberFormatException e) {
                erreurs.append("Le prix doit être un nombre valide.\n");
            }
        }

        if (quantiteField.getText().trim().isEmpty()) {
            erreurs.append("La quantité est obligatoire.\n");
        } else {
            try {
                int quantite = Integer.parseInt(quantiteField.getText().trim());
                if (quantite < 0) {
                    erreurs.append("La quantité ne peut pas être négative.\n");
                }
            } catch (NumberFormatException e) {
                erreurs.append("La quantité doit être un nombre entier valide.\n");
            }
        }

        if (statutField.getText().trim().isEmpty()) {
            erreurs.append("Le statut est obligatoire.\n");
        }

        if (erreurs.length() > 0) {
            showAlert("Erreurs de validation", erreurs.toString());
            return false;
        }

        return true;
    }

    private produits creerProduit() {
        String nom = nomField.getText().trim();
        String description = descriptionArea.getText().trim();
        double prix = Double.parseDouble(prixField.getText().trim());
        int quantite = Integer.parseInt(quantiteField.getText().trim());
        String image = imageField.getText().trim();
        String statut = statutField.getText().trim();
        String dateCreation = LocalDate.now().toString();

        return new produits(nom, description, prix, quantite, image, dateCreation, statut);
    }

    private void fermerFenetre() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
