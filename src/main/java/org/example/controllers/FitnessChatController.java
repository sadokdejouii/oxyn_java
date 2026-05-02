package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.example.services.GroqService;

import java.util.concurrent.CompletableFuture;

/**
 * Contrôleur JavaFX pour le chat fitness avec Groq
 * Remplace l'ancienne intégration Gemini
 */
public class FitnessChatController {
    
    @FXML private TextArea questionTextArea;
    @FXML private Button sendButton;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesContainer;
    @FXML private ProgressBar loadingIndicator;
    @FXML private Label statusLabel;
    
    private final GroqService groqService = new GroqService();
    
    @FXML
    public void initialize() {
        // Initialiser l'interface
        setupUI();
        
        // Message de bienvenue
        addBotMessage("🏋️‍♂️ Bonjour ! Je suis votre coach fitness virtuel. Demandez-moi tout sur le yoga, pilates, musculation et fitness !");
    }
    
    private void setupUI() {
        // Configurer le bouton d'envoi
        sendButton.setOnAction(e -> sendQuestion());
        
        // Configurer le TextArea pour envoyer avec Enter
        questionTextArea.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER") && !e.isShiftDown()) {
                e.consume();
                sendQuestion();
            }
        });
        
        // Masquer l'indicateur de chargement
        loadingIndicator.setVisible(false);
        
        // Message de statut
        statusLabel.setText("Prêt à vous aider !");
    }
    
    @FXML
    private void sendQuestion() {
        String question = questionTextArea.getText().trim();
        
        if (question.isEmpty()) {
            showAlert("Veuillez entrer une question.", Alert.AlertType.WARNING);
            return;
        }
        
        // Ajouter la question de l'utilisateur au chat
        addUserMessage(question);
        
        // Vider le champ de question
        questionTextArea.clear();
        
        // Désactiver le bouton et montrer le chargement
        setLoadingState(true);
        
        // Envoyer la question à Groq de manière asynchrone
        CompletableFuture<String> responseFuture = groqService.askFitnessQuestionAsync(question);
        
        responseFuture.thenAccept(response -> {
            // Mettre à jour l'UI sur le thread JavaFX
            Platform.runLater(() -> {
                addBotMessage(response);
                setLoadingState(false);
                statusLabel.setText("Réponse reçue !");
            });
        }).exceptionally(throwable -> {
            // Gérer les erreurs
            Platform.runLater(() -> {
                addBotMessage("❌ Désolé, une erreur est survenue. Réessayez plus tard.");
                setLoadingState(false);
                statusLabel.setText("Erreur de connexion");
                System.err.println("Erreur Groq: " + throwable.getMessage());
            });
            return null;
        });
    }
    
    private void addUserMessage(String message) {
        TextFlow messageFlow = createMessageFlow("Vous: " + message, "#2196F3", true);
        chatMessagesContainer.getChildren().add(messageFlow);
        scrollToBottom();
    }
    
    private void addBotMessage(String message) {
        TextFlow messageFlow = createMessageFlow("Coach: " + message, "#4CAF50", false);
        chatMessagesContainer.getChildren().add(messageFlow);
        scrollToBottom();
    }
    
    private TextFlow createMessageFlow(String message, String color, boolean isUser) {
        TextFlow flow = new TextFlow();
        flow.setStyle("-fx-padding: 10px; -fx-background-color: " + 
                     (isUser ? "#E3F2FD" : "#E8F5E8") + 
                     "; -fx-background-radius: 10px; -fx-margin: 5px;");
        
        Text text = new Text(message);
        text.setStyle("-fx-fill: " + color + "; -fx-font-size: 14px; -fx-font-weight: " +
                     (isUser ? "bold" : "normal") + ";");
        
        flow.getChildren().add(text);
        return flow;
    }
    
    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(1.0);
        });
    }
    
    private void setLoadingState(boolean loading) {
        sendButton.setDisable(loading);
        questionTextArea.setDisable(loading);
        loadingIndicator.setVisible(loading);
        
        if (loading) {
            statusLabel.setText("Envoi en cours...");
        } else {
            statusLabel.setText("Prêt à vous aider !");
        }
    }
    
    @FXML
    private void clearChat() {
        chatMessagesContainer.getChildren().clear();
        addBotMessage("🏋️‍♂️ Chat effacé. Quelle est votre question ?");
    }
    
    @FXML
    private void testConnection() {
        setLoadingState(true);
        statusLabel.setText("Test de connexion...");
        
        CompletableFuture.supplyAsync(() -> (boolean) groqService.testConnection())
                .thenAccept(success -> {
                    Platform.runLater(() -> {
                        setLoadingState(false);
                        if (success) {
                            statusLabel.setText("✅ Connexion Groq réussie !");
                            addBotMessage("✅ La connexion avec le coach est établie !");
                        } else {
                            statusLabel.setText("❌ Connexion Groq échouée");
                            addBotMessage("❌ Vérifiez votre clé API Groq dans le code.");
                        }
                    });
                });
    }
    
    private void showAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
