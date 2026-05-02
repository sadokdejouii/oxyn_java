package org.example.controllers;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.example.models.VideoValidationResult;
import org.example.services.YouTubeService;
import org.example.services.YouTubeServiceFallback;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ Lecteur YouTube SIMPLE — WebView uniquement, ZÉRO JCEF
 */
public class VideoPlayerController {

    @FXML private TextField videoUrlField;
    @FXML private Button loadVideoButton;
    @FXML private Button stopVideoButton;
    @FXML private Button tvModeButton;
    @FXML private Button addToPlaylistButton;
    @FXML private ProgressBar loadingProgress;
    @FXML private Label statusLabel;
    @FXML private Label titleLabel;
    @FXML private Label durationLabel;
    @FXML private AnchorPane videoContainer;
    @FXML private VBox playlistContainer;
    @FXML private ImageView thumbnailView;
    @FXML private HBox controlsContainer;

    // ✅ WebView simple — remplace tout JCEF
    private WebView webView;
    private WebEngine webEngine;

    private String currentVideoId;
    private VideoValidationResult currentValidation;
    private boolean tvMode = false;
    private boolean isPlaying = false;
    private List<String> playlist = new ArrayList<>();
    private int currentPlaylistIndex = 0;
    private Timeline autoPlayTimeline;

    @FXML
    public void initialize() {

        // ✅ WebView créé UNE SEULE FOIS
        webView = new WebView();
        webEngine = webView.getEngine();

        // ✅ User-agent Chrome — évite le blocage YouTube
        webEngine.setUserAgent(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"
        );
        webEngine.setJavaScriptEnabled(true);

        // ✅ Attacher au conteneur JavaFX
        videoContainer.getChildren().setAll(webView);
        AnchorPane.setTopAnchor(webView, 0.0);
        AnchorPane.setBottomAnchor(webView, 0.0);
        AnchorPane.setLeftAnchor(webView, 0.0);
        AnchorPane.setRightAnchor(webView, 0.0);

        // Forcer WebView à remplir tout le conteneur
        webView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        webView.prefWidthProperty().bind(videoContainer.widthProperty());
        webView.prefHeightProperty().bind(videoContainer.heightProperty());

        // Page noire par défaut
        webEngine.loadContent(
            "<html><body style='margin:0;background:#000'></body></html>",
            "text/html"
        );

        setupUI();
        setupEventHandlers();
        setupAutoPlay();

        statusLabel.setText("📺 Prêt — Entrez une URL YouTube");
        System.out.println("✅ VideoPlayerController initialisé — WebView prêt");

        // Auto-charger si URL déjà présente dans le champ
        Platform.runLater(() -> {
            String url = videoUrlField.getText();
            if (url != null && !url.trim().isEmpty()) {
                String videoId = extraireVideoId(url.trim());
                if (videoId != null && !videoId.isEmpty()) {
                    loadVideo(videoId);
                    System.out.println("⚡ Vidéo chargée automatiquement: " + videoId);
                    statusLabel.setText("🎬 Vidéo: " + videoId);
                }
            }
        });
    }

    /**
     * ✅ Charge la vidéo YouTube dans WebView via iframe embed
     */
    private void loadVideo(String videoId) {
        if (videoId == null || videoId.isEmpty()) return;

        currentVideoId = videoId;
        isPlaying = true;

        String embedUrl = "https://www.youtube-nocookie.com/embed/" + videoId
            + "?autoplay=1&rel=0&modestbranding=1&playsinline=1&controls=1";

        String html =
            "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<style>" +
            "  * { margin:0; padding:0; box-sizing:border-box; }" +
            "  html, body { width:100%; height:100%; background:#000; overflow:hidden; }" +
            "  iframe { position:absolute; top:0; left:0; width:100%; height:100%; border:none; }" +
            "</style></head><body>" +
            "<iframe src='" + embedUrl + "' " +
            "allow='autoplay; fullscreen; encrypted-media; picture-in-picture' " +
            "allowfullscreen='true'></iframe>" +
            "</body></html>";

        Platform.runLater(() -> {
            // Forcer la solution de repli - JavaFX WebKit ne peut pas lire YouTube
            try {
                System.out.println("⚠️ JavaFX WebKit limitation - ouverture navigateur système");
                Desktop.getDesktop().browse(new URI(
                    "https://www.youtube.com/watch?v=" + videoId
                ));
                statusLabel.setText("🌐 Ouvert dans navigateur: " + videoId);
                thumbnailView.setVisible(false);
            } catch (Exception ex) {
                System.err.println("❌ Erreur ouverture navigateur: " + ex.getMessage());
                statusLabel.setText("❌ Erreur navigateur: " + videoId);
            }
        });
    }

    private void onLoadVideo() {
        String url = videoUrlField.getText().trim();
        String videoId = extraireVideoId(url);

        if (videoId == null || videoId.isEmpty()) {
            showError("🔗 URL YouTube invalide");
            return;
        }

        showLoading("🎬 Chargement...");
        Platform.runLater(() -> {
            hideLoading();
            loadVideo(videoId);
        });
    }

    private void onStopVideo() {
        Platform.runLater(() -> {
            webEngine.loadContent(
                "<html><body style='margin:0;background:#000'></body></html>",
                "text/html"
            );
            currentVideoId = null;
            isPlaying = false;
            stopAutoPlay();
            statusLabel.setText("⏹️ Arrêté");
        });
    }

    private String extraireVideoId(String url) {
        if (url == null || url.isEmpty()) return "";
        url = url.trim();
        if (url.contains("v=")) {
            String id = url.split("v=")[1];
            return id.contains("&") ? id.split("&")[0] : id;
        }
        if (url.contains("youtu.be/")) {
            String id = url.split("youtu.be/")[1];
            return id.contains("?") ? id.split("\\?")[0] : id;
        }
        if (url.contains("/shorts/") || url.contains("/embed/")) {
            String[] parts = url.split("/");
            return parts[parts.length - 1].split("\\?")[0];
        }
        if (url.matches("[a-zA-Z0-9_-]{11}")) return url;
        return url;
    }

    private void setupUI() {
        loadingProgress.setVisible(false);
        thumbnailView.setVisible(false);
        playlistContainer.setVisible(false);
        tvModeButton.setStyle("-fx-background-color: #FF6B6B; -fx-text-fill: white;");
        addToPlaylistButton.setStyle("-fx-background-color: #4ECDC4; -fx-text-fill: white;");
        stopVideoButton.setStyle("-fx-background-color: #95A5A6; -fx-text-fill: white;");
    }

    private void setupEventHandlers() {
        videoUrlField.textProperty().addListener((obs, oldVal, newVal) -> {
            String videoId = extraireVideoId(newVal);
            loadVideoButton.setDisable(newVal.isEmpty());
            if (videoId != null && !videoId.isEmpty() && videoId.length() == 11) {
                statusLabel.setText("✅ URL détectée: " + videoId);
                loadThumbnail(videoId);
            } else if (!newVal.isEmpty()) {
                statusLabel.setText("❌ URL invalide");
                thumbnailView.setVisible(false);
            } else {
                statusLabel.setText("📺 Prêt — Entrez une URL YouTube");
                thumbnailView.setVisible(false);
            }
        });

        loadVideoButton.setOnAction(e -> onLoadVideo());
        stopVideoButton.setOnAction(e -> onStopVideo());
        tvModeButton.setOnAction(e -> toggleTVMode());
        addToPlaylistButton.setOnAction(e -> addToPlaylist());
    }

    private void loadThumbnail(String videoId) {
        try {
            Image thumbnail = new Image(
                "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg", true
            );
            thumbnailView.setImage(thumbnail);
            thumbnailView.setVisible(true);
            thumbnailView.setOnMouseClicked(e -> onLoadVideo());
        } catch (Exception e) {
            thumbnailView.setVisible(false);
        }
    }

    private void showLoading(String message) {
        loadingProgress.setVisible(true);
        loadingProgress.setProgress(-1);
        statusLabel.setText(message);
        loadVideoButton.setDisable(true);
    }

    private void hideLoading() {
        loadingProgress.setVisible(false);
        loadVideoButton.setDisable(false);
    }

    private void showError(String message) {
        hideLoading();
        statusLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void toggleTVMode() {
        tvMode = !tvMode;
        if (tvMode) {
            tvModeButton.setText("📺 Mode TV: ON");
            tvModeButton.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white;");
            statusLabel.setText("📺 Mode TV activé");
            if (!playlist.isEmpty()) playlistContainer.setVisible(true);
            startAutoPlay();
        } else {
            tvModeButton.setText("📺 Mode TV");
            tvModeButton.setStyle("-fx-background-color: #FF6B6B; -fx-text-fill: white;");
            statusLabel.setText("📺 Mode TV désactivé");
            playlistContainer.setVisible(false);
            stopAutoPlay();
        }
    }

    private void setupAutoPlay() {
        autoPlayTimeline = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            if (tvMode && isPlaying && !playlist.isEmpty()) playNextVideo();
        }));
        autoPlayTimeline.setCycleCount(Animation.INDEFINITE);
    }

    private void startAutoPlay() {
        if (autoPlayTimeline != null && tvMode) autoPlayTimeline.play();
    }

    private void stopAutoPlay() {
        if (autoPlayTimeline != null) autoPlayTimeline.stop();
    }

    private void playNextVideo() {
        if (!playlist.isEmpty()) {
            currentPlaylistIndex = (currentPlaylistIndex + 1) % playlist.size();
            loadVideo(playlist.get(currentPlaylistIndex));
            statusLabel.setText("📺 Playlist: " + (currentPlaylistIndex + 1) + "/" + playlist.size());
        }
    }

    private void addToPlaylist() {
        if (currentVideoId != null && !playlist.contains(currentVideoId)) {
            playlist.add(currentVideoId);
            updatePlaylistUI();
            statusLabel.setText("➕ Ajouté (" + playlist.size() + " vidéos)");
            if (tvMode) playlistContainer.setVisible(true);
        }
    }

    private void updatePlaylistUI() {
        playlistContainer.getChildren().clear();
        Label title = new Label("📋 Playlist (" + playlist.size() + ")");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");
        playlistContainer.getChildren().add(title);
        for (int i = 0; i < playlist.size(); i++) {
            final int index = i;
            final String vid = playlist.get(i);
            Button btn = new Button((i + 1) + ". " + vid);
            btn.setStyle("-fx-background-color: #34495E; -fx-text-fill: white;");
            btn.setOnAction(e -> { currentPlaylistIndex = index; loadVideo(vid); });
            playlistContainer.getChildren().add(btn);
        }
    }

    // API publique
    public void loadVideoById(String videoId) {
        // Mettre l'URL dans le champ
        Platform.runLater(() -> {
            videoUrlField.setText(
                "https://www.youtube.com/watch?v=" + videoId
            );
        });
        // Charger directement
        loadVideo(videoId);
    }
    public void setPlaylist(List<String> ids) {
        playlist = new ArrayList<>(ids);
        currentPlaylistIndex = 0;
        updatePlaylistUI();
        if (!playlist.isEmpty()) loadVideo(playlist.get(0));
    }
    public void stopPlaylist() { stopAutoPlay(); }
    public void playVideoSmart(String videoId, String url) { loadVideo(videoId); }
    public void showCacheStats() { statusLabel.setText("📊 " + YouTubeService.getCacheStats()); }
    public void clearCache() { YouTubeService.clearCache(); statusLabel.setText("🗑️ Cache vidé"); }
    public void enableApiMode() {
        YouTubeServiceFallback.setValidationMode(YouTubeServiceFallback.ValidationMode.API_FULL);
        statusLabel.setText("🔑 Mode API activé");
    }
    public void enableFallbackMode() {
        YouTubeServiceFallback.setValidationMode(YouTubeServiceFallback.ValidationMode.FALLBACK);
        statusLabel.setText("🔄 Mode fallback activé");
    }
    public void showServiceStatus() {
        YouTubeServiceFallback.printServiceStatus();
        statusLabel.setText("📊 Statut dans console");
    }
}
