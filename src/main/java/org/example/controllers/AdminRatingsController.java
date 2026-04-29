package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.entities.GymRating;
import org.example.dao.GymRatingDAO;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class AdminRatingsController implements Initializable {
    
    @FXML private TableView<GymRating> ratingsTable;
    @FXML private TableColumn<GymRating, Integer> idColumn;
    @FXML private TableColumn<GymRating, Integer> ratingColumn;
    @FXML private TableColumn<GymRating, String> commentColumn;
    @FXML private TableColumn<GymRating, String> userColumn;
    @FXML private TableColumn<GymRating, String> salleColumn;
    @FXML private TableColumn<GymRating, String> dateColumn;
    @FXML private TableColumn<GymRating, Void> actionsColumn;
    
    @FXML private ComboBox<String> salleFilterCombo;
    @FXML private ComboBox<String> ratingFilterCombo;
    @FXML private Button refreshBtn;
    @FXML private Button deleteBtn;
    @FXML private Label statsLabel;
    
    private GymRatingDAO ratingDAO;
    private ObservableList<GymRating> ratingsList = FXCollections.observableArrayList();
    private GymRating selectedRating;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Initialiser le DAO
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/gym_db", "root", "");
            ratingDAO = new GymRatingDAO(connection);
            
            // Configurer les colonnes
            setupColumns();
            
            // Configurer les filtres
            setupFilters();
            
            // Configurer les boutons
            setupButtons();
            
            // Charger les données
            loadRatings();
            
            // Afficher les statistiques
            updateStats();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur de connexion à la base de données");
        }
    }
    
    /**
     * Configure les colonnes du tableau
     */
    private void setupColumns() {
        // Colonne ID
        idColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());
        
        // Colonne Note avec étoiles
        ratingColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getRating()).asObject());
        ratingColumn.setCellFactory(column -> new TableCell<GymRating, Integer>() {
            @Override
            protected void updateItem(Integer rating, boolean empty) {
                super.updateItem(rating, empty);
                if (empty || rating == null) {
                    setText("");
                } else {
                    setText("".repeat(rating) + " ".repeat(5 - rating));
                    setStyle("-fx-font-family: monospace; -fx-font-size: 16px;");
                }
            }
        });
        
        // Colonne Commentaire
        commentColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getComment()));
        commentColumn.setCellFactory(column -> new TableCell<GymRating, String>() {
            @Override
            protected void updateItem(String comment, boolean empty) {
                super.updateItem(comment, empty);
                if (empty || comment == null || comment.trim().isEmpty()) {
                    setText("Aucun commentaire");
                    setStyle("-fx-font-style: italic; -fx-text-fill: #888;");
                } else {
                    String truncated = comment.length() > 50 ? comment.substring(0, 50) + "..." : comment;
                    setText(truncated);
                    setStyle("");
                }
                setWrapText(true);
            }
        });
        
        // Colonne Utilisateur
        userColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getUserId())));
        
        // Colonne Salle
        salleColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getGymnasiumId())));
        
        // Colonne Date
        dateColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCreatedAt().toString()));
        dateColumn.setCellFactory(column -> new TableCell<GymRating, String>() {
            @Override
            protected void updateItem(String date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText("");
                } else {
                    try {
                        // Formater la date
                        DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
                        DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                        String formattedDate = java.time.LocalDateTime.parse(date, inputFormat).format(outputFormat);
                        setText(formattedDate);
                    } catch (Exception e) {
                        setText(date);
                    }
                }
            }
        });
        
        // Colonne Actions
        actionsColumn.setCellFactory(column -> new TableCell<GymRating, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    GymRating rating = getTableView().getItems().get(getIndex());
                    HBox actions = new HBox(5);
                    
                    Button viewBtn = new Button("Voir");
                    viewBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                    viewBtn.setOnAction(e -> viewRatingDetails(rating));
                    
                    Button deleteBtn = new Button("Supprimer");
                    deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                    deleteBtn.setOnAction(e -> deleteRating(rating));
                    
                    actions.getChildren().addAll(viewBtn, deleteBtn);
                    setGraphic(actions);
                }
            }
        });
        
        // Configuration du tableau
        ratingsTable.setItems(ratingsList);
        ratingsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                selectedRating = newSelection;
                deleteBtn.setDisable(selectedRating == null);
            });
    }
    
    /**
     * Configure les filtres
     */
    private void setupFilters() {
        // Filtre par salle
        salleFilterCombo.getItems().addAll("Toutes les salles", "Salle 1", "Salle 2", "Salle 3"); // À adapter
        salleFilterCombo.getSelectionModel().selectFirst();
        salleFilterCombo.setOnAction(e -> applyFilters());
        
        // Filtre par note
        ratingFilterCombo.getItems().addAll("Toutes les notes", "5 étoiles", "4 étoiles", "3 étoiles", "2 étoiles", "1 étoile");
        ratingFilterCombo.getSelectionModel().selectFirst();
        ratingFilterCombo.setOnAction(e -> applyFilters());
    }
    
    /**
     * Configure les boutons
     */
    private void setupButtons() {
        refreshBtn.setOnAction(e -> {
            loadRatings();
            updateStats();
        });
        
        deleteBtn.setOnAction(e -> {
            if (selectedRating != null) {
                deleteRating(selectedRating);
            }
        });
        
        deleteBtn.setDisable(true);
    }
    
    /**
     * Charge tous les avis
     */
    private void loadRatings() {
        try {
            List<GymRating> ratings = ratingDAO.getAllRatings();
            ratingsList.clear();
            ratingsList.addAll(ratings);
            
            // Appliquer les filtres
            applyFilters();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement des avis");
        }
    }
    
    /**
     * Applique les filtres
     */
    private void applyFilters() {
        ObservableList<GymRating> filteredList = FXCollections.observableArrayList();
        
        for (GymRating rating : ratingsList) {
            boolean include = true;
            
            // Filtrer par salle
            String salleFilter = salleFilterCombo.getSelectionModel().getSelectedItem();
            if (!"Toutes les salles".equals(salleFilter)) {
                // Adapter selon vos noms de salles
                int salleId = getSalleIdFromName(salleFilter);
                if (rating.getGymnasiumId() != salleId) {
                    include = false;
                }
            }
            
            // Filtrer par note
            String ratingFilter = ratingFilterCombo.getSelectionModel().getSelectedItem();
            if (!"Toutes les notes".equals(ratingFilter)) {
                int expectedRating = getRatingFromFilter(ratingFilter);
                if (rating.getRating() != expectedRating) {
                    include = false;
                }
            }
            
            if (include) {
                filteredList.add(rating);
            }
        }
        
        ratingsTable.setItems(filteredList);
    }
    
    /**
     * Met à jour les statistiques
     */
    private void updateStats() {
        try {
            List<GymRating> allRatings = ratingDAO.getAllRatings();
            int totalRatings = allRatings.size();
            
            if (totalRatings > 0) {
                double avgRating = allRatings.stream()
                    .mapToInt(GymRating::getRating)
                    .average()
                    .orElse(0.0);
                
                long fiveStars = allRatings.stream().filter(r -> r.getRating() == 5).count();
                long fourStars = allRatings.stream().filter(r -> r.getRating() == 4).count();
                long threeStars = allRatings.stream().filter(r -> r.getRating() == 3).count();
                long twoStars = allRatings.stream().filter(r -> r.getRating() == 2).count();
                long oneStar = allRatings.stream().filter(r -> r.getRating() == 1).count();
                
                String stats = String.format(
                    "Total: %d avis | Moyenne: %.1f/5 | Distribution: %d (%.1f%%), %d (%.1f%%), %d (%.1f%%), %d (%.1f%%), %d (%.1f%%)",
                    totalRatings, avgRating,
                    fiveStars, (fiveStars * 100.0 / totalRatings),
                    fourStars, (fourStars * 100.0 / totalRatings),
                    threeStars, (threeStars * 100.0 / totalRatings),
                    twoStars, (twoStars * 100.0 / totalRatings),
                    oneStar, (oneStar * 100.0 / totalRatings)
                );
                
                statsLabel.setText(stats);
            } else {
                statsLabel.setText("Aucun avis enregistré");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            statsLabel.setText("Erreur de calcul des statistiques");
        }
    }
    
    /**
     * Affiche les détails d'un avis
     */
    private void viewRatingDetails(GymRating rating) {
        String details = String.format(
            "ID: %d\n" +
            "Note: %d/5\n" +
            "Commentaire: %s\n" +
            "ID Utilisateur: %d\n" +
            "ID Salle: %d\n" +
            "Date: %s",
            rating.getId(),
            rating.getRating(),
            rating.getComment() != null ? rating.getComment() : "Aucun",
            rating.getUserId(),
            rating.getGymnasiumId(),
            rating.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de l'avis");
        alert.setHeaderText("Avis #" + rating.getId());
        alert.setContentText(details);
        alert.showAndWait();
    }
    
    /**
     * Supprime un avis
     */
    private void deleteRating(GymRating rating) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer l'avis #" + rating.getId());
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cet avis ? Cette action est irréversible.");
        
        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                if (ratingDAO.deleteRating(rating.getId())) {
                    showSuccess("Avis supprimé avec succès");
                    loadRatings();
                    updateStats();
                } else {
                    showError("Impossible de supprimer l'avis");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }
    
    // Méthodes utilitaires
    private int getSalleIdFromName(String name) {
        // Adapter selon votre système
        switch (name) {
            case "Salle 1": return 1;
            case "Salle 2": return 2;
            case "Salle 3": return 3;
            default: return 0;
        }
    }
    
    private int getRatingFromFilter(String filter) {
        switch (filter) {
            case "5 étoiles": return 5;
            case "4 étoiles": return 4;
            case "3 étoiles": return 3;
            case "2 étoiles": return 2;
            case "1 étoile": return 1;
            default: return 0;
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
