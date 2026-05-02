package org.example.utils;

import javafx.scene.Scene;

import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Centralise l'application des styles JavaFX (thème OXYN).
 *
 * Objectif: éviter les vues "cassées" quand une Scene est créée sans stylesheets,
 * ou quand une vue est chargée hors de son shell habituel.
 */
public final class AppStyles {

    private AppStyles() {}

    // Core theme files (dark)
    private static final String[] GLOBAL = {
            "/css/login.css",
            "/css/dashboard-saas.css",
            "/css/client-shell-premium.css",
            "/css/sidebar.css",
            "/css/topbar.css",
            "/css/home.css",
            "/css/cards.css",
            "/css/user-management.css",
            "/css/dialogs.css",
            /* Profile overrides must load after shell / shared sheets */
            "/css/profile-page.css"
    };

    public static void apply(Scene scene) {
        if (scene == null) return;

        Set<String> existing = new LinkedHashSet<>(scene.getStylesheets());
        for (String path : GLOBAL) {
            String css = toExternalForm(path);
            if (css != null && !existing.contains(css)) {
                scene.getStylesheets().add(css);
                existing.add(css);
            }
        }
    }

    private static String toExternalForm(String absClasspathPath) {
        try {
            URL url = AppStyles.class.getResource(absClasspathPath);
            return url != null ? url.toExternalForm() : null;
        } catch (Exception e) {
            return null;
        }
    }
}

