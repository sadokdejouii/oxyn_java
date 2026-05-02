package org.example.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.example.services.LocationService;
import org.example.services.LocationService.LocationResult;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class LocationPickerController implements Initializable {

    @FXML
    private TextField searchField;

    @FXML
    private Button searchButton;

    @FXML
    private ListView<LocationResult> resultsListView;

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private Button selectButton;

    @FXML
    private Label selectedLocationLabel;

    @FXML
    private VBox detailsPanel;

    @FXML
    private WebView mapView;

    @FXML
    private VBox mapContainer;

    private final LocationService locationService = new LocationService();
    private LocationResult selectedLocation;
    private Runnable onLocationSelected;
    private Runnable onCancel;

    public void setOnLocationSelected(Runnable callback) {
        this.onLocationSelected = callback;
    }

    public void setOnCancel(Runnable callback) {
        this.onCancel = callback;
    }

    public LocationResult getSelectedLocation() {
        return selectedLocation;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadingIndicator.setVisible(false);
        detailsPanel.setVisible(false);
        
        // Initialize the interactive map
        initializeMap();
        
        // Configure ListView to display location results
        resultsListView.setCellFactory(param -> new ListCell<LocationResult>() {
            @Override
            protected void updateItem(LocationResult item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });

        // Select location when item is clicked in the list
        resultsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectLocation(newVal);
                // Pan map to selected location
                updateMapLocation(newVal.getLat(), newVal.getLon());
            }
        });

        // Allow search on Enter key
        searchField.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                performSearch();
            }
        });
    }

    private void initializeMap() {
        if (mapView == null) return;

        mapView.setContextMenuEnabled(false);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("  <meta charset='utf-8'>\n");
        html.append("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("  <link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css' />\n");
        html.append("  <script src='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js'></script>\n");
        html.append("  <style>\n");
        html.append("    html, body { width: 100%; height: 100%; margin: 0; padding: 0; overflow: hidden; }\n");
        html.append("    #map { width: 100%; height: 100%; background: #eef4fb; }\n");
        html.append("    .leaflet-container { background: #eef4fb; }\n");
        html.append("    .leaflet-tile { image-rendering: auto; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <div id='map'></div>\n");
        html.append("  <script>\n");
        html.append("    (function() {\n");
        html.append("      try {\n");
        html.append("        if (L.Browser) { L.Browser.any3d = false; }\n");
        html.append("        window.mapObj = L.map('map', {\n");
        html.append("          zoomControl: true,\n");
        html.append("          attributionControl: true,\n");
        html.append("          zoomAnimation: false,\n");
        html.append("          fadeAnimation: false,\n");
        html.append("          markerZoomAnimation: false,\n");
        html.append("          inertia: false,\n");
        html.append("          preferCanvas: false\n");
        html.append("        }).setView([48.8566, 2.3522], 4);\n");
        html.append("        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n");
        html.append("          attribution: '© OpenStreetMap contributors',\n");
        html.append("          maxZoom: 19,\n");
        html.append("          minZoom: 2,\n");
        html.append("          updateWhenIdle: true,\n");
        html.append("          updateWhenZooming: false,\n");
        html.append("          keepBuffer: 1\n");
        html.append("        }).addTo(window.mapObj);\n");
        html.append("        window.currentMarker = null;\n");
        html.append("        window.mapObj.on('click', function(e) {\n");
        html.append("          const lat = e.latlng.lat.toFixed(6);\n");
        html.append("          const lng = e.latlng.lng.toFixed(6);\n");
        html.append("          if (window.currentMarker) { window.mapObj.removeLayer(window.currentMarker); }\n");
        html.append("          window.currentMarker = L.marker([Number(lat), Number(lng)]).addTo(window.mapObj);\n");
        html.append("          if (window.javaController) { window.javaController.mapClickHandler(lat, lng); }\n");
        html.append("        });\n");
        html.append("        window.moveMarker = function(lat, lng) {\n");
        html.append("          if (window.currentMarker) { window.mapObj.removeLayer(window.currentMarker); }\n");
        html.append("          window.currentMarker = L.marker([lat, lng]).addTo(window.mapObj);\n");
        html.append("          window.mapObj.setView([lat, lng], 13, {animate: false});\n");
        html.append("        };\n");
        html.append("        setTimeout(function() { if (window.mapObj) { window.mapObj.invalidateSize(false); } }, 150);\n");
        html.append("      } catch (err) { console.error('Leaflet init failed', err); }\n");
        html.append("    })();\n");
        html.append("  </script>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        mapView.getEngine().documentProperty().addListener((obs, oldDoc, newDoc) -> {
            if (newDoc != null) {
                try {
                    JSObject window = (JSObject) mapView.getEngine().executeScript("window");
                    window.setMember("javaController", this);
                    mapView.getEngine().executeScript("if (window.mapObj) { window.mapObj.invalidateSize(false); }");
                } catch (Exception e) {
                    System.out.println("Error wiring map bridge: " + e.getMessage());
                }
            }
        });

        mapView.getEngine().loadContent(html.toString());
    }

    private void updateMapLocation(double lat, double lng) {
        if (mapView != null) {
            try {
                mapView.getEngine().executeScript(
                        String.format("if (window.moveMarker) { window.moveMarker(%.6f, %.6f); }", lat, lng)
                );
            } catch (Exception e) {
                System.out.println("Error updating map location: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handle map click by performing reverse geocoding
     */
    public void mapClickHandler(String lat, String lng) {
        try {
            double latitude = Double.parseDouble(lat);
            double longitude = Double.parseDouble(lng);
            
            // Perform reverse geocoding search
            new Thread(() -> {
                try {
                    LocationResult result = locationService.reverseGeocode(latitude, longitude);
                    Platform.runLater(() -> {
                        if (result != null) {
                            // Create a list with the single result
                            List<LocationResult> results = new ArrayList<>();
                            results.add(result);
                            
                            statusLabel.setText("Lieu trouvé: " + result.getDisplayName());
                            resultsListView.setItems(FXCollections.observableArrayList(results));
                            
                            // Auto-select the result
                            selectLocation(result);
                            updateMapLocation(result.getLat(), result.getLon());
                        } else {
                            statusLabel.setText("Aucun lieu trouvé pour ces coordonnées");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Erreur reverse geocoding: " + e.getMessage());
                    });
                }
            }).start();
        } catch (Exception e) {
            statusLabel.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    void performSearch(ActionEvent event) {
        performSearch();
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            statusLabel.setText("Veuillez entrer un lieu à rechercher");
            return;
        }

        loadingIndicator.setVisible(true);
        statusLabel.setText("Recherche en cours...");
        resultsListView.setItems(FXCollections.observableArrayList());
        detailsPanel.setVisible(false);

        // Run search in background thread
        new Thread(() -> {
            try {
                List<LocationResult> results = locationService.searchLocation(query);
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    if (results.isEmpty()) {
                        statusLabel.setText("Aucun résultat trouvé");
                    } else {
                        statusLabel.setText("Résultats: " + results.size() + " lieu(x) trouvé(s)");
                        resultsListView.setItems(FXCollections.observableArrayList(results));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    statusLabel.setText("Erreur: " + e.getMessage());
                });
            }
        }).start();
    }

    private void selectLocation(LocationResult location) {
        selectedLocation = location;
        detailsPanel.setVisible(true);
        
        String details = String.format(
                "Lieu: %s\nVille: %s\nPays: %s\nCoordonnées: %.4f, %.4f",
                location.getPlace() != null ? location.getPlace() : "N/A",
                location.getCity() != null ? location.getCity() : "N/A",
                location.getCountry() != null ? location.getCountry() : "N/A",
                location.getLat(),
                location.getLon()
        );
        selectedLocationLabel.setText(details);
        statusLabel.setText("Lieu sélectionné: " + location.getDisplayName());
    }

    @FXML
    void confirmSelection(ActionEvent event) {
        if (selectedLocation == null) {
            statusLabel.setText("Veuillez sélectionner un lieu d'abord");
            return;
        }

        if (onLocationSelected != null) {
            onLocationSelected.run();
        }

        // Only try to close stage if in window mode (not in-page modal)
        try {
            Stage stage = (Stage) selectButton.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            // Modal is in-page, no stage to close
        }
    }

    @FXML
    void cancelSelection(ActionEvent event) {
        selectedLocation = null;
        if (onCancel != null) {
            onCancel.run();
        }
        
        // Only try to close stage if in window mode (not in-page modal)
        try {
            Stage stage = (Stage) selectButton.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            // Modal is in-page, no stage to close
        }
    }
}
