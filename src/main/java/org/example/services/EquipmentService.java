package org.example.services;

import org.example.entities.Equipment;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipmentService {

    private static Connection fresh() throws SQLException {
        Connection c = DriverManager.getConnection(
            MyDataBase.getURL(), MyDataBase.getUSERNAME(), MyDataBase.getPASSWORD());
        c.setAutoCommit(true);
        return c;
    }

    public void ajouter(Equipment e) throws SQLException {
        try (Connection c = fresh()) {
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO equipments (name, description, quantity, created_at, gymnasium_id, is_active) VALUES (?,?,?,NOW(),?,1)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, e.getName()); ps.setString(2, e.getDescription()); ps.setInt(3, e.getQuantity());
            if (e.getGymnasiumId() != null) ps.setInt(4, e.getGymnasiumId()); else ps.setNull(4, Types.INTEGER);
            ps.executeUpdate();
            ResultSet k = ps.getGeneratedKeys(); if (k.next()) e.setId(k.getInt(1));
        }
    }

    public List<Equipment> afficher() throws SQLException {
        try (Connection c = fresh()) {
            List<Equipment> list = new ArrayList<>();
            ResultSet rs = c.createStatement().executeQuery(
                "SELECT e.*, g.name AS gym_name FROM equipments e LEFT JOIN gymnasia g ON e.gymnasium_id=g.id ORDER BY e.created_at DESC");
            while (rs.next()) {
                Equipment e = new Equipment();
                e.setId(rs.getInt("id")); e.setName(rs.getString("name"));
                e.setDescription(rs.getString("description")); e.setQuantity(rs.getInt("quantity"));
                e.setCreatedAt(rs.getTimestamp("created_at"));
                int gid = rs.getInt("gymnasium_id"); e.setGymnasiumId(rs.wasNull() ? null : gid);
                e.setGymnasiumName(rs.getString("gym_name")); e.setActive(rs.getInt("is_active") == 1);
                list.add(e);
            }
            return list;
        }
    }

    public void modifier(Equipment e) throws SQLException {
        try (Connection c = fresh()) {
            PreparedStatement ps = c.prepareStatement(
                "UPDATE equipments SET name=?, description=?, quantity=?, gymnasium_id=? WHERE id=?");
            ps.setString(1, e.getName()); ps.setString(2, e.getDescription()); ps.setInt(3, e.getQuantity());
            if (e.getGymnasiumId() != null) ps.setInt(4, e.getGymnasiumId()); else ps.setNull(4, Types.INTEGER);
            ps.setInt(5, e.getId()); ps.executeUpdate();
        }
    }

    public void toggleActive(int id, boolean active) throws SQLException {
        try (Connection c = fresh()) {
            PreparedStatement ps = c.prepareStatement("UPDATE equipments SET is_active=? WHERE id=?");
            ps.setInt(1, active ? 1 : 0); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        try (Connection c = fresh()) {
            PreparedStatement ps = c.prepareStatement("DELETE FROM equipments WHERE id=?");
            ps.setInt(1, id); ps.executeUpdate();
        }
    }
}
