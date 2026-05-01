package org.example.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import org.example.controllers.MainLayoutController;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public final class PageLoader {

    private PageLoader() {
    }

    public static void show(StackPane target, String classpathResource) throws IOException {
        show(target, classpathResource, null);
    }

    public static void show(StackPane target, String classpathResource, MainLayoutController mainLayoutController) throws IOException {
        Objects.requireNonNull(target, "target");
        URL url = PageLoader.class.getResource(classpathResource);
        if (url == null) {
            throw new IOException("Resource not found: " + classpathResource);
        }
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        
        // Injecter le MainLayoutController si le controller de la page le supporte
        Object controller = loader.getController();
        if (controller != null && mainLayoutController != null) {
            try {
                // Vérifier si le controller a une méthode setMainLayoutController
                controller.getClass().getMethod("setMainLayoutController", MainLayoutController.class)
                    .invoke(controller, mainLayoutController);
            } catch (NoSuchMethodException e) {
                // Le controller n'a pas de méthode setMainLayoutController, c'est normal
            } catch (Exception e) {
                // Erreur lors de l'injection, on continue quand même
                System.err.println("Erreur lors de l'injection du MainLayoutController: " + e.getMessage());
            }
        }
        
        target.getChildren().setAll(root);
    }
}
