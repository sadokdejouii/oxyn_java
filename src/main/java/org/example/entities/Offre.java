package org.example.entities;

/**
 * Entité représentant une offre d'abonnement dans une salle de sport
 * Correspond à la table "offres" en base de données
 */
public class Offre {
    
    private int id;
    private String nom;
    private double prix;
    private int salleId;
    private String description;
    private boolean active;
    private java.sql.Timestamp createdAt;
    private java.sql.Timestamp updatedAt;
    
    // Constructeurs
    
    /**
     * Constructeur par défaut
     */
    public Offre() {
        this.active = true;
    }
    
    /**
     * Constructeur avec paramètres essentiels
     */
    public Offre(String nom, double prix, int salleId) {
        this();
        this.nom = nom;
        this.prix = prix;
        this.salleId = salleId;
    }
    
    /**
     * Constructeur complet
     */
    public Offre(int id, String nom, double prix, int salleId, String description, 
                 boolean active, java.sql.Timestamp createdAt, java.sql.Timestamp updatedAt) {
        this.id = id;
        this.nom = nom;
        this.prix = prix;
        this.salleId = salleId;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters et Setters
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getNom() {
        return nom;
    }
    
    public void setNom(String nom) {
        this.nom = nom;
    }
    
    public double getPrix() {
        return prix;
    }
    
    public void setPrix(double prix) {
        this.prix = prix;
    }
    
    public int getSalleId() {
        return salleId;
    }
    
    public void setSalleId(int salleId) {
        this.salleId = salleId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(java.sql.Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public java.sql.Timestamp getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(java.sql.Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Méthodes utilitaires
    
    /**
     * Retourne le prix formaté en TND
     */
    public String getPrixFormate() {
        return String.format("%.2f TND", prix);
    }
    
    /**
     * Retourne une représentation textuelle de l'offre
     */
    @Override
    public String toString() {
        return "Offre{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prix=" + prix +
                ", salleId=" + salleId +
                ", active=" + active +
                '}';
    }
    
    /**
     * Compare deux offres par ID
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Offre offre = (Offre) o;
        return id == offre.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
    
    /**
     * Validation des données de l'offre
     */
    public boolean isValid() {
        return nom != null && !nom.trim().isEmpty() && 
               prix > 0 && 
               salleId > 0;
    }
    
    /**
     * Retourne le statut de l'offre sous forme de texte
     */
    public String getStatutTexte() {
        return active ? "Active" : "Inactive";
    }
    
    /**
     * Clone l'offre (copie superficielle)
     */
    public Offre clone() {
        Offre clone = new Offre();
        clone.setId(this.id);
        clone.setNom(this.nom);
        clone.setPrix(this.prix);
        clone.setSalleId(this.salleId);
        clone.setDescription(this.description);
        clone.setActive(this.active);
        clone.setCreatedAt(this.createdAt);
        clone.setUpdatedAt(this.updatedAt);
        return clone;
    }
}
