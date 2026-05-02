package org.example.entities;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing user gamification data.
 * One-to-One relationship with User table.
 * Stores points, levels, badges, and activity statistics.
 */
public class UserGamification {

    private int id;
    private int userId;
    private int points;
    private String level;
    private String badgesJson; // JSON string representing list of badges
    private int postsCount;
    private int commentsCount;
    private int likesReceived;

    // Constructor for creating new gamification record
    public UserGamification(int userId) {
        this.userId = userId;
        this.points = 0;
        this.level = "Beginner";
        this.badgesJson = "[]"; // Empty JSON array
        this.postsCount = 0;
        this.commentsCount = 0;
        this.likesReceived = 0;
    }

    // Full constructor
    public UserGamification(int id, int userId, int points, String level, 
                           String badgesJson, int postsCount, int commentsCount, int likesReceived) {
        this.id = id;
        this.userId = userId;
        this.points = points;
        this.level = level;
        this.badgesJson = badgesJson;
        this.postsCount = postsCount;
        this.commentsCount = commentsCount;
        this.likesReceived = likesReceived;
    }

    // Getters and Setters
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

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getBadgesJson() {
        return badgesJson;
    }

    public void setBadgesJson(String badgesJson) {
        this.badgesJson = badgesJson;
    }

    /**
     * Get badges as a List<String>
     */
    public List<String> getBadges() {
        if (badgesJson == null || badgesJson.isEmpty()) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Type listType = new TypeToken<List<String>>(){}.getType();
        return gson.fromJson(badgesJson, listType);
    }

    /**
     * Set badges from a List<String>
     */
    public void setBadges(List<String> badges) {
        Gson gson = new Gson();
        this.badgesJson = gson.toJson(badges);
    }

    /**
     * Add a single badge to the badges list
     */
    public void addBadge(String badge) {
        List<String> badges = getBadges();
        if (!badges.contains(badge)) {
            badges.add(badge);
            setBadges(badges);
        }
    }

    public int getPostsCount() {
        return postsCount;
    }

    public void setPostsCount(int postsCount) {
        this.postsCount = postsCount;
    }

    public int getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }

    public int getLikesReceived() {
        return likesReceived;
    }

    public void setLikesReceived(int likesReceived) {
        this.likesReceived = likesReceived;
    }

    @Override
    public String toString() {
        return "UserGamification{" +
                "id=" + id +
                ", userId=" + userId +
                ", points=" + points +
                ", level='" + level + '\'' +
                ", badges=" + getBadges() +
                ", postsCount=" + postsCount +
                ", commentsCount=" + commentsCount +
                ", likesReceived=" + likesReceived +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserGamification)) return false;
        UserGamification that = (UserGamification) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
