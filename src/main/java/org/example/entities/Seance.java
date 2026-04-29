package org.example.entities;

import java.time.LocalDateTime;

/**
 * Entité représentant une séance de sport dans une salle
 */
public class Seance {

    private int id;
    private int salleId;
    private String nom;
    private LocalDateTime date;
    private LocalDateTime heureDebut;
    private LocalDateTime heureFin;
    private String coach;
    private int maxParticipants;
    private String couleur;

    // Constructeurs
    public Seance() {
        this.maxParticipants = 0;
        this.couleur = "#64748b"; // Couleur par défaut (gris)
    }

    public Seance(int salleId, String nom, LocalDateTime date, LocalDateTime heureDebut, LocalDateTime heureFin) {
        this.salleId = salleId;
        this.nom = nom;
        this.date = date;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.maxParticipants = 0;
        this.couleur = "#64748b";
    }

    public Seance(int salleId, String nom, LocalDateTime date, LocalDateTime heureDebut, LocalDateTime heureFin, String coach, int maxParticipants, String couleur) {
        this.salleId = salleId;
        this.nom = nom;
        this.date = date;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.coach = coach;
        this.maxParticipants = maxParticipants;
        this.couleur = couleur != null ? couleur : "#64748b";
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSalleId() {
        return salleId;
    }

    public void setSalleId(int salleId) {
        this.salleId = salleId;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public LocalDateTime getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(LocalDateTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    public LocalDateTime getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(LocalDateTime heureFin) {
        this.heureFin = heureFin;
    }

    public String getCoach() {
        return coach;
    }

    public void setCoach(String coach) {
        this.coach = coach;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    // Méthodes utilitaires

    /**
     * Retourne la durée de la séance en minutes
     * @return Durée en minutes
     */
    public int getDureeMinutes() {
        if (heureDebut != null && heureFin != null) {
            return (int) java.time.Duration.between(heureDebut, heureFin).toMinutes();
        }
        return 0;
    }

    /**
     * Retourne la durée formatée (ex: "1h30", "45min")
     * @return Durée formatée
     */
    public String getDureeFormatee() {
        int minutes = getDureeMinutes();
        if (minutes >= 60) {
            int heures = minutes / 60;
            int mins = minutes % 60;
            if (mins > 0) {
                return heures + "h" + mins;
            } else {
                return heures + "h";
            }
        } else {
            return minutes + "min";
        }
    }

    /**
     * Vérifie si la séance est valide
     * @return true si valide
     */
    public boolean isValid() {
        return nom != null && !nom.trim().isEmpty() 
               && date != null 
               && heureDebut != null 
               && heureFin != null 
               && heureDebut.isBefore(heureFin)
               && salleId > 0;
    }

    /**
     * Retourne le type de séance pour le style CSS
     * @return Type de séance (yoga, combat, cardio, etc.)
     */
    public String getTypeSeance() {
        if (nom == null) return "default";
        
        String lowerNom = nom.toLowerCase();
        if (lowerNom.contains("yoga") || lowerNom.contains("pilates")) return "yoga";
        if (lowerNom.contains("box") || lowerNom.contains("combat") || lowerNom.contains("karate")) return "combat";
        if (lowerNom.contains("cardio") || lowerNom.contains("hiit")) return "cardio";
        if (lowerNom.contains("muscu") || lowerNom.contains("musculation") || lowerNom.contains("force")) return "muscu";
        if (lowerNom.contains("danse") || lowerNom.contains("zumba")) return "danse";
        if (lowerNom.contains("fitness") || lowerNom.contains("aérobic")) return "fitness";
        
        return "default";
    }

    @Override
    public String toString() {
        return "Seance{" +
                "id=" + id +
                ", salleId=" + salleId +
                ", nom='" + nom + '\'' +
                ", date=" + date +
                ", heureDebut=" + heureDebut +
                ", heureFin=" + heureFin +
                ", coach='" + coach + '\'' +
                ", maxParticipants=" + maxParticipants +
                ", couleur='" + couleur + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Seance seance = (Seance) o;

        return id == seance.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
