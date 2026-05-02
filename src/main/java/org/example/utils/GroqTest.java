package org.example.utils;

import org.example.services.GroqService;

import java.util.concurrent.CompletableFuture;

/**
 * Test rapide pour vérifier la connexion Groq
 */
public class GroqTest {
    
    public static void main(String[] args) {
        System.out.println("🚀 Test de connexion à Groq API...");
        
        GroqService groqService = new GroqService();
        
        // Test de connexion
        boolean isConnected = groqService.testConnection();
        System.out.println("📡 Connexion: " + (isConnected ? "✅ Succès" : "❌ Échec"));
        
        if (isConnected) {
            // Test de question fitness
            System.out.println("\n🏋️‍♂️ Test de question fitness...");
            CompletableFuture<String> response = groqService.askFitnessQuestionAsync("Donne-moi 3 exercices pour débutants");
            
            response.thenAccept(answer -> {
                System.out.println("💬 Réponse du coach: " + answer);
                System.out.println("\n🎉 Groq est opérationnel !");
            }).exceptionally(throwable -> {
                System.err.println("❌ Erreur: " + throwable.getMessage());
                return null;
            });
            
            // Attendre la réponse
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("❌ Vérifiez votre clé API ou la connexion internet");
        }
    }
}
