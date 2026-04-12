package org.example.entities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Panier en mémoire pour la session applicative (singleton).
 */
public final class PanierSession {

    private static final PanierSession INSTANCE = new PanierSession();

    private final Map<Integer, LignePanier> lignes = new LinkedHashMap<>();

    private PanierSession() {
    }

    public static PanierSession getInstance() {
        return INSTANCE;
    }

    public void ajouterProduit(produits p) {
        if (p == null) {
            return;
        }
        int id = p.getId_produit();
        lignes.merge(id, new LignePanier(p, 1), (ex, ignored) -> {
            ex.setQuantite(ex.getQuantite() + 1);
            return ex;
        });
    }

    public List<LignePanier> getLignes() {
        return new ArrayList<>(lignes.values());
    }

    public double getTotal() {
        return lignes.values().stream().mapToDouble(LignePanier::getSousTotal).sum();
    }

    public void viderPanier() {
        lignes.clear();
    }

    public boolean estVide() {
        return lignes.isEmpty();
    }
}
