package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.Cursor;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import org.example.entities.Salle;
import org.example.entities.Session;
import org.example.entities.User;
import org.example.services.SessionService;
import org.example.services.UserService;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Contrôleur pour la page de planning d'une salle de sport
 * Affiche un planning hebdomadaire avec grille de 7 jours comme Galaxy Gym
 */
public class SallePlanningPageController implements Initializable {

    @FXML
    private VBox rootContainer;

    private SessionService sessionService;
    private UserService userService;
    private Salle currentSalle;
    private List<Session> allSessions;
    private LocalDate currentWeekStart;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("d");
    private static final DateTimeFormatter WEEK_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM");
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    
    // UI Components
    private Label salleNameLabel;
    private Label weekLabel;
    private ScrollPane planningScrollPane;
    private GridPane weekGrid;
    private final Map<Integer, VBox> dayColumns = new HashMap<>();
    
    // Couleurs par catégorie
    private static final Map<String, String> CATEGORY_COLORS = new HashMap<>();
    static {
        CATEGORY_COLORS.put("yoga", "#10b981");
        CATEGORY_COLORS.put("pilates", "#10b981");
        CATEGORY_COLORS.put("box", "#ef4444");
        CATEGORY_COLORS.put("combat", "#ef4444");
        CATEGORY_COLORS.put("attack", "#ef4444");
        CATEGORY_COLORS.put("karate", "#ef4444");
        CATEGORY_COLORS.put("cardio", "#f59e0b");
        CATEGORY_COLORS.put("hiit", "#f59e0b");
        CATEGORY_COLORS.put("sprint", "#f59e0b");
        CATEGORY_COLORS.put("tabata", "#f59e0b");
        CATEGORY_COLORS.put("core", "#f59e0b");
        CATEGORY_COLORS.put("blast", "#f59e0b");
        CATEGORY_COLORS.put("functional", "#f59e0b");
        CATEGORY_COLORS.put("bike", "#f59e0b");
        CATEGORY_COLORS.put("pump", "#f59e0b");
        CATEGORY_COLORS.put("muscu", "#8b5cf6");
        CATEGORY_COLORS.put("musculation", "#8b5cf6");
        CATEGORY_COLORS.put("force", "#8b5cf6");
        CATEGORY_COLORS.put("cross", "#8b5cf6");
        CATEGORY_COLORS.put("body", "#8b5cf6");
        CATEGORY_COLORS.put("zumba", "#ec4899");
        CATEGORY_COLORS.put("danse", "#ec4899");
        CATEGORY_COLORS.put("default", "#3b82f6");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sessionService = new SessionService();
        userService = new UserService();
        
        // Initialiser la semaine courante
        currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
        // Construire toute l'interface programmatiquement
        buildUI();
    }

    /**
     * Construit toute l'interface programmatiquement
     */
    private void buildUI() {
        rootContainer.setSpacing(0);
        
        // 1. Toolbar supérieure
        HBox toolbar = buildToolbar();
        rootContainer.getChildren().add(toolbar);
        
        // 2. Barre de légende
        HBox legendBar = buildLegendBar();
        rootContainer.getChildren().add(legendBar);
        
        // 3. Barre de navigation semaine
        HBox weekNav = buildWeekNavigation();
        rootContainer.getChildren().add(weekNav);
        
        // 4. ScrollPane avec grille
        planningScrollPane = new ScrollPane();
        planningScrollPane.setFitToWidth(true);
        planningScrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        planningScrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        planningScrollPane.setStyle("-fx-background: #f8fafc;");
        
        VBox.setVgrow(planningScrollPane, Priority.ALWAYS);
        rootContainer.getChildren().add(planningScrollPane);
        
        // Construire la grille de semaine initiale
        buildWeekGrid();
    }

    /**
     * Construit la toolbar supérieure
     */
    private HBox buildToolbar() {
        HBox toolbar = new HBox();
        toolbar.setStyle("-fx-background-color: #1e293b;");
        toolbar.setPadding(new Insets(16));
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Bouton retour
        Button btnBack = new Button("← Retour à la salle");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
        btnBack.setOnMouseClicked(e -> handleBack());
        
        // Centre: nom salle + sous-titre
        salleNameLabel = new Label("Planning de la salle");
        salleNameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px;");
        
        Label subtitleLabel = new Label("Planning hebdomadaire des séances");
        subtitleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-opacity: 0.7;");
        
        VBox centerBox = new VBox(2, salleNameLabel, subtitleLabel);
        HBox.setHgrow(centerBox, Priority.ALWAYS);
        
        // Bouton refresh
        Button btnRefresh = new Button("⟳ Actualiser");
        btnRefresh.setStyle("-fx-background-color: #374151; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;");
        btnRefresh.setOnMouseClicked(e -> buildWeekGrid());
        
        toolbar.getChildren().addAll(btnBack, centerBox, btnRefresh);
        return toolbar;
    }

    /**
     * Construit la barre de légende
     */
    private HBox buildLegendBar() {
        HBox legendBar = new HBox();
        legendBar.setStyle("-fx-background-color: white; -fx-padding: 12px 20px; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1px 0;");
        legendBar.setAlignment(javafx.geometry.Pos.CENTER);
        legendBar.setSpacing(16);
        
        legendBar.getChildren().add(new Label("Légende :"));
        
        String[][] categories = {
            {"#10b981", "Yoga/Pilates"},
            {"#ef4444", "Box/Combat"},
            {"#f59e0b", "Cardio/HIIT"},
            {"#8b5cf6", "Muscu"},
            {"#ec4899", "Zumba/Danse"},
            {"#3b82f6", "Circuit/Blast"}
        };
        
        for (String[] category : categories) {
            HBox item = new HBox(4);
            item.setAlignment(javafx.geometry.Pos.CENTER);
            
            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: " + category[0] + "; -fx-font-size: 14px;");
            
            Label label = new Label(category[1]);
            label.setStyle("-fx-text-fill: #374151; -fx-font-size: 13px;");
            
            item.getChildren().addAll(dot, label);
            legendBar.getChildren().add(item);
            
            // Ajouter le séparateur "·"
            if (!category[1].equals("Circuit/Blast")) {
                Label separator = new Label("·");
                separator.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px;");
                legendBar.getChildren().add(separator);
            }
        }
        
        return legendBar;
    }

    /**
     * Construit la barre de navigation semaine
     */
    private HBox buildWeekNavigation() {
        HBox weekNav = new HBox();
        weekNav.setPadding(new Insets(10));
        weekNav.setAlignment(javafx.geometry.Pos.CENTER);
        weekNav.setSpacing(20);
        
        Button btnPrev = new Button("◀");
        btnPrev.setStyle("-fx-background-color: #374151; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 12;");
        btnPrev.setOnMouseClicked(e -> previousWeek());
        
        weekLabel = new Label();
        weekLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1f2937;");
        
        Button btnNext = new Button("▶");
        btnNext.setStyle("-fx-background-color: #374151; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 12;");
        btnNext.setOnMouseClicked(e -> nextWeek());
        
        weekNav.getChildren().addAll(btnPrev, weekLabel, btnNext);
        
        return weekNav;
    }

    /**
     * Construit la grille de semaine
     */
    private void buildWeekGrid() {
        // Réinitialiser la map des colonnes
        dayColumns.clear();
        
        weekGrid = new GridPane();
        weekGrid.setHgap(6);
        weekGrid.setVgap(0);
        weekGrid.setPadding(new Insets(12));
        weekGrid.setStyle("-fx-background-color: #f8fafc;");
        
        String[] dayNames = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"};
        
        // En-têtes des jours
        for (int i = 0; i < 7; i++) {
            LocalDate dayDate = currentWeekStart.plusDays(i);
            String headerText = dayNames[i] + " " + dayDate.format(DAY_DATE_FORMATTER);
            
            Label headerLabel = new Label(headerText);
            headerLabel.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-alignment: center; -fx-padding: 10px; -fx-background-radius: 8px; -fx-min-width: 140;");
            headerLabel.setMaxWidth(Double.MAX_VALUE);
            headerLabel.setMaxHeight(Double.MAX_VALUE);
            
            weekGrid.add(headerLabel, i, 0);
        }
        
        // Colonnes de jours (VBox pour empiler les cartes)
        for (int i = 0; i < 7; i++) {
            VBox dayColumn = new VBox(6);
            dayColumn.setMinWidth(140);
            dayColumn.setPadding(new Insets(8, 4, 8, 4));
            dayColumn.setStyle("-fx-background-color: white; -fx-background-radius: 8px;");
            
            // Stocker la colonne dans la map AVANT de l'ajouter à la grille
            dayColumns.put(i, dayColumn);
            
            weekGrid.add(dayColumn, i, 1);
        }
        
        // Mettre à jour le label de semaine et charger les sessions
        updateWeekLabel();
        if (currentSalle != null) {
            loadSessions();
        }
        
        planningScrollPane.setContent(weekGrid);
    }

    /**
     * Charge les sessions depuis le service
     */
    private void loadSessions() {
        try {
            allSessions = sessionService.findByGymnasiumId(currentSalle.getId());
            populateWeekGrid();
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des sessions: " + e.getMessage());
            e.printStackTrace();
            
            // En cas d'erreur, utiliser une liste vide
            allSessions = new ArrayList<>();
            populateWeekGrid();
        }
    }
    
    
    /**
     * Remplit la grille avec les sessions de la semaine
     */
    private void populateWeekGrid() {
        if (allSessions == null) return;
        
        // Organiser les sessions par jour
        Map<Integer, List<Session>> sessionsByDay = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            sessionsByDay.put(i, new ArrayList<>());
        }
        
        LocalDate weekEnd = currentWeekStart.plusDays(6);
        
        for (Session session : allSessions) {
            if (session.getStartAt() != null) {
                LocalDate sessionDate = session.getStartAt().toLocalDate();
                if (!sessionDate.isBefore(currentWeekStart) && !sessionDate.isAfter(weekEnd)) {
                    int dayIndex = session.getStartAt().getDayOfWeek().getValue() - 1; // Mon=0, Sun=6
                    sessionsByDay.get(dayIndex).add(session);
                }
            }
        }
        
        // Trier les sessions de chaque jour par heure
        for (List<Session> daySessions : sessionsByDay.values()) {
            daySessions.sort((s1, s2) -> s1.getStartAt().compareTo(s2.getStartAt()));
        }
        
        // Ajouter les cartes à chaque colonne
        for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
            VBox dayColumn = dayColumns.get(dayIndex);
            
            // Vérification null pour éviter les erreurs
            if (dayColumn == null) {
                System.err.println("Erreur: colonne de jour " + dayIndex + " non trouvée");
                continue;
            }
            
            dayColumn.getChildren().clear();
            
            List<Session> daySessions = sessionsByDay.get(dayIndex);
            
            if (daySessions.isEmpty()) {
                Label emptyLabel = new Label("—");
                emptyLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 16px; -fx-alignment: center;");
                dayColumn.getChildren().add(emptyLabel);
            } else {
                for (Session session : daySessions) {
                    VBox sessionCard = createSessionCard(session);
                    dayColumn.getChildren().add(sessionCard);
                }
            }
        }
    }

    /**
     * Crée une carte de session
     */
    private VBox createSessionCard(Session session) {
        VBox card = new VBox(2);
        String color = getCategoryColor(session.getTitle());
        card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 10; -fx-padding: 10 12 10 12;");
        card.setCursor(Cursor.HAND);
        
        // Titre
        Label titleLabel = new Label(session.getTitle());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-wrap-text: true;");
        
        // Heure
        String timeRange = session.getStartAt() != null
            ? session.getStartAt().toLocalTime().format(TIME_FORMATTER) + " – " + 
              (session.getEndAt() != null ? session.getEndAt().toLocalTime().format(TIME_FORMATTER) : "?")
            : "Heure non définie";
        Label timeLabel = new Label(timeRange);
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-opacity: 0.85;");
        
        // Coach
        String coachName = "Coach #" + session.getCoachUserId();
        try {
            if (session.getCoachUserId() > 0) {
                User coach = userService.getUserById(session.getCoachUserId());
                if (coach != null && coach.getFullName() != null && !coach.getFullName().trim().isEmpty()) {
                    coachName = coach.getFullName();
                }
            }
        } catch (Exception e) {
            // Garder le coach par défaut
        }
        Label coachLabel = new Label(coachName);
        coachLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px; -fx-opacity: 0.7;");
        
        card.getChildren().addAll(titleLabel, timeLabel, coachLabel);
        
        // Clic pour afficher les détails
        card.setOnMouseClicked(e -> showSessionDetails(session));
        
        return card;
    }

    /**
     * Retourne la couleur selon la catégorie de la session
     */
    private String getCategoryColor(String title) {
        if (title == null) return CATEGORY_COLORS.get("default");
        
        String lowerTitle = title.toLowerCase();
        for (Map.Entry<String, String> entry : CATEGORY_COLORS.entrySet()) {
            if (lowerTitle.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return CATEGORY_COLORS.get("default");
    }

    /**
     * Affiche les détails d'une session
     */
    private void showSessionDetails(Session session) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la session");
        alert.setHeaderText(session.getTitle());
        
        String coachName = "Coach #" + session.getCoachUserId();
        try {
            if (session.getCoachUserId() > 0) {
                User coach = userService.getUserById(session.getCoachUserId());
                if (coach != null && coach.getFullName() != null && !coach.getFullName().trim().isEmpty()) {
                    coachName = coach.getFullName();
                }
            }
        } catch (Exception e) {
            // Garder le coach par défaut
        }
        
        String content = String.format(
            "Date : %s\n" +
            "Heure : %s – %s\n" +
            "Coach : %s\n" +
            "Capacité : %d personnes\n" +
            "Prix : %.2f €\n" +
            "Salle : %s\n\n" +
            "Description : %s",
            session.getStartAt() != null ? session.getStartAt().toLocalDate().format(WEEK_DATE_FORMATTER) : "Date non définie",
            session.getStartAt() != null ? session.getStartAt().toLocalTime().format(TIME_FORMATTER) : "?",
            session.getEndAt() != null ? session.getEndAt().toLocalTime().format(TIME_FORMATTER) : "?",
            coachName,
            session.getCapacity(),
            session.getPrice(),
            session.getGymnasiumName(),
            session.getDescription() != null ? session.getDescription() : "Aucune description"
        );
        
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Met à jour le label de la semaine
     */
    private void updateWeekLabel() {
        LocalDate weekEnd = currentWeekStart.plusDays(6);
        String weekText;
        
        if (currentWeekStart.getYear() == weekEnd.getYear()) {
            weekText = String.format("%s – %s %s", 
                currentWeekStart.format(WEEK_DATE_FORMATTER),
                weekEnd.format(WEEK_DATE_FORMATTER),
                weekEnd.format(YEAR_FORMATTER));
        } else {
            weekText = String.format("%s %s – %s %s", 
                currentWeekStart.format(WEEK_DATE_FORMATTER),
                currentWeekStart.format(YEAR_FORMATTER),
                weekEnd.format(WEEK_DATE_FORMATTER),
                weekEnd.format(YEAR_FORMATTER));
        }
        
        weekLabel.setText(weekText);
    }

    /**
     * Définit la salle courante
     */
    public void setSalle(Salle salle) {
        this.currentSalle = salle;
        if (salle != null) {
            salleNameLabel.setText("Planning - " + salle.getName());
            loadSessions();
        }
    }

    /**
     * Navigation semaine précédente
     */
    private void previousWeek() {
        currentWeekStart = currentWeekStart.minusWeeks(1);
        updateWeekLabel();
        populateWeekGrid();
    }

    /**
     * Navigation semaine suivante
     */
    private void nextWeek() {
        currentWeekStart = currentWeekStart.plusWeeks(1);
        updateWeekLabel();
        populateWeekGrid();
    }

    /**
     * Gère le clic sur le bouton retour
     */
    private void handleBack() {
        try {
            Stage stage = (Stage) rootContainer.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            System.err.println("Erreur lors de la fermeture: " + e.getMessage());
        }
    }
}
