package org.example.services;

/**
 * Accès unique à la session applicative (point d’extension MVVM / contrôleurs du module Planning).
 */
public final class CurrentSession {

    private CurrentSession() {
    }

    public static SessionContext context() {
        return SessionContext.getInstance();
    }
}
