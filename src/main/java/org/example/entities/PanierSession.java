package org.example.entities;

import org.example.services.PanierPersistenceService;
import org.example.services.SessionContext;

import java.sql.SQLException;
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
    private final PanierPersistenceService persistenceService = new PanierPersistenceService();
    private int loadedClientId = 0;

    private PanierSession() {
    }

    public static PanierSession getInstance() {
        return INSTANCE;
    }

    public void ajouterProduit(produits p) {
        if (p == null) {
            return;
        }
        ensureLoadedForCurrentClient();
        int id = p.getId_produit();
        LignePanier updated = lignes.merge(id, new LignePanier(p, 1), (ex, ignored) -> {
            ex.setQuantite(ex.getQuantite() + 1);
            return ex;
        });
        int clientId = currentClientId();
        if (clientId > 0) {
            try {
                persistenceService.upsertQuantite(clientId, id, updated.getQuantite());
            } catch (SQLException ignored) {
            }
        }
    }

    public List<LignePanier> getLignes() {
        ensureLoadedForCurrentClient();
        return new ArrayList<>(lignes.values());
    }

    public double getTotal() {
        ensureLoadedForCurrentClient();
        return lignes.values().stream().mapToDouble(LignePanier::getSousTotal).sum();
    }

    public void viderPanier() {
        lignes.clear();
        int clientId = currentClientId();
        if (clientId > 0) {
            try {
                persistenceService.clearPanier(clientId);
            } catch (SQLException ignored) {
            }
        }
    }

    public boolean estVide() {
        ensureLoadedForCurrentClient();
        return lignes.isEmpty();
    }

    /**
     * Nettoie le panier en mémoire (utilisé à la déconnexion pour éviter de mélanger les paniers).
     */
    public void resetMemory() {
        lignes.clear();
        loadedClientId = 0;
    }

    private void ensureLoadedForCurrentClient() {
        int clientId = currentClientId();
        if (clientId <= 0) {
            return;
        }
        if (loadedClientId == clientId) {
            return;
        }
        try {
            lignes.clear();
            lignes.putAll(persistenceService.loadPanier(clientId));
            loadedClientId = clientId;
        } catch (SQLException ignored) {
            loadedClientId = clientId; // évite de retry en boucle en cas d'erreur DB
        }
    }

    private static int currentClientId() {
        SessionContext ctx = SessionContext.getInstance();
        if (!ctx.isClientUser()) {
            return 0;
        }
        int id = ctx.getUserId();
        return id > 0 ? id : 0;
    }
}
