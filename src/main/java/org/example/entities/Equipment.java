package org.example.entities;

import java.sql.Timestamp;

public class Equipment {
    private int id;
    private String name;
    private String description;
    private int quantity;
    private Timestamp createdAt;
    private Integer gymnasiumId;  // ← gymnasium_id pas salle_id !
    private boolean active;
    private String gymnasiumName;

    public Equipment() {}

    public Equipment(String name, String description, int quantity, Integer gymnasiumId) {
        this.name = name; 
        this.description = description; 
        this.quantity = quantity;
        this.gymnasiumId = gymnasiumId; 
        this.active = true;
    }

    // Getters/Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public Integer getGymnasiumId() { return gymnasiumId; }
    public void setGymnasiumId(Integer gymnasiumId) { this.gymnasiumId = gymnasiumId; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public String getGymnasiumName() { return gymnasiumName; }
    public void setGymnasiumName(String gymnasiumName) { this.gymnasiumName = gymnasiumName; }
}
