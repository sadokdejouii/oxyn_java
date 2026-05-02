package org.example.entities;

import java.time.LocalDateTime;

/**
 * Entity representing a like relationship between a user and a post.
 * Ensures a user can only like a post once.
 */
public class PostLike {
    private int id;
    private int userId;
    private int postId;
    private LocalDateTime likedAt;

    public PostLike() {}

    public PostLike(int userId, int postId) {
        this.userId = userId;
        this.postId = postId;
        this.likedAt = LocalDateTime.now();
    }

    public PostLike(int id, int userId, int postId, LocalDateTime likedAt) {
        this.id = id;
        this.userId = userId;
        this.postId = postId;
        this.likedAt = likedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public LocalDateTime getLikedAt() {
        return likedAt;
    }

    public void setLikedAt(LocalDateTime likedAt) {
        this.likedAt = likedAt;
    }

    @Override
    public String toString() {
        return "PostLike{" +
                "id=" + id +
                ", userId=" + userId +
                ", postId=" + postId +
                ", likedAt=" + likedAt +
                '}';
    }
}
