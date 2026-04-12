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
        String sql = "INSERT INTO comment (content_comment, created_at_comment, updated_at_comment, like_count, is_edited, id_author_comment, post_id, parent_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, comment.getContent_comment());
        ps.setTimestamp(2, new Timestamp(comment.getCreated_at_comment().getTime()));
        ps.setTimestamp(3, new Timestamp(comment.getCreated_at_comment().getTime()));
        ps.setInt(4, 0); // like_count starts at 0
        ps.setBoolean(5, false); // is_edited starts as false
        ps.setInt(6, comment.getId_author_comment());
        ps.setInt(7, comment.getPost_id());
        ps.setObject(8, comment.getParent_id()); // Can be null

        ps.executeUpdate();

        // Get generated ID
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            comment.setId_comment(rs.getInt(1));
        }

        rs.close();
        ps.close();
        System.out.println("Forum comment ajouté avec succès!");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM comment WHERE id_comment = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
        System.out.println("Forum comment supprimé avec succès!");
    }

    @Override
    public List<ForumComment> afficher() throws SQLException {
        List<ForumComment> comments = new ArrayList<>();

        String sql = "SELECT * FROM comment ORDER BY created_at_comment DESC";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            comments.add(mapResultSetToComment(rs));
        }

        rs.close();
        st.close();
        return comments;
    }

    public List<ForumComment> getCommentsByPostId(int postId) throws SQLException {
        List<ForumComment> comments = new ArrayList<>();

        String sql = "SELECT * FROM comment WHERE post_id = ? AND parent_id IS NULL ORDER BY created_at_comment DESC";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, postId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            comments.add(mapResultSetToComment(rs));
        }

        rs.close();
        ps.close();
        return comments;
    }

    public List<ForumComment> getRepliesByCommentId(int commentId) throws SQLException {
        List<ForumComment> replies = new ArrayList<>();

        String sql = "SELECT * FROM comment WHERE parent_id = ? ORDER BY created_at_comment ASC";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, commentId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            replies.add(mapResultSetToComment(rs));
        }

        rs.close();
        ps.close();
        return replies;
    }

    public void likeComment(int commentId) throws SQLException {
        String sql = "UPDATE comment SET like_count = like_count + 1 WHERE id_comment = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, commentId);
        ps.executeUpdate();
        ps.close();
    }

    public void updateComment(int commentId, String newContent) throws SQLException {
        String sql = "UPDATE comment SET content_comment = ?, updated_at_comment = ?, is_edited = ? WHERE id_comment = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, newContent);
        ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        ps.setBoolean(3, true);
        ps.setInt(4, commentId);
        ps.executeUpdate();
        ps.close();
    }

    private ForumComment mapResultSetToComment(ResultSet rs) throws SQLException {
        ForumComment comment = new ForumComment();
        comment.setId_comment(rs.getInt("id_comment"));
        comment.setContent_comment(rs.getString("content_comment"));
        comment.setCreated_at_comment(rs.getTimestamp("created_at_comment"));
        comment.setUpdated_at_comment(rs.getTimestamp("updated_at_comment"));
        comment.setLike_count(rs.getInt("like_count"));
        comment.setIs_edited(rs.getBoolean("is_edited"));
        comment.setId_author_comment(rs.getInt("id_author_comment"));
        comment.setPost_id(rs.getInt("post_id"));

        int parentId = rs.getInt("parent_id");
        if (!rs.wasNull()) {
            comment.setParent_id(parentId);
        }

        return comment;
    }

    @Override
    public void modifier(int id) throws SQLException {
        // Not used - use updateComment instead
    }
}
