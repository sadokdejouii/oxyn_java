package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connexion JDBC MySQL (base {@code oxyn}) — paramètres alignés sur {@code main} (fuseau, UTF-8).
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

    private static MyDataBase instance;

    private Connection connection;

    private MyDataBase() {
        connect();
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connexion à la base « " + DB_NAME + " » établie.");
        } catch (SQLException e) {
            System.err.println("Échec de connexion à « " + DB_NAME + " » : " + e.getMessage());
            connection = null;
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(1)) {
                connect();
            }
        } catch (SQLException e) {
            connect();
        }
        return connection;
    }

    /** Reset after writes so next read gets fresh data */
    public void resetConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
        connect();
    }

    public static String getURL() {
        return URL;
    }

    public static String getUSERNAME() {
        return USERNAME;
    }

    public static String getPASSWORD() {
        return PASSWORD;
    }
}
