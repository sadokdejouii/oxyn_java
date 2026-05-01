package org.example.controllers;

/**
 * Transmet le libellé du module à {@link PlaceholderPageController} avant chargement du FXML.
 */
public final class PlaceholderContext {

    private static final ThreadLocal<String> MODULE = new ThreadLocal<>();

    private PlaceholderContext() {
    }

    public static void setModuleLabel(String label) {
        MODULE.set(label);
    }

    static String consumeModuleLabel() {
        try {
            return MODULE.get();
        } finally {
            MODULE.remove();
        }
    }
}
