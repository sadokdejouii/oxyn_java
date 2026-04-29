package org.example.services;

import org.example.entities.Salle;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalleService {

    private final Connection con;

    public SalleService() {
        con = MyDataBase.getConnection();
    }

    public void ajouter(Salle s) throws SQLException {
        String sql = "INSERT INTO gymnasia (name, description, address, phone, email, image_url, youtube_url, rating, rating_count, is_active, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, s.getName());
        ps.setString(2, s.getDescription());
        ps.setString(3, s.getAddress());
        ps.setString(4, s.getPhone());
        ps.setString(5, s.getEmail());
        ps.setString(6, s.getImageUrl());
        ps.setString(7, s.getYoutubeUrl());
        ps.setDouble(8, 0);
        ps.setInt(9, 0);
        ps.setInt(10, 1);
        ps.setTimestamp(11, new Timestamp(System.currentTimeMillis()));
        ps.setTimestamp(12, new Timestamp(System.currentTimeMillis()));
        ps.executeUpdate();
    }

    public List<Salle> afficher() throws SQLException {
        List<Salle> list = new ArrayList<>();
        String sql = 
            "SELECT g.id, g.name, g.description, g.address, g.phone, g.email, g.image_url, g.google_maps_url, g.youtube_url, g.rating, g.rating_count, g.is_active, g.created_at, g.updated_at, " +
            "COALESCE(AVG(r.rating), 0) as avg_rating, " +
            "COUNT(r.id) as total_ratings " +
            "FROM gymnasia g " +
            "LEFT JOIN gym_ratings r ON g.id = r.gymnasium_id " +
            "WHERE g.is_active = 1 " +
            "GROUP BY g.id, g.name, g.description, g.address, g.phone, g.email, g.image_url, g.google_maps_url, g.youtube_url, g.rating, g.rating_count, g.is_active, g.created_at, g.updated_at " +
            "ORDER BY g.created_at DESC";
        
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Salle s = new Salle();
            s.setId(rs.getInt("id"));
            s.setName(rs.getString("name"));
            s.setDescription(rs.getString("description"));
            s.setAddress(rs.getString("address"));
            s.setPhone(rs.getString("phone"));
            s.setEmail(rs.getString("email"));
            s.setImageUrl(rs.getString("image_url"));
            s.setGoogleMapsUrl(rs.getString("google_maps_url"));
            s.setYoutubeUrl(rs.getString("youtube_url"));
            s.setRating(rs.getDouble("avg_rating"));
            s.setRatingCount(rs.getInt("total_ratings"));
            s.setActive(rs.getInt("is_active") == 1);
            s.setCreatedAt(rs.getTimestamp("created_at"));
            s.setUpdatedAt(rs.getTimestamp("updated_at"));
            list.add(s);
        }
        return list;
    }

    public void modifier(Salle s) throws SQLException {
        String sql = "UPDATE gymnasia SET name=?, description=?, address=?, phone=?, email=?, image_url=?, youtube_url=?, updated_at=? WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, s.getName());
        ps.setString(2, s.getDescription());
        ps.setString(3, s.getAddress());
        ps.setString(4, s.getPhone());
        ps.setString(5, s.getEmail());
        ps.setString(6, s.getImageUrl());
        ps.setString(7, s.getYoutubeUrl());
        ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
        ps.setInt(9, s.getId());
        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        String sql = "UPDATE gymnasia SET is_active = 0 WHERE id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }
    
    // ===== MÉTHODES DE COMPATIBILITÉ =====
    
    /**
     * Alias pour afficher() - compatibilité
     */
    public List<Salle> getAll() throws SQLException {
        return afficher();
    }
    
    /**
     * Alias pour ajouter() - compatibilité
     */
    public void add(Salle s) throws SQLException {
        ajouter(s);
    }
    
    /**
     * Alias pour modifier() - compatibilité
     */
    public void update(Salle s) throws SQLException {
        modifier(s);
    }
    
    /**
     * Alias pour supprimer() - compatibilité
     */
    public void delete(int id) throws SQLException {
        supprimer(id);
    }
}