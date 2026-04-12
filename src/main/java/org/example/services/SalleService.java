package org.example.services;

import org.example.entities.Salle;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalleService {

    private final Connection con;

    public SalleService() {
        con = MyDataBase.getInstance().getConnection();
    }

    public void ajouter(Salle s) throws SQLException {
        String sql = "INSERT INTO gymnasia (name, description, address, phone, email, image_url, rating, rating_count, is_active, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, s.getName());
        ps.setString(2, s.getDescription());
        ps.setString(3, s.getAddress());
        ps.setString(4, s.getPhone());
        ps.setString(5, s.getEmail());
        ps.setString(6, s.getImageUrl());
        ps.setDouble(7, 0);
        ps.setInt(8, 0);
        ps.setInt(9, 1);
        ps.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
        ps.setTimestamp(11, new Timestamp(System.currentTimeMillis()));
        ps.executeUpdate();
    }

    public List<Salle> afficher() throws SQLException {
        List<Salle> list = new ArrayList<>();
        String sql = "SELECT * FROM gymnasia WHERE is_active = 1 ORDER BY created_at DESC";
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
            s.setRating(rs.getDouble("rating"));
            s.setRatingCount(rs.getInt("rating_count"));
            s.setActive(rs.getInt("is_active") == 1);
            s.setCreatedAt(rs.getTimestamp("created_at"));
            s.setUpdatedAt(rs.getTimestamp("updated_at"));
            list.add(s);
        }
        return list;
    }

    public void modifier(Salle s) throws SQLException {
        String sql = "UPDATE gymnasia SET name=?, description=?, address=?, phone=?, email=?, image_url=?, updated_at=? WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, s.getName());
        ps.setString(2, s.getDescription());
        ps.setString(3, s.getAddress());
        ps.setString(4, s.getPhone());
        ps.setString(5, s.getEmail());
        ps.setString(6, s.getImageUrl());
        ps.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
        ps.setInt(8, s.getId());
        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        String sql = "UPDATE gymnasia SET is_active = 0 WHERE id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
