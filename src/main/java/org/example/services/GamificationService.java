package org.example.services;

import org.example.entities.UserGamification;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * GamificationService - Handles all gamification logic for users.
 * 
 * This service manages:
 * - User gamification records (points, levels, badges, stats)
 * - Point updates based on user actions
 * - Level progression
 * - Badge assignment
 * - Leaderboard functionality
 * 
 * Point System:
 * - Create post: +5 points
 * - Add comment: +2 points
 * - Receive like: +1 point
 * 
 * Level System:
 * - 0-30 points: Beginner
 * - 31-80 points: Intermediate
 * - 81+ points: Advanced
 * 
 * Badge System (only highest displayed):
 * - "First Post": when postsCount == 1 (removed after second post)
 * - "Rising Star": when postsCount >= 5
 * - "Content Creator": when postsCount >= 10
 * - "Chatty": when commentsCount >= 5
 * - "Conversation Starter": when commentsCount >= 15
 * - "Liked": when likesReceived >= 20
 * - "Beloved": when likesReceived >= 50
 * - "Superstar": when points >= 100
 */
public class GamificationService {

    private Connection con;

    public GamificationService() {
        con = MyDataBase.getInstance().getConnection();
    }

    /**
     * Action types for gamification updates
     */
    public enum ActionType {
        CREATE_POST,
        ADD_COMMENT,
        RECEIVE_LIKE
    }

    /**
     * Creates a new gamification record for a user.
     * Should be called when a new user is created.
     * 
     * @param userId The ID of the user
     * @throws SQLException if database operation fails
     */
    public void createGamificationForUser(int userId) throws SQLException {
        String sql = "INSERT INTO user_gamification (user_id, points, level, badges_json, posts_count, comments_count, likes_received) " +
                     "VALUES (?, 0, 'Beginner', '[]', 0, 0, 0)";
        
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
        ps.close();
        
        System.out.println("Gamification record created for user ID: " + userId);
    }

    /**
     * Handles user actions and updates gamification accordingly.
     * This is the main entry point for updating gamification data.
     * 
     * @param userId The ID of the user performing the action
     * @param actionType The type of action performed
     * @throws SQLException if database operation fails
     */
    public void handleUserAction(int userId, ActionType actionType) throws SQLException {
        UserGamification gamification = getGamificationByUserId(userId);
        
        if (gamification == null) {
            // Create gamification record if it doesn't exist
            createGamificationForUser(userId);
            gamification = getGamificationByUserId(userId);
        }
        
        // Update points and stats based on action type
        switch (actionType) {
            case CREATE_POST:
                gamification.setPoints(gamification.getPoints() + 5);
                gamification.setPostsCount(gamification.getPostsCount() + 1);
                break;
            case ADD_COMMENT:
                gamification.setPoints(gamification.getPoints() + 2);
                gamification.setCommentsCount(gamification.getCommentsCount() + 1);
                break;
            case RECEIVE_LIKE:
                gamification.setPoints(gamification.getPoints() + 1);
                gamification.setLikesReceived(gamification.getLikesReceived() + 1);
                break;
        }
        
        // Update level based on new points
        updateLevel(gamification);
        
        // Assign badges based on new stats
        assignBadges(gamification);
        
        // Save to database
        updateGamification(gamification);
        
        System.out.println("Gamification updated for user ID: " + userId + 
                          " - Action: " + actionType + 
                          " - New Points: " + gamification.getPoints() +
                          " - Level: " + gamification.getLevel());
    }

    /**
     * Updates the user's level based on their points.
     * 
     * Level progression:
     * - 0-30 points: Beginner
     * - 31-80 points: Intermediate
     * - 81+ points: Advanced
     * 
     * @param gamification The UserGamification object to update
     */
    public void updateLevel(UserGamification gamification) {
        int points = gamification.getPoints();
        
        if (points <= 30) {
            gamification.setLevel("Beginner");
        } else if (points <= 80) {
            gamification.setLevel("Intermediate");
        } else {
            gamification.setLevel("Advanced");
        }
    }

    /**
     * Assigns badges to the user based on their statistics.
     * Only the highest priority badge will be displayed.
     * 
     * Badge priority (highest to lowest):
     * 1. Superstar (points >= 100)
     * 2. Beloved (likesReceived >= 50)
     * 3. Content Creator (postsCount >= 10)
     * 4. Conversation Starter (commentsCount >= 15)
     * 5. Rising Star (postsCount >= 5)
     * 6. Liked (likesReceived >= 20)
     * 7. Chatty (commentsCount >= 5)
     * 8. First Post (postsCount == 1, removed after second post)
     * 
     * @param gamification The UserGamification object to update
     */
    public void assignBadges(UserGamification gamification) {
        List<String> badges = gamification.getBadges();
        
        // Remove "First Post" badge if user has more than 1 post
        if (gamification.getPostsCount() > 1 && badges.contains("First Post")) {
            badges.remove("First Post");
            gamification.setBadges(badges);
            System.out.println("Badge 'First Post' removed from user ID: " + gamification.getUserId());
        }
        
        // Check for "First Post" badge (only when exactly 1 post)
        if (gamification.getPostsCount() == 1 && !badges.contains("First Post")) {
            gamification.addBadge("First Post");
            System.out.println("Badge 'First Post' assigned to user ID: " + gamification.getUserId());
        }
        
        // Check for "Rising Star" badge
        if (gamification.getPostsCount() >= 5 && !badges.contains("Rising Star")) {
            gamification.addBadge("Rising Star");
            System.out.println("Badge 'Rising Star' assigned to user ID: " + gamification.getUserId());
        }
        
        // Check for "Content Creator" badge
        if (gamification.getPostsCount() >= 10 && !badges.contains("Content Creator")) {
            gamification.addBadge("Content Creator");
            System.out.println("Badge 'Content Creator' assigned to user ID: " + gamification.getUserId());
        }
        
        // Check for "Chatty" badge
        if (gamification.getCommentsCount() >= 5 && !badges.contains("Chatty")) {
            gamification.addBadge("Chatty");
            System.out.println("Badge 'Chatty' assigned to user ID: " + gamification.getUserId());
        }
        
        // Check for "Conversation Starter" badge
        if (gamification.getCommentsCount() >= 15 && !badges.contains("Conversation Starter")) {
            gamification.addBadge("Conversation Starter");
            System.out.println("Badge 'Conversation Starter' assigned to user ID: " + gamification.getUserId());
        }
        
        // Check for "Liked" badge
        if (gamification.getLikesReceived() >= 20 && !badges.contains("Liked")) {
            gamification.addBadge("Liked");
            System.out.println("Badge 'Liked' assigned to user ID: " + gamification.getUserId());
        }
        
        // Check for "Beloved" badge
        if (gamification.getLikesReceived() >= 50 && !badges.contains("Beloved")) {
            gamification.addBadge("Beloved");
            System.out.println("Badge 'Beloved' assigned to user ID: " + gamification.getUserId());
        }
        
        // Check for "Superstar" badge
        if (gamification.getPoints() >= 100 && !badges.contains("Superstar")) {
            gamification.addBadge("Superstar");
            System.out.println("Badge 'Superstar' assigned to user ID: " + gamification.getUserId());
        }
    }

    /**
     * Retrieves gamification data for a specific user.
     * 
     * @param userId The ID of the user
     * @return UserGamification object or null if not found
     * @throws SQLException if database operation fails
     */
    public UserGamification getGamificationByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM user_gamification WHERE user_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        
        UserGamification gamification = null;
        if (rs.next()) {
            gamification = mapResultSetToGamification(rs);
        }
        
        rs.close();
        ps.close();
        return gamification;
    }

    /**
     * Updates an existing gamification record in the database.
     * 
     * @param gamification The UserGamification object to update
     * @throws SQLException if database operation fails
     */
    public void updateGamification(UserGamification gamification) throws SQLException {
        String sql = "UPDATE user_gamification SET points = ?, level = ?, badges_json = ?, " +
                     "posts_count = ?, comments_count = ?, likes_received = ? WHERE user_id = ?";
        
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, gamification.getPoints());
        ps.setString(2, gamification.getLevel());
        ps.setString(3, gamification.getBadgesJson());
        ps.setInt(4, gamification.getPostsCount());
        ps.setInt(5, gamification.getCommentsCount());
        ps.setInt(6, gamification.getLikesReceived());
        ps.setInt(7, gamification.getUserId());
        
        ps.executeUpdate();
        ps.close();
    }

    /**
     * Retrieves the leaderboard - top users sorted by points in descending order.
     * 
     * @param limit Maximum number of users to return (default: 10)
     * @return List of UserGamification objects sorted by points
     * @throws SQLException if database operation fails
     */
    public List<UserGamification> getLeaderboard(int limit) throws SQLException {
        String sql = "SELECT ug.*, u.nom, u.prenom " +
                     "FROM user_gamification ug " +
                     "JOIN user u ON ug.user_id = u.id_user " +
                     "ORDER BY ug.points DESC " +
                     "LIMIT ?";
        
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, limit);
        ResultSet rs = ps.executeQuery();
        
        List<UserGamification> leaderboard = new ArrayList<>();
        while (rs.next()) {
            leaderboard.add(mapResultSetToGamification(rs));
        }
        
        rs.close();
        ps.close();
        return leaderboard;
    }

    /**
     * Retrieves the leaderboard with default limit of 10 users.
     * 
     * @return List of UserGamification objects sorted by points
     * @throws SQLException if database operation fails
     */
    public List<UserGamification> getLeaderboard() throws SQLException {
        return getLeaderboard(10);
    }

    /**
     * Gets the highest priority badge from a list of badges.
     * Priority order (highest to lowest):
     * 1. Superstar
     * 2. Beloved
     * 3. Content Creator
     * 4. Conversation Starter
     * 5. Rising Star
     * 6. Liked
     * 7. Chatty
     * 8. First Post
     * 
     * @param badges List of badge names
     * @return The highest priority badge, or null if list is empty
     */
    public String getHighestPriorityBadge(List<String> badges) {
        if (badges == null || badges.isEmpty()) {
            return null;
        }
        
        // Priority order (highest first)
        String[] priorityOrder = {
            "Superstar",
            "Beloved",
            "Content Creator",
            "Conversation Starter",
            "Rising Star",
            "Liked",
            "Chatty",
            "First Post"
        };
        
        for (String priorityBadge : priorityOrder) {
            if (badges.contains(priorityBadge)) {
                return priorityBadge;
            }
        }
        
        // If no known badge found, return the first one in the list
        return badges.get(0);
    }

    /**
     * Gets the rank of a specific user on the leaderboard.
     * 
     * @param userId The ID of the user
     * @return The rank (1-based) or -1 if user not found
     * @throws SQLException if database operation fails
     */
    public int getUserRank(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) + 1 as rank " +
                     "FROM user_gamification " +
                     "WHERE points > (SELECT points FROM user_gamification WHERE user_id = ?)";
        
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        
        int rank = -1;
        if (rs.next()) {
            rank = rs.getInt("rank");
        }
        
        rs.close();
        ps.close();
        return rank;
    }

    /**
     * Maps a ResultSet to a UserGamification object.
     * 
     * @param rs The ResultSet to map
     * @return UserGamification object
     * @throws SQLException if database operation fails
     */
    private UserGamification mapResultSetToGamification(ResultSet rs) throws SQLException {
        UserGamification gamification = new UserGamification(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("points"),
                rs.getString("level"),
                rs.getString("badges_json"),
                rs.getInt("posts_count"),
                rs.getInt("comments_count"),
                rs.getInt("likes_received")
        );
        return gamification;
    }

    /**
     * Deletes a gamification record for a user.
     * Should be called when a user is deleted.
     * 
     * @param userId The ID of the user
     * @throws SQLException if database operation fails
     */
    public void deleteGamification(int userId) throws SQLException {
        String sql = "DELETE FROM user_gamification WHERE user_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
        ps.close();
        
        System.out.println("Gamification record deleted for user ID: " + userId);
    }

    /**
     * Gets all gamification records (for admin purposes).
     * 
     * @return List of all UserGamification objects
     * @throws SQLException if database operation fails
     */
    public List<UserGamification> getAllGamification() throws SQLException {
        String sql = "SELECT * FROM user_gamification ORDER BY points DESC";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        
        List<UserGamification> allGamification = new ArrayList<>();
        while (rs.next()) {
            allGamification.add(mapResultSetToGamification(rs));
        }
        
        rs.close();
        st.close();
        return allGamification;
    }
}
