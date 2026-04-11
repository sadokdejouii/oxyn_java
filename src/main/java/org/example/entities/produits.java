package org.example.entities;

import java.math.BigDecimal;
import java.util.Date;

public class produits {

    public Object getQuantite_stock_produit;
    protected int id_produit;
    protected String nom_produit;
    protected String description_produit;
    protected double prix_produit;
    protected int quantite_stock_produit;
    protected String image_produit;
    protected String date_creation_produit;
    protected String statut_produit;

    // Constructeur vide
    public produits() {
    }

    // Constructeur avec id
    public produits(int id_produit, String nom_produit, String description_produit,
                    double prix_produit, int quantite_stock_produit,
                    String image_produit, String date_creation_produit,
                    String statut_produit) {

        this.id_produit = id_produit;
        this.nom_produit = nom_produit;
        this.description_produit = description_produit;
        this.prix_produit = prix_produit;
        this.quantite_stock_produit = quantite_stock_produit;
        this.image_produit = image_produit;
        this.date_creation_produit = date_creation_produit;
        this.statut_produit = statut_produit;
    }

    // Constructeur sans id
    public produits(String nom_produit, String description_produit,
                    double prix_produit, int quantite_stock_produit,
                    String image_produit, String date_creation_produit,
                    String statut_produit) {

        this.nom_produit = nom_produit;
        this.description_produit = description_produit;
        this.prix_produit = prix_produit;
        this.quantite_stock_produit = quantite_stock_produit;
        this.image_produit = image_produit;
        this.date_creation_produit = date_creation_produit;
        this.statut_produit = statut_produit;
    }


    // Getters & Setters


    public int getId_produit() {
        return id_produit;
    }

    public void setId_produit(int id_produit) {
        this.id_produit = id_produit;
    }

    public String getNom_produit() {
        return nom_produit;
    }

    public void setNom_produit(String nom_produit) {
        this.nom_produit = nom_produit;
    }

    public String getDescription_produit() {
        return description_produit;
    }

    public void setDescription_produit(String description_produit) {
        this.description_produit = description_produit;
    }

    public double getPrix_produit() {
        return prix_produit;
    }

    public void setPrix_produit(double prix_produit) {
        this.prix_produit = prix_produit;
    }

    public int getQuantite_stock_produit() {
        return quantite_stock_produit;
    }

    public void setQuantite_stock_produit(int quantite_stock_produit) {
        this.quantite_stock_produit = quantite_stock_produit;
    }

    public String getImage_produit() {
        return image_produit;
    }

    public void setImage_produit(String image_produit) {
        this.image_produit = image_produit;
    }

    public String getDate_creation_produit() {
        return date_creation_produit;
    }

    public void setDate_creation_produit(String date_creation_produit) {
        this.date_creation_produit = date_creation_produit;
    }

    public String getStatut_produit() {
        return statut_produit;
    }

    public void setStatut_produit(String statut_produit) {
        this.statut_produit = statut_produit;
    }

    @Override
    public String toString() {
        return "produits{" +
                "getQuantite_stock_produit=" + getQuantite_stock_produit +
                ", id_produit=" + id_produit +
                ", nom_produit='" + nom_produit + '\'' +
                ", description_produit='" + description_produit + '\'' +
                ", prix_produit=" + prix_produit +
                ", quantite_stock_produit=" + quantite_stock_produit +
                ", image_produit='" + image_produit + '\'' +
                ", date_creation_produit='" + date_creation_produit + '\'' +
                ", statut_produit='" + statut_produit + '\'' +
                '}';
    }
}
