package org.example.entities;

import java.util.Date;

public class ForumReaction {

    private int id;
    private int postId;
    private int userId;
    private String emoji;
    private Date createdAt;

    // Constructeur par défaut
    public ForumReaction() {}

    // Constructeur paramétré (sans id)
    public ForumReaction(int postId, int userId, String emoji, Date createdAt) {
        this.postId = postId;
        this.userId = userId;
        this.emoji = emoji;
        this.createdAt = createdAt;
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

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ForumReaction{" +
                "id=" + id +
                ", postId=" + postId +
                ", emoji='" + emoji + '\'' +
                '}';
    }
}
