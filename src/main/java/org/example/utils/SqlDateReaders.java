package org.example.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/** Lecture DATETIME MySQL y compris {@code 0000-00-00} sans lever d'exception côté JDBC. */
public final class SqlDateReaders {

    private SqlDateReaders() {
    }

    public static Timestamp readTimestampOrNull(ResultSet rs, String columnLabel) throws SQLException {
        String raw;
        try {
            raw = rs.getString(columnLabel);
        } catch (SQLException ex) {
            String msg = ex.getMessage() == null ? "" : ex.getMessage();
            if (msg.contains("Zero date") || msg.toLowerCase().contains("prohibited")) {
                return null;
            }
            throw ex;
        }
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty() || t.startsWith("0000-00-00")) {
            return null;
        }
        try {
            if (t.length() >= 19) {
                return Timestamp.valueOf(t.substring(0, 19).replace('T', ' '));
            }
            if (t.length() == 10 && t.charAt(4) == '-' && t.charAt(7) == '-') {
                return Timestamp.valueOf(t + " 00:00:00");
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }
}
