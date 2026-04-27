package org.example.entities;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Entity representing user recommendation data.
 * One-to-One relationship with User table.
 * Stores category interaction scores and preferences for personalized recommendations.
 */
public class UserRecommendation {

    private int id;
    private int userId;
    private String categoryScoresJson; // JSON string representing category scores (Map<String, Integer>)
    private int totalInteractions;
    private String lastUpdated;

    // Constructor for creating new recommendation record
    public UserRecommendation(int userId) {
        this.userId = userId;
        this.categoryScoresJson = "{}"; // Empty JSON object
        this.totalInteractions = 0;
        this.lastUpdated = java.time.LocalDateTime.now().toString();
    }

    // Full constructor
    public UserRecommendation(int id, int userId, String categoryScoresJson, 
                              int totalInteractions, String lastUpdated) {
        this.id = id;
        this.userId = userId;
        this.categoryScoresJson = categoryScoresJson;
        this.totalInteractions = totalInteractions;
        this.lastUpdated = lastUpdated;
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

    public String getCategoryScoresJson() {
        return categoryScoresJson;
    }

    public void setCategoryScoresJson(String categoryScoresJson) {
        this.categoryScoresJson = categoryScoresJson;
    }

    /**
     * Get category scores as a Map<String, Integer>
     */
    public Map<String, Integer> getCategoryScores() {
        if (categoryScoresJson == null || categoryScoresJson.isEmpty()) {
            return new HashMap<>();
        }
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, Integer>>(){}.getType();
        return gson.fromJson(categoryScoresJson, mapType);
    }

    /**
     * Set category scores from a Map<String, Integer>
     */
    public void setCategoryScores(Map<String, Integer> scores) {
        Gson gson = new Gson();
        this.categoryScoresJson = gson.toJson(scores);
        this.lastUpdated = java.time.LocalDateTime.now().toString();
    }

    /**
     * Get score for a specific category
     */
    public int getCategoryScore(String category) {
        Map<String, Integer> scores = getCategoryScores();
        return scores.getOrDefault(category, 0);
    }

    /**
     * Increment score for a specific category
     */
    public void incrementCategoryScore(String category, int amount) {
        Map<String, Integer> scores = getCategoryScores();
        scores.put(category, scores.getOrDefault(category, 0) + amount);
        setCategoryScores(scores);
        this.totalInteractions += amount;
    }

    public int getTotalInteractions() {
        return totalInteractions;
    }

    public void setTotalInteractions(int totalInteractions) {
        this.totalInteractions = totalInteractions;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Get top recommended categories based on scores
     */
    public Map<String, Integer> getTopCategories(int limit) {
        Map<String, Integer> scores = getCategoryScores();
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
    }

    @Override
    public String toString() {
        return "UserRecommendation{" +
                "id=" + id +
                ", userId=" + userId +
                ", categoryScores=" + getCategoryScores() +
                ", totalInteractions=" + totalInteractions +
                ", lastUpdated='" + lastUpdated + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRecommendation)) return false;
        UserRecommendation that = (UserRecommendation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
