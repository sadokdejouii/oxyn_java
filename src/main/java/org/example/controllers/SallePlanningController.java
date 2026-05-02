package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SallePlanningController implements Initializable {

    @FXML private Button btnBack;
    @FXML private Label salleNameLabel;
    @FXML private GridPane planningGrid;
    @FXML private Label emptyLabel;

    private MainLayoutController mainLayoutController;
    private org.example.entities.Salle salle;
    private final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configuration initiale de la grille
        setupGridColumns();
    }

    /**
     * Configure les 7 colonnes de la grille (Lundi à Dimanche)
     */
    private void setupGridColumns() {
        // En-têtes des jours
        String[] days = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"};
        
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7.0);
            col.setHgrow(Priority.ALWAYS);
            planningGrid.getColumnConstraints().add(col);
            
            // En-tête du jour
            Label dayHeader = new Label(days[i]);
            dayHeader.getStyleClass().add("planning-day-header");
            planningGrid.add(dayHeader, i, 0);
        }
    }

    /**
     * Définit la salle et charge le planning
     */
    public void setSalle(org.example.entities.Salle salle, MainLayoutController mlc) {
        this.salle = salle;
        this.mainLayoutController = mlc;
        
        // Nom de la salle
        salleNameLabel.setText(salle.getName());
        
        // Charger les sessions
        loadPlanning();
    }

    /**
     * Charge et affiche les sessions de la semaine
     */
    private void loadPlanning() {
        try {
            System.out.println("📅 Planning pour salle: " + salle.getName() + " (ID: " + salle.getId() + ")");
            
            // Récupérer les sessions via le service
            org.example.services.SessionService sessionService = new org.example.services.SessionService();
            List<org.example.entities.Session> allSessions = sessionService.findByGymnasiumId(salle.getId());
            
            // Si aucune session, créer des données de test pour cette salle
            if (allSessions.isEmpty()) {
                System.out.println("🔧 Aucune session trouvée pour " + salle.getName() + " (ID: " + salle.getId() + ") - création des données de test...");
                createTestData();
                allSessions = sessionService.findByGymnasiumId(salle.getId());
            }
            
            // Filtrer pour la semaine en cours
            System.out.println("🔍 DEBUG: Total sessions trouvées: " + allSessions.size());
            if (!allSessions.isEmpty()) {
                System.out.println("📅 DEBUG: Première session: " + allSessions.get(0).getStartAt());
                System.out.println("📅 DEBUG: Dernière session: " + allSessions.get(allSessions.size() - 1).getStartAt());
            }
            
            List<org.example.entities.Session> weekSessions = filterCurrentWeekSessions(allSessions);
            System.out.println("🔍 DEBUG: Sessions cette semaine: " + weekSessions.size());
            
            // Organiser par jour
            Map<DayOfWeek, List<org.example.entities.Session>> sessionsByDay = weekSessions.stream()
                .collect(Collectors.groupingBy(session -> session.getStartAt().getDayOfWeek()));
            
            System.out.println("🔍 DEBUG: Sessions par jour:");
            sessionsByDay.forEach((day, sessions) -> {
                System.out.println("   " + day + ": " + sessions.size() + " sessions");
            });
            
            // Vider la grille (sauf en-têtes)
            clearGrid();
            
            if (weekSessions.isEmpty()) {
                System.out.println("❌ DEBUG: Aucune session cette semaine - affichage message vide");
                emptyLabel.setText("Aucune session prévue cette semaine");
                emptyLabel.setVisible(true);
                emptyLabel.setManaged(true);
                planningGrid.setVisible(false);
                planningGrid.setManaged(false);
            } else {
                System.out.println("✅ DEBUG: " + weekSessions.size() + " sessions à afficher");
                emptyLabel.setVisible(false);
                emptyLabel.setManaged(false);
                planningGrid.setVisible(true);
                planningGrid.setManaged(true);
                
                // Afficher les sessions
                displaySessions(sessionsByDay);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            // Afficher message d'erreur
            emptyLabel.setText("Erreur lors du chargement du planning");
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            planningGrid.setVisible(false);
            planningGrid.setManaged(false);
        }
    }
    
    /**
     * Crée des données de test pour California Gymm
     */
    private void createTestData() {
        try {
            System.out.println("🔧 Création des données de test pour California Gymm...");
            
            // Créer quelques sessions de test pour cette semaine (27 avril - 3 mai 2026)
            LocalDateTime now = LocalDateTime.now();
            org.example.entities.Session[] testSessions = {
                createTestSession("Cardio HIIT Intense", "Session de cardio haute intensité", 
                    now.withDayOfMonth(27).withHour(7).withMinute(0), now.withDayOfMonth(27).withHour(8).withMinute(0), 25, 15.0, 1),
                createTestSession("Yoga Relaxation", "Session de yoga pour tous niveaux", 
                    now.withDayOfMonth(27).withHour(9).withMinute(0), now.withDayOfMonth(27).withHour(10).withMinute(0), 20, 18.0, 2),
                createTestSession("Musculation Force", "Entraînement de musculation", 
                    now.withDayOfMonth(27).withHour(10).withMinute(30), now.withDayOfMonth(27).withHour(12).withMinute(0), 15, 20.0, 3),
                createTestSession("Zumba Party", "Session de danse latine", 
                    now.withDayOfMonth(27).withHour(18).withMinute(0), now.withDayOfMonth(27).withHour(19).withMinute(0), 30, 12.0, 4),
                createTestSession("Boxing Technique", "Apprentissage des techniques de boxe", 
                    now.withDayOfMonth(28).withHour(7).withMinute(0), now.withDayOfMonth(28).withHour(8).withMinute(30), 12, 22.0, 5),
                createTestSession("Circuit Training", "Entraînement circuit", 
                    now.withDayOfMonth(28).withHour(9).withMinute(0), now.withDayOfMonth(28).withHour(10).withMinute(30), 18, 25.0, 6),
                createTestSession("Pilates Core", "Renforcement des muscles profonds", 
                    now.withDayOfMonth(28).withHour(11).withMinute(0), now.withDayOfMonth(28).withHour(12).withMinute(0), 16, 20.0, 7),
                createTestSession("Body Combat", "Entraînement complet corps à corps", 
                    now.withDayOfMonth(28).withHour(17).withMinute(0), now.withDayOfMonth(28).withHour(18).withMinute(30), 10, 25.0, 8),
                createTestSession("Fitness Senior", "Fitness adapté pour seniors", 
                    now.withDayOfMonth(29).withHour(8).withMinute(0), now.withDayOfMonth(29).withHour(9).withMinute(0), 12, 15.0, 9),
                createTestSession("Core Training", "Renforcement gainage et stabilité", 
                    now.withDayOfMonth(29).withHour(9).withMinute(30), now.withDayOfMonth(29).withHour(10).withMinute(30), 15, 16.0, 10),
                createTestSession("Step Aerobic", "Choregraphies step sur plateformes", 
                    now.withDayOfMonth(29).withHour(11).withMinute(0), now.withDayOfMonth(29).withHour(12).withMinute(0), 22, 12.0, 11),
                createTestSession("Danse Moderne", "Initiation à la danse contemporaine", 
                    now.withDayOfMonth(29).withHour(19).withMinute(0), now.withDayOfMonth(29).withHour(20).withMinute(0), 25, 15.0, 12),
                createTestSession("CrossFit WOD", "Workout of the day CrossFit", 
                    now.withDayOfMonth(30).withHour(6).withMinute(0), now.withDayOfMonth(30).withHour(7).withMinute(30), 8, 28.0, 13),
                createTestSession("Yoga Power", "Yoga dynamique avec enchaînements fluides", 
                    now.withDayOfMonth(30).withHour(8).withMinute(0), now.withDayOfMonth(30).withHour(9).withMinute(0), 18, 18.0, 14),
                createTestSession("Body Pump", "Entraînement avec barres et poids légers", 
                    now.withDayOfMonth(30).withHour(9).withMinute(30), now.withDayOfMonth(30).withHour(10).withMinute(30), 18, 18.0, 15),
                createTestSession("Spinning RPM", "Cours de spinning avec musique dynamique", 
                    now.withDayOfMonth(30).withHour(17).withMinute(0), now.withDayOfMonth(30).withHour(18).withMinute(0), 25, 14.0, 16),
                createTestSession("Running Club", "Course à pied en groupe tous niveaux", 
                    now.withDayOfMonth(1).withHour(6).withMinute(30), now.withDayOfMonth(1).withHour(7).withMinute(30), 20, 10.0, 17),
                createTestSession("HIIT Supreme", "HIIT très haute intensité", 
                    now.withDayOfMonth(1).withHour(8).withMinute(0), now.withDayOfMonth(1).withHour(9).withMinute(0), 5, 28.0, 18),
                createTestSession("Musculation Power", "Entraînement force avec surcharge progressive", 
                    now.withDayOfMonth(1).withHour(9).withMinute(30), now.withDayOfMonth(1).withHour(11).withMinute(0), 12, 22.0, 19),
                createTestSession("Salsa Latino", "Salsa latine passionnée", 
                    now.withDayOfMonth(1).withHour(18).withMinute(30), now.withDayOfMonth(1).withHour(20).withMinute(0), 16, 18.0, 20),
                createTestSession("Bachata Romantique", "Bachata romantique pour couples", 
                    now.withDayOfMonth(2).withHour(10).withMinute(0), now.withDayOfMonth(2).withHour(11).withMinute(30), 14, 18.0, 21),
                createTestSession("Circuit Elite", "Circuit avancé pour athlètes", 
                    now.withDayOfMonth(2).withHour(11).withMinute(0), now.withDayOfMonth(2).withHour(12).withMinute(30), 6, 35.0, 22),
                createTestSession("Stretching Doux", "Étirements doux et relaxation", 
                    now.withDayOfMonth(2).withHour(14).withMinute(0), now.withDayOfMonth(2).withHour(14).withMinute(45), 20, 10.0, 23),
                createTestSession("Boxe Sparring", "Combat d'entraînement boxe", 
                    now.withDayOfMonth(2).withHour(15).withMinute(0), now.withDayOfMonth(2).withHour(16).withMinute(0), 3, 25.0, 24),
                createTestSession("Tai Chi Chinois", "Tai Chi pour équilibre et souplesse", 
                    now.withDayOfMonth(3).withHour(15).withMinute(0), now.withDayOfMonth(3).withHour(16).withMinute(0), 18, 12.0, 25),
                createTestSession("Pilates Rééducation", "Pilates pour rééducation posturale", 
                    now.withDayOfMonth(3).withHour(16).withMinute(30), now.withDayOfMonth(3).withHour(17).withMinute(30), 10, 25.0, 26),
                createTestSession("Méditation Guidée", "Méditation de pleine conscience", 
                    now.withDayOfMonth(3).withHour(17).withMinute(0), now.withDayOfMonth(3).withHour(17).withMinute(30), 25, 10.0, 27),
                createTestSession("Yoga Restauratif", "Yoga pour récupération et souplesse", 
                    now.withDayOfMonth(3).withHour(18).withMinute(0), now.withDayOfMonth(3).withHour(19).withMinute(0), 8, 18.0, 28)
            };
            
            org.example.services.SessionService sessionService = new org.example.services.SessionService();
            for (org.example.entities.Session session : testSessions) {
                sessionService.ajouter(session);
                System.out.println("✅ Session créée: " + session.getTitle() + " (" + session.getStartAt() + ")");
            }
            
            System.out.println("🎯 " + testSessions.length + " sessions de test créées !");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création des données de test: " + e.getMessage());
        }
    }
    
    /**
     * Crée une session de test
     */
    private org.example.entities.Session createTestSession(String title, String description, 
            LocalDateTime startAt, LocalDateTime endAt, int capacity, double price, int coachId) {
        org.example.entities.Session session = new org.example.entities.Session();
        session.setTitle(title);
        session.setDescription(description);
        session.setStartAt(startAt);
        session.setEndAt(endAt);
        session.setCapacity(capacity);
        session.setPrice(price);
        session.setActive(true);
        session.setGymnasiumId(salle.getId());
        session.setCoachUserId(coachId);
        return session;
    }

    /**
     * Filtre les sessions pour la semaine en cours (du lundi au dimanche)
     */
    private List<org.example.entities.Session> filterCurrentWeekSessions(List<org.example.entities.Session> allSessions) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .toLocalDate().atStartOfDay();
        LocalDateTime weekEnd = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            .toLocalDate().atTime(23, 59, 59);
        
        return allSessions.stream()
            .filter(session -> !session.getStartAt().isBefore(weekStart) && !session.getStartAt().isAfter(weekEnd))
            .collect(Collectors.toList());
    }

    /**
     * Vide la grille (sauf les en-têtes)
     */
    private void clearGrid() {
        planningGrid.getChildren().removeIf(node -> 
            GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) > 0);
    }

    /**
     * Affiche les sessions dans la grille
     */
    private void displaySessions(Map<DayOfWeek, List<org.example.entities.Session>> sessionsByDay) {
        // Pour chaque jour de la semaine
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SUNDAY) continue; // Sauter dimanche (index 7)
            
            int dayIndex = day.getValue() - 1; // Lundi=0, Mardi=1, etc.
            List<org.example.entities.Session> daySessions = sessionsByDay.get(day);
            
            if (daySessions != null && !daySessions.isEmpty()) {
                // Trier par heure de début
                daySessions.sort((s1, s2) -> s1.getStartAt().compareTo(s2.getStartAt()));
                
                // Afficher chaque session
                for (int i = 0; i < daySessions.size(); i++) {
                    org.example.entities.Session session = daySessions.get(i);
                    VBox sessionCard = createSessionCard(session);
                    planningGrid.add(sessionCard, dayIndex, i + 1);
                }
            }
        }
    }

    /**
     * Crée une carte de session
     */
    private VBox createSessionCard(org.example.entities.Session session) {
        VBox card = new VBox(4);
        card.getStyleClass().add("planning-session-card");
        
        // Déterminer le style de couleur selon le titre
        card.getStyleClass().add(getStyleClass(session.getTitle()));
        
        // Titre
        Label titleLabel = new Label(session.getTitle());
        titleLabel.getStyleClass().add("session-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        
        // Horaire
        String timeText = TIME_FORMATTER.format(session.getStartAt()) + " – " + 
                         TIME_FORMATTER.format(session.getEndAt());
        Label timeLabel = new Label(timeText);
        timeLabel.getStyleClass().add("session-time");
        
        // Coach
        String coachText = "Coach: " + (session.getCoachUserId() > 0 ? "ID " + session.getCoachUserId() : "Non spécifié");
        Label coachLabel = new Label(coachText);
        coachLabel.getStyleClass().add("session-coach");
        
        // Places
        String placesText = session.getCapacity() + " places";
        Label placesLabel = new Label(placesText);
        placesLabel.getStyleClass().add("session-places");
        
        card.getChildren().addAll(titleLabel, timeLabel, coachLabel, placesLabel);
        
        return card;
    }

    /**
     * Détermine le style de couleur selon les mots-clés dans le titre
     */
    private String getStyleClass(String title) {
        if (title == null) return "style-fitness";
        
        String lowerTitle = title.toLowerCase();
        
        if (lowerTitle.contains("yoga") || lowerTitle.contains("pilates") || lowerTitle.contains("stretch")) {
            return "style-yoga";
        } else if (lowerTitle.contains("box") || lowerTitle.contains("combat") || lowerTitle.contains("karate") || 
                   lowerTitle.contains("muay") || lowerTitle.contains("self-defense")) {
            return "style-combat";
        } else if (lowerTitle.contains("cardio") || lowerTitle.contains("hiit") || lowerTitle.contains("aérobic") || 
                   lowerTitle.contains("running") || lowerTitle.contains("step")) {
            return "style-cardio";
        } else if (lowerTitle.contains("musculation") || lowerTitle.contains("cross") || lowerTitle.contains("pump") || 
                   lowerTitle.contains("circuit") || lowerTitle.contains("force") || lowerTitle.contains("weight")) {
            return "style-muscu";
        } else if (lowerTitle.contains("zumba") || lowerTitle.contains("danse") || lowerTitle.contains("salsa") || 
                   lowerTitle.contains("bachata")) {
            return "style-danse";
        } else if (lowerTitle.contains("fitness") || lowerTitle.contains("core") || lowerTitle.contains("bike") || 
                   lowerTitle.contains("body") || lowerTitle.contains("tonic")) {
            return "style-fitness";
        } else {
            return "style-fitness"; // défaut
        }
    }

    @FXML
    private void handleBack() {
        if (mainLayoutController != null) {
            mainLayoutController.handleSalle();
        }
    }
}
