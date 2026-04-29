package org.example.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.utils.PrimaryStageLayout;
import org.example.integration.SubscriptionIntegration;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("🚀 MainFX.start() - Début de la méthode start()");
        
        System.out.println(" Démarrage des services...");
        // Démarrer les services dans un thread séparé
        Thread servicesThread = new Thread(() -> {
            try {
                SubscriptionIntegration.startServices();
                System.out.println("✅ Services d'abonnement démarrés");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        servicesThread.setDaemon(true); // ← important : s'arrête avec l'app
        servicesThread.setName("subscription-services");
        servicesThread.start();
        
        System.out.println("📂 Chargement du FXML Login...");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/Login.fxml"));
        Parent root = loader.load();
        System.out.println("✅ FXML Login chargé avec succès");

        System.out.println("🎨 Création de la scene...");
        Scene scene = new Scene(root, 1080, 720);

        System.out.println("🖼️ Configuration de la fenêtre...");
        primaryStage.setTitle("OXYN — Connexion");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true); // Alternative temporaire
        
        System.out.println("📺 Affichage de la fenêtre...");
        primaryStage.show();
        System.out.println("✅ Fenêtre affichée avec succès !");
    }

    @Override
    public void stop() {
        // Arrêter proprement les services d'abonnement
        SubscriptionIntegration.stopServices();
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("💥 CRASH dans thread " + thread.getName());
            throwable.printStackTrace();
        });
        System.out.println("🚀 MainFX.main() - Démarrage de l'application");
        System.out.println("📡 Lancement de JavaFX...");
        launch(args);
        System.out.println("❌ JavaFX terminé - ceci ne devrait pas apparaître");
    }
}
