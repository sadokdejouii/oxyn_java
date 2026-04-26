package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.entities.Evenement;
import org.example.services.EvenementServices;
import org.example.services.LocationService;

import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class ModifierEvenementController implements Initializable {

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
    private Button modifierBtn;

    @FXML
    private Label formFeedbackLabel;

    @FXML
    private StackPane rootStackPane;

    private final EvenementServices es = new EvenementServices();
    private Evenement currentEvenement;
    private int eventId = -1;
    private boolean embeddedMode = false;
    private Consumer<Evenement> onDone;

    public void setEmbeddedMode(Consumer<Evenement> onDone) {
        this.embeddedMode = true;
        this.onDone = onDone;
    }

    public void setEventData(int eventId) {
        this.eventId = eventId;
        loadEventData();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statut_evenement.setItems(FXCollections.observableArrayList(
                "Sélectionner",
            "A venir",
                "En cours",
            "Terminee",
            "Annulee"
        ));
        statut_evenement.setValue("Sélectionner");
        clearFeedback();
    }

    private void loadEventData() {
        try {
            currentEvenement = es.afficherById(eventId);
            if (currentEvenement != null) {
                // Populate form fields with event data
                titre_evenement.setText(currentEvenement.getTitre());
                description_evenement.setText(currentEvenement.getDescription());
                lieu_evenement.setText(currentEvenement.getLieu());
                ville_evenement.setText(currentEvenement.getVille());
                places_max_evenement.setText(String.valueOf(currentEvenement.getPlacesMax()));
                statut_evenement.setValue(normalizeStatus(currentEvenement.getStatut()));

                // Set dates - convert java.util.Date to LocalDate
                if (currentEvenement.getDateDebut() != null) {
                    java.util.Date dateDebutUtil = currentEvenement.getDateDebut();
                    LocalDate dateDebut = Instant.ofEpochMilli(dateDebutUtil.getTime())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    date_debut_evenement.setValue(dateDebut);
                }
                if (currentEvenement.getDateFin() != null) {
                    java.util.Date dateFinUtil = currentEvenement.getDateFin();
                    LocalDate dateFin = Instant.ofEpochMilli(dateFinUtil.getTime())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    date_fin_evenement.setValue(dateFin);
                }
            }
        } catch (SQLException e) {
            showError("Erreur lors du chargement de l'événement : " + e.getMessage(), "Erreur base de données");
        }
    }

    @FXML
    void modifierEvenement(ActionEvent event) {
        clearFeedback();

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
            Timestamp dateDebut = Timestamp.valueOf(date_debut_evenement.getValue().atStartOfDay());
            Timestamp dateFin = Timestamp.valueOf(date_fin_evenement.getValue().atStartOfDay());

            Evenement evenementModifie = new Evenement(
                    titre_evenement.getText().trim(),
                    description_evenement.getText().trim(),
                    dateDebut,
                    dateFin,
                    lieu_evenement.getText().trim(),
                    ville_evenement.getText().trim(),
                    Integer.parseInt(places_max_evenement.getText().trim()),
                    statut_evenement.getValue(),
                    currentEvenement.getCreatedAt(),
                    currentEvenement.getCreatedBy()
            );
            
            // Set the ID for update
            evenementModifie.setId(eventId);

            es.modifier(evenementModifie);
            showSuccess("Événement modifié avec succès !");
            closeOrReturn(evenementModifie);

        } catch (SQLException ex) {
            showError("Erreur lors de la modification de l'événement : " + ex.getMessage(), "Erreur base de données");
        } catch (Exception ex) {
            showError("Une erreur inattendue s'est produite : " + ex.getMessage(), "Erreur");
        }
    }

    @FXML
    void openLocationPicker(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/LocationPicker.fxml"));
            if (loader.getLocation() == null) {
                showError("Fichier LocationPicker.fxml introuvable", "Erreur");
                return;
            }
            VBox locationPickerContent = loader.load();

            // Create dimming overlay
            Region dimOverlay = new Region();
            dimOverlay.setStyle("""
                -fx-background-color: rgba(0, 0, 0, 0.4);
                """);
            
            // Click on overlay to close
            dimOverlay.setOnMouseClicked(e -> closeLocationModal(rootStackPane, dimOverlay));

            // Create popup container
            VBox popupContainer = new VBox();
            popupContainer.setStyle("""
                -fx-background-color: linear-gradient(from 0% 0% to 100% 100%, rgba(34, 65, 138, 0.24) 0%, rgba(13, 27, 62, 0.96) 100%);
                -fx-background-radius: 18;
                -fx-border-color: rgba(79, 195, 247, 0.28);
                -fx-border-width: 1;
                -fx-border-radius: 18;
                -fx-padding: 12;
                """);
            popupContainer.setEffect(new javafx.scene.effect.DropShadow(
                    javafx.scene.effect.BlurType.GAUSSIAN,
                    javafx.scene.paint.Color.web("#000000", 0.45),
                    24, 0.2, 0, 6
            ));
            
            // Set max size for modal
            popupContainer.setMaxWidth(1000);
            popupContainer.setMaxHeight(720);
            popupContainer.setPrefWidth(1000);
            popupContainer.setPrefHeight(720);

            // Add content
            VBox.setVgrow(locationPickerContent, javafx.scene.layout.Priority.ALWAYS);
            popupContainer.getChildren().add(locationPickerContent);

            // Center the popup
            StackPane.setAlignment(popupContainer, javafx.geometry.Pos.CENTER);
            StackPane.setMargin(popupContainer, new Insets(20));

            // Add overlay and popup to root
            rootStackPane.getChildren().addAll(dimOverlay, popupContainer);

            LocationPickerController controller = loader.getController();
            if (controller == null) {
                showError("Erreur lors du chargement du contrôleur LocationPicker", "Erreur");
                return;
            }
            
            controller.setOnLocationSelected(() -> {
                LocationService.LocationResult result = controller.getSelectedLocation();
                if (result != null) {
                    if (result.getPlace() != null && !result.getPlace().isEmpty()) {
                        lieu_evenement.setText(result.getPlace());
                    }
                    if (result.getCity() != null && !result.getCity().isEmpty()) {
                        ville_evenement.setText(result.getCity());
                    }
                    showSuccess("Lieu sélectionné: " + result.getPlace() + ", " + result.getCity());
                    closeLocationModal(rootStackPane, dimOverlay);
                }
            });
            
            controller.setOnCancel(() -> {
                closeLocationModal(rootStackPane, dimOverlay);
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'ouverture de la carte: " + e.getMessage(), "Erreur");
        }
    }
    
    private void closeLocationModal(StackPane root, Region dimOverlay) {
        // Remove the last two children (dimOverlay and popupContainer)
        if (root.getChildren().size() >= 2) {
            root.getChildren().remove(root.getChildren().size() - 1);
            root.getChildren().remove(root.getChildren().size() - 1);
        }
    }

    private void closeOrReturn(Evenement savedEvent) {
        if (embeddedMode) {
            if (onDone != null) {
                onDone.accept(savedEvent);
            }
            return;
        }
        Stage stage = (Stage) titre_evenement.getScene().getWindow();
        stage.close();
    }

    private void showError(String message, String title) {
        showFeedback(message, false);
    }

    private void showSuccess(String message) {
        showFeedback(message, true);
    }

    private void showFeedback(String message, boolean success) {
        if (formFeedbackLabel == null) {
            return;
        }

        formFeedbackLabel.setText(message);
        formFeedbackLabel.getStyleClass().removeAll("form-feedback-error", "form-feedback-success");
        formFeedbackLabel.getStyleClass().add(success ? "form-feedback-success" : "form-feedback-error");
        formFeedbackLabel.setVisible(true);
        formFeedbackLabel.setManaged(true);
    }

    private void clearFeedback() {
        if (formFeedbackLabel == null) {
            return;
        }

        formFeedbackLabel.setText("");
        formFeedbackLabel.getStyleClass().removeAll("form-feedback-error", "form-feedback-success");
        formFeedbackLabel.setVisible(false);
        formFeedbackLabel.setManaged(false);
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : java.text.Normalizer.normalize(status, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .trim();

        if (normalized.contains("annul")) {
            return "Annulee";
        }
        if (normalized.contains("termin")) {
            return "Terminee";
        }
        if (normalized.contains("en cours")) {
            return "En cours";
        }
        return "A venir";
    }
}
