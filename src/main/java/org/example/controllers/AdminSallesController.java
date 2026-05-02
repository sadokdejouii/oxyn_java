package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.entities.Salle;
import org.example.services.SalleService;
import org.example.dao.GymRatingDAO;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class AdminSallesController implements Initializable {
    
    @FXML private TableView<Salle> sallesTable;
    @FXML private TableColumn<Salle, Integer> idColumn;
    @FXML private TableColumn<Salle, String> nameColumn;
    @FXML private TableColumn<Salle, String> addressColumn;
    @FXML private TableColumn<Salle, String> phoneColumn;
    @FXML private TableColumn<Salle, Double> ratingColumn;
    @FXML private TableColumn<Salle, Integer> ratingCountColumn;
    @FXML private TableColumn<Salle, Boolean> activeColumn;
    @FXML private TableColumn<Salle, Void> actionsColumn;
    
    @FXML private Button refreshBtn;
    @FXML private Button addSalleBtn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterActiveCombo;
    @FXML private Label statsLabel;
    
    private SalleService salleService;
    private GymRatingDAO ratingDAO;
    private ObservableList<Salle> sallesList = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Initialiser les services
            salleService = new SalleService();
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/oxyn", "root", "");
            ratingDAO = new GymRatingDAO(connection);
            
            // Configurer les colonnes
            setupColumns();
            
            // Configurer les filtres
            setupFilters();
            
            // Configurer les boutons
            setupButtons();
            
            // Charger les données
            loadSalles();
            
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
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        ratingColumn.setCellValueFactory(new PropertyValueFactory<>("rating"));
        ratingCountColumn.setCellValueFactory(new PropertyValueFactory<>("ratingCount"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        
        // Formater la colonne rating
        ratingColumn.setCellFactory(column -> new TableCell<Salle, Double>() {
            @Override
            protected void updateItem(Double rating, boolean empty) {
                super.updateItem(rating, empty);
                if (empty || rating == null || rating == 0.0) {
                    setText("Non noté");
                    setStyle("-fx-text-fill: #888;");
                } else {
                    setText(String.format("%.1f/5", rating));
                    // Colorer selon la note
                    if (rating >= 4.0) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;"); // Vert
                    } else if (rating >= 3.0) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;"); // Orange
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); // Rouge
                    }
                }
            }
        });
        
        // Formater la colonne active
        activeColumn.setCellFactory(column -> new TableCell<Salle, Boolean>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setText("");
                } else {
                    setText(active ? "Actif" : "Inactif");
                    setStyle(active ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #e74c3c;");
                }
            }
        });
        
        // Colonne actions
        actionsColumn.setCellFactory(column -> new TableCell<Salle, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Salle salle = getTableView().getItems().get(getIndex());
                    javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(5);
                    
                    Button viewBtn = new Button("Voir");
                    viewBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                    viewBtn.setOnAction(e -> viewSalleDetails(salle));
                    
                    Button editBtn = new Button("Modifier");
                    editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
                    editBtn.setOnAction(e -> editSalle(salle));
                    
                    Button ratingsBtn = new Button("Avis");
                    ratingsBtn.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
                    ratingsBtn.setOnAction(e -> viewRatings(salle));
                    
                    actions.getChildren().addAll(viewBtn, editBtn, ratingsBtn);
                    setGraphic(actions);
                }
            }
        });
        
        sallesTable.setItems(sallesList);
    }
    
    /**
     * Configure les filtres
     */
    private void setupFilters() {
        filterActiveCombo.getItems().addAll("Toutes", "Actives", "Inactives");
        filterActiveCombo.getSelectionModel().selectFirst();
        filterActiveCombo.setOnAction(e -> applyFilters());
        
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());
    }
    
    /**
     * Configure les boutons
     */
    private void setupButtons() {
        refreshBtn.setOnAction(e -> {
            loadSalles();
            updateStats();
        });
        
        addSalleBtn.setOnAction(e -> addNewSalle());
    }
    
    /**
     * Charge toutes les salles
     */
    private void loadSalles() {
        try {
            List<Salle> salles = salleService.afficher();
            sallesList.clear();
            sallesList.addAll(salles);
            
            // Appliquer les filtres
            applyFilters();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement des salles");
        }
    }
    
    /**
     * Applique les filtres
     */
    private void applyFilters() {
        ObservableList<Salle> filteredList = FXCollections.observableArrayList();
        
        for (Salle salle : sallesList) {
            boolean include = true;
            
            // Filtrer par statut
            String statusFilter = filterActiveCombo.getSelectionModel().getSelectedItem();
            if (!"Toutes".equals(statusFilter)) {
                boolean isActive = "Actives".equals(statusFilter);
                if (salle.isActive() != isActive) {
                    include = false;
                }
            }
            
            // Filtrer par recherche
            String searchText = searchField.getText().toLowerCase();
            if (!searchText.isEmpty() && 
                !salle.getName().toLowerCase().contains(searchText) &&
                !salle.getAddress().toLowerCase().contains(searchText)) {
                include = false;
            }
            
            if (include) {
                filteredList.add(salle);
            }
        }
        
        sallesTable.setItems(filteredList);
    }
    
    /**
     * Met à jour les statistiques
     */
    private void updateStats() {
        try {
            List<Salle> allSalles = salleService.afficher();
            int totalSalles = allSalles.size();
            long activeSalles = allSalles.stream().filter(Salle::isActive).count();
            long inactiveSalles = totalSalles - activeSalles;
            
            // Calculer la moyenne générale
            double avgGeneral = allSalles.stream()
                .mapToDouble(Salle::getRating)
                .average()
                .orElse(0.0);
            
            long ratedSalles = allSalles.stream()
                .filter(s -> s.getRating() > 0)
                .count();
            
            String stats = String.format(
                "Total: %d salles | Actives: %d | Inactives: %d | Moyenne générale: %.1f/5 | Salles notées: %d",
                totalSalles, activeSalles, inactiveSalles, avgGeneral, ratedSalles
            );
            
            statsLabel.setText(stats);
            
        } catch (SQLException e) {
            e.printStackTrace();
            statsLabel.setText("Erreur de calcul des statistiques");
        }
    }
    
    /**
     * Affiche les détails d'une salle
     */
    private void viewSalleDetails(Salle salle) {
        String details = String.format(
            "ID: %d\n" +
            "Nom: %s\n" +
            "Adresse: %s\n" +
            "Téléphone: %s\n" +
            "Email: %s\n" +
            "Note: %.1f/5 (%d avis)\n" +
            "Statut: %s",
            salle.getId(),
            salle.getName(),
            salle.getAddress(),
            salle.getPhone(),
            salle.getEmail(),
            salle.getRating(),
            salle.getRatingCount(),
            salle.isActive() ? "Actif" : "Inactif"
        );
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la salle");
        alert.setHeaderText(salle.getName());
        alert.setContentText(details);
        alert.showAndWait();
    }
    
    /**
     * Modifie une salle
     */
    private void editSalle(Salle salle) {
        // TODO: Implémenter l'édition de salle
        showInfo("Modification", "Fonctionnalité d'édition à implémenter pour la salle: " + salle.getName());
    }
    
    /**
     * Affiche les avis d'une salle
     */
    private void viewRatings(Salle salle) {
        try {
            List<org.example.entities.GymRating> ratings = ratingDAO.getRatingsBySalle(salle.getId());
            
            if (ratings.isEmpty()) {
                showInfo("Avis", "Aucun avis pour cette salle");
                return;
            }
            
            StringBuilder ratingsText = new StringBuilder();
            ratingsText.append("Avis pour ").append(salle.getName()).append(":\n\n");
            
            for (org.example.entities.GymRating rating : ratings) {
                ratingsText.append("Note: ").append(rating.getRating()).append("/5\n");
                if (rating.getComment() != null && !rating.getComment().trim().isEmpty()) {
                    ratingsText.append("Commentaire: ").append(rating.getComment()).append("\n");
                }
                ratingsText.append("Date: ").append(rating.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
                ratingsText.append("ID Utilisateur: ").append(rating.getUserId()).append("\n");
                ratingsText.append("---\n");
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Avis de la salle");
            alert.setHeaderText(salle.getName() + " - " + ratings.size() + " avis");
            alert.setContentText(ratingsText.toString());
            alert.getDialogPane().setPrefWidth(500);
            alert.showAndWait();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement des avis");
        }
    }
    
    /**
     * Ajoute une nouvelle salle
     */
    private void addNewSalle() {
        // TODO: Implémenter l'ajout de salle
        showInfo("Ajout", "Fonctionnalité d'ajout à implémenter");
    }
    
    /**
     * Affiche un message d'erreur
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Affiche un message d'information
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
