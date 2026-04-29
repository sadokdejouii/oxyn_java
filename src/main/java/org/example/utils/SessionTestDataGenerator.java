package org.example.utils;

import org.example.entities.Session;
import org.example.entities.Salle;
import org.example.services.SalleService;
import org.example.services.SessionService;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utilitaire pour générer des données de test de sessions pour chaque salle
 */
public class SessionTestDataGenerator {
    
    private static final Random random = new Random();
    
    // Types de séances avec leurs catégories de couleurs
    private static final String[][] SESSION_TYPES = {
        {"Yoga Matinal", "yoga"},
        {"Yoga Relaxant", "yoga"},
        {"Pilates Core", "pilates"},
        {"Boxe Technique", "box"},
        {"Boxe Combat", "combat"},
        {"Muay Thai", "combat"},
        {"Cardio HIIT", "cardio"},
        {"HIIT Intense", "hiit"},
        {"Circuit Training", "circuit"},
        {"Sprint Cardio", "sprint"},
        {"Tabata", "tabata"},
        {"Core Training", "core"},
        {"Blast Cardio", "blast"},
        {"Functional Training", "functional"},
        {"Spinning", "bike"},
        {"Body Pump", "pump"},
        {"Musculation", "muscu"},
        {"Force", "force"},
        {"Cross Training", "cross"},
        {"Body Combat", "body"},
        {"Zumba", "zumba"},
        {"Danse Fitness", "danse"},
        {"Step", "default"},
        {"Aqua Fitness", "default"},
        {"Stretching", "default"}
    };
    
    // Noms de coachs fictifs
    private static final String[] COACH_NAMES = {
        "Marie Dubois", "Paul Martin", "Sophie Lemaire", "Jean Dupont",
        "Claire Bernard", "Lucas Petit", "Emma Rousseau", "Nicolas Durand",
        "Laura Leroy", "David Moreau", "Julie Robert", "Thomas Girard"
    };
    
    /**
     * Génère des sessions de test pour toutes les salles existantes
     */
    public static void generateSessionsForAllSalles() {
        try {
            SalleService salleService = new SalleService();
            SessionService sessionService = new SessionService();
            
            List<Salle> salles = salleService.getAll();
            
            for (Salle salle : salles) {
                List<Session> sessions = generateSessionsForSalle(salle);
                
                for (Session session : sessions) {
                    try {
                        sessionService.ajouter(session);
                        System.out.println("Session créée: " + session.getTitle() + " pour la salle " + salle.getName());
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la création de la session " + session.getTitle() + ": " + e.getMessage());
                    }
                }
            }
            
            System.out.println("Génération des sessions terminée !");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération des sessions: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Génère des sessions pour une salle spécifique
     */
    private static List<Session> generateSessionsForSalle(Salle salle) {
        List<Session> sessions = new ArrayList<>();
        
        // Générer entre 15 et 25 sessions par semaine
        int sessionCount = 15 + random.nextInt(11);
        
        for (int i = 0; i < sessionCount; i++) {
            Session session = createRandomSession(salle.getId(), salle.getName());
            sessions.add(session);
        }
        
        return sessions;
    }
    
    /**
     * Crée une session aléatoire pour une salle
     */
    private static Session createRandomSession(int salleId, String salleName) {
        // Choisir un type de séance
        String[] sessionType = SESSION_TYPES[random.nextInt(SESSION_TYPES.length)];
        String title = sessionType[0];
        
        // Générer une date et heure aléatoire pour la semaine courante
        LocalDateTime startAt = generateRandomDateTime();
        LocalDateTime endAt = startAt.plusMinutes(45 + random.nextInt(45)); // 45-90 minutes
        
        // Capacité et prix selon le type de séance
        int capacity = generateCapacity(title);
        double price = generatePrice(title);
        
        // Coach aléatoire
        int coachUserId = 1 + random.nextInt(12); // Coaches de 1 à 12
        
        // Description
        String description = generateDescription(title);
        
        return new Session(title, description, startAt, endAt, capacity, price, salleId, coachUserId);
    }
    
    /**
     * Génère une date/heure aléatoire pour la semaine courante
     */
    private static LocalDateTime generateRandomDateTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                                         .with(LocalTime.of(0, 0));
        
        // Jour aléatoire dans la semaine (0-6)
        int dayOffset = random.nextInt(7);
        LocalDateTime sessionDate = weekStart.plusDays(dayOffset);
        
        // Heure aléatoire entre 6h et 21h
        int hour = 6 + random.nextInt(16);
        int minute = random.nextInt(4) * 15; // Minutes par tranches de 15
        
        return sessionDate.withHour(hour).withMinute(minute);
    }
    
    /**
     * Génère une capacité selon le type de séance
     */
    private static int generateCapacity(String sessionType) {
        if (sessionType.toLowerCase().contains("yoga") || sessionType.toLowerCase().contains("pilates")) {
            return 12 + random.nextInt(7); // 12-18
        } else if (sessionType.toLowerCase().contains("cardio") || sessionType.toLowerCase().contains("hiit") || 
                   sessionType.toLowerCase().contains("sprint") || sessionType.toLowerCase().contains("tabata")) {
            return 15 + random.nextInt(11); // 15-25
        } else if (sessionType.toLowerCase().contains("muscu") || sessionType.toLowerCase().contains("force")) {
            return 10 + random.nextInt(11); // 10-20
        } else if (sessionType.toLowerCase().contains("zumba") || sessionType.toLowerCase().contains("danse")) {
            return 20 + random.nextInt(11); // 20-30
        } else {
            return 12 + random.nextInt(13); // 12-24 par défaut
        }
    }
    
    /**
     * Génère un prix selon le type de séance
     */
    private static double generatePrice(String sessionType) {
        if (sessionType.toLowerCase().contains("yoga") || sessionType.toLowerCase().contains("pilates")) {
            return 15.0 + random.nextInt(11); // 15-25€
        } else if (sessionType.toLowerCase().contains("cardio") || sessionType.toLowerCase().contains("hiit")) {
            return 12.0 + random.nextInt(9); // 12-20€
        } else if (sessionType.toLowerCase().contains("muscu") || sessionType.toLowerCase().contains("force")) {
            return 18.0 + random.nextInt(13); // 18-30€
        } else if (sessionType.toLowerCase().contains("zumba") || sessionType.toLowerCase().contains("danse")) {
            return 20.0 + random.nextInt(11); // 20-30€
        } else {
            return 15.0 + random.nextInt(11); // 15-25€ par défaut
        }
    }
    
    /**
     * Génère une description pour la séance
     */
    private static String generateDescription(String sessionType) {
        if (sessionType.toLowerCase().contains("yoga")) {
            return "Séance de yoga douce pour améliorer la flexibilité et la relaxation. Adaptée à tous les niveaux.";
        } else if (sessionType.toLowerCase().contains("pilates")) {
            return "Renforcement musculaire profond avec focus sur le gainage et la posture. Matériel fourni.";
        } else if (sessionType.toLowerCase().contains("box") || sessionType.toLowerCase().contains("combat")) {
            return "Entraînement de combat et self-défense. Gants et protège-pieds obligatoires.";
        } else if (sessionType.toLowerCase().contains("cardio") || sessionType.toLowerCase().contains("hiit")) {
            return "Entraînement cardiovasculaire intense par intervalles. Brûle les calories rapidement.";
        } else if (sessionType.toLowerCase().contains("muscu") || sessionType.toLowerCase().contains("force")) {
            return "Séance de musculation avec poids libres et machines. Programme complet du corps.";
        } else if (sessionType.toLowerCase().contains("zumba") || sessionType.toLowerCase().contains("danse")) {
            return "Séance de danse fitness sur musique entraînante. Ambiance festive et énergique.";
        } else {
            return "Séance de fitness variée pour le bien-être et la forme physique.";
        }
    }
    
    /**
     * Point d'entrée principal pour générer les données de test
     */
    public static void main(String[] args) {
        System.out.println("Début de la génération des sessions de test...");
        generateSessionsForAllSalles();
    }
}
