# Map API Integration Feature - Event Management

## Overview
A new interactive map location picker feature has been added to the "Ajouter Événement" (Add Event) form that uses the Nominatim API to allow users to search for locations and auto-fill the "Lieu" (Place) and "Ville" (City) fields.

## Changes Made

### 1. New Services
**File**: `src/main/java/org/example/services/LocationService.java`

- Implements location search using the Nominatim OpenStreetMap API
- **Key Methods**:
  - `searchLocation(String query)` - Searches for locations matching a query string
  - `reverseGeocode(double lat, double lon)` - Converts coordinates to location details
  - Automatically extracts city, place name, and country information from API responses

### 2. New Controllers
**File**: `src/main/java/org/example/controllers/LocationPickerController.java`

- Implements the location picker dialog UI
- **Key Features**:
  - Real-time location search with suggestions
  - List of search results from the Nominatim API
  - Location details display with coordinates
  - Background thread execution to prevent UI freezing during API calls
  - Callback mechanism to auto-fill parent form fields

### 3. New FXML Dialog
**File**: `src/main/resources/FXML/LocationPicker.fxml`

- Modern location picker dialog interface
- Components:
  - Search field and button
  - Results ListView
  - Details panel showing selected location info
  - Confirmation and cancel buttons

### 4. Updated Event Management Form
**File**: `src/main/resources/FXML/AjouterEvenements.fxml`

- Added a "📍 Carte" (Map) button next to the Lieu field
- Button opens the LocationPicker dialog
- When a location is selected, it auto-fills both:
  - **Lieu** field with the place/street name
  - **Ville** field with the city name

### 5. Enhanced Form Controller
**File**: `src/main/java/org/example/controllers/AjouterEvenementsController.java`

- New method: `openLocationPicker(ActionEvent event)`
- Loads the LocationPicker dialog
- Sets up callback to receive selected location and auto-fill fields
- Maintains all existing validation and event creation logic

### 6. CSS Styling
**File**: `src/main/resources/css/dashboard-saas.css`

Added new styles:
- `.location-picker-dialog` - Dialog container styling
- `.form-btn-secondary` - Secondary button (Map button) styling
- `.form-btn-cancel` - Cancel button styling
- `.form-list-view` - ListView styling for location results
- `.form-feedback` styles - Success and error message styling

## How to Use

1. **Click "Ajouter Événement"** to open the Add Event form
2. **Click the "📍 Carte" button** next to the Lieu (Place) field
3. **Enter a search query** (address, city, landmark, etc.)
4. **Click "Rechercher"** or press Enter to search
5. **Select a location** from the results list
6. **View location details** (Place, City, Country, Coordinates)
7. **Click "Confirmer la sélection"** to auto-fill the form
8. **Fields are automatically updated**:
   - Lieu → Place/Street name
   - Ville → City name

## Technologies Used

- **API**: Nominatim OpenStreetMap Geocoding API (free, no API key required)
- **HTTP Client**: Java 11+ built-in `java.net.http.HttpClient`
- **JSON Parsing**: Google's GSON library (already in project dependencies)
- **UI Framework**: JavaFX 21 with FXML

## Key Features

✅ **Free & No API Keys** - Uses Nominatim API, no setup required
✅ **Async Operations** - Background threads prevent UI freezing
✅ **Rich Location Data** - Automatically extracts Place, City, and Country info
✅ **User-Friendly** - Modern dialog interface with search and selection flow
✅ **Error Handling** - Graceful error messages for network issues
✅ **Responsive** - Press Enter to search, click to select

## Error Handling

The feature includes error handling for:
- Network connectivity issues
- Invalid search queries
- API response errors
- Missing location data

## Future Enhancements

Possible improvements:
- Add map visualization (OpenStreetMap or Leaflet integration)
- Save favorite locations
- Geocoding from address input (without search dialog)
- Support for multiple location sources (Google Maps API, etc.)
- Caching of search results
