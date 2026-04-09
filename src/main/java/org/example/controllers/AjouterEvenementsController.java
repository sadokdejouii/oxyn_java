package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import org.example.entities.Evenement;
import org.example.services.EvenementServices;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;

public class AjouterEvenementsController {

    @FXML
    private DatePicker created_at_evenement;

    @FXML
    private TextField created_by_evenement;

    @FXML
    private DatePicker date_debut_evenement;

    @FXML
    private DatePicker date_fin_evenement;

    @FXML
    private TextField description_evenement;

    @FXML
    private TextField lieu_evenement;

    @FXML
    private TextField places_max_evenement;

    @FXML
    private TextField statut_evenement;

    @FXML
    private TextField titre_evenement;

    @FXML
    private TextField ville_evenement;

    private EvenementServices es = new EvenementServices();

    @FXML
    void ajouterEvenement(ActionEvent event) {
        try {
            // Vérification simple
            if (titre_evenement.getText().isEmpty() ||
                    description_evenement.getText().isEmpty() ||
                    lieu_evenement.getText().isEmpty() ||
                    ville_evenement.getText().isEmpty() ||
                    places_max_evenement.getText().isEmpty() ||
                    statut_evenement.getText().isEmpty() ||
                    created_by_evenement.getText().isEmpty() ||
                    date_debut_evenement.getValue() == null ||
                    date_fin_evenement.getValue() == null ||
                    created_at_evenement.getValue() == null) {

                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Champs manquants");
                alert.setHeaderText(null);
                alert.setContentText("Veuillez remplir tous les champs.");
                alert.show();
                return;
            }

            // Conversion LocalDate -> Timestamp
            Timestamp dateDebut = Timestamp.valueOf(date_debut_evenement.getValue().atStartOfDay());
            Timestamp dateFin = Timestamp.valueOf(date_fin_evenement.getValue().atStartOfDay());
            Timestamp createdAt = Timestamp.valueOf(created_at_evenement.getValue().atStartOfDay());

            // Création de l'objet Evenement
            Evenement e = new Evenement(
                    titre_evenement.getText(),
                    description_evenement.getText(),
                    dateDebut,
                    dateFin,
                    lieu_evenement.getText(),
                    ville_evenement.getText(),
                    Integer.parseInt(places_max_evenement.getText()),
                    statut_evenement.getText(),
                    createdAt,
                    Integer.parseInt(created_by_evenement.getText())
            );

            // Ajout en base
            es.ajouter(e);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText("Événement ajouté avec succès !");
            alert.show();

            clearFields();

        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de saisie");
            alert.setHeaderText(null);
            alert.setContentText("created_by et places_max doivent être des nombres.");
            alert.show();
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur SQL");
            alert.setHeaderText(null);
            alert.setContentText("Erreur lors de l'ajout : " + e.getMessage());
            alert.show();
        }
    }

    private void clearFields() {
        titre_evenement.clear();
        description_evenement.clear();
        lieu_evenement.clear();
        ville_evenement.clear();
        places_max_evenement.clear();
        statut_evenement.clear();
        created_by_evenement.clear();

        date_debut_evenement.setValue(null);
        date_fin_evenement.setValue(null);
        created_at_evenement.setValue(null);
    }
}