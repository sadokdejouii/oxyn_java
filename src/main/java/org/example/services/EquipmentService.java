package org.example.services;

import org.example.entities.Equipment;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipmentService {
    private final Connection con;
    public EquipmentService() { con = MyDataBase.getConnection(); }

    /**
     * ✅ Ajouter un équipement - INSERT en BD sans throws SQLException
     */
    public void add(Equipment e) {
        String sql = """
            INSERT INTO equipments 
            (name, description, quantity, gymnasium_id, is_active, created_at)
            VALUES (?, ?, ?, ?, 1, NOW())
            """;
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, e.getName());
            stmt.setString(2, e.getDescription() != null ? e.getDescription() : "");
            stmt.setInt(3, e.getQuantity());
            
            if (e.getGymnasiumId() != null) {
                stmt.setInt(4, e.getGymnasiumId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            
            stmt.executeUpdate();
            
            // Récupérer l'ID généré
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    e.setId(generatedKeys.getInt(1));
                }
            }
            
            System.out.println("✅ Équipement ajouté en BD avec ID: " + e.getId());
            
        } catch (SQLException ex) {
            System.err.println("❌ add(): " + ex.getMessage());
        }
    }
    
    /**
     * ✅ Récupérer les équipements par gymnase
     */
    public List<Equipment> getByGym(int gymnasiumId) {
        List<Equipment> list = new ArrayList<>();
        String sql = "SELECT * FROM equipments WHERE gymnasium_id = ? AND is_active = 1 ORDER BY created_at DESC";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, gymnasiumId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Equipment e = new Equipment();
                e.setId(rs.getInt("id"));
                e.setName(rs.getString("name"));
                e.setDescription(rs.getString("description"));
                e.setQuantity(rs.getInt("quantity"));
                e.setGymnasiumId(rs.getInt("gymnasium_id"));
                e.setActive(rs.getInt("is_active") == 1);
                e.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(e);
            }
            
        } catch (SQLException ex) {
            System.err.println("❌ getByGym(): " + ex.getMessage());
        }
        
        return list;
    }

    public List<Equipment> afficher() throws SQLException {
        List<Equipment> list = new ArrayList<>();
        ResultSet rs = con.createStatement().executeQuery(
            "SELECT e.*, g.name AS gym_name FROM equipments e LEFT JOIN gymnasia g ON e.gymnasium_id=g.id ORDER BY e.created_at DESC");
        while (rs.next()) {
            Equipment e = new Equipment();
            e.setId(rs.getInt("id")); e.setName(rs.getString("name")); e.setDescription(rs.getString("description"));
            e.setQuantity(rs.getInt("quantity")); e.setCreatedAt(rs.getTimestamp("created_at"));
            int gid = rs.getInt("gymnasium_id"); e.setGymnasiumId(rs.wasNull() ? null : gid);
            e.setGymnasiumName(rs.getString("gym_name")); e.setActive(rs.getInt("is_active") == 1);
            list.add(e);
        }
        return list;
    }

    public List<Equipment> afficherParSalle(int gymnasiumId) throws SQLException {
        List<Equipment> list = new ArrayList<>();
        PreparedStatement ps = con.prepareStatement(
            "SELECT e.*, g.name AS gym_name FROM equipments e LEFT JOIN gymnasia g ON e.gymnasium_id=g.id " +
                "WHERE e.gymnasium_id=? AND e.is_active=1 ORDER BY e.created_at DESC");
        ps.setInt(1, gymnasiumId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Equipment e = new Equipment();
            e.setId(rs.getInt("id")); e.setName(rs.getString("name")); e.setDescription(rs.getString("description"));
            e.setQuantity(rs.getInt("quantity")); e.setCreatedAt(rs.getTimestamp("created_at"));
            int gid = rs.getInt("gymnasium_id"); e.setGymnasiumId(rs.wasNull() ? null : gid);
            e.setGymnasiumName(rs.getString("gym_name")); e.setActive(rs.getInt("is_active") == 1);
            list.add(e);
        }
        return list;
    }

    public int countParSalle(int gymnasiumId) {
        String sql = "SELECT COUNT(*) FROM equipments WHERE gymnasium_id=? AND is_active=1";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, gymnasiumId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur countParSalle (Equipment): " + e.getMessage());
        }
        return 0;
    }

    public void modifier(Equipment e) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
            "UPDATE equipments SET name=?, description=?, quantity=?, gymnasium_id=? WHERE id=?");
        ps.setString(1, e.getName()); ps.setString(2, e.getDescription()); ps.setInt(3, e.getQuantity());
        if (e.getGymnasiumId() != null) ps.setInt(4, e.getGymnasiumId()); else ps.setNull(4, Types.INTEGER);
        ps.setInt(5, e.getId()); ps.executeUpdate();
    }

    public void toggleActive(int id, boolean active) throws SQLException {
        PreparedStatement ps = con.prepareStatement("UPDATE equipments SET is_active=? WHERE id=?");
        ps.setInt(1, active ? 1 : 0); ps.setInt(2, id); ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = con.prepareStatement("DELETE FROM equipments WHERE id=?");
        ps.setInt(1, id); ps.executeUpdate();
    }
}
