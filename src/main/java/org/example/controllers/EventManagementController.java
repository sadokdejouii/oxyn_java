package org.example.controllers;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.WritableImage;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.entities.AvisEvenement;
import org.example.entities.Evenement;
import org.example.entities.InscriptionEvenement;
import org.example.entities.User;
import org.example.services.AvisEvenementServices;
import org.example.services.EvenementServices;
import org.example.services.EventCoverPhotoService;
import org.example.services.EventPosterAiService;
import org.example.services.InscriptionEvenementServices;
import org.example.services.UserService;
import org.example.services.WeatherService;

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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.util.Duration;

import javax.imageio.ImageIO;

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

    private final EventCoverPhotoService coverPhotoService = new EventCoverPhotoService();

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
        private final String eventTitre;

        public InscriptionItem(int id, String userName, String dateInscription, String statut, int idEvenement, int idUser, String eventTitre) {
            this.id = id;
            this.userName = userName;
            this.dateInscription = dateInscription;
            this.statut = statut;
            this.idEvenement = idEvenement;
            this.idUser = idUser;
            this.eventTitre = eventTitre;
        }

        public int getId() { return id; }
        public String getUserName() { return userName; }
        public String getDateInscription() { return dateInscription; }
        public String getStatut() { return statut; }
        public int getIdEvenement() { return idEvenement; }
        public int getIdUser() { return idUser; }
        public String getEventTitre() { return eventTitre; }
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
        private final String eventTitre;

        public AvisItem(int id, String userName, int note, String commentaire, String createdAt, int idEvenement, int idUser, String eventTitre) {
            this.id = id;
            this.userName = userName;
            this.note = note;
            this.commentaire = commentaire;
            this.createdAt = createdAt;
            this.idEvenement = idEvenement;
            this.idUser = idUser;
            this.eventTitre = eventTitre;
        }

        public int getId() { return id; }
        public String getUserName() { return userName; }
        public int getNote() { return note; }
        public String getCommentaire() { return commentaire; }
        public String getCreatedAt() { return createdAt; }
        public int getIdEvenement() { return idEvenement; }
        public int getIdUser() { return idUser; }
        public String getEventTitre() { return eventTitre; }
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
    @FXML private HBox statusBoardColumns;
    @FXML private StackPane weatherOverlay;
    @FXML private VBox weatherContentHost;
    @FXML private Label weatherEventTitleLabel;
    @FXML private Label weatherCityDateLabel;

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
    private static final List<String> EVENT_STATUS_ORDER = List.of("a_venir", "en_cours", "terminee", "annulee");
    private static final String EVENT_DRAG_PREFIX = "event-status:";
    
    private final ObservableList<EventItem> eventsList = FXCollections.observableArrayList();
    /** Snapshot used by the grid + pagination (search/sort apply on top of {@link #eventsList}). */
    private List<EventItem> eventsForGrid = new ArrayList<>();
    private final ObservableList<InscriptionItem> inscriptionsList = FXCollections.observableArrayList();
    private final ObservableList<AvisItem> reviewsList = FXCollections.observableArrayList();

    private final EvenementServices evenementServices = new EvenementServices();
    private final InscriptionEvenementServices inscriptionServices = new InscriptionEvenementServices();
    private final AvisEvenementServices avisServices = new AvisEvenementServices();
    private final EventPosterAiService eventPosterAiService = new EventPosterAiService();
    private final UserService userService = new UserService();
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
        weatherOverlay.setVisible(false);
        weatherOverlay.setManaged(false);
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
            StackPane formRoot = loader.load();
            
            // Add CSS stylesheets to embedded form
            String dashboardCss = getClass().getResource("/css/dashboard-saas.css").toExternalForm();
            String eventMgmtCss = getClass().getResource("/css/event-management.css").toExternalForm();
            formRoot.getStylesheets().add(dashboardCss);
            formRoot.getStylesheets().add(eventMgmtCss);
            
            AjouterEvenementsController controller = loader.getController();
            controller.setEmbeddedMode(savedEvent -> {
                upsertEventItem(savedEvent);
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
            StackPane formRoot = loader.load();
            
            // Add CSS stylesheets to embedded form
            String dashboardCss = getClass().getResource("/css/dashboard-saas.css").toExternalForm();
            String eventMgmtCss = getClass().getResource("/css/event-management.css").toExternalForm();
            formRoot.getStylesheets().add(dashboardCss);
            formRoot.getStylesheets().add(eventMgmtCss);
            
            // Set event data in the controller
            ModifierEvenementController controller = loader.getController();
            controller.setEventData(eventId);
            controller.setEmbeddedMode(savedEvent -> {
                upsertEventItem(savedEvent);
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
        card.getStyleClass().addAll("list-card", "event-card-wide");
        card.setPadding(new Insets(18));
        card.setAlignment(Pos.TOP_CENTER);

        String visualKey = resolveVisualKey(item.getTitre() + " " + item.getDescription());
        StackPane coverPane = buildEventPhotoCover(visualKey, resolveVisualText(visualKey), item.getTitre(), item.getDescription());

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

        Button inscriptionsBtn = new Button("📋 Inscriptions");
        inscriptionsBtn.getStyleClass().addAll("event-action-btn", "event-btn-primary");
        inscriptionsBtn.setOnAction(e -> showInscriptionsForEvent(item.getId()));

        Button reviewsBtn = new Button("💬 Avis");
        reviewsBtn.getStyleClass().addAll("event-action-btn", "event-btn-primary");
        reviewsBtn.setOnAction(e -> showReviewsForEvent(item.getId()));

        HBox actionBox1 = new HBox(10, inscriptionsBtn, reviewsBtn);
        actionBox1.setAlignment(Pos.CENTER);

        Button modifierBtn = new Button("🛠 Modifier");
        modifierBtn.getStyleClass().addAll("event-action-btn", "event-btn-primary");
        modifierBtn.setOnAction(e -> openModifyEventForm(item.getId(), item.getTitre()));

        Button deleteBtn = new Button("🗑 Supprimer");
        deleteBtn.getStyleClass().addAll("event-action-btn", "event-btn-delete");
        deleteBtn.setOnAction(e -> deleteEventWithConfirmation(item.getId(), item.getTitre()));

        HBox actionBox2 = new HBox(10, modifierBtn, deleteBtn);
        actionBox2.setAlignment(Pos.CENTER);

        Button weatherBtn = new Button("☁ Météo");
        weatherBtn.getStyleClass().addAll("event-action-btn", "event-btn-primary");
        weatherBtn.setOnAction(e -> showWeatherPopup(item.getId(), item.getVille(), item.getDebut(), item.getTitre()));

        HBox actionBox3 = new HBox(weatherBtn);
        actionBox3.setAlignment(Pos.CENTER);

        Button shareBtn = new Button("📘 Partager sur Facebook");
        shareBtn.getStyleClass().addAll("event-action-btn", "event-btn-primary");
        shareBtn.setOnAction(e -> shareEventOnFacebook(item));

        Button posterBtn = new Button("✨ Poster IA");
        posterBtn.getStyleClass().addAll("event-action-btn", "event-btn-primary");
        posterBtn.setOnAction(e -> generateAiPosterForEvent(item, posterBtn));

        HBox actionBox4 = new HBox(10, shareBtn, posterBtn);
        actionBox4.setAlignment(Pos.CENTER);

        card.getChildren().addAll(coverPane, titleLabel, descLabel, detailsBox, statusBox, actionBox1, actionBox2, actionBox4, actionBox3);
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
                createDetailLabel("🎫 " + item.getEventTitre()),
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
                createDetailLabel("🎫 " + item.getEventTitre()),
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
        coverPane.setMouseTransparent(true);
        coverPane.getStyleClass().addAll("back-card-cover", "back-cover-" + visualKey);
        coverPane.setPrefHeight(132);
        coverPane.setMinHeight(132);
        coverPane.setMaxWidth(Double.MAX_VALUE);
        applyRoundedClip(coverPane, 44);

        Label coverText = new Label(text);
        coverText.getStyleClass().add("back-card-cover-text");
        coverPane.getChildren().add(coverText);
        return coverPane;
    }

    private StackPane buildEventPhotoCover(String visualKey, String fallbackText, String title, String description) {
        StackPane coverPane = buildCoverPane(visualKey, fallbackText);
        requestPhotoCover(coverPane, visualKey, resolveVisualPhotoText(visualKey), safe(title), safe(description));
        return coverPane;
    }

    private void requestPhotoCover(StackPane coverPane, String visualKey, String labelText, String title, String description) {
        coverPhotoService.resolveCoverImageAsync(visualKey, title, description)
                .thenAccept(imageBytes -> imageBytes.ifPresent(bytes -> Platform.runLater(() -> applyPhotoCover(coverPane, bytes, labelText))));
    }

    private void applyPhotoCover(StackPane coverPane, byte[] imageBytes, String labelText) {
        Image image = new Image(new ByteArrayInputStream(imageBytes));
        if (image.isError()) {
            return;
        }

        Region photoLayer = new Region();
        photoLayer.setMouseTransparent(true);
        photoLayer.prefWidthProperty().bind(coverPane.widthProperty());
        photoLayer.prefHeightProperty().bind(coverPane.heightProperty());
        photoLayer.setBackground(new Background(new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, false, true)
        )));

        Region scrim = new Region();
        scrim.getStyleClass().add("back-card-cover-scrim");
        scrim.setMouseTransparent(true);
        scrim.prefWidthProperty().bind(coverPane.widthProperty());
        scrim.prefHeightProperty().bind(coverPane.heightProperty());

        Label photoLabel = new Label(labelText);
        photoLabel.getStyleClass().add("back-card-cover-photo-label");
        photoLabel.setMouseTransparent(true);

        coverPane.getChildren().setAll(photoLayer, scrim, photoLabel);
        StackPane.setAlignment(photoLabel, Pos.CENTER);
    }

    private void applyRoundedClip(StackPane coverPane, double arcSize) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(arcSize);
        clip.setArcHeight(arcSize);
        clip.widthProperty().bind(coverPane.widthProperty());
        clip.heightProperty().bind(coverPane.heightProperty());
        coverPane.setClip(clip);
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

    private String resolveVisualPhotoText(String visualKey) {
        return switch (visualKey) {
            case "yoga" -> "Yoga & Bien-être";
            case "football" -> "Football";
            case "basket" -> "Basketball";
            case "running" -> "Course & Fitness";
            case "music" -> "Événement spécial";
            default -> "Event Spotlight";
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
        String statusKey = canonicalEventStatusKey(status);
        if ("terminee".equals(statusKey)) {
            statusLabel.getStyleClass().add("status-finished");
        } else if ("annulee".equals(statusKey)) {
            statusLabel.getStyleClass().add("status-error");
        } else if ("en_cours".equals(statusKey)) {
            statusLabel.getStyleClass().add("status-active");
        } else if ("a_venir".equals(statusKey)) {
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

        if (totalInscriptionsStatValue != null) {
            try {
                totalInscriptions = inscriptionServices.afficher().size();
            } catch (SQLException ignored) {
            }
        }

        if (totalAvisStatValue != null) {
            try {
                totalAvis = avisServices.afficher().size();
            } catch (SQLException ignored) {
            }
        }

        long activeEvents = eventsList.stream()
                .filter(item -> {
                String statusKey = canonicalEventStatusKey(item.getStatut());
                return "a_venir".equals(statusKey) || "en_cours".equals(statusKey);
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

    private void upsertEventItem(Evenement event) {
        if (event == null) {
            return;
        }

        EventItem updatedItem = toEventItem(event);
        for (int index = 0; index < eventsList.size(); index++) {
            if (eventsList.get(index).getId() == updatedItem.getId()) {
                eventsList.set(index, updatedItem);
                updateEventDerivedUi();
                return;
            }
        }

        eventsList.add(updatedItem);
        updateEventDerivedUi();
    }

    private void updateEventDerivedUi() {
        updateDashboardStats();
        filterAndSortEvents();
    }

    private EventItem toEventItem(Evenement event) {
        Date debut = event.getDateDebut();
        Date fin = event.getDateFin();
        Date createdAt = event.getCreatedAt();
        return new EventItem(
                event.getId(),
                safe(event.getTitre()),
                safe(event.getDescription()),
                safe(event.getLieu()),
                safe(event.getVille()),
                event.getPlacesMax(),
                debut == null ? "—" : fmt.format(debut),
                fin == null ? "—" : fmt.format(fin),
                normalizeEventStatusLabel(event.getStatut()),
                createdAt == null ? "—" : fmt.format(createdAt)
        );
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
                        normalizeEventStatusLabel(e.getStatut()),
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
            String eventTitre = "Événement";
            try {
                Evenement ev = evenementServices.afficherById(eventId);
                if (ev != null && ev.getTitre() != null && !ev.getTitre().isBlank()) {
                    eventTitre = ev.getTitre();
                }
            } catch (SQLException ignored) {}

            List<InscriptionEvenement> allInscriptions = inscriptionServices.afficher();
            List<InscriptionEvenement> filtered = allInscriptions.stream()
                    .filter(insc -> insc.getIdEvenement() == eventId)
                    .collect(Collectors.toList());

            for (InscriptionEvenement insc : filtered) {
                Date d = insc.getDateInscription();
                String userName = userService.getUserDisplayName(insc.getIdUser());
                inscriptionsList.add(new InscriptionItem(
                        insc.getId(),
                        userName,
                        d == null ? "—" : fmt.format(d),
                        safe(insc.getStatut()),
                        insc.getIdEvenement(),
                        insc.getIdUser(),
                        eventTitre
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
            String eventTitre = "Événement";
            try {
                Evenement ev = evenementServices.afficherById(eventId);
                if (ev != null && ev.getTitre() != null && !ev.getTitre().isBlank()) {
                    eventTitre = ev.getTitre();
                }
            } catch (SQLException ignored) {}

            List<AvisEvenement> allReviews = avisServices.afficher();
            List<AvisEvenement> filtered = allReviews.stream()
                    .filter(avis -> avis.getIdEvenement() == eventId)
                    .collect(Collectors.toList());

            for (AvisEvenement avis : filtered) {
                Date d = avis.getCreatedAt();
                String userName = userService.getUserDisplayName(avis.getIdUser());
                reviewsList.add(new AvisItem(
                        avis.getId(),
                        userName,
                        avis.getNote(),
                        safe(avis.getCommentaire()),
                        d == null ? "—" : fmt.format(d),
                        avis.getIdEvenement(),
                        avis.getIdUser(),
                        eventTitre
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
            renderEventStatusBoard(List.of());
            return;
        }

        int totalPages = (int) Math.ceil((double) eventsForGrid.size() / ITEMS_PER_PAGE);
        int startIndex = eventsPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, eventsForGrid.size());

        for (int i = startIndex; i < endIndex; i++) {
            eventsGridPane.getChildren().add(buildEventCard(eventsForGrid.get(i)));
        }

        buildPaginationControls(eventsPaginationBox, eventsPage, totalPages, this::handleEventsPrevPage, this::handleEventsNextPage);
        renderEventStatusBoard(eventsForGrid);
    }

    private void renderEventStatusBoard(List<EventItem> items) {
        if (statusBoardColumns == null) {
            return;
        }

        statusBoardColumns.getChildren().clear();

        Map<String, List<EventItem>> groupedItems = new LinkedHashMap<>();
        for (String statusKey : EVENT_STATUS_ORDER) {
            groupedItems.put(statusKey, new ArrayList<>());
        }

        for (EventItem item : items) {
            groupedItems.computeIfAbsent(canonicalEventStatusKey(item.getStatut()), key -> new ArrayList<>()).add(item);
        }

        for (String statusKey : EVENT_STATUS_ORDER) {
            statusBoardColumns.getChildren().add(buildStatusColumn(statusKey, groupedItems.getOrDefault(statusKey, List.of())));
        }
    }

    private VBox buildStatusColumn(String statusKey, List<EventItem> items) {
        VBox column = new VBox(14);
        column.getStyleClass().addAll("kanban-column", "kanban-column-" + statusKey);
        column.setFillWidth(true);
        column.setPrefWidth(280);
        column.setMinWidth(260);

        Label statusLabel = new Label(eventStatusTitle(statusKey));
        statusLabel.getStyleClass().add("kanban-column-title");

        Label countLabel = new Label(items.size() + " evenement" + (items.size() > 1 ? "s" : ""));
        countLabel.getStyleClass().add("kanban-column-count");

        VBox header = new VBox(4, statusLabel, countLabel);
        header.getStyleClass().add("kanban-column-header");

        VBox cardsHost = new VBox(12);
        cardsHost.getStyleClass().add("kanban-cards-host");

        if (items.isEmpty()) {
            Label emptyLabel = new Label("Deposez un evenement ici");
            emptyLabel.getStyleClass().add("kanban-empty");
            emptyLabel.setWrapText(true);
            cardsHost.getChildren().add(emptyLabel);
        } else {
            for (EventItem item : items) {
                cardsHost.getChildren().add(buildStatusBoardCard(item));
            }
        }

        configureStatusColumnDragAndDrop(column, statusKey);
        column.getChildren().addAll(header, cardsHost);
        return column;
    }

    private VBox buildStatusBoardCard(EventItem item) {
        VBox card = new VBox(10);
        card.getStyleClass().add("kanban-card");
        card.setPadding(new Insets(14));

        Label titleLabel = new Label(item.getTitre().isBlank() ? "Evenement sans titre" : item.getTitre());
        titleLabel.getStyleClass().add("kanban-card-title");
        titleLabel.setWrapText(true);

        Label metaLabel = new Label((item.getVille().isBlank() ? "Ville a confirmer" : item.getVille()) + "  •  " + item.getDebut());
        metaLabel.getStyleClass().add("kanban-card-meta");
        metaLabel.setWrapText(true);

        Label descLabel = new Label(item.getDescription().isBlank() ? "Aucune description disponible." : item.getDescription());
        descLabel.getStyleClass().add("kanban-card-description");
        descLabel.setWrapText(true);

        HBox footer = new HBox(8,
                createKanbanPill("Capacite " + item.getPlacesMax()),
                createKanbanPill(normalizeEventStatusLabel(item.getStatut()))
        );
        footer.getStyleClass().add("kanban-card-footer");

        card.getChildren().addAll(titleLabel, metaLabel, descLabel, footer);
        configureStatusCardDrag(card, item);
        return card;
    }

    private Label createKanbanPill(String text) {
        Label pill = new Label(text);
        pill.getStyleClass().add("kanban-pill");
        return pill;
    }

    private void configureStatusCardDrag(VBox card, EventItem item) {
        card.setOnDragDetected(event -> {
            Dragboard dragboard = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(EVENT_DRAG_PREFIX + item.getId());
            dragboard.setContent(content);
            SnapshotParameters snapshotParameters = new SnapshotParameters();
            snapshotParameters.setFill(Color.TRANSPARENT);
            WritableImage dragPreview = card.snapshot(snapshotParameters, null);
            dragboard.setDragView(dragPreview, dragPreview.getWidth() / 2, 18);
            card.getStyleClass().add("kanban-card-dragging");
            animateKanbanCardDrag(card, true);
            event.consume();
        });

        card.setOnDragDone(event -> {
            card.getStyleClass().remove("kanban-card-dragging");
            animateKanbanCardDrag(card, false);
            event.consume();
        });
    }

    private void configureStatusColumnDragAndDrop(VBox column, String targetStatusKey) {
        column.setOnDragOver(event -> {
            if (hasEventDragPayload(event.getDragboard())) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        column.setOnDragEntered(event -> {
            if (hasEventDragPayload(event.getDragboard())) {
                column.getStyleClass().add("kanban-column-active");
                animateKanbanColumnFocus(column, true);
            }
            event.consume();
        });

        column.setOnDragExited(event -> {
            column.getStyleClass().remove("kanban-column-active");
            animateKanbanColumnFocus(column, false);
            event.consume();
        });

        column.setOnDragDropped(event -> {
            boolean dropCompleted = false;
            Dragboard dragboard = event.getDragboard();
            if (hasEventDragPayload(dragboard)) {
                int eventId = Integer.parseInt(dragboard.getString().substring(EVENT_DRAG_PREFIX.length()));
                EventItem item = findEventItem(eventId);
                if (item != null && !targetStatusKey.equals(canonicalEventStatusKey(item.getStatut()))) {
                    dropCompleted = persistEventStatusChange(eventId, targetStatusKey);
                }
            }
            column.getStyleClass().remove("kanban-column-active");
            animateKanbanColumnFocus(column, false);
            event.setDropCompleted(dropCompleted);
            event.consume();
        });
    }

    private void animateKanbanCardDrag(VBox card, boolean dragging) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(dragging ? 120 : 160), card);
        scaleTransition.setToX(dragging ? 1.04 : 1.0);
        scaleTransition.setToY(dragging ? 1.04 : 1.0);
        scaleTransition.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition translateTransition = new TranslateTransition(Duration.millis(dragging ? 120 : 160), card);
        translateTransition.setToY(dragging ? -8 : 0);
        translateTransition.setInterpolator(Interpolator.EASE_BOTH);

        new ParallelTransition(scaleTransition, translateTransition).play();
    }

    private void animateKanbanColumnFocus(VBox column, boolean focused) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(focused ? 140 : 180), column);
        scaleTransition.setToX(focused ? 1.015 : 1.0);
        scaleTransition.setToY(focused ? 1.015 : 1.0);
        scaleTransition.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition translateTransition = new TranslateTransition(Duration.millis(focused ? 140 : 180), column);
        translateTransition.setToY(focused ? -3 : 0);
        translateTransition.setInterpolator(Interpolator.EASE_BOTH);

        new ParallelTransition(scaleTransition, translateTransition).play();
    }

    private boolean hasEventDragPayload(Dragboard dragboard) {
        return dragboard != null
                && dragboard.hasString()
                && dragboard.getString() != null
                && dragboard.getString().startsWith(EVENT_DRAG_PREFIX);
    }

    private EventItem findEventItem(int eventId) {
        return eventsList.stream()
                .filter(item -> item.getId() == eventId)
                .findFirst()
                .orElse(null);
    }

    private boolean persistEventStatusChange(int eventId, String statusKey) {
        try {
            Evenement event = evenementServices.afficherById(eventId);
            if (event == null) {
                return false;
            }

            event.setStatut(eventStatusTitle(statusKey));
            evenementServices.modifier(event);
            upsertEventItem(event);
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            showStyledError("Mise a jour impossible", ex.getMessage() != null ? ex.getMessage() : "Le statut n'a pas pu etre modifie.");
            return false;
        }
    }

    private String normalizeEventStatusLabel(String status) {
        return eventStatusTitle(canonicalEventStatusKey(status));
    }

    private String canonicalEventStatusKey(String status) {
        String normalized = Normalizer.normalize(safe(status), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .trim();

        if (normalized.contains("annul") || normalized.contains("cancel") || normalized.contains("close")) {
            return "annulee";
        }
        if (normalized.contains("termin") || normalized.contains("finish") || normalized.contains("done") || normalized.contains("complete")) {
            return "terminee";
        }
        if (normalized.contains("en cours") || normalized.contains("ongoing") || normalized.contains("active")) {
            return "en_cours";
        }
        if (normalized.contains("a venir") || normalized.contains("avenir") || normalized.contains("draft") || normalized.contains("pending") || normalized.isBlank()) {
            return "a_venir";
        }
        return "a_venir";
    }

    private String eventStatusTitle(String statusKey) {
        return switch (statusKey) {
            case "en_cours" -> "En cours";
            case "terminee" -> "Terminée";
            case "annulee" -> "Annulée";
            default -> "A venir";
        };
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

    // ==================== WEATHER POPUP ====================

    /**
     * Shows a weather popup for the given event city and date
     */
    private void showWeatherPopup(int eventId, String ville, String dateDebut, String eventTitre) {
        weatherEventTitleLabel.setText(eventTitre);
        weatherCityDateLabel.setText("Ville: " + ville + "   |   Date: " + dateDebut);
        weatherOverlay.setManaged(true);
        weatherOverlay.setVisible(true);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(36, 36);
        Label loadingLabel = new Label("Chargement des données météo...");
        loadingLabel.getStyleClass().add("event-popup-loading");
        VBox loadingBox = new VBox(10, spinner, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(14, 0, 14, 0));
        weatherContentHost.getChildren().setAll(loadingBox);

        // Fetch weather in background thread
        WeatherService weatherService = new WeatherService();
        new Thread(() -> {
            try {
                WeatherService.WeatherResult result = weatherService.getWeather(ville);
                final boolean statusChangedToCancelled = result.isRainy() && cancelEventIfRain(eventId);
                Platform.runLater(() -> {
                    Label emojiLabel = new Label(result.getConditionIcon());
                    emojiLabel.getStyleClass().add("event-popup-emoji");

                    Label tempBigLabel = new Label(String.format("%.1f°C", result.temperature));
                    tempBigLabel.getStyleClass().add("event-popup-temp");

                    Label descLabel = new Label(result.description);
                    descLabel.getStyleClass().add("event-popup-desc");

                    VBox topWeather = new VBox(3, emojiLabel, tempBigLabel, descLabel);
                    topWeather.setAlignment(Pos.CENTER);

                    GridPane grid = new GridPane();
                    grid.getStyleClass().add("event-popup-grid");
                    grid.setHgap(16);
                    grid.setVgap(10);

                    addWeatherRow(grid, 0, "Ressenti", String.format("%.1f°C", result.feelsLike));
                    addWeatherRow(grid, 1, "Max / Min", String.format("%.1f°C / %.1f°C", result.tempMax, result.tempMin));
                    addWeatherRow(grid, 2, "Humidite", result.humidity + "%");
                    addWeatherRow(grid, 3, "Vent", String.format("%.1f m/s (%s)", result.windSpeed, result.getWindDirection()));
                    addWeatherRow(grid, 4, "Nuages", result.cloudiness + "%");
                    if (result.rainVolume > 0) {
                        addWeatherRow(grid, 5, "Pluie (1h)", String.format("%.1f mm", result.rainVolume));
                    }
                    if (result.snowVolume > 0) {
                        addWeatherRow(grid, 6, "Neige (1h)", String.format("%.1f mm", result.snowVolume));
                    }
                    if (result.visibility > 0) {
                        int visRow = (result.rainVolume > 0 ? 1 : 0) + (result.snowVolume > 0 ? 1 : 0) + 5;
                        addWeatherRow(grid, visRow, "Visibilite", (result.visibility / 1000.0) + " km");
                    }

                    Label pluieLabel = new Label();
                    if (result.isRainy()) {
                        String precipText = "";
                        if (result.rainVolume > 0) {
                            precipText += String.format("Pluie: %.1f mm", result.rainVolume);
                        }
                        if (result.snowVolume > 0) {
                            if (!precipText.isEmpty()) precipText += " | ";
                            precipText += String.format("Neige: %.1f mm", result.snowVolume);
                        }
                        if (precipText.isEmpty()) {
                            precipText = "Pluie detectee (par icone)";
                        }
                        pluieLabel.setText("⚠️ " + precipText);
                        pluieLabel.getStyleClass().add("event-popup-rain-yes");
                    } else {
                        pluieLabel.setText("✓ Aucune precipitation");
                        pluieLabel.getStyleClass().add("event-popup-rain-no");
                    }

                    Label autoStatusLabel = new Label();
                    if (result.isRainy() && statusChangedToCancelled) {
                        autoStatusLabel.setText("Statut evenement mis a jour automatiquement: annulee");
                        autoStatusLabel.getStyleClass().add("event-popup-rain-yes");
                        loadData();
                    } else if (result.isRainy()) {
                        autoStatusLabel.setText("Pluie detectee: statut deja annulee");
                        autoStatusLabel.getStyleClass().add("event-popup-rain-no");
                    }

                    Label noteLabel = new Label("Donnees meteo actuelles pour " + result.ville + ", " + result.pays);
                    noteLabel.getStyleClass().add("event-popup-note");
                    noteLabel.setMaxWidth(370);

                    VBox weatherContent = new VBox(10, topWeather, pluieLabel, autoStatusLabel, new Separator(), grid, noteLabel);
                    weatherContent.setAlignment(Pos.TOP_CENTER);
                    weatherContentHost.getChildren().setAll(weatherContent);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Label errLabel = new Label("Erreur: " + ex.getMessage());
                    errLabel.getStyleClass().add("event-popup-error");
                    errLabel.setMaxWidth(370);
                    VBox errBox = new VBox(12, errLabel);
                    errBox.setAlignment(Pos.CENTER);
                    errBox.setPadding(new Insets(20));
                    weatherContentHost.getChildren().setAll(errBox);
                });
            }
        }).start();
    }

    private boolean cancelEventIfRain(int eventId) {
        try {
            Evenement event = evenementServices.afficherById(eventId);
            if (event == null) {
                return false;
            }

            String currentStatus = event.getStatut() == null ? "" : event.getStatut().trim();
            if ("annulee".equals(canonicalEventStatusKey(currentStatus))) {
                return false;
            }

            event.setStatut(eventStatusTitle("annulee"));
            evenementServices.modifier(event);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void closeWeatherPopup() {
        weatherOverlay.setVisible(false);
        weatherOverlay.setManaged(false);
        weatherContentHost.getChildren().clear();
    }

    private void addWeatherRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("event-popup-k");
        Label val = new Label(value);
        val.getStyleClass().add("event-popup-v");
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
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

    private void shareEventOnFacebook(EventItem item) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                showStyledError("Partage indisponible", "Le navigateur n'est pas disponible sur cette machine.");
                return;
            }

            String description = item.getDescription().isBlank() ? "Description non disponible" : item.getDescription();
            String lieu = item.getLieu().isBlank() ? "Lieu a confirmer" : item.getLieu();
            String ville = item.getVille().isBlank() ? "Ville non precisee" : item.getVille();

                String quote = "Evenement : " + item.getTitre()
                    + "\nDescription : " + description
                    + "\nDate : " + item.getDebut() + " -> " + item.getFin()
                    + "\nLieu : " + lieu + " (" + ville + ")";

                ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putString(quote);
                Clipboard.getSystemClipboard().setContent(clipboardContent);

                String encodedUrl = URLEncoder.encode("https://www.facebook.com", StandardCharsets.UTF_8.name());
                String encodedQuote = URLEncoder.encode(quote, StandardCharsets.UTF_8.name());
                URI uri = new URI("https://www.facebook.com/sharer/sharer.php?u=" + encodedUrl + "&quote=" + encodedQuote);
            Desktop.getDesktop().browse(uri);
        } catch (Exception ex) {
            showStyledError("Echec du partage", "Impossible d'ouvrir Facebook pour le partage.");
        }
    }

    private void generateAiPosterForEvent(EventItem item, Button triggerButton) {
        String originalText = triggerButton.getText();
        triggerButton.setDisable(true);
        triggerButton.setText("⏳ Generation...");

        Thread worker = new Thread(() -> {
            try {
                EventPosterAiService.PosterRequest request = new EventPosterAiService.PosterRequest(
                        item.getTitre(),
                        item.getDescription(),
                        item.getLieu(),
                        item.getVille(),
                        item.getDebut(),
                        item.getFin(),
                        item.getPlacesMax()
                );

                byte[] backgroundBytes = null;
                Exception aiFailure = null;
                try {
                    backgroundBytes = eventPosterAiService.generatePosterBackground(request);
                } catch (Exception ex) {
                    aiFailure = ex;
                }

                byte[] resolvedBackgroundBytes = backgroundBytes;
                Exception resolvedAiFailure = aiFailure;

                Platform.runLater(() -> {
                    try {
                        Path posterPath;
                        String messageTitle;
                        String messageBody;

                        if (resolvedBackgroundBytes != null) {
                            posterPath = savePosterToDownloads(request, resolvedBackgroundBytes);
                            messageTitle = "Poster IA genere";
                            messageBody = "L'affiche a ete enregistree ici :\n" + posterPath
                                    + "\n\nDans Facebook, cliquez sur choisir depuis le PC puis selectionnez cette image.";
                        } else {
                            posterPath = savePosterToDownloads(request, buildFallbackBackgroundImage(request));
                            messageTitle = "Poster genere";
                            String fallbackReason = resolvedAiFailure == null || resolvedAiFailure.getMessage() == null || resolvedAiFailure.getMessage().isBlank()
                                    ? "Le service IA n'etait pas disponible"
                                    : resolvedAiFailure.getMessage();
                            messageBody = fallbackReason + ".\n\nOXYN a genere une affiche premium locale ici :\n"
                                    + posterPath
                                    + "\n\nDans Facebook, cliquez sur choisir depuis le PC puis selectionnez cette image.";
                        }

                        resetPosterButton(triggerButton, originalText);
                        openFolderIfPossible(posterPath.getParent());
                    } catch (Exception ex) {
                        resetPosterButton(triggerButton, originalText);
                        showStyledError("Generation poster IA", buildPosterErrorMessage(ex));
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    try {
                        EventPosterAiService.PosterRequest request = new EventPosterAiService.PosterRequest(
                                item.getTitre(),
                                item.getDescription(),
                                item.getLieu(),
                                item.getVille(),
                                item.getDebut(),
                                item.getFin(),
                                item.getPlacesMax()
                        );
                        Path posterPath = savePosterToDownloads(request, buildFallbackBackgroundImage(request));
                        resetPosterButton(triggerButton, originalText);
                        openFolderIfPossible(posterPath.getParent());
                    } catch (Exception fallbackEx) {
                        resetPosterButton(triggerButton, originalText);
                        showStyledError("Generation poster IA", buildPosterErrorMessage(fallbackEx));
                    }
                });
            }
        }, "event-ai-poster-" + item.getId());
        worker.setDaemon(true);
        worker.start();
    }

    private Path savePosterToDownloads(EventPosterAiService.PosterRequest request, byte[] backgroundBytes) throws IOException {
        Image backgroundImage = new Image(new ByteArrayInputStream(backgroundBytes));
        return savePosterToDownloads(request, backgroundImage);
    }

    private Path savePosterToDownloads(EventPosterAiService.PosterRequest request, Image backgroundImage) throws IOException {
        WritableImage posterSnapshot = buildPosterSnapshot(request, backgroundImage);

        Path postersDir = Path.of(System.getProperty("user.dir"), "src", "main", "resources", "images");
        Files.createDirectories(postersDir);

        String fileName = slugify(request.titre()) + "-poster-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + ".png";
        Path outputPath = postersDir.resolve(fileName);

        ImageIO.write(SwingFXUtils.fromFXImage(posterSnapshot, null), "png", outputPath.toFile());
        return outputPath;
    }

        private Image buildFallbackBackgroundImage(EventPosterAiService.PosterRequest request) {
        StackPane backgroundRoot = new StackPane();
        backgroundRoot.setPrefSize(1080, 1350);
        backgroundRoot.setMinSize(1080, 1350);
        backgroundRoot.setMaxSize(1080, 1350);
        backgroundRoot.setStyle("-fx-background-color: linear-gradient(to bottom right, #071327 0%, #0A1E43 48%, #030A15 100%);");

        Rectangle baseGlow = new Rectangle(1180, 1440);
        baseGlow.setFill(new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#0F2B68")),
            new Stop(0.45, Color.web("#091A3B")),
            new Stop(1, Color.web("#040B16"))
        ));

        Rectangle diagonalPanel = new Rectangle(950, 360);
        diagonalPanel.setArcWidth(48);
        diagonalPanel.setArcHeight(48);
        diagonalPanel.setRotate(-18);
        diagonalPanel.setTranslateX(220);
        diagonalPanel.setTranslateY(-235);
        diagonalPanel.setFill(new LinearGradient(
            0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#1D4ED8", 0.18)),
            new Stop(0.55, Color.web("#38BDF8", 0.42)),
            new Stop(1, Color.web("#F59E0B", 0.16))
        ));
        diagonalPanel.setEffect(new GaussianBlur(10));

        Circle accentOrbTop = new Circle(215);
        accentOrbTop.setTranslateX(305);
        accentOrbTop.setTranslateY(-360);
        accentOrbTop.setFill(new RadialGradient(
            0,
            0,
            0.5,
            0.5,
            1,
            true,
            CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#22D3EE", 0.88)),
            new Stop(0.45, Color.web("#2563EB", 0.36)),
            new Stop(1, Color.TRANSPARENT)
        ));
        accentOrbTop.setEffect(new GaussianBlur(48));

        Circle accentOrbBottom = new Circle(245);
        accentOrbBottom.setTranslateX(-330);
        accentOrbBottom.setTranslateY(405);
        accentOrbBottom.setFill(new RadialGradient(
            0,
            0,
            0.5,
            0.5,
            1,
            true,
            CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#F59E0B", 0.52)),
            new Stop(0.40, Color.web("#1D4ED8", 0.18)),
            new Stop(1, Color.TRANSPARENT)
        ));
        accentOrbBottom.setEffect(new GaussianBlur(60));

        Rectangle glassCard = new Rectangle(910, 1120);
        glassCard.setArcWidth(64);
        glassCard.setArcHeight(64);
        glassCard.setFill(Color.web("#081628", 0.30));
        glassCard.setStroke(Color.web("#8BD8FF", 0.16));
        glassCard.setStrokeWidth(1.6);

        Rectangle goldStrip = new Rectangle(11, 1030);
        goldStrip.setArcWidth(18);
        goldStrip.setArcHeight(18);
        goldStrip.setTranslateX(-376);
        goldStrip.setFill(new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#F8C35C")),
            new Stop(1, Color.web("#8A5B12"))
        ));

        Label watermark = new Label(compactPosterWatermark(request.titre()));
        watermark.setStyle(
            "-fx-text-fill: rgba(255, 255, 255, 0.045);"
                + "-fx-font-size: 180px;"
                + "-fx-font-weight: 900;"
                + "-fx-letter-spacing: 4px;"
        );
        watermark.setRotate(-90);
        watermark.setTranslateX(355);
        watermark.setTranslateY(-30);

        backgroundRoot.getChildren().addAll(
            baseGlow,
            accentOrbTop,
            accentOrbBottom,
            diagonalPanel,
            glassCard,
            goldStrip,
            watermark
        );
        return renderNodeToImage(backgroundRoot, 1080, 1350);
        }

    private WritableImage buildPosterSnapshot(EventPosterAiService.PosterRequest request, Image backgroundImage) {
        StackPane root = new StackPane();
        root.setPrefSize(1080, 1350);
        root.setMinSize(1080, 1350);
        root.setMaxSize(1080, 1350);
        root.setStyle("-fx-background-color: #08162E; -fx-background-radius: 38px;");

        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setFitWidth(1080);
        backgroundView.setFitHeight(1350);
        backgroundView.setPreserveRatio(false);

        Rectangle clip = new Rectangle(1080, 1350);
        clip.setArcWidth(60);
        clip.setArcHeight(60);
        root.setClip(clip);

        Region darkOverlay = new Region();
        darkOverlay.setPrefSize(1080, 1350);
        darkOverlay.setStyle(
                "-fx-background-color: linear-gradient(to bottom, rgba(4, 10, 26, 0.18) 0%, rgba(5, 16, 38, 0.55) 34%, rgba(7, 20, 46, 0.94) 100%);"
        );

        Region accentGlow = new Region();
        accentGlow.setPrefSize(1080, 1350);
        accentGlow.setStyle(
            "-fx-background-color: radial-gradient(center 82% 12%, radius 58%, rgba(35, 167, 255, 0.38), transparent 64%),"
                + " radial-gradient(center 12% 88%, radius 54%, rgba(8, 211, 255, 0.22), transparent 60%),"
                + " linear-gradient(to bottom, rgba(2, 8, 20, 0.06) 0%, rgba(3, 9, 23, 0.46) 56%, rgba(4, 10, 24, 0.86) 100%);"
        );

        Rectangle heroPanel = new Rectangle(930, 1210);
        heroPanel.setArcWidth(72);
        heroPanel.setArcHeight(72);
        heroPanel.setFill(Color.web("#07152B", 0.30));
        heroPanel.setStroke(Color.web("#8BD8FF", 0.12));
        heroPanel.setStrokeWidth(1.2);

        Rectangle topGlow = new Rectangle(760, 240);
        topGlow.setArcWidth(54);
        topGlow.setArcHeight(54);
        topGlow.setRotate(-8);
        topGlow.setTranslateX(170);
        topGlow.setTranslateY(-370);
        topGlow.setFill(new LinearGradient(
            0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#0EA5E9", 0.18)),
            new Stop(0.55, Color.web("#2563EB", 0.42)),
            new Stop(1, Color.web("#38BDF8", 0.14))
        ));
        topGlow.setEffect(new GaussianBlur(18));

        Rectangle goldAccent = new Rectangle(8, 1010);
        goldAccent.setArcWidth(16);
        goldAccent.setArcHeight(16);
        goldAccent.setTranslateX(-408);
        goldAccent.setTranslateY(52);
        goldAccent.setFill(new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#F8D27A")),
            new Stop(1, Color.web("#9A6517"))
        ));

        Label titleLabel = new Label(safePosterText(request.titre(), "Evenement OXYN"));
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(690);
        titleLabel.setStyle(
                "-fx-text-fill: #FFFFFF;"
            + "-fx-font-size: 92px;"
                + "-fx-font-weight: 900;"
            + "-fx-line-spacing: 4px;"
        );

        Label subtitleLabel = new Label("Visuel premium concu pour une publication Facebook elegante et directe.");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(620);
        subtitleLabel.setStyle(
            "-fx-text-fill: rgba(189, 212, 240, 0.88);"
                + "-fx-font-size: 24px;"
                + "-fx-font-weight: 600;"
        );

        Label descriptionLabel = new Label(shortPosterDescription(request.description()));
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(610);
        descriptionLabel.setStyle(
                "-fx-text-fill: rgba(232, 240, 255, 0.93);"
            + "-fx-font-size: 30px;"
                + "-fx-line-spacing: 8px;"
        );

        Label watermark = new Label(compactPosterWatermark(request.titre()));
        watermark.setStyle(
            "-fx-text-fill: rgba(255, 255, 255, 0.035);"
                + "-fx-font-size: 186px;"
                + "-fx-font-weight: 900;"
                + "-fx-letter-spacing: 3px;"
        );
        watermark.setRotate(-90);

        StackPane watermarkHost = new StackPane(watermark);
        watermarkHost.setPrefWidth(220);
        watermarkHost.setMinWidth(220);
        watermarkHost.setMaxWidth(220);
        watermarkHost.setAlignment(Pos.CENTER);

        Label detailHeader = new Label("DETAILS EVENEMENT");
        detailHeader.setStyle(
            "-fx-text-fill: rgba(220, 235, 252, 0.88);"
                + "-fx-font-size: 18px;"
                + "-fx-font-weight: 800;"
        );

        HBox metaRow1 = new HBox(16,
                createPosterInfoCard("DATE", safePosterText(request.debut(), "A definir")),
                createPosterInfoCard("FIN", safePosterText(request.fin(), "A definir"))
        );
        metaRow1.setAlignment(Pos.CENTER_LEFT);

        HBox metaRow2 = new HBox(16,
                createPosterInfoCard("LIEU", safePosterText(buildPosterLocation(request), "OXYN")),
                createPosterInfoCard("PLACES", String.valueOf(request.placesMax()))
        );
        metaRow2.setAlignment(Pos.CENTER_LEFT);

        VBox leftColumn = new VBox(24, titleLabel, subtitleLabel, descriptionLabel);
        leftColumn.setAlignment(Pos.TOP_LEFT);
        leftColumn.setMaxWidth(650);

        VBox rightColumn = new VBox(18, detailHeader, metaRow1, metaRow2);
        rightColumn.setAlignment(Pos.TOP_LEFT);
        rightColumn.setMaxWidth(620);

        HBox lowerSection = new HBox(34, rightColumn, watermarkHost);
        lowerSection.setAlignment(Pos.BOTTOM_LEFT);

        VBox content = new VBox(58, leftColumn, lowerSection);
        content.setPadding(new Insets(138, 92, 96, 92));
        content.setAlignment(Pos.TOP_LEFT);
        content.setMaxWidth(940);

        StackPane.setAlignment(heroPanel, Pos.CENTER);
        StackPane.setAlignment(topGlow, Pos.TOP_RIGHT);
        StackPane.setAlignment(content, Pos.TOP_LEFT);
        root.getChildren().addAll(backgroundView, darkOverlay, accentGlow, heroPanel, topGlow, goldAccent, content);
        return renderNodeToImage(root, 1080, 1350);
    }

    private WritableImage renderNodeToImage(Parent node, int width, int height) {
        StackPane container = new StackPane(node);
        container.setPrefSize(width, height);
        container.setMinSize(width, height);
        container.setMaxSize(width, height);
        container.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(container, width, height, Color.TRANSPARENT);
        container.applyCss();
        container.layout();

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setViewport(new Rectangle2D(0, 0, width, height));

        WritableImage image = new WritableImage(width, height);
        return container.snapshot(params, image);
    }

    private VBox createPosterInfoCard(String title, String value) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-text-fill: rgba(152, 199, 255, 0.96);"
                        + "-fx-font-size: 18px;"
                        + "-fx-font-weight: bold;"
        );

        Label valueLabel = new Label(value);
        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(340);
        valueLabel.setStyle(
                "-fx-text-fill: #FFFFFF;"
                        + "-fx-font-size: 24px;"
                        + "-fx-font-weight: 700;"
        );

        VBox card = new VBox(8, titleLabel, valueLabel);
        card.setPrefWidth(420);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle(
                "-fx-background-color: rgba(6, 18, 43, 0.62);"
                        + "-fx-background-radius: 22px;"
                        + "-fx-border-color: rgba(123, 216, 255, 0.22);"
                        + "-fx-border-radius: 22px;"
        );
        return card;
    }

    private String buildPosterLocation(EventPosterAiService.PosterRequest request) {
        String lieu = safePosterText(request.lieu(), "Lieu a confirmer");
        String ville = safePosterText(request.ville(), "Ville non precisee");
        return lieu + " - " + ville;
    }

    private String shortPosterDescription(String description) {
        String value = safePosterText(description, "Une experience premium OXYN vous attend.");
        return value.length() > 170 ? value.substring(0, 167) + "..." : value;
    }

    private String compactPosterWatermark(String title) {
        String cleanTitle = safePosterText(title, "OXYN EVENT").toUpperCase();
        String compact = cleanTitle.replaceAll("[^A-Z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        if (compact.length() > 18) {
            compact = compact.substring(0, 18).trim();
        }
        return compact.isBlank() ? "OXYN EVENT" : compact;
    }

    private String safePosterText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private void resetPosterButton(Button button, String originalText) {
        button.setDisable(false);
        button.setText(originalText);
    }

    private void openFolderIfPossible(Path folder) {
        try {
            if (folder != null && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(folder.toFile());
            }
        } catch (Exception ignored) {
            // The saved path is still shown in the success dialog.
        }
    }

    private String buildPosterErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "La generation du poster a echoue. Verifiez la connexion Internet, les fichiers locaux, et votre configuration OpenAI.";
        }
        return message;
    }

    private String slugify(String input) {
        String base = safePosterText(input, "oxyn-evenement").toLowerCase();
        String slug = base.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "oxyn-evenement" : slug;
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
