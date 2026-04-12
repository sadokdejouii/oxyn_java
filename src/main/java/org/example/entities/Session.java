package org.example.entities;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class Session {

    private int id;
    private String title;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private int capacity;
    private double price;
    private boolean active;
    private Timestamp createdAt;
    private Integer gymnasiumId;
    private Integer coachUserId;

    // Non-persisted — loaded via JOIN
    private String gymnasiumName;
    private int placesRestantes;

    public Session() {}

    public Session(String title, String description, LocalDateTime startAt, LocalDateTime endAt,
                   int capacity, double price, Integer gymnasiumId, Integer coachUserId) {
        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.capacity = capacity;
        this.price = price;
        this.active = true;
        this.gymnasiumId = gymnasiumId;
        this.coachUserId = coachUserId;
        this.placesRestantes = capacity;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Integer getGymnasiumId() { return gymnasiumId; }
    public void setGymnasiumId(Integer gymnasiumId) { this.gymnasiumId = gymnasiumId; }
    public Integer getCoachUserId() { return coachUserId; }
    public void setCoachUserId(Integer coachUserId) { this.coachUserId = coachUserId; }
    public String getGymnasiumName() { return gymnasiumName; }
    public void setGymnasiumName(String gymnasiumName) { this.gymnasiumName = gymnasiumName; }
    public int getPlacesRestantes() { return placesRestantes; }
    public void setPlacesRestantes(int placesRestantes) { this.placesRestantes = placesRestantes; }

    public String getStatut() {
        if (!active) return "INACTIVE";
        return placesRestantes <= 0 ? "COMPLETE" : "OUVERTE";
    }
}
