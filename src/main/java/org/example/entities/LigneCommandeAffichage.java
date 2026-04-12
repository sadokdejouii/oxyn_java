package org.example.entities;

/**
 * Ligne de commande pour l’affichage (jointure produit).
 */
public class LigneCommandeAffichage {

    private final String nomProduit;
    private final int quantite;
    private final double prixUnitaire;

    public LigneCommandeAffichage(String nomProduit, int quantite, double prixUnitaire) {
        this.nomProduit = nomProduit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    public String getNomProduit() {
        return nomProduit;
    }

    public int getQuantite() {
        return quantite;
    }

    public double getPrixUnitaire() {
        return prixUnitaire;
    }

    public double getSousTotal() {
        return prixUnitaire * quantite;
    }
}
