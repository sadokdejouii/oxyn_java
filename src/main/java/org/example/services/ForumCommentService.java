package org.example.services;

import org.example.entities.ForumComment;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ForumCommentService implements ICrud<ForumComment> {

    Connection con;

    public ForumCommentService() {
        con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(ForumComment comment) throws SQLException {
        String sql = "INSERT INTO forum_comments (post_id, user_id, username, user_avatar, content, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, comment.getPostId());
        ps.setInt(2, comment.getUserId());
        ps.setString(3, comment.getUsername());
        ps.setString(4, comment.getUserAvatar());
        ps.setString(5, comment.getContent());
        ps.setTimestamp(6, new Timestamp(comment.getCreatedAt().getTime()));

        ps.executeUpdate();
        ps.close();
        System.out.println("Forum comment ajouté avec succès!");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM forum_comments WHERE id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
        System.out.println("Forum comment supprimé avec succès!");
    }

    @Override
    public List<ForumComment> afficher() throws SQLException {
        List<ForumComment> comments = new ArrayList<>();

        String sql = "SELECT * FROM forum_comments ORDER BY created_at DESC";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            ForumComment comment = new ForumComment();
            comment.setId(rs.getInt("id"));
            comment.setPostId(rs.getInt("post_id"));
            comment.setUserId(rs.getInt("user_id"));
            comment.setUsername(rs.getString("username"));
            comment.setUserAvatar(rs.getString("user_avatar"));
            comment.setContent(rs.getString("content"));
            comment.setCreatedAt(rs.getTimestamp("created_at"));
            comment.setUpdatedAt(rs.getTimestamp("updated_at"));

            comments.add(comment);
        }

        rs.close();
        st.close();
        return comments;
    }

    public List<ForumComment> getCommentsByPostId(int postId) throws SQLException {
        List<ForumComment> comments = new ArrayList<>();

        String sql = "SELECT * FROM forum_comments WHERE post_id = ? ORDER BY created_at DESC";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, postId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            ForumComment comment = new ForumComment();
            comment.setId(rs.getInt("id"));
            comment.setPostId(rs.getInt("post_id"));
            comment.setUserId(rs.getInt("user_id"));
            comment.setUsername(rs.getString("username"));
            comment.setUserAvatar(rs.getString("user_avatar"));
            comment.setContent(rs.getString("content"));
            comment.setCreatedAt(rs.getTimestamp("created_at"));
            comment.setUpdatedAt(rs.getTimestamp("updated_at"));

            comments.add(comment);
        }

        rs.close();
        ps.close();
        return comments;
    }

    @Override
    public void modifier(int id) throws SQLException {
        // Implementation if needed
    }
}
