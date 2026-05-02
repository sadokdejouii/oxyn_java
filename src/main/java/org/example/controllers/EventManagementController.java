package org.example.controllers;

import org.example.services.GoogleCalendarSyncService;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.entities.AvisEvenement;
import org.example.entities.Evenement;
import org.example.entities.InscriptionEvenement;
import org.example.services.AvisEvenementServices;
import org.example.services.EvenementServices;
import org.example.services.InscriptionEvenementServices;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    @FXML private VBox eventFormSection;
    @FXML private VBox eventFormContainer;
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
    @FXML private Button backFromFormBtn;
    @FXML private Button ajouterEvenementBtn;

    @FXML private Label totalEventsStatValue;
    @FXML private Label totalInscriptionsStatValue;
    @FXML private Label totalAvisStatValue;
    @FXML private Label activeEventsStatValue;
    @FXML private Label citiesEventsStatValue;

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
    /** Snapshot used by the grid + pagination (search/sort apply on top of {@link #eventsList}). */
    private List<EventItem> eventsForGrid = new ArrayList<>();
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
        EVENTS, FORM, INSCRIPTIONS, REVIEWS
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
            VBox formRoot = loader.load();
            AjouterEvenementsController controller = loader.getController();
            controller.setEmbeddedMode(() -> {
                loadData();
                showView(View.EVENTS);
            });

            eventFormContainer.getChildren().setAll(formRoot);
            showView(View.FORM);
        } catch (Exception e) {
            e.printStackTrace();
            showStyledError("Erreur lors de l'ouverture du formulaire", "Détail: " + e.getMessage());
        }
    }

    private void openModifyEventForm(int eventId, String eventTitle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/ModifierEvenements.fxml"));
            if (loader.getLocation() == null) {
                throw new RuntimeException("FXML file not found: /FXML/ModifierEvenements.fxml");
            }
            VBox formRoot = loader.load();
            
            // Set event data in the controller
            ModifierEvenementController controller = loader.getController();
            controller.setEventData(eventId);
            controller.setEmbeddedMode(() -> {
                loadData();
                showView(View.EVENTS);
            });

            eventFormContainer.getChildren().setAll(formRoot);
            showView(View.FORM);
        } catch (Exception e) {
            e.printStackTrace();
            showStyledError("Erreur lors de l'ouverture du formulaire", "Détail: " + e.getMessage());
        }
    }

    // ==================== VIEW MANAGEMENT ====================

    private void showView(View view) {
        currentView = view;

        eventsMainSection.setVisible(false);
        eventsMainSection.setManaged(false);
        eventFormSection.setVisible(false);
        eventFormSection.setManaged(false);
        inscriptionsDetailSection.setVisible(false);
        inscriptionsDetailSection.setManaged(false);
        reviewsDetailSection.setVisible(false);
        reviewsDetailSection.setManaged(false);

        switch (view) {
            case EVENTS:
                eventsMainSection.setVisible(true);
                eventsMainSection.setManaged(true);
                break;
            case FORM:
                eventFormSection.setVisible(true);
                eventFormSection.setManaged(true);
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
        VBox card = new VBox(12);
        card.getStyleClass().add("list-card");
        card.setPadding(new Insets(18));
        card.setAlignment(Pos.TOP_CENTER);

        String visualKey = resolveVisualKey(item.getTitre() + " " + item.getDescription());
        StackPane coverPane = buildCoverPane(visualKey, resolveVisualText(visualKey));

        Label titleLabel = new Label(item.getTitre());
        titleLabel.getStyleClass().add("list-card-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        Label descLabel = new Label(item.getDescription().isBlank() ? "Description non disponible." : item.getDescription());
        descLabel.getStyleClass().add("list-card-comment");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(Double.MAX_VALUE);

        VBox detailsBox = new VBox(6,
                createDetailLabel("📍 " + (item.getLieu().isBlank() ? "Lieu à confirmer" : item.getLieu()) + " • " + (item.getVille().isBlank() ? "Ville non précisée" : item.getVille())),
                createDetailLabel("👥 Capacité : " + item.getPlacesMax() + " places"),
                createDetailLabel("🗓️ Début : " + item.getDebut()),
                createDetailLabel("⏳ Fin : " + item.getFin()),
                createDetailLabel("🕒 Créé : " + item.getCreatedAt())
        );
        detailsBox.setAlignment(Pos.CENTER);

        Label statusLabel = new Label(item.getStatut().isBlank() ? "Statut non défini" : item.getStatut());
        statusLabel.getStyleClass().addAll("list-card-status", "event-status");
        applyEventStatusStyle(statusLabel, item.getStatut());

        HBox statusBox = new HBox(statusLabel);
        statusBox.setAlignment(Pos.CENTER);

        Button inscriptionsBtn = new Button("� Inscriptions");
        inscriptionsBtn.getStyleClass().addAll("event-action-btn", "event-btn-inscriptions");
        inscriptionsBtn.setOnAction(e -> showInscriptionsForEvent(item.getId()));

        Button reviewsBtn = new Button("💬 Avis");
        reviewsBtn.getStyleClass().addAll("event-action-btn", "event-btn-avis");
        reviewsBtn.setOnAction(e -> showReviewsForEvent(item.getId()));

        HBox actionBox1 = new HBox(10, inscriptionsBtn, reviewsBtn);
        actionBox1.setAlignment(Pos.CENTER);

        Button modifierBtn = new Button("🛠 Modifier");
        modifierBtn.getStyleClass().addAll("event-action-btn", "event-btn-edit");
        modifierBtn.setOnAction(e -> openModifyEventForm(item.getId(), item.getTitre()));

        Button deleteBtn = new Button("🗑 Supprimer");
        deleteBtn.getStyleClass().addAll("event-action-btn", "event-btn-delete");
        deleteBtn.setOnAction(e -> deleteEventWithConfirmation(item.getId(), item.getTitre()));

        HBox actionBox2 = new HBox(10, modifierBtn, deleteBtn);
        actionBox2.setAlignment(Pos.CENTER);

        card.getChildren().addAll(coverPane, titleLabel, descLabel, detailsBox, statusBox, actionBox1, actionBox2);
        return card;
    }

    private VBox buildInscriptionCard(InscriptionItem item) {
        VBox card = new VBox(12);
        card.getStyleClass().add("list-card");
        card.setPadding(new Insets(18));
        card.setAlignment(Pos.TOP_CENTER);

        StackPane coverPane = buildCoverPane("inscription", "👥 Inscriptions");

        Label titleLabel = new Label(item.getUserName().isBlank() ? "Participant" : item.getUserName());
        titleLabel.getStyleClass().add("list-card-title");
        titleLabel.setWrapText(true);

        VBox detailsBox = new VBox(6,
                createDetailLabel("🎫 Événement #" + item.getIdEvenement()),
                createDetailLabel("🆔 Utilisateur #" + item.getIdUser()),
                createDetailLabel("📅 Inscrit le : " + item.getDateInscription())
        );
        detailsBox.setAlignment(Pos.CENTER);

        Label statusLabel = new Label(item.getStatut().isBlank() ? "Statut non défini" : item.getStatut());
        statusLabel.getStyleClass().addAll("list-card-status", "inscription-status");
        applyInscriptionStatusStyle(statusLabel, item.getStatut());

        HBox statusBox = new HBox(statusLabel);
        statusBox.setAlignment(Pos.CENTER);

        Button deleteBtn = new Button("🗑 Supprimer");
        deleteBtn.getStyleClass().addAll("event-action-btn", "event-btn-delete");
        deleteBtn.setOnAction(e -> deleteInscriptionWithConfirmation(item.getId(), item.getUserName()));

        HBox actionBox = new HBox(deleteBtn);
        actionBox.setAlignment(Pos.CENTER);

        card.getChildren().addAll(coverPane, titleLabel, detailsBox, statusBox, actionBox);
        return card;
    }

    private VBox buildReviewCard(AvisItem item) {
        VBox card = new VBox(12);
        card.getStyleClass().add("list-card");
        card.setPadding(new Insets(18));
        card.setAlignment(Pos.TOP_CENTER);

        StackPane coverPane = buildCoverPane("review", "⭐ Avis & Notes");

        Label titleLabel = new Label(item.getUserName().isBlank() ? "Utilisateur" : item.getUserName());
        titleLabel.getStyleClass().add("list-card-title");
        titleLabel.setWrapText(true);

        Label ratingLabel = new Label(buildStarRating(item.getNote()));
        ratingLabel.getStyleClass().addAll("list-card-rating", "avis-rating");

        Label commentLabel = new Label(item.getCommentaire().isBlank() ? "Aucun commentaire ajouté." : item.getCommentaire());
        commentLabel.getStyleClass().add("list-card-comment");
        commentLabel.setWrapText(true);
        commentLabel.setMaxWidth(Double.MAX_VALUE);

        VBox detailsBox = new VBox(6,
                createDetailLabel("🎫 Événement #" + item.getIdEvenement()),
                createDetailLabel("🆔 Utilisateur #" + item.getIdUser()),
                createDetailLabel("📅 Publié le : " + item.getCreatedAt())
        );
        detailsBox.setAlignment(Pos.CENTER);

        Button deleteBtn = new Button("🗑 Supprimer");
        deleteBtn.getStyleClass().addAll("event-action-btn", "event-btn-delete");
        deleteBtn.setOnAction(e -> deleteAvisWithConfirmation(item.getId(), item.getUserName()));

        HBox actionBox = new HBox(deleteBtn);
        actionBox.setAlignment(Pos.CENTER);

        card.getChildren().addAll(coverPane, titleLabel, ratingLabel, commentLabel, detailsBox, actionBox);
        return card;
    }

    // ==================== STYLING HELPERS ====================

    private Label createDetailLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("list-card-detail");
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private StackPane buildCoverPane(String visualKey, String text) {
        StackPane coverPane = new StackPane();
        coverPane.getStyleClass().addAll("back-card-cover", "back-cover-" + visualKey);
        coverPane.setPrefHeight(86);
        coverPane.setMinHeight(86);
        coverPane.setMaxWidth(Double.MAX_VALUE);

        Label coverText = new Label(text);
        coverText.getStyleClass().add("back-card-cover-text");
        coverPane.getChildren().add(coverText);
        return coverPane;
    }

    private String resolveVisualKey(String value) {
        String text = safe(value).toLowerCase();
        if (text.contains("yoga") || text.contains("zen") || text.contains("pilates")) {
            return "yoga";
        }
        if (text.contains("foot") || text.contains("football")) {
            return "football";
        }
        if (text.contains("basket")) {
            return "basket";
        }
        if (text.contains("run") || text.contains("course") || text.contains("marathon")) {
            return "running";
        }
        if (text.contains("music") || text.contains("concert") || text.contains("festival")) {
            return "music";
        }
        return "event";
    }

    private String resolveVisualText(String visualKey) {
        return switch (visualKey) {
            case "yoga" -> "☯ Yoga & Bien-être";
            case "football" -> "⚽ Football";
            case "basket" -> "🏀 Basketball";
            case "running" -> "🏃 Course & Fitness";
            case "music" -> "🎵 Événement spécial";
            default -> "✨ Event Spotlight";
        };
    }

    private String buildStarRating(int note) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < note; i++) {
            stars.append("⭐");
        }
        stars.append(" ").append(note).append("/5");
        return stars.toString();
    }

    private void applyEventStatusStyle(Label statusLabel, String status) {
        status = safe(status).toLowerCase();
        if (status.contains("termin")) {
            statusLabel.getStyleClass().add("status-finished");
        } else if (status.contains("annul") || status.contains("cancelled") || status.contains("closed")) {
            statusLabel.getStyleClass().add("status-error");
        } else if (status.contains("en cours") || status.contains("active") || status.contains("ongoing")) {
            statusLabel.getStyleClass().add("status-active");
        } else if (status.contains("à venir") || status.contains("a venir") || status.contains("draft") || status.contains("pending")) {
            statusLabel.getStyleClass().add("status-upcoming");
        } else {
            statusLabel.getStyleClass().add("status-default");
        }
    }

    private void applyInscriptionStatusStyle(Label statusLabel, String status) {
        status = safe(status).toLowerCase();
        if (status.contains("confirm") || status.contains("accepted")) {
            statusLabel.getStyleClass().add("status-active");
        } else if (status.contains("rejet") || status.contains("refus") || status.contains("cancelled") || status.contains("annul")) {
            statusLabel.getStyleClass().add("status-error");
        } else if (status.contains("pending") || status.contains("waiting") || status.contains("attente")) {
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
        updateDashboardStats();
    }

    private void updateDashboardStats() {
        int totalEvents = eventsList.size();
        int totalInscriptions = 0;
        int totalAvis = 0;

        try {
            totalInscriptions = inscriptionServices.afficher().size();
        } catch (SQLException ignored) {
        }

        try {
            totalAvis = avisServices.afficher().size();
        } catch (SQLException ignored) {
        }

        long activeEvents = eventsList.stream()
                .filter(item -> {
                    String status = safe(item.getStatut()).toLowerCase();
                    return status.contains("à venir") || status.contains("a venir") || status.contains("en cours")
                            || status.contains("active") || status.contains("ongoing");
                })
                .count();

        long coveredCities = eventsList.stream()
                .map(EventItem::getVille)
                .map(EventManagementController::safe)
                .filter(city -> !city.isBlank() && !city.equals("—"))
                .distinct()
                .count();

        if (totalEventsStatValue != null) {
            totalEventsStatValue.setText(String.valueOf(totalEvents));
        }
        if (totalInscriptionsStatValue != null) {
            totalInscriptionsStatValue.setText(String.valueOf(totalInscriptions));
        }
        if (totalAvisStatValue != null) {
            totalAvisStatValue.setText(String.valueOf(totalAvis));
        }
        if (activeEventsStatValue != null) {
            activeEventsStatValue.setText(String.valueOf(activeEvents));
        }
        if (citiesEventsStatValue != null) {
            citiesEventsStatValue.setText(String.valueOf(coveredCities));
        }
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
            ex.printStackTrace();
            showStyledError(
                    "Impossible de charger les événements",
                    ex.getMessage() != null ? ex.getMessage() : "Vérifiez la base de données et la table evenements."
            );
        }

        eventsPage = 0;
        filterAndSortEvents();
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
            ex.printStackTrace();
            showStyledError(
                    "Inscriptions",
                    ex.getMessage() != null ? ex.getMessage() : "Impossible de charger les inscriptions."
            );
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
            ex.printStackTrace();
            showStyledError(
                    "Avis",
                    ex.getMessage() != null ? ex.getMessage() : "Impossible de charger les avis."
            );
        }

        reviewsPage = 0;
        displayReviewsPaginated();
    }

    // ==================== PAGINATION HELPERS ====================

    private void displayEventsPaginated() {
        eventsGridPane.getChildren().clear();
        eventsPaginationBox.getChildren().clear();

        if (eventsForGrid == null || eventsForGrid.isEmpty()) {
            Label emptyLabel = new Label("Aucun événement disponible. Créez des événements pour les voir ici.");
            emptyLabel.getStyleClass().add("empty-state-label");
            emptyLabel.setStyle("-fx-text-alignment: center; -fx-padding: 40;");
            eventsGridPane.getChildren().add(emptyLabel);
            return;
        }

        int totalPages = (int) Math.ceil((double) eventsForGrid.size() / ITEMS_PER_PAGE);
        int startIndex = eventsPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, eventsForGrid.size());

        for (int i = startIndex; i < endIndex; i++) {
            eventsGridPane.getChildren().add(buildEventCard(eventsForGrid.get(i)));
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
        int totalPages = (int) Math.ceil((double) eventsForGrid.size() / ITEMS_PER_PAGE);
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
        boolean confirmed = showCustomConfirmation(
                "Supprimer cet événement ?",
                "Êtes-vous sûr de vouloir supprimer \"" + eventTitle + "\" ?\n\nCette action est irréversible et supprimera aussi toutes les inscriptions et avis associés.",
                "Supprimer"
        );

        if (confirmed) {
            try {
                evenementServices.supprimer(eventId);
                showStyledSuccess("Événement supprimé ✓", "L'événement \"" + eventTitle + "\" a été supprimé avec succès.");

                loadData();
                showView(View.EVENTS);
            } catch (SQLException ex) {
                showStyledError("Erreur lors de la suppression", "Détail: " + ex.getMessage());
            }
        }
    }

    private void deleteInscriptionWithConfirmation(int inscriptionId, String userName) {
        boolean confirmed = showCustomConfirmation(
                "Supprimer cette inscription ?",
                "Êtes-vous sûr de vouloir supprimer l'inscription de \"" + userName + "\" ?\n\nCette action est irréversible.",
                "Supprimer"
        );

        if (confirmed) {
            try {
                inscriptionServices.supprimer(inscriptionId);
                showStyledSuccess("Inscription supprimée ✓", "L'inscription de \"" + userName + "\" a été supprimée avec succès.");

                filterInscriptionsByEvent(currentSelectedEventId);
                updateDashboardStats();
            } catch (SQLException ex) {
                showStyledError("Erreur lors de la suppression", "Détail: " + ex.getMessage());
            }
        }
    }

    private void deleteAvisWithConfirmation(int avisId, String userName) {
        boolean confirmed = showCustomConfirmation(
                "Supprimer cet avis ?",
                "Êtes-vous sûr de vouloir supprimer l'avis de \"" + userName + "\" ?\n\nCette action est irréversible.",
                "Supprimer"
        );

        if (confirmed) {
            try {
                avisServices.supprimer(avisId);
                showStyledSuccess("Avis supprimé ✓", "L'avis de \"" + userName + "\" a été supprimé avec succès.");

                filterReviewsByEvent(currentSelectedEventId);
                updateDashboardStats();
            } catch (SQLException ex) {
                showStyledError("Erreur lors de la suppression", "Détail: " + ex.getMessage());
            }
        }
    }

    private boolean showCustomConfirmation(String title, String message, String confirmText) {
        final boolean[] confirmed = {false};

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        if (ajouterEvenementBtn != null && ajouterEvenementBtn.getScene() != null) {
            dialogStage.initOwner(ajouterEvenementBtn.getScene().getWindow());
        }
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.setTitle(title);

        Label badgeLabel = new Label("Confirmation");
        badgeLabel.setStyle(
                "-fx-background-color: rgba(220, 53, 69, 0.18);" +
                "-fx-text-fill: #FFB7BE;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 6 12;" +
                "-fx-background-radius: 999px;"
        );

        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setStyle(
                "-fx-text-fill: #FFFFFF;" +
                "-fx-font-size: 19px;" +
                "-fx-font-weight: bold;"
        );

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle(
                "-fx-text-fill: rgba(226, 236, 248, 0.92);" +
                "-fx-font-size: 13px;" +
                "-fx-line-spacing: 4px;"
        );

        Button cancelButton = new Button("Annuler");
        cancelButton.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                "-fx-border-color: rgba(123,216,255,0.22);" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;" +
                "-fx-text-fill: #EAF4FF;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 18;" +
                "-fx-cursor: hand;"
        );
        cancelButton.setOnAction(e -> dialogStage.close());

        Button confirmButton = new Button(confirmText);
        confirmButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, rgba(220, 53, 69, 0.94) 0%, rgba(164, 28, 44, 0.98) 100%);" +
                "-fx-border-color: rgba(255, 180, 186, 0.55);" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;" +
                "-fx-text-fill: #FFFFFF;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 18;" +
                "-fx-cursor: hand;"
        );
        confirmButton.setOnAction(e -> {
            confirmed[0] = true;
            dialogStage.close();
        });

        HBox actions = new HBox(10, cancelButton, confirmButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(16, badgeLabel, titleLabel, messageLabel, actions);
        root.setPadding(new Insets(22));
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom, rgba(18, 32, 62, 0.98) 0%, rgba(10, 19, 40, 0.98) 100%);" +
                "-fx-border-color: rgba(123, 216, 255, 0.25);" +
                "-fx-border-width: 1.2px;" +
                "-fx-border-radius: 18px;" +
                "-fx-background-radius: 18px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.45), 30, 0.15, 0, 10);"
        );
        root.setPrefWidth(480);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();

        return confirmed[0];
    }

    private void showCustomMessageDialog(String badgeText, String title, String message, boolean isError) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        if (ajouterEvenementBtn != null && ajouterEvenementBtn.getScene() != null) {
            dialogStage.initOwner(ajouterEvenementBtn.getScene().getWindow());
        }
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.setTitle(title);

        String badgeColor = isError ? "rgba(220, 53, 69, 0.18)" : "rgba(46, 204, 113, 0.18)";
        String badgeTextColor = isError ? "#FFB7BE" : "#A9F5C8";
        String buttonBackground = isError
                ? "linear-gradient(to bottom, rgba(220, 53, 69, 0.94) 0%, rgba(164, 28, 44, 0.98) 100%)"
                : "linear-gradient(to bottom, rgba(46, 204, 113, 0.94) 0%, rgba(29, 139, 78, 0.98) 100%)";

        Label badgeLabel = new Label(badgeText);
        badgeLabel.setStyle(
                "-fx-background-color: " + badgeColor + ";" +
                "-fx-text-fill: " + badgeTextColor + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 6 12;" +
                "-fx-background-radius: 999px;"
        );

        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setStyle(
                "-fx-text-fill: #FFFFFF;" +
                "-fx-font-size: 19px;" +
                "-fx-font-weight: bold;"
        );

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle(
                "-fx-text-fill: rgba(226, 236, 248, 0.92);" +
                "-fx-font-size: 13px;" +
                "-fx-line-spacing: 4px;"
        );

        Button closeButton = new Button("Fermer");
        closeButton.setStyle(
                "-fx-background-color: " + buttonBackground + ";" +
                "-fx-border-color: rgba(255, 255, 255, 0.24);" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;" +
                "-fx-text-fill: #FFFFFF;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 18;" +
                "-fx-cursor: hand;"
        );
        closeButton.setOnAction(e -> dialogStage.close());

        HBox actions = new HBox(closeButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(16, badgeLabel, titleLabel, messageLabel, actions);
        root.setPadding(new Insets(22));
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom, rgba(18, 32, 62, 0.98) 0%, rgba(10, 19, 40, 0.98) 100%);" +
                "-fx-border-color: rgba(123, 216, 255, 0.25);" +
                "-fx-border-width: 1.2px;" +
                "-fx-border-radius: 18px;" +
                "-fx-background-radius: 18px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.45), 30, 0.15, 0, 10);"
        );
        root.setPrefWidth(480);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private void showStyledSuccess(String title, String message) {
        showCustomMessageDialog("Succès", title, message, false);
    }

    private void showStyledError(String title, String message) {
        showCustomMessageDialog("Erreur", title, message, true);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    // ==================== SEARCH & SORT METHODS ====================

    private void filterAndSortEvents() {
        String rawSearch = eventsSearchField.getText();
        String searchText = rawSearch == null ? "" : rawSearch.toLowerCase();
        String sortByRaw = eventsSortByCombo.getValue();
        final String sortKey = sortByRaw == null ? "Titre" : sortByRaw;
        String orderVal = eventsSortOrderCombo.getValue();
        boolean ascending = orderVal == null || !orderVal.contains("Décroissant");

        List<EventItem> filtered = eventsList.stream()
            .filter(item -> searchText.isEmpty() ||
                item.getTitre().toLowerCase().contains(searchText) ||
                item.getLieu().toLowerCase().contains(searchText) ||
                item.getVille().toLowerCase().contains(searchText) ||
                item.getStatut().toLowerCase().contains(searchText))
            .sorted((a, b) -> {
                int result = 0;
                switch (sortKey) {
                    case "Titre": result = a.getTitre().compareTo(b.getTitre()); break;
                    case "Lieu": result = a.getLieu().compareTo(b.getLieu()); break;
                    case "Ville": result = a.getVille().compareTo(b.getVille()); break;
                    case "Statut": result = a.getStatut().compareTo(b.getStatut()); break;
                    case "Date Début": result = a.getDebut().compareTo(b.getDebut()); break;
                    default: result = a.getTitre().compareTo(b.getTitre()); break;
                }
                return ascending ? result : -result;
            })
            .collect(Collectors.toList());

        eventsForGrid = new ArrayList<>(filtered);
        eventsPage = 0;
        displayEventsPaginated();
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
