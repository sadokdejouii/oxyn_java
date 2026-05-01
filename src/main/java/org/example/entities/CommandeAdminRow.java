package org.example.entities;

/**
 * Ligne affichée côté admin : libellé client sans identifiant ; l’entité {@link commandes}
 * sert uniquement aux actions internes (suppression, etc.).
 */
public final class CommandeAdminRow {

    private final commandes commande;
    private final String libelleClient;

    public CommandeAdminRow(commandes commande, String libelleClient) {
        this.commande = commande;
        this.libelleClient = libelleClient != null && !libelleClient.isBlank() ? libelleClient.trim() : "—";
    }

    public commandes getCommande() {
        return commande;
    }

    public String getLibelleClient() {
        return libelleClient;
    }
}
