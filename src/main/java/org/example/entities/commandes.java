package org.example.entities;

public class commandes {
    protected int id_commande;
    protected String date_commande;
    protected double total_commande;
    protected String statut_commande;
    protected String mode_paiement_commande;
    protected int id_client_commande;
    protected String adresse_commande;

    // Constructeur vide
    public commandes() {
    }

    // Constructeur avec id
    public commandes(int id_commande, String date_commande, double total_commande,
                     String statut_commande, String mode_paiement_commande,
                     int id_client_commande, String adresse_commande) {

        this.id_commande = id_commande;
        this.date_commande = date_commande;
        this.total_commande = total_commande;
        this.statut_commande = statut_commande;
        this.mode_paiement_commande = mode_paiement_commande;
        this.id_client_commande = id_client_commande;
        this.adresse_commande = adresse_commande;
    }

    // Constructeur sans id
    public commandes(String date_commande, double total_commande,
                     String statut_commande, String mode_paiement_commande,
                     int id_client_commande, String adresse_commande) {

        this.date_commande = date_commande;
        this.total_commande = total_commande;
        this.statut_commande = statut_commande;
        this.mode_paiement_commande = mode_paiement_commande;
        this.id_client_commande = id_client_commande;
        this.adresse_commande = adresse_commande;
    }

    public int getId_commande() {
        return id_commande;
    }

    public void setId_commande(int id_commande) {
        this.id_commande = id_commande;
    }

    public String getDate_commande() {
        return date_commande;
    }

    public void setDate_commande(String date_commande) {
        this.date_commande = date_commande;
    }

    public double getTotal_commande() {
        return total_commande;
    }

    public void setTotal_commande(double total_commande) {
        this.total_commande = total_commande;
    }

    public String getStatut_commande() {
        return statut_commande;
    }

    public void setStatut_commande(String statut_commande) {
        this.statut_commande = statut_commande;
    }

    public String getMode_paiement_commande() {
        return mode_paiement_commande;
    }

    public void setMode_paiement_commande(String mode_paiement_commande) {
        this.mode_paiement_commande = mode_paiement_commande;
    }

    public int getId_client_commande() {
        return id_client_commande;
    }

    public void setId_client_commande(int id_client_commande) {
        this.id_client_commande = id_client_commande;
    }

    public String getAdresse_commande() {
        return adresse_commande;
    }

    public void setAdresse_commande(String adresse_commande) {
        this.adresse_commande = adresse_commande;
    }

    @Override
    public String toString() {
        return "commandes{" +
                "id_commande=" + id_commande +
                ", date_commande='" + date_commande + '\'' +
                ", total_commande=" + total_commande +
                ", statut_commande='" + statut_commande + '\'' +
                ", mode_paiement_commande='" + mode_paiement_commande + '\'' +
                ", id_client_commande=" + id_client_commande +
                ", adresse_commande='" + adresse_commande + '\'' +
                '}';
    }
}
