package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.example.entities.produits;
import org.example.services.ProduitsService;
import org.example.utils.ProductImageStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;

public class AjouterProduitPageController {

    @FXML
    private TextField nomField;
    
    @FXML
    private TextArea descriptionField;
    
    @FXML
    private TextField prixField;
    
    @FXML
    private TextField quantiteField;
    
    @FXML
    private ImageView imagePreview;

    @FXML
    private Label imageFileLabel;

    @FXML
    private TextField statutField;
    
    @FXML
    private TextField dateField;
    
    private ProduitsService produitsService;
    private MainLayoutController mainLayoutController;

    /** Fichier image choisi par l’utilisateur (avant copie vers product_images). */
    private Path selectedImageSource;

    public AjouterProduitPageController() {
        this.produitsService = new ProduitsService();
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    @FXML
    public void initialize() {
        // Auto-générer la date du jour
        dateField.setText(LocalDate.now().toString());
        
        // Valeurs par défaut
        statutField.setText("Disponible");
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) {
            return;
        }

        try {
            String imageName = "default.jpg";
            if (selectedImageSource != null) {
                try {
                    imageName = ProductImageStorage.copyUploadedFile(selectedImageSource);
                } catch (IOException ex) {
                    showAlert("Erreur", "Impossible d’enregistrer l’image : " + ex.getMessage());
                    return;
                }
            }

            produits produit = new produits(
                nomField.getText().trim(),
                descriptionField.getText().trim(),
                Double.parseDouble(prixField.getText().trim()),
                Integer.parseInt(quantiteField.getText().trim()),
                imageName,
                dateField.getText(),
                statutField.getText().trim()
            );

            // Ajouter le produit
            produitsService.ajouter(produit);
            
            showAlert("Succès", "Produit ajouté avec succès !");
            
            // Retourner à la page boutique
            if (mainLayoutController != null) {
                mainLayoutController.navigate("/FXML/pages/BoutiquePage.fxml", "Boutique", null);
            }
            
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Veuillez vérifier les champs numériques (prix et quantité)");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de l'ajout du produit: " + e.getMessage());
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
    private void handleChooseImage() {
        Window owner = nomField != null && nomField.getScene() != null
                ? nomField.getScene().getWindow()
                : null;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image produit");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.bmp"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
        var file = chooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }
        selectedImageSource = file.toPath();
        if (imageFileLabel != null) {
            imageFileLabel.setText(file.getName());
        }
        if (imagePreview != null) {
            imagePreview.setImage(new Image(file.toURI().toString(), 200, 200, true, true));
        }
    }

    @FXML
    private void handleClear() {
        nomField.clear();
        descriptionField.clear();
        prixField.clear();
        quantiteField.clear();
        selectedImageSource = null;
        if (imagePreview != null) {
            imagePreview.setImage(null);
        }
        if (imageFileLabel != null) {
            imageFileLabel.setText("Aucun fichier sélectionné — « default.jpg » sera utilisé si présent dans /images.");
        }
        statutField.setText("Disponible");
        dateField.setText(LocalDate.now().toString());
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
