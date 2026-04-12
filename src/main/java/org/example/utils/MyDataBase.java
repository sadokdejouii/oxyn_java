package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connexion JDBC MySQL (base {@code oxyn}) — paramètres alignés sur {@code main} (fuseau, UTF-8).
 */
public final class MyDataBase {

    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DB_NAME = "oxyn";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME
            + "?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";

    private static MyDataBase instance;

    private Connection connection;

    private MyDataBase() {
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
        return connection;
    }
}
