package org.example.entities;

import java.util.Date;

public class ForumComment {

    private int id_comment;
    private String content_comment;
    private Date created_at_comment;
    private Date updated_at_comment;
    private int like_count;
    private boolean is_edited;
    private int id_author_comment;
    private int post_id;
    private Integer parent_id; // Can be null for top-level comments

    // Constructeur par défaut
    public ForumComment() {}

    // Constructeur paramétré pour ajout
    public ForumComment(String content_comment, int id_author_comment, int post_id) {
        this.content_comment = content_comment;
        this.id_author_comment = id_author_comment;
        this.post_id = post_id;
        this.created_at_comment = new Date();
        this.like_count = 0;
        this.is_edited = false;
    }

    // GETTERS & SETTERS

    public int getId_comment() {
        return id_comment;
    }

    public void setId_comment(int id_comment) {
        this.id_comment = id_comment;
    }

    public String getContent_comment() {
        return content_comment;
    }

    public void setContent_comment(String content_comment) {
        this.content_comment = content_comment;
    }

    public Date getCreated_at_comment() {
        return created_at_comment;
    }

    public void setCreated_at_comment(Date created_at_comment) {
        this.created_at_comment = created_at_comment;
    }

    public Date getUpdated_at_comment() {
        return updated_at_comment;
    }

    public void setUpdated_at_comment(Date updated_at_comment) {
        this.updated_at_comment = updated_at_comment;
    }

    public int getLike_count() {
        return like_count;
    }

    public void setLike_count(int like_count) {
        this.like_count = like_count;
    }

    public boolean isIs_edited() {
        return is_edited;
    }

    public void setIs_edited(boolean is_edited) {
        this.is_edited = is_edited;
    }

    public int getId_author_comment() {
        return id_author_comment;
    }

    public void setId_author_comment(int id_author_comment) {
        this.id_author_comment = id_author_comment;
    }

    public int getPost_id() {
        return post_id;
    }

    public void setPost_id(int post_id) {
        this.post_id = post_id;
    }

    public Integer getParent_id() {
        return parent_id;
    }

    public void setParent_id(Integer parent_id) {
        this.parent_id = parent_id;
    }

    // Legacy getters for compatibility
    public int getId() { return id_comment; }
    public int getPostId() { return post_id; }
    public int getUserId() { return id_author_comment; }
    public String getContent() { return content_comment; }
    public Date getCreatedAt() { return created_at_comment; }
    public Date getUpdatedAt() { return updated_at_comment; }

    @Override
    public String toString() {
        return "ForumComment{" +
                "id_comment=" + id_comment +
                ", post_id=" + post_id +
                ", id_author_comment=" + id_author_comment +
                ", content='" + content_comment + '\'' +
                ", created_at=" + created_at_comment +
                '}';
    }
}
