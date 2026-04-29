package org.example.services;

import org.example.entities.Post;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ForumPostService implements ICrud<Post> {

    Connection con;

    public ForumPostService() {
        con = MyDataBase.getConnection();
    }

    @Override
    public void ajouter(Post post) throws SQLException {
        String sql = "INSERT INTO `post`(`content_post`, `media_url_post`, `media_type_post`, `visibility_post`, `created_at_post`, `updated_at_post`, `like_count_post`, `category_post`, `id_author_post`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, post.getContent_post());
        ps.setString(2, post.getMedia_url_post());
        ps.setString(3, post.getMedia_type_post());
        ps.setString(4, post.getVisibility_post());
        ps.setString(5, post.getCreated_at_post());
        ps.setString(6, post.getUpdated_at_post());
        ps.setInt(7, post.getLike_count_post());
        ps.setString(8, post.getCategory_post());
        ps.setInt(9, post.getId_author_post());

        ps.executeUpdate();
        ps.close();
        System.out.println("Post added successfully.");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM `post` WHERE `id_post` = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        int rows = ps.executeUpdate();
        ps.close();
        if (rows > 0) {
            System.out.println("Post deleted successfully.");
        } else {
            System.out.println("No post found with id: " + id);
        }
    }

    @Override
    public List<Post> afficher() throws SQLException {
        List<Post> posts = new ArrayList<>();

        String sql = "SELECT * FROM `post`";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Post post = new Post(
                    rs.getString("content_post"),
                    rs.getString("media_url_post"),
                    rs.getString("media_type_post"),
                    rs.getString("visibility_post"),
                    rs.getString("created_at_post"),
                    rs.getString("updated_at_post"),
                    rs.getInt("like_count_post"),
                    rs.getString("category_post"),
                    rs.getInt("id_author_post")
            );
            post.setId_post(rs.getInt("id_post"));
            // Fetch BLOB data from media_url_post column (now stores BLOB)
            Blob mediaBlob = rs.getBlob("media_url_post");
            if (mediaBlob != null) {
                post.setMedia_blob_post(mediaBlob.getBytes(1, (int) mediaBlob.length()));
            }
            posts.add(post);
        }

        rs.close();
        st.close();
        return posts;
    }

    public Post getPostById(int id) throws SQLException {
        String sql = "SELECT * FROM `post` WHERE `id_post` = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        Post post = null;
        if (rs.next()) {
            post = new Post(
                    rs.getString("content_post"),
                    rs.getString("media_url_post"),
                    rs.getString("media_type_post"),
                    rs.getString("visibility_post"),
                    rs.getString("created_at_post"),
                    rs.getString("updated_at_post"),
                    rs.getInt("like_count_post"),
                    rs.getString("category_post"),
                    rs.getInt("id_author_post")
            );
            post.setId_post(rs.getInt("id_post"));
            // Fetch BLOB data from media_url_post column (now stores BLOB)
            Blob mediaBlob = rs.getBlob("media_url_post");
            if (mediaBlob != null) {
                post.setMedia_blob_post(mediaBlob.getBytes(1, (int) mediaBlob.length()));
            }
        }

        rs.close();
        ps.close();
        return post;
    }

    public void updatePost(int id, Post post) throws SQLException {
        String sql = "UPDATE `post` SET `content_post`=?, `media_url_post`=?, `media_type_post`=?, `visibility_post`=?, `updated_at_post`=?, `like_count_post`=?, `category_post`=? WHERE `id_post`=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, post.getContent_post());
        ps.setString(2, post.getMedia_url_post());
        ps.setString(3, post.getMedia_type_post());
        ps.setString(4, post.getVisibility_post());
        ps.setString(5, post.getUpdated_at_post());
        ps.setInt(6, post.getLike_count_post());
        ps.setString(7, post.getCategory_post());
        ps.setInt(8, id);
        ps.executeUpdate();
        ps.close();
        System.out.println("Post updated successfully.");
    }

    @Override
    public void modifier(int id) throws SQLException {
        // Implementation if needed - use the overloaded modifier(int id, Post post) instead
    }

    public void updateLikeCount(long postId) throws SQLException {
        String sql = "UPDATE `post` SET `like_count_post` = `like_count_post` + 1 WHERE `id_post` = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setLong(1, postId);
        ps.executeUpdate();
        ps.close();
    }
}
