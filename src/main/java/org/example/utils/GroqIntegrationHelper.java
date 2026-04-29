package org.example.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.example.services.GroqService;

import java.util.concurrent.CompletableFuture;

/**
 * Helper pour intégrer facilement Groq dans vos contrôleurs existants
 * Remplace l'ancien code Gemini
 */
public class GroqIntegrationHelper {
    
    private final GroqService groqService;
    
    public GroqIntegrationHelper() {
        this.groqService = new GroqService();
    }
    
    /**
     * Configure un bouton pour utiliser Groq au lieu de Gemini
     * @param sendButton Le bouton d'envoi existant
     * @param inputField Le champ de texte de l'utilisateur
     * @param responseLabel Le label où afficher la réponse
     */
    public void setupGroqButton(Button sendButton, TextArea inputField, Label responseLabel) {
        // Remplacer l'ancien action listener Gemini par Groq
        sendButton.setOnAction(e -> {
            String question = inputField.getText().trim();
            
            if (question.isEmpty()) {
                showAlert("Veuillez entrer une question.", Alert.AlertType.WARNING);
                return;
            }
            
            // Désactiver le bouton pendant le traitement
            sendButton.setDisable(true);
            inputField.setDisable(true);
            responseLabel.setText("🤖 Réflexion en cours...");
            
            // Appel asynchrone à Groq
            CompletableFuture<String> responseFuture = groqService.askFitnessQuestionAsync(question);
            
            responseFuture.thenAccept(response -> {
                // Mettre à jour l'UI sur le thread JavaFX
                Platform.runLater(() -> {
                    responseLabel.setText("🏋️‍♂️ " + response);
                    sendButton.setDisable(false);
                    inputField.setDisable(false);
                    inputField.clear();
                });
            }).exceptionally(throwable -> {
                // Gérer les erreurs
                Platform.runLater(() -> {
                    responseLabel.setText("❌ Erreur: " + throwable.getMessage());
                    sendButton.setDisable(false);
                    inputField.setDisable(false);
                    System.err.println("Erreur Groq: " + throwable.getMessage());
                });
                return null;
            });
        });
    }
    
    /**
     * Version simplifiée pour un remplacement rapide
     * @param oldGeminiCode Votre ancien code Gemini à remplacer
     * @return Le nouveau code Groq équivalent
     */
    public static String replaceGeminiWithGroq(String oldGeminiCode) {
        return """
            // Ancien code Gemini (à supprimer):
            // %s
            
            // Nouveau code Groq (à utiliser):
            GroqIntegrationHelper groqHelper = new GroqIntegrationHelper();
            groqHelper.setupGroqButton(sendButton, questionField, responseLabel);
            """.formatted(oldGeminiCode);
    }
    
    /**
     * Test rapide de l'intégration Groq
     */
    public void quickTest() {
        CompletableFuture<String> test = groqService.askFitnessQuestionAsync("Donne-moi 3 exercices pour débutants");
        
        test.thenAccept(response -> {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("✅ Test Groq réussi !");
                alert.setHeaderText("L'intégration fonctionne !");
                alert.setContentText("Réponse test: " + response);
                alert.show();
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("❌ Test Groq échoué");
                alert.setHeaderText("Vérifiez votre clé API");
                alert.setContentText("Erreur: " + e.getMessage());
                alert.show();
            });
            return null;
        });
    }
    
    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Exemple d'utilisation dans un contrôleur existant
     */
    public static void exampleIntegration() {
        /*
        DANS VOTRE CONTRÔLEUR EXISTANT:
        
        1. Importez la classe:
        import org.example.utils.GroqIntegrationHelper;
        
        2. Dans la méthode initialize():
        @Override
        public void initialize() {
            GroqIntegrationHelper groqHelper = new GroqIntegrationHelper();
            groqHelper.setupGroqButton(votreBoutonEnvoyer, votreChampTexte, votreLabelReponse);
            
            // Optionnel: tester la connexion
            groqHelper.quickTest();
        }
        
        3. Supprimez tout votre ancien code Gemini
        */
    }
}
