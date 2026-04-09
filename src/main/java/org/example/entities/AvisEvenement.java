package org.example.entities;

import java.util.Date;

public class AvisEvenement {

    // attributs
    private int id;
    private int note;
    private String commentaire;
    private Date createdAt;
    private int idEvenement;
    private int idUser;

    // constructeur par défaut
    public AvisEvenement() {
    }

    // constructeur paramétré (sans id)
    public AvisEvenement(int note, String commentaire, Date createdAt, int idEvenement, int idUser) {
        this.note = note;
        this.commentaire = commentaire;
        this.createdAt = createdAt;
        this.idEvenement = idEvenement;
        this.idUser = idUser;
    }

    // getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getIdEvenement() {
        return idEvenement;
    }

    public void setIdEvenement(int idEvenement) {
        this.idEvenement = idEvenement;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    // toString
    @Override
    public String toString() {
        return "AvisEvenement{" +
                "id=" + id +
                ", note=" + note +
                ", commentaire='" + commentaire + '\'' +
                ", createdAt=" + createdAt +
                ", idEvenement=" + idEvenement +
                ", idUser=" + idUser +
                '}';
    }
}