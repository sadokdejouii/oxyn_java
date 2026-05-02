package org.example.utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseTest {
    public static void main(String[] args) {
        System.out.println("Test de connexion à la base de données...");
        
        try {
            Connection conn = MyDataBase.getConnectionWithException();
            
            if (conn != null) {
                System.out.println("✅ Connexion réussie !");
                
                // Afficher les informations de la base
                DatabaseMetaData metaData = conn.getMetaData();
                System.out.println("Base de données: " + metaData.getDatabaseProductName());
                System.out.println("Version: " + metaData.getDatabaseProductVersion());
                
                // Vérifier si la table training_sessions existe
                ResultSet tables = conn.getMetaData().getTables(null, null, "training_sessions", null);
                if (tables.next()) {
                    System.out.println("✅ Table 'training_sessions' trouvée");
                    
                    // Afficher la structure de la table
                    ResultSet columns = conn.getMetaData().getColumns(null, null, "training_sessions", null);
                    System.out.println("\nStructure de la table 'training_sessions':");
                    while (columns.next()) {
                        String columnName = columns.getString("COLUMN_NAME");
                        String columnType = columns.getString("TYPE_NAME");
                        int columnSize = columns.getInt("COLUMN_SIZE");
                        System.out.println("  - " + columnName + " (" + columnType + " " + columnSize + ")");
                    }
                    
                    // Compter les sessions existantes
                    ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) as total FROM training_sessions");
                    if (rs.next()) {
                        int total = rs.getInt("total");
                        System.out.println("\n📊 Total des sessions dans la base: " + total);
                    }
                    
                    // Afficher quelques sessions si elles existent
                    rs = conn.createStatement().executeQuery("SELECT id, title, gymnasium_id, start_at FROM training_sessions LIMIT 5");
                    System.out.println("\n📋 Exemples de sessions:");
                    while (rs.next()) {
                        System.out.println("  - ID: " + rs.getInt("id") + 
                                         ", Titre: " + rs.getString("title") + 
                                         ", Salle: " + rs.getInt("gymnasium_id") + 
                                         ", Début: " + rs.getTimestamp("start_at"));
                    }
                    
                } else {
                    System.out.println("❌ Table 'training_sessions' NON trouvée");
                }
                
                conn.close();
            } else {
                System.out.println("❌ Connexion null");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
