package org.example.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class WeatherService {

    // Replace with your OpenWeatherMap API key
    private static final String API_KEY = "9b42fd9b244305176adcb8389998d504";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    private final HttpClient httpClient;

    public WeatherService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Fetches current weather data for the given city.
     *
     * @param ville city name (e.g. "Tunis", "Paris")
     * @return WeatherResult containing weather info
     * @throws Exception if the API call fails
     */
    public WeatherResult getWeather(String ville) throws Exception {
        String encodedVille = URLEncoder.encode(ville, StandardCharsets.UTF_8);
        String url = BASE_URL + "?q=" + encodedVille + "&appid=" + API_KEY + "&units=metric&lang=fr";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return parseResponse(response.body());
        } else if (response.statusCode() == 404) {
            throw new Exception("Ville introuvable: " + ville);
        } else if (response.statusCode() == 401) {
            throw new Exception("Clé API invalide. Veuillez configurer votre clé OpenWeatherMap.");
        } else {
            throw new Exception("Erreur API (" + response.statusCode() + "): " + response.body());
        }
    }

    private WeatherResult parseResponse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        WeatherResult result = new WeatherResult();

        // City name returned by API
        result.ville = root.get("name").getAsString();

        // Country
        if (root.has("sys")) {
            result.pays = root.getAsJsonObject("sys").get("country").getAsString();
        }

        // Main weather description
        if (root.has("weather") && root.getAsJsonArray("weather").size() > 0) {
            JsonObject weatherObj = root.getAsJsonArray("weather").get(0).getAsJsonObject();
            result.description = capitalize(weatherObj.get("description").getAsString());
            result.iconCode = weatherObj.get("icon").getAsString();
        }

        // Temperature data
        if (root.has("main")) {
            JsonObject mainObj = root.getAsJsonObject("main");
            result.temperature = mainObj.get("temp").getAsDouble();
            result.feelsLike = mainObj.get("feels_like").getAsDouble();
            result.tempMin = mainObj.get("temp_min").getAsDouble();
            result.tempMax = mainObj.get("temp_max").getAsDouble();
            result.humidity = mainObj.get("humidity").getAsInt();
        }

        // Wind
        if (root.has("wind")) {
            JsonObject windObj = root.getAsJsonObject("wind");
            result.windSpeed = windObj.get("speed").getAsDouble();
            if (windObj.has("deg")) {
                result.windDeg = windObj.get("deg").getAsInt();
            }
        }

        // Visibility
        if (root.has("visibility")) {
            result.visibility = root.get("visibility").getAsInt();
        }

        // Cloudiness
        if (root.has("clouds")) {
            result.cloudiness = root.getAsJsonObject("clouds").get("all").getAsInt();
        }

        // Precipitation data (rain in last hour)
        if (root.has("rain")) {
            JsonObject rainObj = root.getAsJsonObject("rain");
            if (rainObj.has("1h")) {
                result.rainVolume = rainObj.get("1h").getAsDouble();
            }
        }

        // Snow data (snow in last hour)
        if (root.has("snow")) {
            JsonObject snowObj = root.getAsJsonObject("snow");
            if (snowObj.has("1h")) {
                result.snowVolume = snowObj.get("1h").getAsDouble();
            }
        }

        return result;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static class WeatherResult {
        public String ville;
        public String pays;
        public String description;
        public String iconCode;
        public double temperature;
        public double feelsLike;
        public double tempMin;
        public double tempMax;
        public int humidity;
        public double windSpeed;
        public int windDeg;
        public int visibility;
        public int cloudiness;
        public double rainVolume;   // mm of rain in last hour
        public double snowVolume;   // mm of snow in last hour

        public String getWeatherEmoji() {
            if (iconCode == null) return "🌡️";
            if (iconCode.startsWith("01")) return "☀️";
            if (iconCode.startsWith("02")) return "⛅";
            if (iconCode.startsWith("03") || iconCode.startsWith("04")) return "☁️";
            if (iconCode.startsWith("09")) return "🌧️";
            if (iconCode.startsWith("10")) return "🌦️";
            if (iconCode.startsWith("11")) return "⛈️";
            if (iconCode.startsWith("13")) return "❄️";
            if (iconCode.startsWith("50")) return "🌫️";
            return "🌡️";
        }

        public String getConditionIcon() {
            if (iconCode == null) return "○";
            if (iconCode.startsWith("01")) return "☀";
            if (iconCode.startsWith("02")) return "⛅";
            if (iconCode.startsWith("03") || iconCode.startsWith("04")) return "☁";
            if (iconCode.startsWith("09") || iconCode.startsWith("10")) return "☂";
            if (iconCode.startsWith("11")) return "⚡";
            if (iconCode.startsWith("13")) return "❄";
            if (iconCode.startsWith("50")) return "≋";
            return "○";
        }

        public boolean isRainy() {
            // Check actual precipitation data first (most reliable)
            if (rainVolume > 0 || snowVolume > 0) {
                return true;
            }
            // Fallback to icon code if precipitation data is missing
            if (iconCode == null) return false;
            return iconCode.startsWith("09") || iconCode.startsWith("10") || iconCode.startsWith("11");
        }

        public String getDebugInfo() {
            return String.format("Icon: %s | Rain: %.2f mm | Snow: %.2f mm | Desc: %s", 
                iconCode, rainVolume, snowVolume, description);
        }

        public String getWindDirection() {
            String[] dirs = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
            return dirs[(int) Math.round(windDeg / 45.0) % 8];
        }
    }
}
