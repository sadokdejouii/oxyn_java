package org.example.controllers;

import org.example.services.GroqService;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Contrôleur du chatbot IA flottant OXYN
 */
public class ChatBotController implements Initializable {
    
    @FXML private Button fabButton;
    @FXML private VBox chatWindow;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox messagesBox;
    @FXML private TextField inputField;
    @FXML private Button sendBtn;
    @FXML private Label errorBanner;
    
    private GroqService groqService;
    private boolean isChatOpen = false;
    private boolean isLoading = false;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUI();
        setupAnimations();
        addWelcomeMessage();
        try {
            this.groqService = new GroqService();
        } catch (Exception ex) {
            this.groqService = null;
        }
    }
    
    /**
     * Configuration initiale de l'interface
     */
    private void setupUI() {
        // Masquer la fenêtre de chat au démarrage
        chatWindow.setVisible(false);
        chatWindow.setManaged(false);
        fabButton.setVisible(true);
        fabButton.setManaged(true);
        
        // Configurer le bouton FAB
        fabButton.setText("💬");
        fabButton.getStyleClass().add("fab-button");
        
        // Configurer les éléments du chat
        sendBtn.setText("➤");
        sendBtn.getStyleClass().add("send-button");
        inputField.getStyleClass().add("message-input");
        messagesBox.getStyleClass().add("messages-container");
        chatWindow.getStyleClass().add("chat-window");
        scrollPane.getStyleClass().add("scroll-pane");
        
        // Configurer l'action sur le champ de saisie
        inputField.setOnAction(event -> sendMessage());
    }
    
    /**
     * Configuration des animations
     */
    private void setupAnimations() {
        // Animation FAB au démarrage
        ScaleTransition fabAnimation = new ScaleTransition(Duration.millis(800), fabButton);
        fabAnimation.setFromX(0);
        fabAnimation.setFromY(0);
        fabAnimation.setToX(1);
        fabAnimation.setToY(1);
        fabAnimation.play();
    }
    
    /**
     * Ouvre/ferme la fenêtre de chat avec animation fluide
     */
    @FXML
    private void toggleChat() {
        if (isChatOpen) {
            closeChat();
        } else {
            openChat();
        }
    }
    
    /**
     * Ouvre la fenêtre de chat avec animation
     */
    private void openChat() {
        chatWindow.setVisible(true);
        chatWindow.setManaged(true);
        
        // Animation d'ouverture
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), chatWindow);
        scaleTransition.setFromX(0.8);
        scaleTransition.setFromY(0.8);
        scaleTransition.setToX(1.0);
        scaleTransition.setToY(1.0);
        
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(200), chatWindow);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        
        ParallelTransition parallelTransition = new ParallelTransition(scaleTransition, fadeTransition);
        parallelTransition.play();
        
        isChatOpen = true;
        fabButton.setText("✕");
        inputField.requestFocus();
    }
    
    /**
     * Ferme la fenêtre de chat avec animation
     */
    private void closeChat() {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), chatWindow);
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(0.8);
        scaleTransition.setToY(0.8);
        
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(200), chatWindow);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);
        
        ParallelTransition parallelTransition = new ParallelTransition(scaleTransition, fadeTransition);
        parallelTransition.setOnFinished(event -> {
            chatWindow.setVisible(false);
            chatWindow.setManaged(false);
        });
        parallelTransition.play();
        
        isChatOpen = false;
        fabButton.setText("💬");
    }
    
    /**
     * Envoie le message de l'utilisateur et obtient la réponse du bot
     */
    @FXML
    private void sendMessage() {
        if (isLoading) return;
        
        String userMessage = inputField.getText().trim();
        if (userMessage.isEmpty()) return;
        if (groqService == null) {
            showErrorBanner("⚠️ Service Groq non disponible. Vérifiez la configuration.", true);
            return;
        }
        hideErrorBanner();
        
        // Ajouter le message utilisateur
        addUserBubble(userMessage);
        inputField.clear();
        
        // Afficher l'indicateur de chargement
        addTypingIndicator();
        setLoading(true);
        
        // Envoyer à l'API et traiter la réponse
        groqService.askFitnessQuestionAsync(userMessage)
                .thenAccept(response -> Platform.runLater(() -> {
                    removeTypingIndicator();
                    if (response == null || response.isBlank()) {
                        showErrorBanner("Réponse du coach vide. Réessayez.", true);
                    } else {
                        addBotBubble(response);
                    }
                    setLoading(false);
                    scrollToBottom();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        removeTypingIndicator();
                        setLoading(false);

                        // remonter la vraie cause (CompletableFuture enveloppe dans ExecutionException)
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        String msg = cause.getMessage() != null ? cause.getMessage() : "";
                        System.err.println("[ChatBot] Exception type : " + cause.getClass().getName());
                        System.err.println("[ChatBot] Exception message : " + msg);

                        if (cause instanceof IllegalStateException) {
                            // clé API absente — persistant, l'utilisateur doit agir
                            showErrorBanner(
                                "⚠️ Clé API Groq manquante. Vérifiez la configuration dans GroqService.java.",
                                false
                            );

                        } else if (msg.contains("429") || msg.toLowerCase().contains("quota")) {
                            // quota épuisé — temporaire, auto-hide
                            showErrorBanner(
                                "⏳ Quota Groq atteint. Réessayez dans quelques secondes.",
                                true
                            );

                        } else if (msg.toLowerCase().contains("http")) {
                            // autre erreur HTTP (500, 503...) — temporaire
                            showErrorBanner(
                                "🔴 Erreur serveur Groq (" + extractHttpCode(msg) + "). Réessayez.",
                                true
                            );

                        } else if (cause instanceof java.net.UnknownHostException
                                || cause instanceof java.net.SocketTimeoutException
                                || msg.toLowerCase().contains("timeout")
                                || msg.toLowerCase().contains("network")) {
                            // pas de réseau ou timeout
                            showErrorBanner(
                                "📡 Pas de connexion réseau. Vérifiez votre internet.",
                                true
                            );

                        } else {
                            // erreur inconnue — afficher le message brut tronqué
                            String display = msg.length() > 80 ? msg.substring(0, 80) + "…" : msg;
                            showErrorBanner("❌ Erreur inattendue : " + display, true);
                        }

                        scrollToBottom();
                    });
                    return null;
                });
    }
    
    /**
     * Ajoute le message de bienvenue
     */
    private void addWelcomeMessage() {
        String welcomeText = "👋 Bonjour ! Je suis l'assistant OXYN. Comment puis-je vous aider ?";
        addBotBubble(welcomeText);
    }
    
    /**
     * Ajoute une bulle utilisateur à l'interface
     */
    private void addUserBubble(String message) {
        HBox messageContainer = new HBox();
        messageContainer.getStyleClass().add("user-message-container");
        
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("bubble-text");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(240);
        
        messageContainer.getChildren().add(messageLabel);
        messagesBox.getChildren().add(messageContainer);
        scrollToBottom();
    }
    
    /**
     * Ajoute une bulle bot à l'interface
     */
    private void addBotBubble(String message) {
        HBox messageContainer = new HBox();
        messageContainer.getStyleClass().add("bot-message-container");

        // Message avec heure
        VBox messageWrapper = new VBox();
        messageWrapper.getStyleClass().add("bot-bubble");
        
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("bubble-text");
        messageLabel.setWrapText(true);
        
        Label timeLabel = new Label(getCurrentTime());
        timeLabel.getStyleClass().add("message-time");
        
        messageWrapper.getChildren().addAll(messageLabel, timeLabel);
        
        messageContainer.getChildren().add(messageWrapper);
        
        // Animation d'apparition
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(300), messageContainer);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
        
        messagesBox.getChildren().add(messageContainer);
        scrollToBottom();
    }
    
    /**
     * Affiche l'indicateur de frappe animé
     */
    private void addTypingIndicator() {
        HBox typingContainer = new HBox();
        typingContainer.setId("typing");
        typingContainer.getStyleClass().add("typing-container");
        
        Label typingLabel = new Label("● ● ●");
        typingLabel.getStyleClass().add("typing-dots");
        
        typingContainer.getChildren().add(typingLabel);
        messagesBox.getChildren().add(typingContainer);
        
        // Animation de pulsation
        Timeline timeline = new Timeline(
            new javafx.animation.KeyFrame(Duration.ZERO, new javafx.animation.KeyValue(typingLabel.styleProperty(), "-fx-opacity: 0.3")),
            new javafx.animation.KeyFrame(Duration.millis(500), new javafx.animation.KeyValue(typingLabel.styleProperty(), "-fx-opacity: 1.0")),
            new javafx.animation.KeyFrame(Duration.millis(1000), new javafx.animation.KeyValue(typingLabel.styleProperty(), "-fx-opacity: 0.3")),
            new javafx.animation.KeyFrame(Duration.millis(1500), new javafx.animation.KeyValue(typingLabel.styleProperty(), "-fx-opacity: 1.0"))
        );
        timeline.setAutoReverse(true);
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        
        scrollToBottom();
    }
    
    /**
     * Retire l'indicateur de frappe
     */
    private void removeTypingIndicator() {
        messagesBox.getChildren().removeIf(node -> 
            node.getId() != null && node.getId().equals("typing"));
    }
    
    /**
     * Active/désactive l'état de chargement
     */
    private void setLoading(boolean loading) {
        isLoading = loading;
        sendBtn.setDisable(loading);
        inputField.setDisable(loading);
    }
    
    /**
     * Fait défiler vers le bas de la conversation
     */
    private void scrollToBottom() {
        Platform.runLater(() -> {
            scrollPane.setVvalue(1.0);
            scrollPane.requestLayout();
        });
    }
    
    /**
     * Obtient l'heure actuelle formatée
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /** Extrait le code HTTP d'un message style "Erreur Groq HTTP 503" -> "503" */
    private String extractHttpCode(String msg) {
        java.util.regex.Matcher m =
            java.util.regex.Pattern.compile("\\d{3}").matcher(msg);
        return m.find() ? m.group() : "?";
    }

    private void showErrorBanner(String message) {
        if (errorBanner == null) {
            return;
        }
        errorBanner.setText(message);
        errorBanner.setVisible(true);
        errorBanner.setManaged(true);
    }

    private void showErrorBanner(String message, boolean autoHide) {
        showErrorBanner(message);
        if (autoHide) {
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(Duration.seconds(5));
            delay.setOnFinished(event -> hideErrorBanner());
            delay.play();
        }
    }

    private void hideErrorBanner() {
        if (errorBanner == null) {
            return;
        }
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
        errorBanner.setText("");
    }
}
