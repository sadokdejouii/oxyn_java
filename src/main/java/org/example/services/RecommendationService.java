package org.example.services;

import org.example.entities.UserRecommendation;



import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.example.utils.MyDataBase;

/**
 * Service class for managing user recommendation data.
 * Tracks category interactions and calculates personalized recommendations.
 */
public class RecommendationService {

    private Connection con;

    public RecommendationService() {
        this.con =MyDataBase.getInstance().getConnection();
    }

    /**
     * Action types for category interactions
     */
    public enum ActionType {
        CREATE_POST,      // User creates a post in a category
        ADD_COMMENT,      // User comments on a post in a category
        LIKE_POST,        // User likes a post in a category
        VIEW_POST         // User views a post in a category
    }

    /**
     * Score weights for different interaction types
     */
    private static final Map<ActionType, Integer> SCORE_WEIGHTS = new HashMap<>();
    static {
        SCORE_WEIGHTS.put(ActionType.CREATE_POST, 5);      // Creating posts shows strong interest
        SCORE_WEIGHTS.put(ActionType.ADD_COMMENT, 3);      // Commenting shows engagement
        SCORE_WEIGHTS.put(ActionType.LIKE_POST, 2);        // Liking shows mild interest
        SCORE_WEIGHTS.put(ActionType.VIEW_POST, 1);        // Viewing shows basic interest
    }

    /**
     * Create a new recommendation record for a user
     */
    public void createRecommendationForUser(int userId) throws SQLException {
        String sql = "INSERT INTO user_recommendation (user_id, category_scores_json, total_interactions) VALUES (?, '{}', 0)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
        ps.close();
    }

    /**
     * Get recommendation data for a user
     */
    public UserRecommendation getRecommendationByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM user_recommendation WHERE user_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        UserRecommendation recommendation = null;
        if (rs.next()) {
            recommendation = new UserRecommendation(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("category_scores_json"),
                rs.getInt("total_interactions"),
                rs.getString("last_updated")
            );
        }

        rs.close();
        ps.close();
        return recommendation;
    }

    /**
     * Handle user action and update category scores
     */
    public void handleUserAction(int userId, String category, ActionType actionType) throws SQLException {
        UserRecommendation recommendation = getRecommendationByUserId(userId);
        
        if (recommendation == null) {
            createRecommendationForUser(userId);
            recommendation = getRecommendationByUserId(userId);
        }

        int scoreIncrement = SCORE_WEIGHTS.getOrDefault(actionType, 1);
        recommendation.incrementCategoryScore(category, scoreIncrement);
        updateRecommendation(recommendation);
    }

    /**
     * Update recommendation record in database
     */
    public void updateRecommendation(UserRecommendation recommendation) throws SQLException {
        String sql = "UPDATE user_recommendation SET category_scores_json = ?, total_interactions = ?, last_updated = ? WHERE user_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, recommendation.getCategoryScoresJson());
        ps.setInt(2, recommendation.getTotalInteractions());
        ps.setString(3, java.time.LocalDateTime.now().toString());
        ps.setInt(4, recommendation.getUserId());
        ps.executeUpdate();
        ps.close();
    }

    /**
     * Get recommended categories for a user based on their interaction scores
     */
    public List<String> getRecommendedCategories(int userId, int limit) throws SQLException {
        UserRecommendation recommendation = getRecommendationByUserId(userId);
        
        if (recommendation == null || recommendation.getTotalInteractions() == 0) {
            // Return default categories if no interaction data
            return getDefaultCategories(limit);
        }

        Map<String, Integer> topCategories = recommendation.getTopCategories(limit);
        return new ArrayList<>(topCategories.keySet());
    }

    /**
     * Get default categories for new users
     */
    private List<String> getDefaultCategories(int limit) {
        List<String> defaultCategories = Arrays.asList(
            "Général",
            "Technology",
            "Sports",
            "Music",
            "Art",
            "Science",
            "Business",
            "Entertainment"
        );
        return defaultCategories.subList(0, Math.min(limit, defaultCategories.size()));
    }

    /**
     * Get all available categories from posts
     */
    public List<String> getAllCategories() throws SQLException {
        String sql = "SELECT DISTINCT category_post FROM post WHERE category_post IS NOT NULL AND category_post != '' ORDER BY category_post";
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        List<String> categories = new ArrayList<>();
        while (rs.next()) {
            categories.add(rs.getString("category_post"));
        }

        rs.close();
        ps.close();
        return categories;
    }

    /**
     * Get category score for a specific user and category
     */
    public int getCategoryScore(int userId, String category) throws SQLException {
        UserRecommendation recommendation = getRecommendationByUserId(userId);
        if (recommendation == null) {
            return 0;
        }
        return recommendation.getCategoryScore(category);
    }

    /**
     * Reset recommendation data for a user
     */
    public void resetRecommendation(int userId) throws SQLException {
        String sql = "UPDATE user_recommendation SET category_scores_json = '{}', total_interactions = 0 WHERE user_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
        ps.close();
    }

    /**
     * Delete recommendation record for a user
     */
    public void deleteRecommendation(int userId) throws SQLException {
        String sql = "DELETE FROM user_recommendation WHERE user_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
        ps.close();
    }

    /**
     * Get users with highest interaction in a specific category
     */
    public List<Map<String, Object>> getTopUsersByCategory(String category, int limit) throws SQLException {
        String sql = "SELECT u.id, u.nom, u.prenom, ur.category_scores_json, ur.total_interactions " +
                     "FROM user u " +
                     "LEFT JOIN user_recommendation ur ON u.id = ur.user_id " +
                     "WHERE ur.category_scores_json IS NOT NULL " +
                     "ORDER BY ur.total_interactions DESC " +
                     "LIMIT ?";
        
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, limit);
        ResultSet rs = ps.executeQuery();

        List<Map<String, Object>> users = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", rs.getInt("id"));
            userMap.put("nom", rs.getString("nom"));
            userMap.put("prenom", rs.getString("prenom"));
            
            String categoryScoresJson = rs.getString("category_scores_json");
            int categoryScore = 0;
            if (categoryScoresJson != null) {
                UserRecommendation tempRec = new UserRecommendation(0, 0, categoryScoresJson, 0, "");
                categoryScore = tempRec.getCategoryScore(category);
            }
            userMap.put("categoryScore", categoryScore);
            userMap.put("totalInteractions", rs.getInt("total_interactions"));
            
            users.add(userMap);
        }

        rs.close();
        ps.close();
        return users;
    }

    /**
     * Get personalized feed recommendations for a user
     * Returns a list of post IDs based on user's category preferences
     */
    public List<Integer> getRecommendedPosts(int userId, int limit) throws SQLException {
        List<String> recommendedCategories = getRecommendedCategories(userId, 5);
        
        if (recommendedCategories.isEmpty()) {
            // Return random posts if no preferences
            String sql = "SELECT id_post FROM post ORDER BY RAND() LIMIT ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            
            List<Integer> postIds = new ArrayList<>();
            while (rs.next()) {
                postIds.add(rs.getInt("id_post"));
            }
            
            rs.close();
            ps.close();
            return postIds;
        }

        // Build SQL query to get posts from recommended categories
        StringBuilder sql = new StringBuilder(
            "SELECT id_post, category_post FROM post WHERE category_post IN ("
        );
        
        for (int i = 0; i < recommendedCategories.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(") ORDER BY FIELD(category_post");
        
        for (int i = 0; i < recommendedCategories.size(); i++) {
            sql.append(", ?");
        }
        sql.append(") LIMIT ?");

        PreparedStatement ps = con.prepareStatement(sql.toString());
        int paramIndex = 1;
        
        for (String category : recommendedCategories) {
            ps.setString(paramIndex++, category);
        }
        
        for (String category : recommendedCategories) {
            ps.setString(paramIndex++, category);
        }
        
        ps.setInt(paramIndex, limit);
        
        ResultSet rs = ps.executeQuery();
        
        List<Integer> postIds = new ArrayList<>();
        while (rs.next()) {
            postIds.add(rs.getInt("id_post"));
        }

        rs.close();
        ps.close();
        return postIds;
    }
}
