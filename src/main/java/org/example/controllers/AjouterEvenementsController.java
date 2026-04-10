package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.entities.Evenement;
import org.example.services.EvenementServices;

import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class AjouterEvenementsController implements Initializable {

    @FXML
    private TextField titre_evenement;

    @FXML
    private TextArea description_evenement;

    @FXML
    private DatePicker date_debut_evenement;

    @FXML
    private DatePicker date_fin_evenement;

    @FXML
    private TextField lieu_evenement;

    @FXML
    private TextField ville_evenement;

    @FXML
    private TextField places_max_evenement;

    @FXML
    private ComboBox<String> statut_evenement;

    @FXML
    private Button annulerBtn;

    private final EvenementServices es = new EvenementServices();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statut_evenement.setItems(FXCollections.observableArrayList(
                "Sélectionner",
                "À venir",
                "En cours",
                "Terminée"
        ));
        statut_evenement.setValue("Sélectionner");
    }

    @FXML
    void ajouterEvenement(ActionEvent event) {
        if (titre_evenement.getText().trim().isEmpty()) {
            showError("Le titre est obligatoire.", "Champ manquant");
            return;
        }
        if (titre_evenement.getText().length() > 30) {
            showError("Le titre ne doit pas dépasser 30 caractères.", "Titre trop long");
            return;
        }

        if (description_evenement.getText().trim().isEmpty()) {
            showError("La description est obligatoire.", "Champ manquant");
            return;
        }
        if (description_evenement.getText().length() > 100) {
            showError("La description ne doit pas dépasser 100 caractères.", "Description trop longue");
            return;
        }

        if (date_debut_evenement.getValue() == null) {
            showError("La date de début est obligatoire.", "Champ manquant");
            return;
        }

        if (date_fin_evenement.getValue() == null) {
            showError("La date de fin est obligatoire.", "Champ manquant");
            return;
        }
        if (date_fin_evenement.getValue().isBefore(date_debut_evenement.getValue())) {
            showError("La date de fin ne peut pas être avant la date de début.", "Date invalide");
            return;
        }

        if (lieu_evenement.getText().trim().isEmpty()) {
            showError("Le lieu est obligatoire.", "Champ manquant");
            return;
        }

        if (ville_evenement.getText().trim().isEmpty()) {
            showError("La ville est obligatoire.", "Champ manquant");
            return;
        }

        if (places_max_evenement.getText().trim().isEmpty()) {
            showError("Le nombre de places est obligatoire.", "Champ manquant");
            return;
        }

        try {
            int places = Integer.parseInt(places_max_evenement.getText().trim());
            if (places <= 0) {
                showError("Le nombre de places doit être supérieur à 0.", "Nombre invalide");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Le nombre de places doit être un nombre entier valide.", "Format invalide");
            return;
        }

        if (statut_evenement.getValue() == null || statut_evenement.getValue().isEmpty()) {
            showError("Veuillez sélectionner un statut.", "Champ manquant");
            return;
        }
        if (statut_evenement.getValue().equals("Sélectionner")) {
            showError("Veuillez sélectionner un statut valide.", "Statut invalide");
            return;
        }

        try {
            Timestamp createdAt = Timestamp.valueOf(LocalDateTime.now());
            Timestamp dateDebut = Timestamp.valueOf(date_debut_evenement.getValue().atStartOfDay());
            Timestamp dateFin = Timestamp.valueOf(date_fin_evenement.getValue().atStartOfDay());

            Evenement evenement = new Evenement(
                    titre_evenement.getText().trim(),
                    description_evenement.getText().trim(),
                    dateDebut,
                    dateFin,
                    lieu_evenement.getText().trim(),
                    ville_evenement.getText().trim(),
                    Integer.parseInt(places_max_evenement.getText().trim()),
                    statut_evenement.getValue(),
                    createdAt,
                    1
            );

            es.ajouter(evenement);
            showSuccess("Événement ajouté avec succès !");
            clearFields();
            // Close the window after successful addition
            Stage stage = (Stage) titre_evenement.getScene().getWindow();
            stage.close();

        } catch (SQLException ex) {
            showError("Erreur lors de l'ajout de l'événement : " + ex.getMessage(), "Erreur base de données");
        } catch (Exception ex) {
            showError("Une erreur inattendue s'est produite : " + ex.getMessage(), "Erreur");
        }
    }

    @FXML
    void annulerAjout(ActionEvent event) {
        // Close the current window/stage
        Stage stage = (Stage) titre_evenement.getScene().getWindow();
        stage.close();
    }

    private void clearFields() {
        titre_evenement.clear();
        description_evenement.clear();
        date_debut_evenement.setValue(null);
        date_fin_evenement.setValue(null);
        lieu_evenement.clear();
        ville_evenement.clear();
        places_max_evenement.clear();
        statut_evenement.setValue(null);
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message, String title) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}