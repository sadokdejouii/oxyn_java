package org.example.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Front-facing events page with a light web-style card layout.
 */
public class FrontEventsController implements Initializable {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortCombo;

    @FXML
    private FlowPane eventsContainer;

    @FXML
    private Label totalEventsLabel;

    @FXML
    private Label openEventsLabel;

    @FXML
    private Label citiesLabel;

    private static final int LOCAL_USER_ID = 1;

    private final EvenementServices evenementServices = new EvenementServices();
    private final InscriptionEvenementServices inscriptionServices = new InscriptionEvenementServices();
    private final AvisEvenementServices avisServices = new AvisEvenementServices();
    private final List<Evenement> allEvents = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.FRENCH);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Date : plus récentes",
                "Date : plus anciennes",
                "Titre : A à Z",
                "Titre : Z à A",
                "Ville : A à Z",
                "Ville : Z à A",
                "Places : croissant",
                "Places : décroissant"
        ));
        sortCombo.setValue("Date : plus récentes");
        configureSortComboAppearance();

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        eventsContainer.setAlignment(Pos.TOP_CENTER);

        Platform.runLater(this::installFrontStylesheetOnScene);
        loadEvents();
    }

    @FXML
    private void handleRefresh() {
        loadEvents();
    }

    private void configureSortComboAppearance() {
        sortCombo.setVisibleRowCount(8);
        sortCombo.setButtonCell(createLightSortCell());
        sortCombo.setCellFactory(listView -> createLightSortCell());
    }

    private ListCell<String> createLightSortCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                setText(item);
                setStyle(isSelected()
                        ? "-fx-background-color: #7cd8ff; -fx-text-fill: white; -fx-font-size: 12.5px; -fx-padding: 9 12;"
                        : "-fx-background-color: white; -fx-text-fill: #2a618b; -fx-font-size: 12.5px; -fx-padding: 9 12;");
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (getItem() != null) {
                    setStyle(selected
                            ? "-fx-background-color: #7cd8ff; -fx-text-fill: white; -fx-font-size: 12.5px; -fx-padding: 9 12;"
                            : "-fx-background-color: white; -fx-text-fill: #2a618b; -fx-font-size: 12.5px; -fx-padding: 9 12;");
                }
            }
        };
    }

    private void installFrontStylesheetOnScene() {
        if (sortCombo == null || sortCombo.getScene() == null) {
            return;
        }

        String css = getClass().getResource("/css/front-events.css").toExternalForm();
        if (!sortCombo.getScene().getStylesheets().contains(css)) {
            sortCombo.getScene().getStylesheets().add(css);
        }
    }

    private void loadEvents() {
        allEvents.clear();
        try {
            List<Evenement> loadedEvents = evenementServices.afficher();
            applyEventBusinessRules(loadedEvents);
            allEvents.addAll(loadedEvents);
            updateCounters();
            applyFilters();
        } catch (SQLException e) {
            updateCounters();
            renderEmptyState(
                    "Impossible de charger les événements",
                    "Vérifiez la connexion à la base de données puis réessayez."
            );
        }
    }

    private void applyEventBusinessRules(List<Evenement> events) {
        Date now = new Date();

        for (Evenement event : events) {
            if (event.getDateFin() != null && now.after(event.getDateFin()) && !isFinishedStatus(event.getStatut())) {
                event.setStatut("Terminée");
                try {
                    evenementServices.modifier(event);
                } catch (SQLException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    private void updateCounters() {
        if (totalEventsLabel != null) {
            totalEventsLabel.setText(String.valueOf(allEvents.size()));
        }

        long openEvents = allEvents.stream()
                .map(event -> safe(event.getStatut()).toLowerCase(Locale.ROOT))
                .filter(status -> status.contains("ouvert") || status.contains("actif") || status.contains("disponible"))
                .count();
        if (openEventsLabel != null) {
            openEventsLabel.setText(String.valueOf(openEvents));
        }

        long cities = allEvents.stream()
                .map(Evenement::getVille)
                .filter(city -> city != null && !city.isBlank())
                .map(city -> city.toLowerCase(Locale.ROOT))
                .distinct()
                .count();
        if (citiesLabel != null) {
            citiesLabel.setText(String.valueOf(cities));
        }
    }

    private void applyFilters() {
        String query = safe(searchField.getText()).trim().toLowerCase(Locale.ROOT);

        List<Evenement> filtered = allEvents.stream()
                .filter(event -> query.isEmpty()
                        || safe(event.getTitre()).toLowerCase(Locale.ROOT).contains(query)
                        || safe(event.getDescription()).toLowerCase(Locale.ROOT).contains(query)
                        || safe(event.getLieu()).toLowerCase(Locale.ROOT).contains(query)
                        || safe(event.getVille()).toLowerCase(Locale.ROOT).contains(query)
                        || safe(event.getStatut()).toLowerCase(Locale.ROOT).contains(query))
                .sorted(resolveComparator())
                .collect(Collectors.toList());

        renderEvents(filtered);
    }

    private Comparator<Evenement> resolveComparator() {
        String sortBy = safe(sortCombo.getValue());

        return switch (sortBy) {
            case "Titre : A à Z" -> Comparator.comparing(event -> safe(event.getTitre()).toLowerCase(Locale.ROOT));
            case "Titre : Z à A" -> Comparator.comparing((Evenement event) -> safe(event.getTitre()).toLowerCase(Locale.ROOT)).reversed();
            case "Ville : A à Z" -> Comparator.comparing(event -> safe(event.getVille()).toLowerCase(Locale.ROOT));
            case "Ville : Z à A" -> Comparator.comparing((Evenement event) -> safe(event.getVille()).toLowerCase(Locale.ROOT)).reversed();
            case "Places : croissant" -> Comparator.comparingInt(Evenement::getPlacesMax);
            case "Places : décroissant" -> Comparator.comparingInt(Evenement::getPlacesMax).reversed();
            case "Date : plus anciennes" -> Comparator.comparing(
                    (Evenement event) -> event.getDateDebut() == null ? new Date(0) : event.getDateDebut()
            );
            default -> Comparator.comparing(
                    (Evenement event) -> event.getDateDebut() == null ? new Date(0) : event.getDateDebut()
            ).reversed();
        };
    }

    private void renderEvents(List<Evenement> events) {
        eventsContainer.getChildren().clear();

        if (events.isEmpty()) {
            renderEmptyState(
                    "Aucun événement trouvé",
                    "Essayez un autre mot-clé ou changez le tri pour voir plus de résultats."
            );
            return;
        }

        for (Evenement event : events) {
            eventsContainer.getChildren().add(buildEventCard(event));
        }
    }

    private void renderEmptyState(String title, String message) {
        eventsContainer.getChildren().clear();

        VBox emptyBox = new VBox(8);
        emptyBox.setAlignment(Pos.CENTER_LEFT);
        emptyBox.setPadding(new Insets(22));
        emptyBox.setPrefWidth(900);
        emptyBox.getStyleClass().add("front-empty-box");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("front-empty-title");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("front-empty-text");

        emptyBox.getChildren().addAll(titleLabel, messageLabel);
        eventsContainer.getChildren().add(emptyBox);
    }

    private VBox buildEventCard(Evenement event) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(18));
        card.setPrefWidth(320);
        card.setMinHeight(320);
        card.getStyleClass().add("front-event-card");

        StackPane cover = buildEventCover(event);

        HBox chipRow = new HBox(10);
        chipRow.setAlignment(Pos.CENTER_LEFT);
        chipRow.getStyleClass().add("front-chip-row");

        Label statusChip = new Label(formatStatus(event.getStatut()));
        statusChip.getStyleClass().addAll("front-status-chip", resolveStatusClass(event.getStatut()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateChip = new Label(formatSingleDate(event.getDateDebut()));
        dateChip.getStyleClass().add("front-date-chip");

        chipRow.getChildren().addAll(statusChip, spacer, dateChip);

        Label titleLabel = new Label(fallback(event.getTitre(), "Événement à découvrir"));
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.getStyleClass().add("front-card-title");

        Label descriptionLabel = new Label(shorten(fallback(event.getDescription(), "Un événement pensé pour vous avec une expérience simple, claire et agréable."), 145));
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(Double.MAX_VALUE);
        descriptionLabel.setAlignment(Pos.CENTER);
        descriptionLabel.getStyleClass().add("front-card-description");

        VBox metaBox = new VBox(8);
        metaBox.getStyleClass().add("front-meta-box");
        metaBox.getChildren().addAll(
                buildMetaRow("📍", fallback(event.getLieu(), "Lieu à confirmer") + " • " + fallback(event.getVille(), "Ville")),
                buildMetaRow("🗓", formatDateRange(event)),
                buildMetaRow("👥", Math.max(0, event.getPlacesMax()) + " places maximum")
        );

        HBox actionsRow = new HBox(10);
        actionsRow.setAlignment(Pos.CENTER);
        actionsRow.getStyleClass().add("front-card-actions");

        Button inscriptionsButton = new Button("Voir les inscriptions");
        inscriptionsButton.getStyleClass().add("front-card-button");
        inscriptionsButton.setOnAction(actionEvent -> showInscriptionsPopup(event));

        Button avisButton = new Button("Voir les avis");
        avisButton.getStyleClass().addAll("front-card-button", "front-card-button-secondary");
        avisButton.setOnAction(actionEvent -> showAvisPopup(event));

        actionsRow.getChildren().addAll(inscriptionsButton, avisButton);

        card.getChildren().addAll(cover, chipRow, titleLabel, descriptionLabel, metaBox, actionsRow);
        return card;
    }

    private StackPane buildEventCover(Evenement event) {
        String visualKey = resolveVisualKey(event);

        StackPane cover = new StackPane();
        cover.setMinHeight(120);
        cover.setPrefHeight(120);
        cover.setMaxWidth(Double.MAX_VALUE);
        cover.getStyleClass().addAll("front-card-cover", resolveCoverClass(visualKey));

        VBox overlay = new VBox(4);
        overlay.setAlignment(Pos.CENTER);

        Label symbolLabel = new Label(resolveCoverSymbol(visualKey));
        symbolLabel.getStyleClass().add("front-cover-symbol");

        Label typeLabel = new Label(resolveCoverLabel(visualKey));
        typeLabel.getStyleClass().add("front-cover-label");

        overlay.getChildren().addAll(symbolLabel, typeLabel);
        cover.getChildren().add(overlay);
        StackPane.setAlignment(overlay, Pos.CENTER);

        return cover;
    }

    private HBox buildMetaRow(String icon, String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("front-meta-row");

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("front-meta-icon");

        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.getStyleClass().add("front-meta-text");
        HBox.setHgrow(textLabel, Priority.ALWAYS);

        row.getChildren().addAll(iconLabel, textLabel);
        return row;
    }

    private void showInscriptionsPopup(Evenement event) {
        try {
            List<InscriptionEvenement> inscriptions = inscriptionServices.afficher().stream()
                    .filter(item -> item.getIdEvenement() == event.getId())
                    .collect(Collectors.toList());

            List<String> rows = inscriptions.stream()
                    .map(item -> "• " + formatUser(item.getIdUser())
                            + " — " + fallback(item.getStatut(), "Inscrit")
                            + " — " + formatSingleDate(item.getDateInscription()))
                    .collect(Collectors.toList());

            InscriptionEvenement currentUserInscription = inscriptions.stream()
                    .filter(item -> item.getIdUser() == LOCAL_USER_ID)
                    .findFirst()
                    .orElse(null);

            boolean eventIsFull = isEventFull(event, inscriptions);
            boolean canJoin = canJoinEvent(event) && !eventIsFull && currentUserInscription == null;
            String primaryText = canJoin
                    ? "S'inscrire"
                    : (currentUserInscription != null ? "Déjà inscrit" : (eventIsFull ? "Places complètes" : "Inscription fermée"));

            showDataPopup(
                    "Inscriptions",
                    "Inscriptions pour : " + fallback(event.getTitre(), "cet événement"),
                    rows,
                    "Aucune personne n'est inscrite dans cet événement pour le moment.",
                    primaryText,
                    canJoin ? () -> joinEvent(event) : null,
                    true,
                    !canJoin,
                    "Annuler mon inscription",
                    currentUserInscription != null ? () -> cancelMyInscription(event, currentUserInscription.getId()) : null,
                    currentUserInscription != null,
                    false
            );
        } catch (SQLException exception) {
            showDataPopup(
                    "Inscriptions",
                    "Inscriptions pour : " + fallback(event.getTitre(), "cet événement"),
                    List.of(),
                    "Impossible de charger les inscriptions pour le moment.",
                    null,
                    null,
                    false,
                    false,
                    null,
                    null,
                    false,
                    false
            );
        }
    }

    private void showAvisPopup(Evenement event) {
        try {
            List<AvisEvenement> avisList = avisServices.afficher().stream()
                    .filter(item -> item.getIdEvenement() == event.getId())
                    .collect(Collectors.toList());

            List<String> rows = avisList.stream()
                    .map(item -> "• " + formatUser(item.getIdUser())
                            + " — " + formatStars(item.getNote())
                            + "\n" + fallback(item.getCommentaire(), "Aucun commentaire"))
                    .collect(Collectors.toList());

            AvisEvenement currentUserAvis = avisList.stream()
                    .filter(item -> item.getIdUser() == LOCAL_USER_ID)
                    .findFirst()
                    .orElse(null);

            showDataPopup(
                    "Avis",
                    "Avis pour : " + fallback(event.getTitre(), "cet événement"),
                    rows,
                    "Aucun avis n'a été publié pour cet événement pour le moment.",
                    currentUserAvis == null ? "Ajouter mon avis" : "Modifier mon avis",
                    () -> openAvisForm(event, currentUserAvis),
                    true,
                    false,
                    "Supprimer mon avis",
                    currentUserAvis != null ? () -> deleteMyAvis(event, currentUserAvis.getId()) : null,
                    currentUserAvis != null,
                    false
            );
        } catch (SQLException exception) {
            showDataPopup(
                    "Avis",
                    "Avis pour : " + fallback(event.getTitre(), "cet événement"),
                    List.of(),
                    "Impossible de charger les avis pour le moment.",
                    null,
                    null,
                    false,
                    false,
                    null,
                    null,
                    false,
                    false
            );
        }
    }

    private void showDataPopup(String title,
                               String header,
                               List<String> rows,
                               String emptyMessage,
                               String primaryText,
                               Runnable primaryAction,
                               boolean showPrimary,
                               boolean disablePrimary,
                               String secondaryText,
                               Runnable secondaryAction,
                               boolean showSecondary,
                               boolean disableSecondary) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/pages/FrontEventPopup.fxml"));
            Parent root = loader.load();

            FrontEventPopupController controller = loader.getController();
            controller.setData(title, header, rows, emptyMessage);
            controller.configureActions(primaryText, primaryAction, showPrimary, disablePrimary, secondaryText, secondaryAction, showSecondary, disableSecondary);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            if (eventsContainer != null && eventsContainer.getScene() != null) {
                stage.initOwner(eventsContainer.getScene().getWindow());
            }
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.showAndWait();
        } catch (IOException exception) {
            Alert fallbackAlert = new Alert(Alert.AlertType.INFORMATION);
            fallbackAlert.setTitle(title);
            fallbackAlert.setHeaderText(header);
            fallbackAlert.setContentText(rows.isEmpty() ? emptyMessage : String.join("\n\n", rows));
            fallbackAlert.showAndWait();
        }
    }

    private void joinEvent(Evenement event) {
        try {
            boolean alreadyRegistered = inscriptionServices.afficher().stream()
                    .anyMatch(item -> item.getIdEvenement() == event.getId() && item.getIdUser() == LOCAL_USER_ID);

            boolean eventIsFull = isEventFull(event);

            if (!canJoinEvent(event) || alreadyRegistered || eventIsFull) {
                Platform.runLater(() -> showInscriptionsPopup(event));
                return;
            }

            InscriptionEvenement inscription = new InscriptionEvenement(new Date(), "confirmée", event.getId(), LOCAL_USER_ID);
            inscriptionServices.ajouter(inscription);
            loadEvents();
            Platform.runLater(() -> showInscriptionsPopup(event));
        } catch (Exception exception) {
            showDataPopup(
                    "Inscriptions",
                    "Inscriptions pour : " + fallback(event.getTitre(), "cet événement"),
                    List.of(),
                    "Impossible d'effectuer l'inscription pour le moment.",
                    null,
                    null,
                    false,
                    false,
                    null,
                    null,
                    false,
                    false
            );
        }
    }

    private void cancelMyInscription(Evenement event, int inscriptionId) {
        try {
            inscriptionServices.supprimer(inscriptionId);
            loadEvents();
            Platform.runLater(() -> showInscriptionsPopup(event));
        } catch (Exception exception) {
            showDataPopup(
                    "Inscriptions",
                    "Inscriptions pour : " + fallback(event.getTitre(), "cet événement"),
                    List.of(),
                    "Impossible d'annuler votre inscription pour le moment.",
                    null,
                    null,
                    false,
                    false,
                    null,
                    null,
                    false,
                    false
            );
        }
    }

    private void openAvisForm(Evenement event, AvisEvenement existingAvis) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/pages/FrontAvisFormPopup.fxml"));
            Parent root = loader.load();

            FrontAvisFormPopupController controller = loader.getController();
            controller.setData(existingAvis == null ? "Ajouter mon avis" : "Modifier mon avis", existingAvis);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            if (eventsContainer != null && eventsContainer.getScene() != null) {
                stage.initOwner(eventsContainer.getScene().getWindow());
            }
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.showAndWait();

            if (controller.isConfirmed()) {
                saveOrUpdateAvis(event, existingAvis, controller.getSelectedNote(), controller.getComment());
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void saveOrUpdateAvis(Evenement event, AvisEvenement existingAvis, int note, String comment) {
        try {
            if (existingAvis == null) {
                AvisEvenement avis = new AvisEvenement(note, comment, new Date(), event.getId(), LOCAL_USER_ID);
                avisServices.ajouter(avis);
            } else {
                existingAvis.setNote(note);
                existingAvis.setCommentaire(comment);
                existingAvis.setCreatedAt(new Date());
                existingAvis.setIdEvenement(event.getId());
                existingAvis.setIdUser(LOCAL_USER_ID);
                avisServices.modifier(existingAvis);
            }

            loadEvents();
            Platform.runLater(() -> showAvisPopup(event));
        } catch (Exception exception) {
            showDataPopup(
                    "Avis",
                    "Avis pour : " + fallback(event.getTitre(), "cet événement"),
                    List.of(),
                    "Impossible d'enregistrer votre avis pour le moment.",
                    null,
                    null,
                    false,
                    false,
                    null,
                    null,
                    false,
                    false
            );
        }
    }

    private void deleteMyAvis(Evenement event, int avisId) {
        try {
            avisServices.supprimer(avisId);
            loadEvents();
            Platform.runLater(() -> showAvisPopup(event));
        } catch (Exception exception) {
            showDataPopup(
                    "Avis",
                    "Avis pour : " + fallback(event.getTitre(), "cet événement"),
                    List.of(),
                    "Impossible de supprimer votre avis pour le moment.",
                    null,
                    null,
                    false,
                    false,
                    null,
                    null,
                    false,
                    false
            );
        }
    }

    private boolean canJoinEvent(Evenement event) {
        String normalized = safe(event.getStatut()).toLowerCase(Locale.ROOT);
        return normalized.contains("à venir") || normalized.contains("a venir");
    }

    private boolean isEventFull(Evenement event) {
        try {
            List<InscriptionEvenement> inscriptions = inscriptionServices.afficher().stream()
                    .filter(item -> item.getIdEvenement() == event.getId())
                    .collect(Collectors.toList());
            return isEventFull(event, inscriptions);
        } catch (SQLException exception) {
            return false;
        }
    }

    private boolean isEventFull(Evenement event, List<InscriptionEvenement> inscriptions) {
        int maxPlaces = Math.max(0, event.getPlacesMax());
        if (maxPlaces <= 0) {
            return false;
        }
        return inscriptions.size() >= maxPlaces;
    }

    private boolean isFinishedStatus(String status) {
        String normalized = safe(status).toLowerCase(Locale.ROOT);
        return normalized.contains("termin");
    }

    private String formatUser(int userId) {
        return userId == LOCAL_USER_ID ? "Vous" : "Utilisateur";
    }

    private String formatStars(int note) {
        int value = Math.max(0, Math.min(5, note));
        return "★".repeat(value) + "☆".repeat(5 - value) + " (" + value + "/5)";
    }

    private void showEventDetails(Evenement event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(fallback(event.getTitre(), "Détails de l'événement"));
        alert.setHeaderText(null);
        alert.setContentText(
                "Titre : " + fallback(event.getTitre(), "—") + "\n\n"
                        + "Description : " + fallback(event.getDescription(), "—") + "\n\n"
                        + "Lieu : " + fallback(event.getLieu(), "—") + "\n"
                        + "Ville : " + fallback(event.getVille(), "—") + "\n"
                        + "Date : " + formatDateRange(event) + "\n"
                        + "Places max : " + Math.max(0, event.getPlacesMax()) + "\n"
                        + "Statut : " + formatStatus(event.getStatut())
        );
        alert.showAndWait();
    }

    private String formatSingleDate(Date date) {
        return date == null ? "Date à venir" : dateFormat.format(date);
    }

    private String formatDateRange(Evenement event) {
        String start = formatSingleDate(event.getDateDebut());
        if (event.getDateFin() == null) {
            return start;
        }
        return start + " → " + dateFormat.format(event.getDateFin());
    }

    private String formatStatus(String status) {
        return fallback(status, "À venir");
    }

    private String resolveVisualKey(Evenement event) {
        String source = (safe(event.getTitre()) + " " + safe(event.getDescription())).toLowerCase(Locale.ROOT);

        if (source.contains("yoga")) {
            return "yoga";
        }
        if (source.contains("football") || source.contains("foot") || source.contains("soccer")) {
            return "football";
        }
        if (source.contains("basket") || source.contains("basketball")) {
            return "basketball";
        }
        if (source.contains("tennis") || source.contains("padel")) {
            return "tennis";
        }
        if (source.contains("running") || source.contains("course") || source.contains("marathon")) {
            return "running";
        }
        if (source.contains("swim") || source.contains("natation") || source.contains("piscine")) {
            return "swimming";
        }
        return "generic";
    }

    private String resolveCoverClass(String key) {
        return switch (key) {
            case "yoga" -> "front-cover-yoga";
            case "football" -> "front-cover-football";
            case "basketball" -> "front-cover-basketball";
            case "tennis" -> "front-cover-tennis";
            case "running" -> "front-cover-running";
            case "swimming" -> "front-cover-swimming";
            default -> "front-cover-generic";
        };
    }

    private String resolveCoverSymbol(String key) {
        return switch (key) {
            case "yoga" -> "☯";
            case "football" -> "⚽";
            case "basketball" -> "🏀";
            case "tennis" -> "🎾";
            case "running" -> "🏃";
            case "swimming" -> "🏊";
            default -> "🎉";
        };
    }

    private String resolveCoverLabel(String key) {
        return switch (key) {
            case "yoga" -> "Séance de yoga";
            case "football" -> "Match de football";
            case "basketball" -> "Événement basket";
            case "tennis" -> "Tennis / Padel";
            case "running" -> "Course sportive";
            case "swimming" -> "Activité natation";
            default -> "Événement";
        };
    }

    private String resolveStatusClass(String status) {
        String normalized = safe(status).toLowerCase(Locale.ROOT);

        if (normalized.contains("annul")) {
            return "front-status-cancelled";
        }
        if (normalized.contains("cours") || normalized.contains("ouvert") || normalized.contains("actif") || normalized.contains("disponible")) {
            return "front-status-active";
        }
        if (normalized.contains("termin") || normalized.contains("complet") || normalized.contains("fer")) {
            return "front-status-finished";
        }
        return "front-status-upcoming";
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String shorten(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
