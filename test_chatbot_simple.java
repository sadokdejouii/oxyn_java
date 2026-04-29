import com.oxyn.service.GeminiApiService;

/**
 * Classe de test simple pour le chatbot OXYN
 * Permet de tester rapidement l'API Gemini sans interface JavaFX
 */
public class TestChatbotSimple {
    
    public static void main(String[] args) {
        System.out.println("🧪 Test du Chatbot OXYN");
        System.out.println("=" * 50);
        
        // Créer le service
        GeminiApiService service = new GeminiApiService();
        
        // Vérifier la clé API
        if (!service.isApiKeyConfigured()) {
            System.err.println("❌ Clé API non configurée !");
            System.err.println("Veuillez configurer YOUR_GEMINI_API_KEY dans GeminiApiService.java");
            return;
        }
        
        System.out.println("✅ Clé API configurée");
        System.out.println("🚀 Envoi du message test...");
        
        // Test 1 : Message simple
        service.sendMessage("Bonjour, je suis un test")
            .thenAccept(response -> {
                System.out.println("✅ Réponse reçue :");
                System.out.println("-" * 30);
                System.out.println(response);
                System.out.println("-" * 30);
                
                // Test 2 : Question spécifique OXYN
                testQuestionOXYN(service);
            })
            .exceptionally(throwable -> {
                System.err.println("❌ Erreur lors du test 1 : " + throwable.getMessage());
                return null;
            });
        
        // Attendre un peu pour voir les résultats
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("🏁 Test terminé !");
    }
    
    private static void testQuestionOXYN(GeminiApiService service) {
        service.sendMessage("Quels exercices pour débutants à California Gym Tunis ?")
            .thenAccept(response -> {
                System.out.println("✅ Réponse OXYN reçue :");
                System.out.println("-" * 30);
                System.out.println(response);
                System.out.println("-" * 30);
            })
            .exceptionally(throwable -> {
                System.err.println("❌ Erreur lors du test OXYN : " + throwable.getMessage());
                return null;
            });
    }
}
