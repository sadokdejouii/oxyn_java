package org.example.entities;

/**
 * Une ligne du panier (produit + quantité) pour affichage et persistance des lignes de commande.
 */
public class LignePanier {

    private final produits produit;
    private int quantite;

    public LignePanier(produits produit, int quantite) {
        this.produit = produit;
        this.quantite = Math.max(1, quantite);
    }

    public String getNomProduit() {
        return produit != null ? produit.getNom_produit() : "";
    }

    public double getPrixUnitaire() {
        return produit != null ? produit.getPrix_produit() : 0;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = Math.max(1, quantite);
    }

    public double getSousTotal() {
        return getPrixUnitaire() * quantite;
    }

    public produits getProduit() {
        return produit;
    }
}
