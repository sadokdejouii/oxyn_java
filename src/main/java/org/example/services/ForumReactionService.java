package org.example.services;

import org.example.entities.ForumReaction;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.*;

public class ForumReactionService implements ICrud<ForumReaction> {

    Connection con;

    public ForumReactionService() {
        con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(ForumReaction reaction) throws SQLException {
        // First check if user already reacted with this emoji
        String checkSql = "SELECT id FROM forum_reactions WHERE post_id = ? AND user_id = ? AND emoji = ?";
        PreparedStatement checkPs = con.prepareStatement(checkSql);
        checkPs.setInt(1, reaction.getPostId());
        checkPs.setInt(2, reaction.getUserId());
        checkPs.setString(3, reaction.getEmoji());
        ResultSet rs = checkPs.executeQuery();

        if (rs.next()) {
            // Already reacted, so remove the reaction
            supprimer(rs.getInt("id"));
        } else {
            // Add new reaction
            String sql = "INSERT INTO forum_reactions (post_id, user_id, emoji, created_at) " +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, reaction.getPostId());
            ps.setInt(2, reaction.getUserId());
            ps.setString(3, reaction.getEmoji());
            ps.setTimestamp(4, new Timestamp(reaction.getCreatedAt().getTime()));

            ps.executeUpdate();
            ps.close();
            System.out.println("Forum reaction ajouté avec succès!");
        }

        rs.close();
        checkPs.close();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM forum_reactions WHERE id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
        System.out.println("Forum reaction supprimé avec succès!");
    }

    @Override
    public List<ForumReaction> afficher() throws SQLException {
        List<ForumReaction> reactions = new ArrayList<>();

        String sql = "SELECT * FROM forum_reactions";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            ForumReaction reaction = new ForumReaction();
            reaction.setId(rs.getInt("id"));
            reaction.setPostId(rs.getInt("post_id"));
            reaction.setUserId(rs.getInt("user_id"));
            reaction.setEmoji(rs.getString("emoji"));
            reaction.setCreatedAt(rs.getTimestamp("created_at"));

            reactions.add(reaction);
        }

        rs.close();
        st.close();
        return reactions;
    }

    public Map<String, Integer> getReactionsByPostId(int postId) throws SQLException {
        Map<String, Integer> reactions = new LinkedHashMap<>();

        String sql = "SELECT emoji, COUNT(*) as count FROM forum_reactions WHERE post_id = ? GROUP BY emoji ORDER BY count DESC";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, postId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            reactions.put(rs.getString("emoji"), rs.getInt("count"));
        }

        rs.close();
        ps.close();
        return reactions;
    }

    public boolean hasUserReacted(int postId, int userId, String emoji) throws SQLException {
        String sql = "SELECT id FROM forum_reactions WHERE post_id = ? AND user_id = ? AND emoji = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, postId);
        ps.setInt(2, userId);
        ps.setString(3, emoji);
        ResultSet rs = ps.executeQuery();

        boolean exists = rs.next();

        rs.close();
        ps.close();
        return exists;
    }

    @Override
    public void modifier(int id) throws SQLException {
        // Implementation if needed
    }
}
