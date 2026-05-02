package org.example.services;

import org.example.entities.PostLike;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing post likes.
 * Ensures users can only like a post once and provides toggle functionality.
 */
public class PostLikeService {

    private Connection con;

    public PostLikeService() {
        con = MyDataBase.getInstance().getConnection();
    }

    /**
     * Check if a user has liked a specific post.
     *
     * @param userId The user ID
     * @param postId The post ID
     * @return true if the user has liked the post, false otherwise
     */
    public boolean hasUserLikedPost(int userId, int postId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM post_likes WHERE user_id = ? AND post_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, postId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Add a like for a post. If the user already liked it, this will fail due to the unique constraint.
     *
     * @param userId The user ID
     * @param postId The post ID
     * @return true if the like was added, false if it already existed
     */
    public boolean addLike(int userId, int postId) throws SQLException {
        String sql = "INSERT INTO post_likes (user_id, post_id) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, postId);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            // Duplicate entry due to unique constraint
            if (e.getSQLState().equals("23000") || e.getErrorCode() == 1062) {
                return false;
            }
            throw e;
        }
    }

    /**
     * Remove a like for a post.
     *
     * @param userId The user ID
     * @param postId The post ID
     * @return true if the like was removed, false if it didn't exist
     */
    public boolean removeLike(int userId, int postId) throws SQLException {
        String sql = "DELETE FROM post_likes WHERE user_id = ? AND post_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, postId);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Toggle a like for a post. If the user has liked it, unlike it. If not, like it.
     *
     * @param userId The user ID
     * @param postId The post ID
     * @return true if the post is now liked, false if it is now unliked
     */
    public boolean toggleLike(int userId, int postId) throws SQLException {
        if (hasUserLikedPost(userId, postId)) {
            removeLike(userId, postId);
            return false;
        } else {
            addLike(userId, postId);
            return true;
        }
    }

    /**
     * Get all post IDs that a user has liked.
     *
     * @param userId The user ID
     * @return List of post IDs the user has liked
     */
    public List<Integer> getLikedPostIdsByUser(int userId) throws SQLException {
        List<Integer> likedPostIds = new ArrayList<>();
        String sql = "SELECT post_id FROM post_likes WHERE user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    likedPostIds.add(rs.getInt("post_id"));
                }
            }
        }
        return likedPostIds;
    }

    /**
     * Get the count of likes for a specific post.
     *
     * @param postId The post ID
     * @return The number of likes
     */
    public int getLikeCount(int postId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM post_likes WHERE post_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Update the like count in the posts table.
     * This should be called after adding or removing a like.
     *
     * @param postId The post ID
     */
    public void updatePostLikeCount(int postId) throws SQLException {
        int count = getLikeCount(postId);
        String sql = "UPDATE post SET like_count_post = ? WHERE id_post = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, count);
            ps.setInt(2, postId);
            ps.executeUpdate();
        }
    }
}
