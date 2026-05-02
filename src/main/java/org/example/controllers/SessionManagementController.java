package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.entities.Salle;
import org.example.entities.Session;
import org.example.services.SalleService;
import org.example.services.SessionContext;
import org.example.services.SessionService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SessionManagementController implements Initializable {

    @FXML private Label statTotal;
    @FXML private Label statPlanifiees;
    @FXML private Label statInscrits;
    @FXML private Label countLabel;
    @FXML private FlowPane sessionsGrid;

    @FXML private ToggleButton filterAll;
    @FXML private ToggleButton filterActive;
    @FXML private ToggleButton filterInactive;

    @FXML private StackPane dialogOverlay;
    @FXML private Label     dialogTitle;
    @FXML private Label     dialogError;
    @FXML private TextField fieldTitle;
    @FXML private ComboBox<String> fieldActivite;
    @FXML private ComboBox<Salle>  fieldSalle;
    @FXML private DatePicker       fieldDate;
    @FXML private TextField        fieldHeureDebut;
    @FXML private TextField        fieldHeureFin;
    @FXML private TextField        fieldCapacite;
    @FXML private TextField        fieldPrice;
    @FXML private TextArea         fieldDescription;

    private static final DateTimeFormatter FMT         = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final List<String> ACTIVITES = List.of(
        "Fitness", "Yoga", "Cardio", "Musculation", "Pilates",
        "Boxe", "Natation", "Zumba", "CrossFit", "Arts martiaux"
    );

    private final SessionService sessionService = new SessionService();
    private final SalleService   salleService   = new SalleService();

    private List<Session> allSessions  = List.of();
    private Session sessionEnEdition   = null;
    private String  currentFilter      = "ALL";

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fieldActivite.getItems().addAll(ACTIVITES);
        // Block past dates — only today and future selectable
        fieldDate.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        fieldDate.setValue(LocalDate.now());
        loadSallesCombo();
        loadSessions();
    }

    private void loadSallesCombo() {
        fieldSalle.getItems().clear();
        Salle none = new Salle();
        none.setId(0);
        none.setName("Aucune salle");
        fieldSalle.getItems().add(none);
        try {
            if (dialogError != null) {
                dialogError.setText("");
            }
            fieldSalle.getItems().addAll(salleService.afficher());
        } catch (SQLException ex) {
            ex.printStackTrace();
            if (dialogError != null) {
                dialogError.setText("Salles : " + (ex.getMessage() != null ? ex.getMessage() : ex.toString()));
            }
        }
        fieldSalle.getSelectionModel().selectFirst();
        fieldSalle.setConverter(new javafx.util.StringConverter<Salle>() {
            @Override public String toString(Salle s)    { return s == null ? "" : s.getName(); }
            @Override public Salle  fromString(String s) { return null; }
        });
    }

    // ── Load & filter ────────────────────────────────────────────────────────

    private void loadSessions() {
        try {
            SessionContext ctx = SessionContext.getInstance();
            if (!ctx.hasDbUser()) {
                allSessions = List.of();
            } else if (ctx.isEncadrant()) {
                allSessions = sessionService.afficherPourCoach(ctx.getUserId());
            } else {
                allSessions = sessionService.afficher();
            }
        } catch (SQLException e) {
            allSessions = List.of();
            countLabel.setText("Erreur de chargement");
            e.printStackTrace();
        }
        updateStats();
        applyFilter();
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allSessions.size()));
        long planif = allSessions.stream()
            .filter(s -> s.getStartAt() != null && s.getStartAt().isAfter(LocalDateTime.now()))
            .count();
        statPlanifiees.setText(String.valueOf(planif));
        int totalCap = allSessions.stream().mapToInt(Session::getCapacity).sum();
        statInscrits.setText(String.valueOf(totalCap));
    }

    private void applyFilter() {
        if (!SessionContext.getInstance().hasDbUser()) {
            sessionsGrid.getChildren().clear();
            countLabel.setText("Connectez-vous pour gérer vos sessions");
            return;
        }
        List<Session> filtered;
        switch (currentFilter) {
            case "ACTIVE":   filtered = allSessions.stream().filter(Session::isActive).collect(Collectors.toList()); break;
            case "INACTIVE": filtered = allSessions.stream().filter(s -> !s.isActive()).collect(Collectors.toList()); break;
            default:         filtered = allSessions;
        }
        sessionsGrid.getChildren().clear();
        countLabel.setText(filtered.size() + " session(s)");
        for (Session s : filtered) sessionsGrid.getChildren().add(buildCard(s));
        // Force layout refresh
        sessionsGrid.requestLayout();
        javafx.application.Platform.runLater(() -> {
            sessionsGrid.requestLayout();
            if (sessionsGrid.getParent() != null) sessionsGrid.getParent().requestLayout();
        });
    }

    @FXML
    private void handleFilter(javafx.event.ActionEvent e) {
        ToggleButton src = (ToggleButton) e.getSource();
        currentFilter = (String) src.getUserData();
        filterAll.setSelected("ALL".equals(currentFilter));
        filterActive.setSelected("ACTIVE".equals(currentFilter));
        filterInactive.setSelected("INACTIVE".equals(currentFilter));
        applyFilter();
    }

    @FXML private void handleRefresh() { loadSessions(); }

    // ── Card builder ─────────────────────────────────────────────────────────

    private VBox buildCard(Session s) {
        VBox card = new VBox(0);
        card.setPrefWidth(310);
        card.getStyleClass().add("cl-session-card");

        HBox header = new HBox(10);
        header.getStyleClass().add("enc-sess-card-header");
        header.setAlignment(Pos.CENTER_LEFT);
        Label typeLabel = new Label(s.getTitle());
        typeLabel.getStyleClass().add("cl-card-title");
        typeLabel.setWrapText(true);
        HBox.setHgrow(typeLabel, Priority.ALWAYS);

        Label actBadge = new Label(typeEmoji(s.getTitle()));
        actBadge.getStyleClass().add("enc-sess-emoji-badge");

        header.getChildren().addAll(typeLabel, actBadge, buildBadge(s));

        VBox body = new VBox(7);
        body.getStyleClass().add("enc-sess-card-body");

        int me = SessionContext.getInstance().getUserId();
        String coachLabel = (s.getCoachUserId() != null && s.getCoachUserId() == me && me > 0)
            ? "Vous"
            : ("Coach #" + (s.getCoachUserId() != null ? s.getCoachUserId() : "—"));
        body.getChildren().add(infoRow("👤", coachLabel));

        // Salle
        body.getChildren().add(infoRow("🏢",
            s.getGymnasiumName() != null ? s.getGymnasiumName() : "—"));

        // Horaire start_at → end_at
        String horaire = "—";
        if (s.getStartAt() != null) {
            horaire = s.getStartAt().format(FMT_DISPLAY);
            if (s.getEndAt() != null)
                horaire += " → " + s.getEndAt().format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        body.getChildren().add(infoRow("🕐", horaire));

        // Capacite + Prix
        body.getChildren().add(infoRow("👥", s.getCapacity() + " places"));
        body.getChildren().add(infoRow("💰", String.format("%.2f TND", s.getPrice())));

        // Notes (description)
        if (s.getDescription() != null && !s.getDescription().isBlank()) {
            Label notes = new Label(s.getDescription());
            notes.getStyleClass().add("enc-sess-card-notes");
            notes.setWrapText(true);
            body.getChildren().add(notes);
        }

        // Actions
        HBox actions = new HBox(8);
        actions.getStyleClass().add("enc-sess-card-actions");
        actions.setAlignment(Pos.CENTER_LEFT);
        Button btnEdit = new Button("Modifier");
        btnEdit.getStyleClass().add("enc-sess-btn-edit");
        btnEdit.setOnAction(e -> openEditDialog(s));
        Button btnDel = new Button("Supprimer");
        btnDel.getStyleClass().add("enc-sess-btn-delete");
        btnDel.setOnAction(e -> openConfirm(s));
        actions.getChildren().addAll(btnEdit, btnDel);

        card.getChildren().addAll(header, body, actions);
        return card;
    }

    private Label buildBadge(Session s) {
        Label badge;
        if (!s.isActive()) {
            badge = new Label("Inactive");
            badge.getStyleClass().add("enc-sess-badge-off");
        } else if (s.getStartAt() != null && s.getStartAt().isAfter(LocalDateTime.now())) {
            badge = new Label("Planifiée");
            badge.getStyleClass().add("enc-sess-badge-plan");
        } else {
            badge = new Label("Terminée");
            badge.getStyleClass().add("enc-sess-badge-done");
        }
        return badge;
    }

    private HBox infoRow(String icon, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 13px;");
        Label val = new Label(value != null && !value.isBlank() ? value : "—");
        val.getStyleClass().add("cl-card-info");
        val.setWrapText(true);
        HBox.setHgrow(val, Priority.ALWAYS);
        row.getChildren().addAll(ico, val);
        return row;
    }

    private String typeEmoji(String title) {
        if (title == null) return "🏋️";
        switch (title.toLowerCase()) {
            case "yoga":          return "🧘";
            case "cardio":        return "🏃";
            case "musculation":   return "💪";
            case "pilates":       return "🤸";
            case "boxe":          return "🥊";
            case "natation":      return "🏊";
            case "zumba":         return "💃";
            case "crossfit":      return "🏋️";
            case "arts martiaux": return "🥋";
            case "fitness":       return "🏋️";
            default:              return "🏃";
        }
    }

    // ── Dialog ───────────────────────────────────────────────────────────────

    @FXML
    private void handleAjouter() {
        sessionEnEdition = null;
        dialogTitle.setText("Nouvelle session");
        clearDialog();
        showDialog(true);
    }

    private void openEditDialog(Session s) {
        sessionEnEdition = s;
        dialogTitle.setText("Modifier la session");
        if (ACTIVITES.contains(s.getTitle())) {
            fieldActivite.setValue(s.getTitle());
            fieldTitle.clear();
        } else {
            fieldActivite.setValue(null);
            fieldTitle.setText(s.getTitle() != null ? s.getTitle() : "");
        }
        if (s.getStartAt() != null) {
            fieldDate.setValue(s.getStartAt().toLocalDate());
            fieldHeureDebut.setText(s.getStartAt().format(FMT));
        }
        if (s.getEndAt() != null) fieldHeureFin.setText(s.getEndAt().format(FMT));
        fieldCapacite.setText(String.valueOf(s.getCapacity()));
        fieldPrice.setText(String.format("%.2f", s.getPrice()));
        fieldDescription.setText(s.getDescription() != null ? s.getDescription() : "");
        if (s.getGymnasiumId() != null) {
            fieldSalle.getItems().stream()
                .filter(sl -> sl.getId() == s.getGymnasiumId())
                .findFirst().ifPresent(fieldSalle::setValue);
        } else {
            fieldSalle.getSelectionModel().selectFirst();
        }
        dialogError.setText("");
        showDialog(true);
    }

    @FXML
    private void handleDialogSave() {
        if (!validateForm()) return;
        int coachUserId = SessionContext.getInstance().getUserId();
        if (coachUserId <= 0) {
            dialogError.setText("Vous devez être connecté pour enregistrer une session.");
            return;
        }
        try {
            String title = resolveSessionTitle();
            LocalDate date   = fieldDate.getValue();
            LocalTime tDebut = LocalTime.parse(fieldHeureDebut.getText().trim(), FMT);
            LocalDateTime startAt = LocalDateTime.of(date, tDebut);
            LocalDateTime endAt   = null;
            String hFin = fieldHeureFin.getText().trim();
            if (!hFin.isEmpty())
                endAt = LocalDateTime.of(date, LocalTime.parse(hFin, FMT));
            int    capacity = Integer.parseInt(fieldCapacite.getText().trim());
            double price    = parsePriceRequired(fieldPrice.getText().trim());
            String desc     = fieldDescription.getText().trim();
            Salle  salle    = fieldSalle.getValue();
            Integer gymId   = (salle != null && salle.getId() > 0) ? salle.getId() : null;

            if (sessionEnEdition == null) {
                Session s = new Session(title, desc, startAt, endAt, capacity, price, gymId, coachUserId);
                sessionService.ajouter(s);
            } else {
                sessionEnEdition.setTitle(title);
                sessionEnEdition.setDescription(desc);
                sessionEnEdition.setStartAt(startAt);
                sessionEnEdition.setEndAt(endAt);
                sessionEnEdition.setCapacity(capacity);
                sessionEnEdition.setPrice(price);
                sessionEnEdition.setGymnasiumId(gymId);
                sessionService.modifier(sessionEnEdition);
            }
            showDialog(false);
            loadSessions();
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) {
                msg = e.getClass().getSimpleName();
            }
            dialogError.setText("Erreur : " + msg);
        }
    }

    @FXML private void handleDialogCancel() { showDialog(false); }

    // ── Confirm delete ───────────────────────────────────────────────────────

    private void openConfirm(Session s) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la session");
        confirm.setHeaderText("Supprimer \"" + s.getTitle() + "\" ?");
        confirm.setContentText("La session sera desactivee (is_active = 0).");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try {
                    sessionService.supprimer(s.getId());
                    loadSessions();
                } catch (Exception e) {
                    e.printStackTrace();
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Suppression");
                    err.setHeaderText("Impossible de désactiver la session");
                    err.setContentText(e.getMessage() != null ? e.getMessage() : e.toString());
                    err.showAndWait();
                }
            }
        });
    }

    // ── Validation ───────────────────────────────────────────────────────────

    private boolean validateForm() {
        String title = resolveSessionTitle();
        if (title.isBlank()) {
            dialogError.setText("Choisissez un type d'activité ou saisissez un titre affiché.");
            return false;
        }
        if (fieldDate.getValue() == null) {
            dialogError.setText("La date est obligatoire."); return false;
        }
        if (!fieldHeureDebut.getText().trim().matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
            dialogError.setText("Heure début invalide (format HH:mm, ex. 09:00)."); return false;
        }
        String hFinRaw = fieldHeureFin.getText().trim();
        if (!hFinRaw.isEmpty() && !hFinRaw.matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
            dialogError.setText("Heure fin invalide (format HH:mm) ou laissez vide.");
            return false;
        }
        try {
            LocalDate d = fieldDate.getValue();
            LocalTime t0 = LocalTime.parse(fieldHeureDebut.getText().trim(), FMT);
            LocalDateTime startAt = LocalDateTime.of(d, t0);
            if (!hFinRaw.isEmpty()) {
                LocalTime t1 = LocalTime.parse(hFinRaw, FMT);
                LocalDateTime endAt = LocalDateTime.of(d, t1);
                if (!endAt.isAfter(startAt)) {
                    dialogError.setText("L'heure de fin doit être après l'heure de début.");
                    return false;
                }
            }
        } catch (Exception e) {
            dialogError.setText("Vérifiez la date et les heures.");
            return false;
        }
        try {
            if (Integer.parseInt(fieldCapacite.getText().trim()) <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            dialogError.setText("Capacité invalide (entier > 0)."); return false;
        }
        String priceStr = fieldPrice.getText().trim();
        if (priceStr.isBlank()) {
            dialogError.setText("Indiquez un prix (0 si gratuit)."); return false;
        }
        try {
            double p = parsePriceRequired(priceStr);
            if (p < 0 || Double.isNaN(p) || Double.isInfinite(p)) {
                dialogError.setText("Le prix doit être un nombre ≥ 0."); return false;
            }
        } catch (NumberFormatException e) {
            dialogError.setText("Prix invalide (ex. 15 ou 15,50)."); return false;
        }
        dialogError.setText("");
        return true;
    }

    /** Titre persisté : type d'activité si choisi, sinon titre libre. */
    private String resolveSessionTitle() {
        String act = fieldActivite.getValue();
        if (act != null && !act.isBlank()) {
            return act.trim();
        }
        return fieldTitle.getText().trim();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void showDialog(boolean show) {
        dialogOverlay.setVisible(show);
        dialogOverlay.setManaged(show);
    }

    private void clearDialog() {
        fieldTitle.clear();
        fieldActivite.setValue(null);
        fieldDate.setValue(LocalDate.now());
        fieldHeureDebut.clear();
        fieldHeureFin.clear();
        fieldCapacite.clear();
        fieldPrice.clear();
        fieldDescription.clear();
        fieldSalle.getSelectionModel().selectFirst();
        dialogError.setText("");
    }

    private double parsePriceRequired(String s) {
        return Double.parseDouble(s.replace(",", ".").replace(" ", ""));
    }

}
