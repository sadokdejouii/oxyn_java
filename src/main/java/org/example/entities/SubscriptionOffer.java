package org.example.entities;

import java.sql.Timestamp;

/**
 * Entité complète pour les offres d'abonnement
 */
public class SubscriptionOffer {
    private int id;
    private Integer gymnasiumId;
    private String gymnasiumName;
    private String name;
    private int durationMonths;
    private double price;
    private String description;
    private boolean active;
    private Timestamp createdAt;

    // Constructeur vide
    public SubscriptionOffer() {}

    // Constructeur complet
    public SubscriptionOffer(int gymnasiumId, String name, int durationMonths, double price, String description) {
        this.gymnasiumId = gymnasiumId;
        this.name = name;
        this.durationMonths = durationMonths;
        this.price = price;
        this.description = description;
        this.active = true;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getGymnasiumId() { return gymnasiumId; }
    public void setGymnasiumId(Integer gymnasiumId) { this.gymnasiumId = gymnasiumId; }

    public String getGymnasiumName() { return gymnasiumName; }
    public void setGymnasiumName(String gymnasiumName) { this.gymnasiumName = gymnasiumName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDurationMonths() { return durationMonths; }
    public void setDurationMonths(int durationMonths) { this.durationMonths = durationMonths; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "SubscriptionOffer{" +
                "id=" + id +
                ", gymnasiumId=" + gymnasiumId +
                ", gymnasiumName='" + gymnasiumName + '\'' +
                ", name='" + name + '\'' +
                ", durationMonths=" + durationMonths +
                ", price=" + price +
                ", description='" + description + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}
