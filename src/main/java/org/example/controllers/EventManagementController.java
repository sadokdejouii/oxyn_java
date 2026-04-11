package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.entities.AvisEvenement;
import org.example.entities.Evenement;
import org.example.entities.InscriptionEvenement;
import org.example.services.AvisEvenementServices;
import org.example.services.EvenementServices;
import org.example.services.InscriptionEvenementServices;

import java.net.URL;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Event Management Controller with detailed event cards, inscriptions and reviews filtering
 */
public class EventManagementController implements Initializable {

    // ==================== MODELS ====================

    /**
     * Model for Event list items with full details
     */
    public static class EventItem {
        private final int id;
        private final String titre;
        private final String description;
        private final String lieu;
        private final String ville;
        private final int placesMax;
        private final String debut;
        private final String fin;
        private final String statut;
        private final String createdAt;

        public EventItem(int id, String titre, String description, String lieu, String ville, 
                        int placesMax, String debut, String fin, String statut, String createdAt) {
            this.id = id;
            this.titre = titre;
            this.description = description;
            this.lieu = lieu;
            this.ville = ville;
            this.placesMax = placesMax;
            this.debut = debut;
            this.fin = fin;
            this.statut = statut;
            this.createdAt = createdAt;
        }

        public int getId() { return id; }
        public String getTitre() { return titre; }
        public String getDescription() { return description; }
        public String getLieu() { return lieu; }
        public String getVille() { return ville; }
        public int getPlacesMax() { return placesMax; }
        public String getDebut() { return debut; }
        public String getFin() { return fin; }
        public String getStatut() { return statut; }
        public String getCreatedAt() { return createdAt; }
    }

    /**
     * Model for Inscription list items (simplified)
     */
    public static class InscriptionItem {
        private final int id;
        private final String userName;
        private final String dateInscription;
        private final String statut;
        private final int idEvenement;
        private final int idUser;

        public InscriptionItem(int id, String userName, String dateInscription, String statut, int idEvenement, int idUser) {
            this.id = id;
            this.userName = userName;
            this.dateInscription = dateInscription;
            this.statut = statut;
            this.idEvenement = idEvenement;
            this.idUser = idUser;
        }

        public int getId() { return id; }
        public String getUserName() { return userName; }
        public String getDateInscription() { return dateInscription; }
        public String getStatut() { return statut; }
        public int getIdEvenement() { return idEvenement; }
        public int getIdUser() { return idUser; }
    }

    /**
     * Model for Avis (Review) list items (simplified)
     */
    public static class AvisItem {
        private final int id;
        private final String userName;
        private final int note;
        private final String commentaire;
        private final String createdAt;
        private final int idEvenement;
        private final int idUser;

        public AvisItem(int id, String userName, int note, String commentaire, String createdAt, int idEvenement, int idUser) {
            this.id = id;
            this.userName = userName;
            this.note = note;
            this.commentaire = commentaire;
            this.createdAt = createdAt;
            this.idEvenement = idEvenement;
            this.idUser = idUser;
        }

        public int getId() { return id; }
        public String getUserName() { return userName; }
        public int getNote() { return note; }
        public String getCommentaire() { return commentaire; }
        public String getCreatedAt() { return createdAt; }
        public int getIdEvenement() { return idEvenement; }
        public int getIdUser() { return idUser; }
    }

    // ==================== FXML COMPONENTS ====================
    
    @FXML private VBox eventsMainSection;
    @FXML private VBox inscriptionsDetailSection;
    @FXML private VBox reviewsDetailSection;

    @FXML private TilePane eventsGridPane;
    @FXML private TilePane inscriptionsGridPane;
    @FXML private TilePane reviewsGridPane;

    @FXML private HBox eventsPaginationBox;
    @FXML private HBox inscriptionsPaginationBox;
    @FXML private HBox reviewsPaginationBox;

    @FXML private ListView<InscriptionItem> inscriptionsListView;
    @FXML private ListView<AvisItem> reviewsListView;

    @FXML private Button backFromInscriptionsBtn;
    @FXML private Button backFromReviewsBtn;
    @FXML private Button ajouterEvenementBtn;

    // Search Fields
    @FXML private TextField eventsSearchField;
    @FXML private TextField inscriptionsSearchField;
    @FXML private TextField avisSearchField;

    // Sort ComboBoxes
    @FXML private ComboBox<String> eventsSortByCombo;
    @FXML private ComboBox<String> eventsSortOrderCombo;
    @FXML private ComboBox<String> inscriptionsSortByCombo;
    @FXML private ComboBox<String> inscriptionsSortOrderCombo;
    @FXML private ComboBox<String> avisSortByCombo;
    @FXML private ComboBox<String> avisSortOrderCombo;

    // ==================== DATA & SERVICES ====================
    private static final int ITEMS_PER_PAGE = 6;
    
    private final ObservableList<EventItem> eventsList = FXCollections.observableArrayList();
    private final ObservableList<InscriptionItem> inscriptionsList = FXCollections.observableArrayList();
    private final ObservableList<AvisItem> reviewsList = FXCollections.observableArrayList();

    private final EvenementServices evenementServices = new EvenementServices();
    private final InscriptionEvenementServices inscriptionServices = new InscriptionEvenementServices();
    private final AvisEvenementServices avisServices = new AvisEvenementServices();
    private final DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    private int currentSelectedEventId = -1;
    
    // Pagination tracking
    private int eventsPage = 0;
    private int inscriptionsPage = 0;
    private int reviewsPage = 0;

    private enum View {
        EVENTS, INSCRIPTIONS, REVIEWS
    }

    private View currentView = View.EVENTS;

    // ==================== LIFECYCLE ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSearchAndSort();
        loadData();
        showView(View.EVENTS);
    }

    private void setupSearchAndSort() {
        // Events sort options
        eventsSortByCombo.setItems(FXCollections.observableArrayList(
            "Titre", "Lieu", "Ville", "Statut", "Date Début"
        ));
        eventsSortByCombo.setValue("Titre");
        
        eventsSortOrderCombo.setItems(FXCollections.observableArrayList(
            "Croissant ↑", "Décroissant ↓"
        ));
        eventsSortOrderCombo.setValue("Croissant ↑");

        // Inscriptions sort options
        inscriptionsSortByCombo.setItems(FXCollections.observableArrayList(
            "Statut", "Date Inscription"
        ));
        inscriptionsSortByCombo.setValue("Statut");
        
        inscriptionsSortOrderCombo.setItems(FXCollections.observableArrayList(
            "Croissant ↑", "Décroissant ↓"
        ));
        inscriptionsSortOrderCombo.setValue("Croissant ↑");

        // Avis sort options
        avisSortByCombo.setItems(FXCollections.observableArrayList(
            "Note"
        ));
        avisSortByCombo.setValue("Note");
        
        avisSortOrderCombo.setItems(FXCollections.observableArrayList(
            "Croissant ↑", "Décroissant ↓"
        ));
        avisSortOrderCombo.setValue("Croissant ↑");

        // Add listeners for real-time search and sort
        eventsSearchField.textProperty().addListener((obs, oldVal, newVal) -> filterAndSortEvents());
        eventsSortByCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterAndSortEvents());
        eventsSortOrderCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterAndSortEvents());

        inscriptionsSearchField.textProperty().addListener((obs, oldVal, newVal) -> filterAndSortInscriptions());
        inscriptionsSortByCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterAndSortInscriptions());
        inscriptionsSortOrderCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterAndSortInscriptions());

        avisSearchField.textProperty().addListener((obs, oldVal, newVal) -> filterAndSortAvis());
        avisSortByCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterAndSortAvis());
        avisSortOrderCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterAndSortAvis());
    }

    @FXML
    void openAddEventForm(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/AjouterEvenements.fxml"));
            if (loader.getLocation() == null) {
                throw new RuntimeException("FXML file not found: /FXML/AjouterEvenements.fxml");
            }
            Stage addEventStage = new Stage();
            addEventStage.setTitle("Ajouter un Nouvel Événement");
            Scene scene = new Scene(loader.load(), 700, 850);
            addEventStage.setScene(scene);
            addEventStage.initModality(Modality.APPLICATION_MODAL);
            addEventStage.showAndWait();

            // Refresh data after adding event
            loadData();
            showView(View.EVENTS);
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur lors de l'ouverture du formulaire");
            alert.setContentText("Détail: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void openModifyEventForm(int eventId, String eventTitle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/ModifierEvenements.fxml"));
            if (loader.getLocation() == null) {
                throw new RuntimeException("FXML file not found: /FXML/ModifierEvenements.fxml");
            }
            Stage modifyEventStage = new Stage();
            modifyEventStage.setTitle("Modifier l'Événement: " + eventTitle);
            Scene scene = new Scene(loader.load(), 700, 850);
            modifyEventStage.setScene(scene);
            modifyEventStage.initModality(Modality.APPLICATION_MODAL);
            
            // Set event data in the controller
            ModifierEvenementController controller = loader.getController();
            controller.setEventData(eventId);
            
            modifyEventStage.showAndWait();

            // Refresh data after modifying event
            loadData();
            showView(View.EVENTS);
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur lors de l'ouverture du formulaire");
            alert.setContentText("Détail: " + e.getMessage());
            alert.showAndWait();
        }
    }

    // ==================== VIEW MANAGEMENT ====================

    private void showView(View view) {
        currentView = view;

        eventsMainSection.setVisible(false);
        eventsMainSection.setManaged(false);
        inscriptionsDetailSection.setVisible(false);
        inscriptionsDetailSection.setManaged(false);
        reviewsDetailSection.setVisible(false);
        reviewsDetailSection.setManaged(false);

        switch (view) {
            case EVENTS:
                eventsMainSection.setVisible(true);
                eventsMainSection.setManaged(true);
                break;
            case INSCRIPTIONS:
                inscriptionsDetailSection.setVisible(true);
                inscriptionsDetailSection.setManaged(true);
                break;
            case REVIEWS:
                reviewsDetailSection.setVisible(true);
                reviewsDetailSection.setManaged(true);
                break;
        }
    }

    @FXML
    private void handleBackToEvents() {
        currentSelectedEventId = -1;
        showView(View.EVENTS);
    }

    private void showInscriptionsForEvent(int eventId) {
        currentSelectedEventId = eventId;
        inscriptionsPage = 0;
        filterInscriptionsByEvent(eventId);
        showView(View.INSCRIPTIONS);
    }

    private void showReviewsForEvent(int eventId) {
        currentSelectedEventId = eventId;
        reviewsPage = 0;
        filterReviewsByEvent(eventId);
        showView(View.REVIEWS);
    }

    // ==================== CARD BUILDERS ====================

    private VBox buildEventCard(EventItem item) {
        VBox card = new VBox();
        card.getStyleClass().add("list-card");
        card.setSpacing(10);
        card.setPadding(new Insets(18));

        // Title
        Label titleLabel = new Label(item.getTitre());
        titleLabel.getStyleClass().add("list-card-title");
        titleLabel.setStyle("-fx-font-size: 16px;");

        // Description
        Label descLabel = new Label(item.getDescription());
        descLabel.getStyleClass().add("list-card-comment");
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 13px;");

        // Details row 1: Location and Places
        HBox details1Box = new HBox();
        details1Box.setSpacing(12);
        details1Box.setStyle("-fx-alignment: CENTER_LEFT;");

        Label lieuLabel = new Label("📍 " + (item.getLieu().isEmpty() ? "N/A" : item.getLieu()));
        lieuLabel.getStyleClass().add("list-card-detail");

        Label placesLabel = new Label("Capacité: " + item.getPlacesMax());
        placesLabel.getStyleClass().add("list-card-detail");

        details1Box.getChildren().addAll(lieuLabel, placesLabel);

        // Details row 2: Dates
        HBox details2Box = new HBox();
        details2Box.setSpacing(12);
        details2Box.setStyle("-fx-alignment: CENTER_LEFT;");

        Label startLabel = new Label("Début: " + item.getDebut());
        startLabel.getStyleClass().add("list-card-detail");

        Label endLabel = new Label("Fin: " + item.getFin());
        endLabel.getStyleClass().add("list-card-detail");

        details2Box.getChildren().addAll(startLabel, endLabel);

        // Details row 3: Created at and Status
        HBox details3Box = new HBox();
        details3Box.setSpacing(12);
        details3Box.setStyle("-fx-alignment: CENTER_LEFT;");

        Label createdLabel = new Label("Créé: " + item.getCreatedAt());
        createdLabel.getStyleClass().add("list-card-detail");

        details3Box.getChildren().add(createdLabel);

        // Status badge
        Label statusLabel = new Label(item.getStatut());
        statusLabel.getStyleClass().add("list-card-status");
        statusLabel.getStyleClass().add("event-status");
        applyEventStatusStyle(statusLabel, item.getStatut());

        HBox statusBox = new HBox();
        statusBox.getChildren().add(statusLabel);

        // Action buttons - Row 1: Info buttons
        HBox actionBox1 = new HBox();
        actionBox1.setSpacing(10);
        actionBox1.setStyle("-fx-alignment: CENTER_LEFT;");
        actionBox1.setPadding(new Insets(8, 0, 0, 0));

        Button inscriptionsBtn = new Button("\ud83d\udccb Voir les Inscriptions");
        inscriptionsBtn.getStyleClass().add("action-btn-primary");
        inscriptionsBtn.setStyle("-fx-font-size: 11px;");
        inscriptionsBtn.setOnAction(e -> showInscriptionsForEvent(item.getId()));

        Button reviewsBtn = new Button("\u2b50 Voir les Avis");
        reviewsBtn.getStyleClass().add("action-btn-secondary");
        reviewsBtn.setStyle("-fx-font-size: 11px;");
        reviewsBtn.setOnAction(e -> showReviewsForEvent(item.getId()));

        actionBox1.getChildren().addAll(inscriptionsBtn, reviewsBtn);

        // Action buttons - Row 2: Modify and Delete buttons
        HBox actionBox2 = new HBox();
        actionBox2.setSpacing(10);
        actionBox2.setStyle("-fx-alignment: CENTER_LEFT;");
        actionBox2.setPadding(new Insets(8, 0, 0, 0));

        Button modifierBtn = new Button("✏️ Modifier");
        modifierBtn.getStyleClass().add("action-btn-primary");
        modifierBtn.setStyle("-fx-font-size: 11px;");
        modifierBtn.setOnAction(e -> openModifyEventForm(item.getId(), item.getTitre()));

        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.getStyleClass().add("action-btn-delete");
        deleteBtn.setStyle("-fx-font-size: 11px;");
        deleteBtn.setOnAction(e -> deleteEventWithConfirmation(item.getId(), item.getTitre()));

        actionBox2.getChildren().addAll(modifierBtn, deleteBtn);

        card.getChildren().addAll(titleLabel, descLabel, details1Box, details2Box, details3Box, statusBox, actionBox1, actionBox2);
        return card;
    }

    private VBox buildInscriptionCard(InscriptionItem item) {
        VBox card = new VBox();
        card.getStyleClass().add("list-card");
        card.setSpacing(8);
        card.setPadding(new Insets(16));

        // Title with user name
        Label titleLabel = new Label("👤 " + item.getUserName());
        titleLabel.getStyleClass().add("list-card-title");

        // Details
        HBox detailsBox = new HBox();
        detailsBox.setSpacing(12);
        detailsBox.setStyle("-fx-alignment: CENTER_LEFT;");

        Label dateLabel = new Label("📅 " + item.getDateInscription());
        dateLabel.getStyleClass().add("list-card-detail");

        detailsBox.getChildren().add(dateLabel);

        // Status badge
        Label statusLabel = new Label(item.getStatut());
        statusLabel.getStyleClass().add("list-card-status");
        statusLabel.getStyleClass().add("inscription-status");
        applyInscriptionStatusStyle(statusLabel, item.getStatut());

        HBox statusBox = new HBox();
        statusBox.getChildren().add(statusLabel);

        // Delete button
        HBox actionBox = new HBox();
        actionBox.setSpacing(10);
        actionBox.setStyle("-fx-alignment: CENTER_LEFT;");
        actionBox.setPadding(new Insets(8, 0, 0, 0));

        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.getStyleClass().add("action-btn-delete");
        deleteBtn.setStyle("-fx-font-size: 11px;");
        deleteBtn.setOnAction(e -> deleteInscriptionWithConfirmation(item.getId(), item.getUserName()));

        actionBox.getChildren().add(deleteBtn);

        card.getChildren().addAll(titleLabel, detailsBox, statusBox, actionBox);
        return card;
    }

    private VBox buildReviewCard(AvisItem item) {
        VBox card = new VBox();
        card.getStyleClass().add("list-card");
        card.setSpacing(8);
        card.setPadding(new Insets(16));

        // Title with user name and rating
        HBox titleBox = new HBox();
        titleBox.setSpacing(12);
        titleBox.setStyle("-fx-alignment: CENTER_LEFT;");

        Label titleLabel = new Label("👤 " + item.getUserName());
        titleLabel.getStyleClass().add("list-card-title");

        Label ratingLabel = new Label(buildStarRating(item.getNote()));
        ratingLabel.getStyleClass().add("list-card-rating");
        ratingLabel.getStyleClass().add("avis-rating");

        titleBox.getChildren().addAll(titleLabel, ratingLabel);

        // Comment
        Label commentLabel = new Label(item.getCommentaire());
        commentLabel.getStyleClass().add("list-card-comment");
        commentLabel.setWrapText(true);

        // Details
        HBox detailsBox = new HBox();
        detailsBox.setSpacing(12);
        detailsBox.setStyle("-fx-alignment: CENTER_LEFT;");

        Label dateLabel = new Label("📅 " + item.getCreatedAt());
        dateLabel.getStyleClass().add("list-card-detail");

        detailsBox.getChildren().add(dateLabel);

        // Delete button
        HBox actionBox = new HBox();
        actionBox.setSpacing(10);
        actionBox.setStyle("-fx-alignment: CENTER_LEFT;");
        actionBox.setPadding(new Insets(8, 0, 0, 0));

        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.getStyleClass().add("action-btn-delete");
        deleteBtn.setStyle("-fx-font-size: 11px;");
        deleteBtn.setOnAction(e -> deleteAvisWithConfirmation(item.getId(), item.getUserName()));

        actionBox.getChildren().add(deleteBtn);

        card.getChildren().addAll(titleBox, commentLabel, detailsBox, actionBox);
        return card;
    }

    // ==================== STYLING HELPERS ====================

    private String buildStarRating(int note) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < note; i++) {
            stars.append("⭐");
        }
        stars.append(" ").append(note).append("/5");
        return stars.toString();
    }

    private void applyEventStatusStyle(Label statusLabel, String status) {
        status = status.toLowerCase();
        if (status.contains("active") || status.contains("ongoing")) {
            statusLabel.getStyleClass().add("status-active");
        } else if (status.contains("cancelled") || status.contains("closed")) {
            statusLabel.getStyleClass().add("status-error");
        } else if (status.contains("draft") || status.contains("pending")) {
            statusLabel.getStyleClass().add("status-warning");
        } else {
            statusLabel.getStyleClass().add("status-default");
        }
    }

    private void applyInscriptionStatusStyle(Label statusLabel, String status) {
        status = status.toLowerCase();
        if (status.contains("confirmed") || status.contains("accepted")) {
            statusLabel.getStyleClass().add("status-active");
        } else if (status.contains("rejected") || status.contains("cancelled")) {
            statusLabel.getStyleClass().add("status-error");
        } else if (status.contains("pending") || status.contains("waiting")) {
            statusLabel.getStyleClass().add("status-warning");
        } else {
            statusLabel.getStyleClass().add("status-default");
        }
    }

    // ==================== DATA LOADING ====================

    private void loadData() {
        loadEventsData();
        loadInscriptionsData();
        loadReviewsData();
    }

    private void loadEventsData() {
        eventsList.clear();
        
        try {
            List<Evenement> list = evenementServices.afficher();
            for (Evenement e : list) {
                Date d0 = e.getDateDebut();
                Date d1 = e.getDateFin();
                Date createdDate = e.getCreatedAt();
                EventItem eventItem = new EventItem(
                        e.getId(),
                        safe(e.getTitre()),
                        safe(e.getDescription()),
                        safe(e.getLieu()),
                        safe(e.getVille()),
                        e.getPlacesMax(),
                        d0 == null ? "—" : fmt.format(d0),
                        d1 == null ? "—" : fmt.format(d1),
                        safe(e.getStatut()),
                        createdDate == null ? "—" : fmt.format(createdDate)
                );
                eventsList.add(eventItem);
            }
        } catch (SQLException ex) {
            // Silent fail
        }
        
        eventsPage = 0;
        displayEventsPaginated();
    }

    private void loadInscriptionsData() {
        // This will be populated only when filtering
    }

    private void loadReviewsData() {
        // This will be populated only when filtering
    }

    // ==================== FILTERING ====================

    private void filterInscriptionsByEvent(int eventId) {
        inscriptionsList.clear();

        try {
            List<InscriptionEvenement> allInscriptions = inscriptionServices.afficher();
            List<InscriptionEvenement> filtered = allInscriptions.stream()
                    .filter(insc -> insc.getIdEvenement() == eventId)
                    .collect(Collectors.toList());

            for (InscriptionEvenement insc : filtered) {
                Date d = insc.getDateInscription();
                inscriptionsList.add(new InscriptionItem(
                        insc.getId(),
                        "Utilisateur",
                        d == null ? "—" : fmt.format(d),
                        safe(insc.getStatut()),
                        insc.getIdEvenement(),
                        insc.getIdUser()
                ));
            }
        } catch (SQLException ex) {
            // Silent fail
        }

        inscriptionsPage = 0;
        displayInscriptionsPaginated();
    }

    private void filterReviewsByEvent(int eventId) {
        reviewsList.clear();

        try {
            List<AvisEvenement> allReviews = avisServices.afficher();
            List<AvisEvenement> filtered = allReviews.stream()
                    .filter(avis -> avis.getIdEvenement() == eventId)
                    .collect(Collectors.toList());

            for (AvisEvenement avis : filtered) {
                Date d = avis.getCreatedAt();
                reviewsList.add(new AvisItem(
                        avis.getId(),
                        "Utilisateur",
                        avis.getNote(),
                        safe(avis.getCommentaire()),
                        d == null ? "—" : fmt.format(d),
                        avis.getIdEvenement(),
                        avis.getIdUser()
                ));
            }
        } catch (SQLException ex) {
            // Silent fail
        }

        reviewsPage = 0;
        displayReviewsPaginated();
    }

    // ==================== PAGINATION HELPERS ====================

    private void displayEventsPaginated() {
        eventsGridPane.getChildren().clear();
        eventsPaginationBox.getChildren().clear();

        if (eventsList.isEmpty()) {
            Label emptyLabel = new Label("Aucun événement disponible. Créez des événements pour les voir ici.");
            emptyLabel.getStyleClass().add("empty-state-label");
            emptyLabel.setStyle("-fx-text-alignment: center; -fx-padding: 40;");
            eventsGridPane.getChildren().add(emptyLabel);
            return;
        }

        int totalPages = (int) Math.ceil((double) eventsList.size() / ITEMS_PER_PAGE);
        int startIndex = eventsPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, eventsList.size());

        for (int i = startIndex; i < endIndex; i++) {
            eventsGridPane.getChildren().add(buildEventCard(eventsList.get(i)));
        }

        buildPaginationControls(eventsPaginationBox, eventsPage, totalPages, this::handleEventsPrevPage, this::handleEventsNextPage);
    }

    private void displayInscriptionsPaginated() {
        inscriptionsGridPane.getChildren().clear();
        inscriptionsPaginationBox.getChildren().clear();

        if (inscriptionsList.isEmpty()) {
            Label emptyLabel = new Label("Aucune inscription pour cet événement.");
            emptyLabel.getStyleClass().add("empty-state-label");
            emptyLabel.setStyle("-fx-text-alignment: center; -fx-padding: 60 20;");
            inscriptionsGridPane.getChildren().add(emptyLabel);
            return;
        }

        int totalPages = (int) Math.ceil((double) inscriptionsList.size() / ITEMS_PER_PAGE);
        int startIndex = inscriptionsPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, inscriptionsList.size());

        for (int i = startIndex; i < endIndex; i++) {
            inscriptionsGridPane.getChildren().add(buildInscriptionCard(inscriptionsList.get(i)));
        }

        buildPaginationControls(inscriptionsPaginationBox, inscriptionsPage, totalPages, this::handleInscriptionsPrevPage, this::handleInscriptionsNextPage);
    }

    private void displayReviewsPaginated() {
        reviewsGridPane.getChildren().clear();
        reviewsPaginationBox.getChildren().clear();

        if (reviewsList.isEmpty()) {
            Label emptyLabel = new Label("Aucun avis pour cet événement.");
            emptyLabel.getStyleClass().add("empty-state-label");
            emptyLabel.setStyle("-fx-text-alignment: center; -fx-padding: 60 20;");
            reviewsGridPane.getChildren().add(emptyLabel);
            return;
        }

        int totalPages = (int) Math.ceil((double) reviewsList.size() / ITEMS_PER_PAGE);
        int startIndex = reviewsPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, reviewsList.size());

        for (int i = startIndex; i < endIndex; i++) {
            reviewsGridPane.getChildren().add(buildReviewCard(reviewsList.get(i)));
        }

        buildPaginationControls(reviewsPaginationBox, reviewsPage, totalPages, this::handleReviewsPrevPage, this::handleReviewsNextPage);
    }

    @FunctionalInterface
    private interface PaginationActionListener {
        void onAction();
    }

    private void buildPaginationControls(HBox paginationBox, int currentPage, int totalPages, 
                                         PaginationActionListener prevAction, PaginationActionListener nextAction) {
        if (totalPages <= 1) {
            return;
        }

        Button prevBtn = new Button("← Précédent");
        prevBtn.getStyleClass().add("pagination-btn");
        prevBtn.setDisable(currentPage == 0);
        prevBtn.setOnAction(e -> prevAction.onAction());

        Label pageLabel = new Label("Page " + (currentPage + 1) + " sur " + totalPages);
        pageLabel.getStyleClass().add("pagination-label");

        Button nextBtn = new Button("Suivant →");
        nextBtn.getStyleClass().add("pagination-btn");
        nextBtn.setDisable(currentPage >= totalPages - 1);
        nextBtn.setOnAction(e -> nextAction.onAction());

        paginationBox.getChildren().addAll(prevBtn, pageLabel, nextBtn);
    }

    private void handleEventsPrevPage() {
        if (eventsPage > 0) {
            eventsPage--;
            displayEventsPaginated();
        }
    }

    private void handleEventsNextPage() {
        int totalPages = (int) Math.ceil((double) eventsList.size() / ITEMS_PER_PAGE);
        if (eventsPage < totalPages - 1) {
            eventsPage++;
            displayEventsPaginated();
        }
    }

    private void handleInscriptionsPrevPage() {
        if (inscriptionsPage > 0) {
            inscriptionsPage--;
            displayInscriptionsPaginated();
        }
    }

    private void handleInscriptionsNextPage() {
        int totalPages = (int) Math.ceil((double) inscriptionsList.size() / ITEMS_PER_PAGE);
        if (inscriptionsPage < totalPages - 1) {
            inscriptionsPage++;
            displayInscriptionsPaginated();
        }
    }

    private void handleReviewsPrevPage() {
        if (reviewsPage > 0) {
            reviewsPage--;
            displayReviewsPaginated();
        }
    }

    private void handleReviewsNextPage() {
        int totalPages = (int) Math.ceil((double) reviewsList.size() / ITEMS_PER_PAGE);
        if (reviewsPage < totalPages - 1) {
            reviewsPage++;
            displayReviewsPaginated();
        }
    }

    // ==================== DELETE METHODS ====================

    private void deleteEventWithConfirmation(int eventId, String eventTitle) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("⚠️  Supprimer cet événement ?");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer \"" + eventTitle + "\" ?\n\nCette action est irréversible et supprimera aussi toutes les inscriptions et avis associés.");
        confirmAlert.getDialogPane().setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12;");
        
        stylizeAlert(confirmAlert);

        if (confirmAlert.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
            try {
                evenementServices.supprimer(eventId);
                showStyledSuccess("Événement supprimé ✓", "L'événement \"" + eventTitle + "\" a été supprimé avec succès.");
                
                // Refresh data
                loadData();
                showView(View.EVENTS);
            } catch (SQLException ex) {
                showStyledError("Erreur lors de la suppression", "Détail: " + ex.getMessage());
            }
        }
    }

    private void deleteInscriptionWithConfirmation(int inscriptionId, String userName) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("⚠️  Supprimer cette inscription ?");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer l'inscription de \"" + userName + "\" ?\n\nCette action est irréversible.");
        confirmAlert.getDialogPane().setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12;");
        
        stylizeAlert(confirmAlert);

        if (confirmAlert.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
            try {
                inscriptionServices.supprimer(inscriptionId);
                showStyledSuccess("Inscription supprimée ✓", "L'inscription de \"" + userName + "\" a été supprimée avec succès.");
                
                // Refresh data
                filterInscriptionsByEvent(currentSelectedEventId);
            } catch (SQLException ex) {
                showStyledError("Erreur lors de la suppression", "Détail: " + ex.getMessage());
            }
        }
    }

    private void deleteAvisWithConfirmation(int avisId, String userName) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("⚠️  Supprimer cet avis ?");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer l'avis de \"" + userName + "\" ?\n\nCette action est irréversible.");
        confirmAlert.getDialogPane().setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12;");
        
        stylizeAlert(confirmAlert);

        if (confirmAlert.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
            try {
                avisServices.supprimer(avisId);
                showStyledSuccess("Avis supprimé ✓", "L'avis de \"" + userName + "\" a été supprimé avec succès.");
                
                // Refresh data
                filterReviewsByEvent(currentSelectedEventId);
            } catch (SQLException ex) {
                showStyledError("Erreur lors de la suppression", "Détail: " + ex.getMessage());
            }
        }
    }

    private void stylizeAlert(Alert alert) {
        // Style the dialog pane
        javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #F5F8FF 0%, #FFFFFF 100%);" +
            "-fx-border-color: rgba(0, 153, 204, 0.40);" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 8;" +
            "-fx-padding: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.15), 12, 0, 0, 4);"
        );
        
        // Style all buttons
        dialogPane.getButtonTypes().forEach(buttonType -> {
            javafx.scene.control.Button button = (javafx.scene.control.Button) dialogPane.lookupButton(buttonType);
            if (button != null) {
                if (buttonType == javafx.scene.control.ButtonType.OK) {
                    button.setStyle(
                        "-fx-background-color: linear-gradient(to bottom, rgba(244, 67, 54, 0.35) 0%, rgba(229, 57, 53, 0.45) 100%);" +
                        "-fx-border-color: rgba(244, 67, 54, 0.55);" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-text-fill: #FFFFFF;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 24;" +
                        "-fx-font-size: 12;" +
                        "-fx-cursor: hand;"
                    );
                } else {
                    button.setStyle(
                        "-fx-background-color: rgba(200, 200, 200, 0.25);" +
                        "-fx-border-color: rgba(150, 150, 150, 0.40);" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-text-fill: #333333;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 24;" +
                        "-fx-font-size: 12;" +
                        "-fx-cursor: hand;"
                    );
                }
            }
        });
    }

    private void showStyledSuccess(String title, String message) {
        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
        successAlert.setTitle(title);
        successAlert.setHeaderText(null);
        successAlert.setContentText(message);
        
        javafx.scene.control.DialogPane dialogPane = successAlert.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #F0F8F0 0%, #FFFFFF 100%);" +
            "-fx-border-color: rgba(76, 175, 80, 0.40);" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 8;" +
            "-fx-padding: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.10), 10, 0, 0, 3);"
        );
        
        // Style all buttons
        dialogPane.getButtonTypes().forEach(buttonType -> {
            javafx.scene.control.Button button = (javafx.scene.control.Button) dialogPane.lookupButton(buttonType);
            if (button != null) {
                button.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, rgba(76, 175, 80, 0.35) 0%, rgba(56, 142, 60, 0.45) 100%);" +
                    "-fx-border-color: rgba(76, 175, 80, 0.55);" +
                    "-fx-border-radius: 6;" +
                    "-fx-background-radius: 6;" +
                    "-fx-text-fill: #FFFFFF;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10 24;" +
                    "-fx-font-size: 12;" +
                    "-fx-cursor: hand;"
                );
            }
        });
        
        successAlert.showAndWait();
    }

    private void showStyledError(String title, String message) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle(title);
        errorAlert.setHeaderText(null);
        errorAlert.setContentText(message);
        
        javafx.scene.control.DialogPane dialogPane = errorAlert.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #FFEBEE 0%, #FFFFFF 100%);" +
            "-fx-border-color: rgba(244, 67, 54, 0.40);" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 8;" +
            "-fx-padding: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.10), 10, 0, 0, 3);"
        );
        
        // Style all buttons
        dialogPane.getButtonTypes().forEach(buttonType -> {
            javafx.scene.control.Button button = (javafx.scene.control.Button) dialogPane.lookupButton(buttonType);
            if (button != null) {
                button.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, rgba(244, 67, 54, 0.30) 0%, rgba(229, 57, 53, 0.40) 100%);" +
                    "-fx-border-color: rgba(244, 67, 54, 0.50);" +
                    "-fx-border-radius: 6;" +
                    "-fx-background-radius: 6;" +
                    "-fx-text-fill: #FFFFFF;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10 24;" +
                    "-fx-font-size: 12;" +
                    "-fx-cursor: hand;"
                );
            }
        });
        
        errorAlert.showAndWait();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    // ==================== SEARCH & SORT METHODS ====================

    private void filterAndSortEvents() {
        String searchText = eventsSearchField.getText().toLowerCase();
        String sortBy = eventsSortByCombo.getValue();
        boolean ascending = !eventsSortOrderCombo.getValue().contains("Décroissant");

        List<EventItem> filtered = eventsList.stream()
            .filter(item -> searchText.isEmpty() || 
                item.getTitre().toLowerCase().contains(searchText) ||
                item.getLieu().toLowerCase().contains(searchText) ||
                item.getVille().toLowerCase().contains(searchText) ||
                item.getStatut().toLowerCase().contains(searchText))
            .sorted((a, b) -> {
                int result = 0;
                switch(sortBy) {
                    case "Titre": result = a.getTitre().compareTo(b.getTitre()); break;
                    case "Lieu": result = a.getLieu().compareTo(b.getLieu()); break;
                    case "Ville": result = a.getVille().compareTo(b.getVille()); break;
                    case "Statut": result = a.getStatut().compareTo(b.getStatut()); break;
                    case "Date Début": result = a.getDebut().compareTo(b.getDebut()); break;
                }
                return ascending ? result : -result;
            })
            .collect(Collectors.toList());

        displayEventCards(filtered);
    }

    private void filterAndSortInscriptions() {
        String searchText = inscriptionsSearchField.getText().toLowerCase();
        String sortBy = inscriptionsSortByCombo.getValue();
        boolean ascending = !inscriptionsSortOrderCombo.getValue().contains("Décroissant");

        List<InscriptionItem> filtered = inscriptionsList.stream()
            .filter(item -> searchText.isEmpty() || 
                item.getStatut().toLowerCase().contains(searchText))
            .sorted((a, b) -> {
                int result = 0;
                switch(sortBy) {
                    case "Statut": result = a.getStatut().compareTo(b.getStatut()); break;
                    case "Date Inscription": result = a.getDateInscription().compareTo(b.getDateInscription()); break;
                }
                return ascending ? result : -result;
            })
            .collect(Collectors.toList());

        displayInscriptionCards(filtered);
    }

    private void filterAndSortAvis() {
        String searchText = avisSearchField.getText().toLowerCase();
        String sortBy = avisSortByCombo.getValue();
        boolean ascending = !avisSortOrderCombo.getValue().contains("Décroissant");

        List<AvisItem> filtered = reviewsList.stream()
            .filter(item -> searchText.isEmpty() || 
                String.valueOf(item.getNote()).contains(searchText))
            .sorted((a, b) -> {
                int result = 0;
                if ("Note".equals(sortBy)) {
                    result = Integer.compare(a.getNote(), b.getNote());
                }
                return ascending ? result : -result;
            })
            .collect(Collectors.toList());

        displayAvisCards(filtered);
    }

    private void displayEventCards(List<EventItem> items) {
        eventsGridPane.getChildren().clear();
        for (EventItem item : items) {
            eventsGridPane.getChildren().add(buildEventCard(item));
        }
    }

    private void displayInscriptionCards(List<InscriptionItem> items) {
        inscriptionsGridPane.getChildren().clear();
        for (InscriptionItem item : items) {
            inscriptionsGridPane.getChildren().add(buildInscriptionCard(item));
        }
    }

    private void displayAvisCards(List<AvisItem> items) {
        reviewsGridPane.getChildren().clear();
        for (AvisItem item : items) {
            reviewsGridPane.getChildren().add(buildReviewCard(item));
        }
    }
}
