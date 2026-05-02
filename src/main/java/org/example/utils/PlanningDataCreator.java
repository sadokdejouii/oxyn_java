package org.example.utils;

import org.example.entities.Session;
import org.example.services.SessionService;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Utilitaire pour créer des données de planning de test
 */
public class PlanningDataCreator {
    
    public static void main(String[] args) {
        try {
            System.out.println("🎯 OXYN - Création des données de planning");
            System.out.println("══════════════════════════════════════════════════════════════");
            
            createPlanningData();
            
            System.out.println("✅ Données de planning créées avec succès !");
            System.out.println("🎯 Lancez l'application et testez le planning !");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création des données: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void createPlanningData() throws Exception {
        // Connexion directe à la base
        Connection conn = DriverManager.getConnection(
            MyDataBase.getURL(), 
            MyDataBase.getUSERNAME(), 
            MyDataBase.getPASSWORD()
        );
        
        // Supprimer les sessions existantes pour California Gymm (ID: 1)
        String deleteSql = "DELETE FROM training_sessions WHERE gymnasium_id = 1";
        PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
        deleteStmt.executeUpdate();
        deleteStmt.close();
        
        System.out.println("🗑️ Anciennes sessions supprimées");
        
        // Insérer les nouvelles sessions
        String insertSql = "INSERT INTO training_sessions (title, description, start_at, end_at, capacity, price, is_active, created_at, gymnasium_id, coach_user_id) VALUES (?, ?, ?, ?, ?, ?, 1, NOW(), 1, ?)";
        PreparedStatement insertStmt = conn.prepareStatement(insertSql);
        
        // Sessions pour California Gymm cette semaine
        Object[][] sessions = {
            // Lundi 28 Avril
            {"Cardio HIIT Intense", "Session de cardio haute intensité avec intervalles", "2026-04-28 07:00:00", "2026-04-28 08:00:00", 25, 15.0, 1},
            {"Yoga Relaxation", "Session de yoga pour tous niveaux avec postures et respiration", "2026-04-28 09:00:00", "2026-04-28 10:00:00", 20, 18.0, 2},
            {"Musculation Force", "Entraînement de musculation avec poids libres et machines", "2026-04-28 10:30:00", "2026-04-28 12:00:00", 15, 20.0, 3},
            {"Zumba Party", "Session de danse latine avec musique entraînante", "2026-04-28 18:00:00", "2026-04-28 19:00:00", 30, 12.0, 4},
            
            // Mardi 29 Avril
            {"Boxing Technique", "Apprentissage des techniques de boxe et combat", "2026-04-29 07:00:00", "2026-04-29 08:30:00", 12, 22.0, 5},
            {"Circuit Training", "Entraînement circuit avec plusieurs stations", "2026-04-29 09:00:00", "2026-04-29 10:30:00", 18, 25.0, 6},
            {"Pilates Core", "Renforcement des muscles profonds avec Pilates", "2026-04-29 11:00:00", "2026-04-29 12:00:00", 16, 20.0, 7},
            {"Body Combat", "Entraînement complet corps à corps", "2026-04-29 17:00:00", "2026-04-29 18:30:00", 10, 25.0, 8},
            
            // Mercredi 30 Avril
            {"Fitness Senior", "Fitness adapté pour seniors", "2026-04-30 08:00:00", "2026-04-30 09:00:00", 12, 15.0, 9},
            {"Core Training", "Renforcement gainage et stabilité", "2026-04-30 09:30:00", "2026-04-30 10:30:00", 15, 16.0, 10},
            {"Step Aerobic", "Choregraphies step sur plateformes", "2026-04-30 11:00:00", "2026-04-30 12:00:00", 22, 12.0, 11},
            {"Danse Moderne", "Initiation à la danse contemporaine", "2026-04-30 19:00:00", "2026-04-30 20:00:00", 25, 15.0, 12},
            
            // Jeudi 1 Mai
            {"CrossFit WOD", "Workout of the day CrossFit", "2026-05-01 06:00:00", "2026-05-01 07:30:00", 8, 28.0, 13},
            {"Yoga Power", "Yoga dynamique avec enchaînements fluides", "2026-05-01 08:00:00", "2026-05-01 09:00:00", 18, 18.0, 14},
            {"Body Pump", "Entraînement avec barres et poids légers", "2026-05-01 09:30:00", "2026-05-01 10:30:00", 18, 18.0, 15},
            {"Spinning RPM", "Cours de spinning avec musique dynamique", "2026-05-01 17:00:00", "2026-05-01 18:00:00", 25, 14.0, 16},
            
            // Vendredi 2 Mai
            {"Running Club", "Course à pied en groupe tous niveaux", "2026-05-02 06:30:00", "2026-05-02 07:30:00", 20, 10.0, 17},
            {"HIIT Supreme", "HIIT très haute intensité", "2026-05-02 08:00:00", "2026-05-02 09:00:00", 5, 28.0, 18},
            {"Musculation Power", "Entraînement force avec surcharge progressive", "2026-05-02 09:30:00", "2026-05-02 11:00:00", 12, 22.0, 19},
            {"Salsa Latino", "Salsa latine passionnée", "2026-05-02 18:30:00", "2026-05-02 20:00:00", 16, 18.0, 20},
            
            // Samedi 3 Mai
            {"Bachata Romantique", "Bachata romantique pour couples", "2026-05-03 10:00:00", "2026-05-03 11:30:00", 14, 18.0, 21},
            {"Circuit Elite", "Circuit avancé pour athlètes", "2026-05-03 11:00:00", "2026-05-03 12:30:00", 6, 35.0, 22},
            {"Stretching Doux", "Étirements doux et relaxation", "2026-05-03 14:00:00", "2026-05-03 14:45:00", 20, 10.0, 23},
            {"Boxe Sparring", "Combat d'entraînement boxe", "2026-05-03 15:00:00", "2026-05-03 16:00:00", 3, 25.0, 24}
        };
        
        int inserted = 0;
        for (Object[] session : sessions) {
            insertStmt.setString(1, (String) session[0]);
            insertStmt.setString(2, (String) session[1]);
            insertStmt.setTimestamp(3, Timestamp.valueOf((String) session[2]));
            insertStmt.setTimestamp(4, Timestamp.valueOf((String) session[3]));
            insertStmt.setInt(5, (Integer) session[4]);
            insertStmt.setDouble(6, (Double) session[5]);
            insertStmt.setInt(7, (Integer) session[6]);
            
            insertStmt.executeUpdate();
            inserted++;
            
            String type = getSessionType((String) session[0]);
            System.out.println("📅 " + session[2] + " - " + session[0] + " (" + type + ")");
        }
        
        insertStmt.close();
        conn.close();
        
        System.out.println("══════════════════════════════════════════════════════════════");
        System.out.println("📊 Total: " + inserted + " sessions créées");
        System.out.println("🏋️ California Gymm planning prêt !");
    }
    
    private static String getSessionType(String title) {
        String lowerTitle = title.toLowerCase();
        
        if (lowerTitle.contains("yoga") || lowerTitle.contains("pilates") || lowerTitle.contains("stretch")) {
            return "Yoga";
        } else if (lowerTitle.contains("box") || lowerTitle.contains("combat") || lowerTitle.contains("karate") || 
                   lowerTitle.contains("muay") || lowerTitle.contains("sparring")) {
            return "Combat";
        } else if (lowerTitle.contains("cardio") || lowerTitle.contains("hiit") || lowerTitle.contains("aérobic") || 
                   lowerTitle.contains("running") || lowerTitle.contains("step") || lowerTitle.contains("spinning")) {
            return "Cardio";
        } else if (lowerTitle.contains("musculation") || lowerTitle.contains("cross") || lowerTitle.contains("pump") || 
                   lowerTitle.contains("circuit") || lowerTitle.contains("force") || lowerTitle.contains("weight")) {
            return "Musculation";
        } else if (lowerTitle.contains("zumba") || lowerTitle.contains("danse") || lowerTitle.contains("salsa") || 
                   lowerTitle.contains("bachata")) {
            return "Danse";
        } else if (lowerTitle.contains("fitness") || lowerTitle.contains("core") || lowerTitle.contains("bike") || 
                   lowerTitle.contains("body") || lowerTitle.contains("tonic")) {
            return "Fitness";
        } else {
            return "Autre";
        }
    }
}
