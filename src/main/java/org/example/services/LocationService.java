package org.example.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LocationService {

    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/search";
    private static final String NOMINATIM_REVERSE_URL = "https://nominatim.openstreetmap.org/reverse";

    private final HttpClient httpClient;

    public LocationService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Search for locations by query
     * @param query Search query (e.g., "Paris", "123 Main St")
     * @return List of LocationResult objects
     */
    public List<LocationResult> searchLocation(String query) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format("%s?q=%s&format=json&limit=10", NOMINATIM_BASE_URL, encodedQuery);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "OXYN-Event-Manager")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("API returned status code: " + response.statusCode());
        }

        return parseSearchResults(response.body());
    }

    /**
     * Reverse geocode coordinates to get location details
     * @param latitude Latitude
     * @param longitude Longitude
     * @return LocationResult object
     */
    public LocationResult reverseGeocode(double latitude, double longitude) throws Exception {
        String url = String.format("%s?lat=%f&lon=%f&format=json&zoom=10", NOMINATIM_REVERSE_URL, latitude, longitude);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "OXYN-Event-Manager")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("API returned status code: " + response.statusCode());
        }

        return parseReverseGeocodeResult(response.body());
    }

    /**
     * Parse search results from Nominatim API
     */
    private List<LocationResult> parseSearchResults(String jsonResponse) throws Exception {
        List<LocationResult> results = new ArrayList<>();
        JsonArray jsonArray = JsonParser.parseString(jsonResponse).getAsJsonArray();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject obj = jsonArray.get(i).getAsJsonObject();
            
            LocationResult result = new LocationResult();
            result.setLat(obj.get("lat").getAsDouble());
            result.setLon(obj.get("lon").getAsDouble());
            result.setDisplayName(obj.get("display_name").getAsString());
            
            // Extract city, town, or village from address components
            if (obj.has("address")) {
                JsonObject address = obj.getAsJsonObject("address");
                
                // Try to get the city/town/village with better fallback
                if (address.has("city")) {
                    result.setCity(address.get("city").getAsString());
                } else if (address.has("town")) {
                    result.setCity(address.get("town").getAsString());
                } else if (address.has("village")) {
                    result.setCity(address.get("village").getAsString());
                } else if (address.has("county")) {
                    result.setCity(address.get("county").getAsString());
                } else if (address.has("state")) {
                    result.setCity(address.get("state").getAsString());
                }
                
                // Get the place name (street, landmark, etc.) with better fallback
                if (address.has("road")) {
                    result.setPlace(address.get("road").getAsString());
                } else if (address.has("building")) {
                    result.setPlace(address.get("building").getAsString());
                } else if (address.has("amenity")) {
                    result.setPlace(address.get("amenity").getAsString());
                } else if (address.has("landmark")) {
                    result.setPlace(address.get("landmark").getAsString());
                } else if (address.has("neighbourhood")) {
                    result.setPlace(address.get("neighbourhood").getAsString());
                } else if (address.has("suburb")) {
                    result.setPlace(address.get("suburb").getAsString());
                } else if (address.has("hamlet")) {
                    result.setPlace(address.get("hamlet").getAsString());
                } else {
                    // Fallback: use first part of display name
                    String[] parts = result.getDisplayName().split(",");
                    if (parts.length > 0) {
                        result.setPlace(parts[0].trim());
                    }
                }
                
                // Get country
                if (address.has("country")) {
                    result.setCountry(address.get("country").getAsString());
                }
            } else {
                // If no address object, parse display name
                String[] parts = result.getDisplayName().split(",");
                if (parts.length > 0) {
                    result.setPlace(parts[0].trim());
                }
                if (parts.length > 1) {
                    result.setCity(parts[1].trim());
                }
            }

            results.add(result);
        }

        return results;
    }

    /**
     * Parse reverse geocode result from Nominatim API
     */
    private LocationResult parseReverseGeocodeResult(String jsonResponse) throws Exception {
        JsonObject obj = JsonParser.parseString(jsonResponse).getAsJsonObject();
        
        LocationResult result = new LocationResult();
        result.setLat(obj.get("lat").getAsDouble());
        result.setLon(obj.get("lon").getAsDouble());
        result.setDisplayName(obj.get("display_name").getAsString());

        if (obj.has("address")) {
            JsonObject address = obj.getAsJsonObject("address");
            
            // Extract city/town/village/county
            if (address.has("city")) {
                result.setCity(address.get("city").getAsString());
            } else if (address.has("town")) {
                result.setCity(address.get("town").getAsString());
            } else if (address.has("village")) {
                result.setCity(address.get("village").getAsString());
            } else if (address.has("county")) {
                result.setCity(address.get("county").getAsString());
            } else if (address.has("state")) {
                result.setCity(address.get("state").getAsString());
            }
            
            // Extract place name with fallback chain
            if (address.has("road")) {
                result.setPlace(address.get("road").getAsString());
            } else if (address.has("building")) {
                result.setPlace(address.get("building").getAsString());
            } else if (address.has("amenity")) {
                result.setPlace(address.get("amenity").getAsString());
            } else if (address.has("landmark")) {
                result.setPlace(address.get("landmark").getAsString());
            } else if (address.has("neighbourhood")) {
                result.setPlace(address.get("neighbourhood").getAsString());
            } else if (address.has("suburb")) {
                result.setPlace(address.get("suburb").getAsString());
            } else if (address.has("hamlet")) {
                result.setPlace(address.get("hamlet").getAsString());
            } else {
                // Fallback: use first part of display name
                String[] parts = result.getDisplayName().split(",");
                if (parts.length > 0) {
                    result.setPlace(parts[0].trim());
                }
            }
            
            // Extract country
            if (address.has("country")) {
                result.setCountry(address.get("country").getAsString());
            }
        } else {
            // If no address object, parse display name
            String[] parts = result.getDisplayName().split(",");
            if (parts.length > 0) {
                result.setPlace(parts[0].trim());
            }
            if (parts.length > 1) {
                result.setCity(parts[1].trim());
            }
        }

        return result;
    }

    /**
     * Data class to hold location search results
     */
    public static class LocationResult {
        private double lat;
        private double lon;
        private String displayName;
        private String place;
        private String city;
        private String country;

        // Getters and Setters
        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLon() {
            return lon;
        }

        public void setLon(double lon) {
            this.lon = lon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getPlace() {
            return place;
        }

        public void setPlace(String place) {
            this.place = place;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
