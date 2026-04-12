package org.example.utils;

import org.example.entities.Client;
import org.example.entities.User;
import org.example.services.SessionContext;

/**
 * Résout l’identifiant {@code users.id_user} du client connecté pour le module commandes / panier.
 */
public final class CommandeClientResolver {

    private CommandeClientResolver() {
    }

    /**
     * @return l’id utilisateur du client connecté, ou 0 si aucun client authentifié avec id valide
     */
    public static int idClientConnecte() {
        User u = SessionContext.getInstance().getCurrentUser();
        if (u instanceof Client c && c.getId() > 0) {
            return c.getId();
        }
        return 0;
    }
}
