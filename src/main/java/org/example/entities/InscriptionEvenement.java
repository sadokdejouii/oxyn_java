package org.example.entities;

import java.util.Date;

public class InscriptionEvenement {

    // attributs
    private int id;
    private Date dateInscription;
    private String statut;
    private int idEvenement;
    private int idUser;

    // constructeur par défaut
    public InscriptionEvenement() {
    }

    // constructeur paramétré (sans id)
    public InscriptionEvenement(Date dateInscription, String statut, int idEvenement, int idUser) {
        this.dateInscription = dateInscription;
        this.statut = statut;
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


    public Date getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(Date dateInscription) {
        this.dateInscription = dateInscription;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
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
        return "InscriptionEvenement{" +
                "id=" + id +
                ", dateInscription=" + dateInscription +
                ", statut='" + statut + '\'' +
                ", idEvenement=" + idEvenement +
                ", idUser=" + idUser +
                '}';
    }
}