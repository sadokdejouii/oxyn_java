package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {
    private static final String USERNAME = "root";
    private static final String URL      = "jdbc:mysql://localhost:3306/oxyn";
    private static final String PASSWORD = "";

    private Connection connection;
    private static MyDataBase instance;

    private MyDataBase() { connect(); }

    private void connect() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            connection.setAutoCommit(true);
            System.out.println("Connection established");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null) instance = new MyDataBase();
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
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException ignored) {}
        connect();
    }

    public static String getURL()      { return URL; }
    public static String getUSERNAME() { return USERNAME; }
    public static String getPASSWORD() { return PASSWORD; }
}
