package org.example.controllers;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.example.entities.User;
import org.example.services.CommandesService;
import org.example.services.SessionContext;
import org.example.services.StripePaymentSession;
import org.example.services.TwilioSmsService;

public class PaiementEnLigneController {

    @FXML
    private Label commandeInfoLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private WebView stripeWebView;

    private MainLayoutController mainLayoutController;
    private final StripePaymentSession paymentSession = StripePaymentSession.getInstance();
    private final CommandesService commandesService = new CommandesService();
    private final TwilioSmsService twilioSmsService = new TwilioSmsService();

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    @FXML
    public void initialize() {
        if (!paymentSession.hasActiveSession()) {
            messageLabel.setText("Session de paiement introuvable.");
            if (commandeInfoLabel != null) {
                commandeInfoLabel.setText("Aucune commande en attente de paiement.");
            }
            return;
        }
        commandeInfoLabel.setText(String.format("Total %.2f TND",
                paymentSession.getTotalCommande()));
        initialiserPageStripe();
    }

    @FXML
    private void handleRetourBoutique() {
        if (mainLayoutController != null) {
            mainLayoutController.navigate("/FXML/pages/ClientBoutique.fxml", "Boutique", null);
        }
    }

    private void initialiserPageStripe() {
        WebEngine webEngine = stripeWebView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.setOnAlert(event -> gererMessageStripe(event.getData()));
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                lancerCheckout(webEngine);
            } else if (newState == Worker.State.FAILED) {
                messageLabel.setText("Impossible de charger l'interface Stripe.");
            }
        });
        String resource = getClass().getResource("/web/stripe-checkout.html").toExternalForm();
        webEngine.load(resource);
    }

    private void lancerCheckout(WebEngine webEngine) {
        String clientSecret = toJsString(paymentSession.getClientSecret());
        String publishableKey = toJsString(paymentSession.getPublishableKey());
        webEngine.executeScript("window.startCheckout(" + clientSecret + ", " + publishableKey + ");");
        messageLabel.setText("Entrez vos informations de carte pour finaliser le paiement.");
    }

    private void gererMessageStripe(String payload) {
        if (payload == null) {
            return;
        }
        if (payload.startsWith("PAYMENT_SUCCESS")) {
            confirmerCommandeEtNotifier();
            return;
        }
        if (payload.startsWith("PAYMENT_ERROR:")) {
            String detail = payload.substring("PAYMENT_ERROR:".length());
            messageLabel.setText("Paiement refusé : " + detail);
            return;
        }
        messageLabel.setText(payload);
    }

    private void confirmerCommandeEtNotifier() {
        try {
            int idCommande = paymentSession.getIdCommande();
            int idClient = paymentSession.getIdClient();
            double totalCommande = paymentSession.getTotalCommande();
            boolean updated = commandesService.confirmerPaiementCommande(
                    idCommande,
                    idClient
            );
            if (updated) {
                String smsErreur = envoyerSmsConfirmation(idCommande, totalCommande);
                messageLabel.setText("Paiement accepté. Statut de la commande mis à jour.");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Paiement confirmé");
                alert.setHeaderText(null);
                String contenu = "Le paiement est validé. Votre commande est maintenant validée.";
                if (smsErreur != null) {
                    contenu += "\nSMS non envoyé: " + smsErreur;
                }
                alert.setContentText(contenu);
                alert.showAndWait();
                paymentSession.clear();
                if (mainLayoutController != null) {
                    mainLayoutController.navigate("/FXML/pages/MesCommandes.fxml", "Mes commandes", null);
                }
            } else {
                messageLabel.setText("Paiement reçu, mais mise à jour du statut impossible.");
            }
        } catch (Exception e) {
            messageLabel.setText("Paiement validé, mais erreur de sauvegarde: " + e.getMessage());
        }
    }

    private String envoyerSmsConfirmation(int idCommande, double totalCommande) {
        try {
            User user = SessionContext.getInstance().getCurrentUser();
            String telephone = user != null ? user.getTelephone() : null;
            twilioSmsService.envoyerConfirmationPaiement(
                    telephone,
                    totalCommande
            );
            return null;
        } catch (IllegalStateException e) {
            return e.getMessage();
        } catch (Exception ignored) {
            return "Erreur Twilio.";
        }
    }

    private static String toJsString(String input) {
        if (input == null) {
            return "''";
        }
        String escaped = input
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "'" + escaped + "'";
    }
}
