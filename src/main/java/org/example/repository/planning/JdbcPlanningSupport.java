package org.example.repository.planning;

import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connexion JDBC partagée pour les repositories Planning (base {@code oxyn}).
 */
public final class JdbcPlanningSupport {

    private JdbcPlanningSupport() {
    }

    /**
     * @throws SQLException si pas de connexion ou connexion fermée
     */
    public static Connection requireConnection() throws SQLException {
        Connection c = MyDataBase.getConnection();
        if (c == null || c.isClosed()) {
            throw new SQLException("Pas de connexion MySQL");
        }
        return c;
    }

    public static Connection connectionOrNull() throws SQLException {
        Connection c = MyDataBase.getConnection();
        if (c == null || c.isClosed()) {
            return null;
        }
        return c;
    }
}
