package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import org.example.components.JCEFMapComponent;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le composant JCEF de carte
 */
public class JCEFMapController implements Initializable {
    
    @FXML
    private JCEFMapComponent mapComponent;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Le composant JCEF s'initialise automatiquement
        System.out.println("DEBUG: JCEFMapController initialisé");
    }
    
    /**
     * Charge une carte avec l'adresse spécifiée
     */
    public void loadMap(String address) {
        if (mapComponent != null && mapComponent.isInitialized()) {
            System.out.println("DEBUG: Chargement de la carte JCEF pour: " + address);
            mapComponent.loadMap(address);
        } else {
            System.err.println("DEBUG: JCEFMapComponent non initialisé");
        }
    }
    
    /**
     * Charge une URL spécifique
     */
    public void loadURL(String url) {
        if (mapComponent != null && mapComponent.isInitialized()) {
            System.out.println("DEBUG: Chargement URL JCEF: " + url);
            mapComponent.loadURL(url);
        } else {
            System.err.println("DEBUG: JCEFMapComponent non initialisé pour URL: " + url);
        }
    }
    
    /**
     * Nettoie les ressources
     */
    public void dispose() {
        if (mapComponent != null) {
            mapComponent.dispose();
        }
    }
}
