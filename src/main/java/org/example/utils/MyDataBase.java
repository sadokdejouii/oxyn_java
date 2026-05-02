package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connexion JDBC MySQL (base {@code oxyn}) — paramètres alignés sur {@code main} (fuseau, UTF-8).
 * ✅ Pattern : Toujours retourner une nouvelle connexion fraîche
 */
public final class MyDataBase {

    private static final String HOST = "localhost";
    private static final String PORT = "3307";
    private static final String DB_NAME = "oxyn";

    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME
            + "?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8"
            + "&zeroDateTimeBehavior=CONVERT_TO_NULL";

    /**
     * ✅ Toujours retourner une nouvelle connexion fraîche
     * @return Connection JDBC MySQL (null si erreur)
     */
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("✅ Nouvelle connexion BD créée pour « " + DB_NAME + " »");
            return conn;
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("❌ Connexion BD échouée : " + e.getMessage());
            return null; // ← ne lance plus d'exception
        }
    }

    /**
     * ✅ Alternative qui lance une exception (pour les cas où on veut gérer l'erreur)
     * @return Connection JDBC MySQL
     * @throws SQLException si la connexion échoue
     */
    public static Connection getConnectionWithException() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("✅ Nouvelle connexion BD créée pour « " + DB_NAME + " »");
            return conn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL non trouvé", e);
        } catch (SQLException e) {
            System.err.println("❌ Connexion BD échouée : " + e.getMessage());
            throw e;
        }
    }

    /**
     * ✅ Pattern try-with-resources recommandé
     * @deprecated Utilisez try-with-resources avec getConnection() à la place
     */
    @Deprecated
    public static MyDataBase getInstance() {
        System.err.println("⚠️ getInstance() est déprécié. Utilisez getConnection() avec try-with-resources.");
        return new MyDataBase();
    }

    /**
     * ✅ Fermer une connexion proprement
     * @param conn La connexion à fermer
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                    System.out.println("🔒 Connexion BD fermée");
                }
            } catch (SQLException e) {
                System.err.println("❌ Erreur fermeture connexion: " + e.getMessage());
            }
        }
    }

    /**
     * ✅ Vérifier si une connexion est valide
     * @param conn La connexion à vérifier
     * @return true si la connexion est valide
     */
    public static boolean isValid(Connection conn) {
        if (conn == null) {
            return false;
        }
        try {
            return !conn.isClosed() && conn.isValid(1);
        } catch (SQLException e) {
            return false;
        }
    }

    // Getters pour compatibilité
    public static String getURL() {
        return URL;
    }

    public static String getUSERNAME() {
        return USERNAME;
    }

    public static String getPASSWORD() {
        return PASSWORD;
    }

    // Constructeur privé pour éviter l'instanciation
    private MyDataBase() {
        // Constructeur privé - utiliser les méthodes statiques
    }
}
