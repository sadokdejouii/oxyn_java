package org.example.entities;

import java.util.Date;

public class ForumComment {

    private int id;
    private int postId;
    private int userId;
    private String username;
    private String userAvatar;
    private String content;
    private Date createdAt;
    private Date updatedAt;

    // Constructeur par défaut
    public ForumComment() {}

    // Constructeur paramétré (sans id)
    public ForumComment(int postId, int userId, String username, String userAvatar,
                       String content, Date createdAt) {
        this.postId = postId;
        this.userId = userId;
        this.username = username;
        this.userAvatar = userAvatar;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    // GETTERS & SETTERS

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ForumComment{" +
                "id=" + id +
                ", postId=" + postId +
                ", username='" + username + '\'' +
                ", content='" + content + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
