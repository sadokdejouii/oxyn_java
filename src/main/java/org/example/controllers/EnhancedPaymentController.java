package org.example.controllers;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import org.example.services.EnhancedFlouciService;
import org.example.services.SubscriptionOrderService;
import org.example.services.SubscriptionLifecycleService;
import org.example.entities.SubscriptionOffer;
import org.example.utils.ConfigManager;

import java.awt.Desktop;
import java.net.URI;

/**
 * Contrôleur amélioré pour la gestion des paiements avec annulation et retries
 */
public class EnhancedPaymentController {

    private final EnhancedFlouciService flouciService;
    private final SubscriptionOrderService orderService;
    private final SubscriptionLifecycleService lifecycleService;
    private final ConfigManager config;

    public EnhancedPaymentController() {
        this.flouciService = new EnhancedFlouciService();
        this.orderService = new SubscriptionOrderService();
        this.lifecycleService = new SubscriptionLifecycleService();
        this.config = ConfigManager.getInstance();
    }

    /**
     * Gérer le paiement avec interface utilisateur améliorée
     */
    public void handlePayment(SubscriptionOffer offre, int userId) {
        try {
            // 1. Confirmation initiale
            if (!showPaymentConfirmation(offre)) {
                return;
            }

            // 2. Créer le paiement
            EnhancedFlouciService.FlouciPayment payment = flouciService.createPayment(
                offre.getPrice(), 
                "Abonnement " + offre.getName()
            );

            if (!payment.success) {
                showError("Erreur", "Impossible de créer le paiement Flouci");
                return;
            }

            // 3. Créer une commande en attente dans la BD
            try {
                // Créer une commande PENDING pour suivre le paiement
                // orderService.createPendingOrder(userId, offre.getId(), payment.paymentId, offre.getPrice());
            } catch (Exception e) {
                System.err.println("⚠️ Impossible de créer la commande PENDING: " + e.getMessage());
            }

            // 4. Ouvrir le navigateur
            Desktop.getDesktop().browse(new URI(payment.link));

            // 5. Afficher la fenêtre de vérification avec gestion d'annulation
            showEnhancedVerificationDialog(payment.paymentId, offre, userId);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'initier le paiement: " + e.getMessage());
        }
    }

    /**
     * Afficher la fenêtre de confirmation initiale
     */
    private boolean showPaymentConfirmation(SubscriptionOffer offre) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Paiement Flouci");
        confirm.setHeaderText("S'abonner : " + offre.getName());
        confirm.setContentText(
            "Durée : " + offre.getDurationMonths() + " mois\n" +
            "Prix  : " + String.format("%.2f", offre.getPrice()) + " TND\n\n" +
            "Vous allez être redirigé vers Flouci pour payer.\n" +
            "Environnement : " + (config.isProduction() ? "Production" : "Test")
        );

        ButtonType btnPayer = new ButtonType("Payer avec Flouci");
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnPayer, btnAnnuler);

        return confirm.showAndWait().filter(result -> result == btnPayer).isPresent();
    }

    /**
     * Afficher la fenêtre de vérification améliorée avec gestion d'annulation
     */
    private void showEnhancedVerificationDialog(String paymentId, SubscriptionOffer offre, int userId) {
        Alert waiting = new Alert(Alert.AlertType.CONFIRMATION);
        waiting.setTitle("Vérification Paiement");
        waiting.setHeaderText("⏳ Paiement en cours");
        waiting.setContentText(
            "1. Complétez le paiement sur la page Flouci ouverte\n" +
            "2. Cliquez sur 'Vérifier' pour confirmer le paiement\n" +
            "3. Ou 'Annuler' si vous ne souhaitez plus payer\n\n" +
            "ID Paiement : " + paymentId
        );

        ButtonType btnVerifier = new ButtonType("Vérifier le paiement");
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        waiting.getButtonTypes().setAll(btnVerifier, btnAnnuler);

        waiting.showAndWait().ifPresent(result -> {
            if (result == btnVerifier) {
                handlePaymentVerification(paymentId, offre, userId);
            } else if (result == btnAnnuler || result == ButtonType.CANCEL) {
                handlePaymentCancellation(paymentId);
            }
        });
    }

    /**
     * Gérer la vérification du paiement avec retries
     */
    private void handlePaymentVerification(String paymentId, SubscriptionOffer offre, int userId) {
        // Afficher une alerte de progression
        Alert progress = new Alert(Alert.AlertType.INFORMATION);
        progress.setTitle("Vérification");
        progress.setHeaderText("Vérification du paiement en cours...");
        progress.setContentText("Veuillez patienter pendant que nous vérifions votre paiement.");
        progress.show();

        // Lancer la vérification en arrière-plan
        new Thread(() -> {
            try {
                int maxRetries = config.getFlouciMaxRetries();
                EnhancedFlouciService.PaymentVerificationResult result = 
                    flouciService.verifyPaymentWithRetry(paymentId, maxRetries);

                // Fermer l'alerte de progression
                javafx.application.Platform.runLater(progress::close);

                // Afficher le résultat
                javafx.application.Platform.runLater(() -> {
                    if (result.success) {
                        handlePaymentSuccess(paymentId, offre, userId);
                    } else {
                        handlePaymentFailure(paymentId, result.message);
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    progress.close();
                    showError("Erreur", "Erreur lors de la vérification: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Gérer le succès du paiement
     */
    private void handlePaymentSuccess(String paymentId, SubscriptionOffer offre, int userId) {
        try {
            // Flux : PENDING → PAYÉ → ACTIVE
            lifecycleService.onFlouciConfirmed(paymentId);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Paiement Réussi");
            success.setHeaderText("✅ Abonnement Activé !");
            success.setContentText(
                "Votre paiement a été confirmé avec succès.\n" +
                "Abonnement : " + offre.getName() + "\n" +
                "Durée : " + offre.getDurationMonths() + " mois\n" +
                "ID Paiement : " + paymentId + "\n" +
                "Statut : ACTIF"
            );
            success.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Paiement confirmé mais erreur lors de l'activation: " + e.getMessage());
        }
    }

    /**
     * Gérer l'échec du paiement après retries
     */
    private void handlePaymentFailure(String paymentId, String errorMessage) {
        // Marquer comme échec dans la BD
        flouciService.handleVerificationFailure(paymentId, errorMessage);

        Alert failure = new Alert(Alert.AlertType.ERROR);
        failure.setTitle("Échec Paiement");
        failure.setHeaderText("❌ Paiement non confirmé");
        failure.setContentText(
            "Impossible de confirmer votre paiement après plusieurs tentatives.\n" +
            "Veuillez vérifier que le paiement a bien été effectué.\n\n" +
            "Si le problème persiste, contactez le support avec cet ID : " + paymentId + "\n" +
            "Détail erreur : " + errorMessage
        );
        failure.show();
    }

    /**
     * Gérer l'annulation du paiement par l'utilisateur
     */
    private void handlePaymentCancellation(String paymentId) {
        // Confirmer l'annulation
        Alert confirmCancel = new Alert(Alert.AlertType.CONFIRMATION);
        confirmCancel.setTitle("Annulation Paiement");
        confirmCancel.setHeaderText("Annuler le paiement ?");
        confirmCancel.setContentText(
            "Êtes-vous sûr de vouloir annuler ce paiement ?\n" +
            "Aucun montant ne sera débité de votre compte."
        );

        ButtonType btnConfirmerAnnulation = new ButtonType("Oui, annuler");
        ButtonType btnRetour = new ButtonType("Non, continuer", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmCancel.getButtonTypes().setAll(btnConfirmerAnnulation, btnRetour);

        confirmCancel.showAndWait().ifPresent(result -> {
            if (result == btnConfirmerAnnulation) {
                // Traiter l'annulation
                flouciService.handleUserCancellation(paymentId);

                Alert cancelled = new Alert(Alert.AlertType.INFORMATION);
                cancelled.setTitle("Paiement Annulé");
                cancelled.setHeaderText("🚫 Paiement annulé");
                cancelled.setContentText(
                    "Le paiement a été annulé avec succès.\n" +
                    "Aucun montant n'a été débité.\n" +
                    "ID Paiement : " + paymentId
                );
                cancelled.show();
            } else {
                // Retour à la vérification
                showEnhancedVerificationDialog(paymentId, null, 0); // offre et userId non nécessaires ici
            }
        });
    }

    /**
     * Afficher une erreur
     */
    private void showError(String title, String message) {
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setTitle(title);
        error.setHeaderText("❌ Erreur");
        error.setContentText(message);
        error.show();
    }
}
