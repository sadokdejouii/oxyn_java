package org.example.components;

import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;

import java.awt.BorderLayout;

/**
 * Composant JCEF simplifié pour afficher des cartes OpenStreetMap dans JavaFX
 */
public class JCEFMapComponent extends StackPane {
    
    private CefBrowser browser;
    private boolean initialized = false;
    private WebView fallbackWebView;
    
    public JCEFMapComponent() {
        setPrefSize(800, 400);
        initializeComponent();
    }
    
    private void initializeComponent() {
        try {
            // Initialisation moderne JCEF 122.1.10
            String[] args = {};
            CefApp.startup(args);
            
            // Obtenir l'instance de CefApp
            CefApp cefApp = CefApp.getInstance();
            
            // Créer un client CEF
            CefClient cefClient = cefApp.createClient();
            
            // Configurer le handler AVANT de créer le browser
            cefClient.addLoadHandler(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
                    System.out.println("JCEF: Début chargement");
                }
                
                @Override
                public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                    System.out.println("JCEF: Chargement terminé, code: " + httpStatusCode);
                }
                
                @Override
                public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                    System.err.println("JCEF: Erreur chargement: " + errorText);
                    // Toute modification UI doit se faire sur le thread JavaFX
                    Platform.runLater(() -> {
                        // Afficher fallback WebView en cas d'erreur
                        initializeFallback();
                    });
                }
            });
            
            // Créer le browser avec le client
            browser = cefClient.createBrowser("about:blank", false, false);
            
            // Marquer comme initialisé
            initialized = true;
            System.out.println("DEBUG: JCEF Map initialisée avec succès");
            
            // Intégrer JCEF dans JavaFX (nécessite une approche SwingFX)
            // Pour l'instant, utilisons le fallback WebView
            initializeFallback();
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation JCEF: " + e.getMessage());
            e.printStackTrace();
            // Utiliser le fallback WebView
            initializeFallback();
        }
    }
    
    /**
     * Initialise le WebView en fallback
     */
    private void initializeFallback() {
        System.out.println("DEBUG: Initialisation du fallback WebView pour la carte");
        fallbackWebView = new WebView();
        fallbackWebView.setPrefSize(800, 400);
        this.getChildren().setAll(fallbackWebView);
        initialized = true;
    }
    
    /**
     * Charge une URL dans le browser
     */
    public void loadURL(String url) {
        if (browser != null && initialized) {
            browser.loadURL(url);
        } else if (fallbackWebView != null) {
            fallbackWebView.getEngine().load(url);
        } else {
            System.err.println("Aucun browser disponible pour charger: " + url);
        }
    }
    
    /**
     * Charge du contenu HTML
     */
    public void loadHTML(String html) {
        if (browser != null && initialized) {
            String dataUrl = "data:text/html;charset=utf-8," + 
                java.util.Base64.getEncoder().encodeToString(html.getBytes());
            browser.loadURL(dataUrl);
        } else if (fallbackWebView != null) {
            fallbackWebView.getEngine().loadContent(html);
        } else {
            System.err.println("Aucun browser disponible pour charger HTML");
        }
    }
    
    /**
     * Charge une carte OpenStreetMap avec une adresse
     */
    public void loadMap(String address) {
        System.out.println("DEBUG: Chargement de la carte pour: " + address);
        
        String encodedAddress = address.replace(" ", "+").replace(",", "%2C");
        
        // HTML pour la carte OpenStreetMap avec Leaflet
        String html = "<!DOCTYPE html><html><head>" +
            "<meta charset='utf-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css' />" +
            "<style>" +
            "  html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; }" +
            "</style>" +
            "</head><body>" +
            "<div id='map'></div>" +
            "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
            "<script>" +
            "  var map = L.map('map').setView([34.0, 9.0], 7);" +
            "  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {" +
            "    attribution: '© OpenStreetMap contributors'," +
            "    maxZoom: 19" +
            "  }).addTo(map);" +
            "  " +
            "  // Géocodage de l'adresse" +
            "  fetch('https://nominatim.openstreetmap.org/search?format=json&q=" + encodedAddress + "&limit=1')" +
            "    .then(response => response.json())" +
            "    .then(data => {" +
            "      if (data && data.length > 0) {" +
            "        const result = data[0];" +
            "        const lat = parseFloat(result.lat);" +
            "        const lon = parseFloat(result.lon);" +
            "        const displayName = result.display_name || '" + address.replace("'", "\\'") + "';" +
            "        " +
            "        // Centrer la carte sur l'adresse" +
            "        map.setView([lat, lon], 15);" +
            "        " +
            "        // Ajouter un marqueur personnalisé" +
            "        const customIcon = L.divIcon({" +
            "          html: '<div style=\"background:#3498db;color:white;width:30px;height:30px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-weight:bold;border:2px solid white;box-shadow:0 2px 5px rgba(0,0,0,0.3);\">G</div>'," +
            "          iconSize: [30, 30]," +
            "          iconAnchor: [15, 15]," +
            "          popupAnchor: [0, -15]" +
            "        });" +
            "        " +
            "        const marker = L.marker([lat, lon], { icon: customIcon }).addTo(map);" +
            "        marker.bindPopup('<b>' + displayName + '</b><br><small>Lat: ' + lat.toFixed(4) + ', Lon: ' + lon.toFixed(4) + '</small>').openPopup();" +
            "        " +
            "        console.log('Adresse trouvée:', displayName, lat, lon);" +
            "      } else {" +
            "        console.error('Adresse non trouvée: " + address.replace("'", "\\'") + "');" +
            "      }" +
            "    })" +
            "    .catch(error => {" +
            "      console.error('Erreur de géocodage:', error);" +
            "    });" +
            "</script>" +
            "</body></html>";
        
        loadHTML(html);
    }
    
    /**
     * Nettoie les ressources
     */
    public void dispose() {
        if (browser != null) {
            browser.close(true);
            browser = null;
        }
        initialized = false;
    }
    
    /**
     * Vérifie si le composant est initialisé
     */
    public boolean isInitialized() {
        return initialized;
    }
}
