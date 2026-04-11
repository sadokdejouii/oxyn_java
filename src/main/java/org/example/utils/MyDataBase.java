package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connexion JDBC MySQL (base {@code oxyn}).
 */
public final class MyDataBase {

    private static MyDataBase instance;

    private final String username = "root";
    private final String url = "jdbc:mysql://localhost:3306/oxyn";
    private final String password = "";

    private Connection connection;

    private MyDataBase() {
        try {
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("Connection established");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
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
