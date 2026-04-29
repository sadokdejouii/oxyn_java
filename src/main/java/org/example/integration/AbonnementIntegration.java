package org.example.integration;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.example.controllers.AbonnementController;
import org.example.entities.Salle;

import java.io.IOException;

/**
 * Classe d'intégration pour ouvrir la fenêtre de gestion des abonnements
 * Utilisée par les autres contrôleurs pour lancer la fenêtre
 */
public class AbonnementIntegration {
    
    /**
     * Ouvre la fenêtre de gestion des abonnements pour une salle spécifique
     * @param salle La salle dont on veut gérer les offres
     * @param ownerStage La fenêtre parente (peut être null)
     * @return Le contrôleur de la fenêtre pour interaction
     */
    public static AbonnementController ouvrirFenetreAbonnements(Salle salle, Stage ownerStage) {
        try {
            // Charger le FXML
            FXMLLoader loader = new FXMLLoader(
                AbonnementIntegration.class.getResource("/fxml/abonnement.fxml")
            );
            
            Parent root = loader.load();
            AbonnementController controller = loader.getController();
            
            // Créer et configurer la fenêtre
            Stage stage = new Stage();
            stage.setTitle("Gestion des Abonnements - " + (salle != null ? salle.getName() : "Salle"));
            stage.setScene(new Scene(root));
            
            // Configuration de la fenêtre
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.DECORATED);
            stage.setResizable(true);
            stage.setMinWidth(600);
            stage.setMinHeight(500);
            
            // Définir la fenêtre parente si fournie
            if (ownerStage != null) {
                stage.initOwner(ownerStage);
            }
            
            // Transmettre la salle au contrôleur
            controller.setSalle(salle);
            
            // Afficher la fenêtre et attendre la fermeture
            stage.showAndWait();
            
            return controller;
            
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de l'ouverture de la fenêtre des abonnements : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Ouvre la fenêtre de gestion des abonnements (sans fenêtre parente)
     * @param salle La salle dont on veut gérer les offres
     * @return Le contrôleur de la fenêtre pour interaction
     */
    public static AbonnementController ouvrirFenetreAbonnements(Salle salle) {
        return ouvrirFenetreAbonnements(salle, null);
    }
    
    /**
     * Exemple d'utilisation depuis un autre contrôleur
     */
    public static class ExempleUtilisation {
        
        /**
         * Exemple : Bouton dans ClientSalleDetailController
         */
        public void handleGererAbonnements(Salle salle) {
            // Ouvrir la fenêtre des abonnements
            AbonnementController controller = AbonnementIntegration.ouvrirFenetreAbonnements(salle, getCurrentStage());
            
            if (controller != null && controller.getSalleActuelle() != null) {
                // La fenêtre a été fermée, on peut récupérer des informations si nécessaire
                System.out.println("📊 Nombre d'offres gérées : " + controller.getListeOffres().size());
                
                // Rafraîchir les données dans la fenêtre parente si nécessaire
                rafraichirDonneesParentes();
            }
        }
        
        /**
         * Exemple : Menu principal
         */
        public void handleMenuAbonnements() {
            // Demander à l'utilisateur de choisir une salle
            Salle salle = demanderChoixSalle();
            
            if (salle != null) {
                AbonnementIntegration.ouvrirFenetreAbonnements(salle);
            }
        }
        
        private Stage getCurrentStage() {
            // Implémenter selon votre contexte
            return null;
        }
        
        private Salle demanderChoixSalle() {
            // Implémenter une boîte de dialogue pour choisir une salle
            return null;
        }
        
        private void rafraichirDonneesParentes() {
            // Implémenter le rafraîchissement des données dans la fenêtre parente
        }
    }
    
    /**
     * Configuration pour les tests
     */
    public static class TestConfiguration {
        
        /**
         * Test d'ouverture avec une salle fictive
         */
        public static void testerOuverture() {
            // Créer une salle de test
            Salle salleTest = new Salle();
            salleTest.setId(1);
            salleTest.setName("Salle de Test");
            salleTest.setAddress("Adresse de test");
            
            // Ouvrir la fenêtre
            AbonnementController controller = AbonnementIntegration.ouvrirFenetreAbonnements(salleTest);
            
            if (controller != null) {
                System.out.println("✅ Test réussi : Fenêtre ouverte avec succès");
                System.out.println("📍 Salle : " + controller.getSalleActuelle().getName());
            } else {
                System.err.println("❌ Test échoué : Impossible d'ouvrir la fenêtre");
            }
        }
    }
    
    /**
     * Méthodes utilitaires pour la gestion des erreurs
     */
    public static class Utils {
        
        /**
         * Affiche une erreur d'initialisation
         */
        public static void afficherErreurInitialisation(Exception e) {
            System.err.println("❌ Erreur d'initialisation de la fenêtre des abonnements :");
            System.err.println("   Message : " + e.getMessage());
            System.err.println("   Cause   : " + e.getCause());
            System.err.println("   Solution : Vérifiez que le fichier FXML existe et est correct");
        }
        
        /**
         * Vérifie que la salle est valide avant d'ouvrir la fenêtre
         */
        public static boolean validerSalle(Salle salle) {
            if (salle == null) {
                System.err.println("❌ La salle ne peut pas être null");
                return false;
            }
            
            if (salle.getId() <= 0) {
                System.err.println("❌ L'ID de la salle est invalide : " + salle.getId());
                return false;
            }
            
            if (salle.getName() == null || salle.getName().trim().isEmpty()) {
                System.err.println("❌ Le nom de la salle est invalide");
                return false;
            }
            
            return true;
        }
    }
}
