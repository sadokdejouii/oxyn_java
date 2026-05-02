package org.example.entities;

import java.time.LocalDateTime;

public class GymRating {
    private int id;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
    private int userId;
    private int gymnasiumId;

    public GymRating() {}

    // Constructeur pour création (sans id ni createdAt)
    public GymRating(int rating, String comment, int userId, int gymnasiumId) {
        this.rating = rating;
        this.comment = comment;
        this.userId = userId;
        this.gymnasiumId = gymnasiumId;
        this.createdAt = LocalDateTime.now();
    }

    // Constructeur complet
    public GymRating(int id, int rating, String comment, LocalDateTime createdAt, int userId, int gymnasiumId) {
        this.id = id;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.userId = userId;
        this.gymnasiumId = gymnasiumId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRating() { return rating; }
    public void setRating(int rating) {
        if (rating < 1 || rating > 5)
            throw new IllegalArgumentException("La note doit être entre 1 et 5");
        this.rating = rating;
    }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getGymnasiumId() { return gymnasiumId; }
    public void setGymnasiumId(int gymnasiumId) { this.gymnasiumId = gymnasiumId; }

    @Override
    public String toString() {
        return "GymRating{id=" + id + ", rating=" + rating +
               ", comment='" + comment + "', createdAt=" + createdAt +
               ", userId=" + userId + ", gymnasiumId=" + gymnasiumId + "}";
    }
}
