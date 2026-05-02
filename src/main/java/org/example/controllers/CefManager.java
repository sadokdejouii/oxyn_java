package org.example.controllers;

import org.cef.CefClient;

/**
 * Point d'accès singleton pour JCEF (navigateur embarqué).
 * Les tests et certaines pages peuvent appeler {@link #getCefClient()} avant initialisation complète :
 * dans ce cas la valeur peut être {@code null}.
 */
public final class CefManager {

    private static volatile CefClient cefClient;

    private CefManager() {
    }

    public static CefClient getCefClient() {
        return cefClient;
    }

    public static void setCefClient(CefClient client) {
        cefClient = client;
    }

    public static void dispose() {
        cefClient = null;
    }
}
