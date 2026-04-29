package org.example.dao;

import org.example.entities.GymRating;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GymRatingDAO {
    private final Connection connection;
    
    public GymRatingDAO(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Ajoute une nouvelle note pour une salle
     */
    public boolean addRating(GymRating rating) throws SQLException {
        // Vérifier si l'utilisateur a déjà noté cette salle
        if (hasRated(rating.getUserId(), rating.getGymnasiumId())) {
            throw new SQLException("Cet utilisateur a déjà noté cette salle");
        }
        
        String sql = "INSERT INTO gym_ratings (rating, comment, created_at, user_id, gymnasium_id) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        
        ps.setInt(1, rating.getRating());
        ps.setString(2, rating.getComment());
        ps.setTimestamp(3, Timestamp.valueOf(rating.getCreatedAt()));
        ps.setInt(4, rating.getUserId());
        ps.setInt(5, rating.getGymnasiumId());
        
        int affectedRows = ps.executeUpdate();
        
        if (affectedRows > 0) {
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                rating.setId(rs.getInt(1));
            }
            return true;
        }
        return false;
    }
    
    /**
     * Vérifie si un utilisateur a déjà noté une salle
     */
    public boolean hasRated(int userId, int gymnasiumId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM gym_ratings WHERE user_id = ? AND gymnasium_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, gymnasiumId);
        
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }
    
    /**
     * Récupère la note d'un utilisateur pour une salle spécifique
     */
    public GymRating getUserRating(int userId, int gymnasiumId) throws SQLException {
        String sql = "SELECT * FROM gym_ratings WHERE user_id = ? AND gymnasium_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, gymnasiumId);
        
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapResultSetToGymRating(rs);
        }
        return null;
    }
    
    /**
     * Récupère toutes les notes pour une salle
     */
    public List<GymRating> getRatingsBySalle(int gymnasiumId) throws SQLException {
        String sql = "SELECT * FROM gym_ratings WHERE gymnasium_id = ? ORDER BY created_at DESC";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, gymnasiumId);
        
        List<GymRating> ratings = new ArrayList<>();
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            ratings.add(mapResultSetToGymRating(rs));
        }
        
        return ratings;
    }
    
    /**
     * Calcule la moyenne des notes pour une salle
     */
    public double getAverageRating(int gymnasiumId) throws SQLException {
        String sql = "SELECT AVG(rating) as avg_rating FROM gym_ratings WHERE gymnasium_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, gymnasiumId);
        
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getDouble("avg_rating");
        }
        return 0.0;
    }
    
    /**
     * Compte le nombre de notes pour une salle
     */
    public int getRatingCount(int gymnasiumId) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM gym_ratings WHERE gymnasium_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, gymnasiumId);
        
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("count");
        }
        return 0;
    }
    
    /**
     * Récupère toutes les notes (pour l'admin)
     */
    public List<GymRating> getAllRatings() throws SQLException {
        String sql = "SELECT * FROM gym_ratings ORDER BY created_at DESC";
        Statement st = connection.createStatement();
        
        List<GymRating> ratings = new ArrayList<>();
        ResultSet rs = st.executeQuery(sql);
        
        while (rs.next()) {
            ratings.add(mapResultSetToGymRating(rs));
        }
        
        return ratings;
    }
    
    /**
     * Supprime une note
     */
    public boolean deleteRating(int ratingId) throws SQLException {
        String sql = "DELETE FROM gym_ratings WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, ratingId);
        
        return ps.executeUpdate() > 0;
    }
    
    /**
     * Met à jour une note
     */
    public boolean updateRating(GymRating rating) throws SQLException {
        String sql = "UPDATE gym_ratings SET rating = ?, comment = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        
        ps.setInt(1, rating.getRating());
        ps.setString(2, rating.getComment());
        ps.setInt(3, rating.getId());
        
        return ps.executeUpdate() > 0;
    }
    
    /**
     * Met à jour la note moyenne et le nombre d'avis dans la table gymnasium
     */
    public boolean updateGymnasiumRating(int gymnasiumId) throws SQLException {
        // Calculer la nouvelle moyenne et le nombre d'avis
        double avgRating = getAverageRating(gymnasiumId);
        int ratingCount = getRatingCount(gymnasiumId);
        
        System.out.println("DEBUG: updateGymnasiumRating() pour salle " + gymnasiumId);
        System.out.println("DEBUG: avgRating = " + avgRating + " | ratingCount = " + ratingCount);
        
        String sql = "UPDATE gymnasia SET rating = ?, rating_count = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        
        ps.setDouble(1, avgRating);
        ps.setInt(2, ratingCount);
        ps.setInt(3, gymnasiumId);
        
        int rowsUpdated = ps.executeUpdate();
        System.out.println("DEBUG: rowsUpdated = " + rowsUpdated);
        
        return rowsUpdated > 0;
    }
    
    /**
     * Convertit un ResultSet en objet GymRating
     */
    private GymRating mapResultSetToGymRating(ResultSet rs) throws SQLException {
        GymRating rating = new GymRating();
        rating.setId(rs.getInt("id"));
        rating.setRating(rs.getInt("rating"));
        rating.setComment(rs.getString("comment"));
        rating.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        rating.setUserId(rs.getInt("user_id"));
        rating.setGymnasiumId(rs.getInt("gymnasium_id"));
        return rating;
    }
}
