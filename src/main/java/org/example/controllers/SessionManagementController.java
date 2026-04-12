package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.entities.Salle;
import org.example.entities.Session;
import org.example.services.SalleService;
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
        // Dark cell factory so dropdown text is visible on dark background
        javafx.util.Callback<javafx.scene.control.ListView<String>, javafx.scene.control.ListCell<String>> cellFactory =
            lv -> new javafx.scene.control.ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-background-color: #0d1b3e;");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #e6edf3; -fx-background-color: #0d1b3e; -fx-font-size: 13px; -fx-padding: 8px 12px;");
                    }
                }
            };
        fieldActivite.setCellFactory(cellFactory);
        fieldActivite.setButtonCell(cellFactory.call(null));
        // Block past dates — only today and future selectable
        fieldDate.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        fieldDate.setValue(LocalDate.now());
        // Inject dark CSS into the DatePicker popup (popup has its own scene)
        applyDarkDatePicker(fieldDate);
        loadSallesCombo();
        loadSessions();
    }

    private void applyDarkDatePicker(DatePicker dp) {
        String css = getClass().getResource("/css/datepicker-dark.css").toExternalForm();
        dp.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.Node popupContent = dp.lookup(".date-picker-popup");
                    if (popupContent != null && popupContent.getScene() != null) {
                        popupContent.getScene().getStylesheets().add(css);
                    }
                });
            }
        });
        // Also hook on showing
        dp.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.Node popupContent = dp.lookup(".date-picker-popup");
                    if (popupContent != null && popupContent.getScene() != null) {
                        if (!popupContent.getScene().getStylesheets().contains(css)) {
                            popupContent.getScene().getStylesheets().add(css);
                        }
                    }
                });
            }
        });
    }

    private void loadSallesCombo() {
        fieldSalle.getItems().clear();
        Salle none = new Salle(); none.setId(0); none.setName("Aucune salle");
        fieldSalle.getItems().add(none);
        try { fieldSalle.getItems().addAll(salleService.afficher()); }
        catch (SQLException ignored) {}
        fieldSalle.getSelectionModel().selectFirst();
        fieldSalle.setConverter(new javafx.util.StringConverter<Salle>() {
            @Override public String toString(Salle s)    { return s == null ? "" : s.getName(); }
            @Override public Salle  fromString(String s) { return null; }
        });
    }

    // ── Load & filter ────────────────────────────────────────────────────────

    private void loadSessions() {
        try {
            allSessions = sessionService.afficher();
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
        card.getStyleClass().add("sess-card");

        // Header
        HBox header = new HBox(10);
        header.getStyleClass().add("sess-card-header");
        header.setAlignment(Pos.CENTER_LEFT);
        Label typeLabel = new Label(s.getTitle());
        typeLabel.getStyleClass().add("sess-card-type");
        typeLabel.setWrapText(true);
        HBox.setHgrow(typeLabel, Priority.ALWAYS);

        // Activity badge (small, distinct style)
        Label actBadge = new Label(typeEmoji(s.getTitle()));
        actBadge.setStyle("-fx-font-size: 11px; -fx-background-color: rgba(79,195,247,0.15); " +
                          "-fx-text-fill: #4FC3F7; -fx-background-radius: 12px; -fx-padding: 2px 8px;");

        header.getChildren().addAll(typeLabel, actBadge, buildBadge(s));

        // Body
        VBox body = new VBox(7);
        body.getStyleClass().add("sess-card-body");

        // Coach (coach_user_id fixe = 7)
        String coachLabel = "Coach #" + (s.getCoachUserId() != null ? s.getCoachUserId() : "—");
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
            notes.getStyleClass().add("sess-card-notes");
            notes.setWrapText(true);
            body.getChildren().add(notes);
        }

        // Actions
        HBox actions = new HBox(8);
        actions.getStyleClass().add("sess-card-actions");
        actions.setAlignment(Pos.CENTER_LEFT);
        Button btnEdit = new Button("Modifier");
        btnEdit.getStyleClass().add("sess-btn-edit");
        btnEdit.setOnAction(e -> openEditDialog(s));
        Button btnDel = new Button("Supprimer");
        btnDel.getStyleClass().add("sess-btn-delete");
        btnDel.setOnAction(e -> openConfirm(s));
        actions.getChildren().addAll(btnEdit, btnDel);

        card.getChildren().addAll(header, body, actions);
        return card;
    }

    private Label buildBadge(Session s) {
        Label badge;
        if (!s.isActive()) {
            badge = new Label("Inactive");
            badge.getStyleClass().add("sess-badge-inactive");
        } else if (s.getStartAt() != null && s.getStartAt().isAfter(LocalDateTime.now())) {
            badge = new Label("Planifiee");
            badge.getStyleClass().add("sess-badge-planifiee");
        } else {
            badge = new Label("Terminee");
            badge.getStyleClass().add("sess-badge-terminee");
        }
        return badge;
    }

    private HBox infoRow(String icon, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 13px;");
        Label val = new Label(value != null && !value.isBlank() ? value : "—");
        val.getStyleClass().add("sess-card-info");
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
        fieldTitle.setText(s.getTitle());
        if (ACTIVITES.contains(s.getTitle())) fieldActivite.setValue(s.getTitle());
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
        try {
            String title = fieldActivite.getValue() != null
                ? fieldActivite.getValue() : fieldTitle.getText().trim();
            LocalDate date   = fieldDate.getValue();
            LocalTime tDebut = LocalTime.parse(fieldHeureDebut.getText().trim(), FMT);
            LocalDateTime startAt = LocalDateTime.of(date, tDebut);
            LocalDateTime endAt   = null;
            String hFin = fieldHeureFin.getText().trim();
            if (!hFin.isEmpty() && hFin.matches("^([01]\\d|2[0-3]):[0-5]\\d$"))
                endAt = LocalDateTime.of(date, LocalTime.parse(hFin, FMT));
            int    capacity = Integer.parseInt(fieldCapacite.getText().trim());
            double price    = parsePrice(fieldPrice.getText().trim());
            String desc     = fieldDescription.getText().trim();
            Salle  salle    = fieldSalle.getValue();
            Integer gymId   = (salle != null && salle.getId() > 0) ? salle.getId() : null;

            if (sessionEnEdition == null) {
                Session s = new Session(title, desc, startAt, endAt, capacity, price, gymId, null);
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
            dialogError.setText("Erreur : " + e.getMessage());
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
                }
            }
        });
    }

    // ── Validation ───────────────────────────────────────────────────────────

    private boolean validateForm() {
        String title = fieldActivite.getValue() != null
            ? fieldActivite.getValue() : fieldTitle.getText().trim();
        if (title.isBlank()) {
            dialogError.setText("Le titre est obligatoire."); return false;
        }
        if (fieldDate.getValue() == null) {
            dialogError.setText("La date est obligatoire."); return false;
        }
        if (!fieldHeureDebut.getText().trim().matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
            dialogError.setText("Heure debut invalide (HH:mm)."); return false;
        }
        try {
            if (Integer.parseInt(fieldCapacite.getText().trim()) <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            dialogError.setText("Capacite invalide (entier > 0)."); return false;
        }
        dialogError.setText("");
        return true;
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

    private double parsePrice(String s) {
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (NumberFormatException e) { return 0.0; }
    }

}
