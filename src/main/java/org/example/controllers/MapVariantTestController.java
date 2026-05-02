package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebView;
import java.net.URL;
import java.util.ResourceBundle;

public class MapVariantTestController implements Initializable {

    @FXML private WebView mapSmall;
    @FXML private WebView mapMedium;
    @FXML private WebView mapLarge;
    @FXML private WebView mapSquare;
    @FXML private WebView mapWide;
    @FXML private WebView mapTall;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadMap(mapSmall, "Small Map", 300, 250);
        loadMap(mapMedium, "Medium Map", 500, 350);
        loadMap(mapLarge, "Large Map", 700, 400);
        loadMap(mapSquare, "Square Map", 350, 350);
        loadMap(mapWide, "Wide Map", 650, 300);
        loadMap(mapTall, "Tall Map", 350, 500);
    }

    private void loadMap(WebView webView, String name, int width, int height) {
        if (webView == null) return;

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("  <meta charset='utf-8'>\n");
        html.append("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("  <link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css' />\n");
        html.append("  <script src='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js'></script>\n");
        html.append("  <style>\n");
        html.append("    * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("    html, body { width: 100%; height: 100%; }\n");
        html.append("    #map { width: 100%; height: 100%; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <div id='map'></div>\n");
        html.append("  <script>\n");
        html.append("    (function() {\n");
        html.append("      try {\n");
        html.append("        var mapId = '").append(name.replace(" ", "")).append("';\n");
        html.append("        window[mapId] = L.map('map', {attributionControl: true}).setView([48.8566, 2.3522], 4);\n");
        html.append("        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n");
        html.append("          attribution: '© OpenStreetMap contributors',\n");
        html.append("          maxZoom: 19\n");
        html.append("        }).addTo(window[mapId]);\n");
        html.append("      } catch(err) { console.error('Map error:', err); }\n");
        html.append("    })();\n");
        html.append("  </script>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        webView.getEngine().loadContent(html.toString());
    }
}
