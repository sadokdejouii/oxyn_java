package org.example.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class TwilioSmsService {

    /**
     * Configuration Twilio:
     * - Variables d'environnement: TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM_NUMBER
     * - Ou propriétés JVM: -DTWILIO_ACCOUNT_SID=... (mêmes clés)
     */
    private static final String TWILIO_ACCOUNT_SID = "AC71498e9c846daab63e8ab002f26893ff";
    private static final String TWILIO_AUTH_TOKEN = "159a9d95006c3f3685a4208311fc2913";
    private static final String TWILIO_FROM_NUMBER = "+12186168939";

    public void envoyerConfirmationPaiement(String numeroDestination, double total) {
        verifierConfiguration();
        String destination = normaliserNumero(numeroDestination);
        Twilio.init(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN);
        String corps = String.format(
                "OXYN: Paiement confirme (%.2f TND). Votre commande est validee. Merci pour votre achat.",
                total
        );
        Message.creator(
                new PhoneNumber(destination),
                new PhoneNumber(TWILIO_FROM_NUMBER),
                corps
        ).create();
    }

    private static String readConfig(String key) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromProp = System.getProperty(key);
        if (fromProp != null && !fromProp.isBlank()) {
            return fromProp.trim();
        }
        return null;
    }

    private static void verifierConfiguration() {
        if (TWILIO_ACCOUNT_SID == null || TWILIO_ACCOUNT_SID.isBlank() || TWILIO_ACCOUNT_SID.contains("xxxx")) {
            throw new IllegalStateException("TWILIO_ACCOUNT_SID invalide.");
        }
        if (TWILIO_AUTH_TOKEN == null || TWILIO_AUTH_TOKEN.isBlank() || TWILIO_AUTH_TOKEN.contains("xxxx")) {
            throw new IllegalStateException("TWILIO_AUTH_TOKEN invalide.");
        }
        if (TWILIO_FROM_NUMBER == null || TWILIO_FROM_NUMBER.isBlank() || !TWILIO_FROM_NUMBER.startsWith("+")) {
            throw new IllegalStateException("TWILIO_FROM_NUMBER invalide.");
        }
    }

    private static String normaliserNumero(String numeroDestination) {
        if (numeroDestination == null || numeroDestination.isBlank()) {
            throw new IllegalStateException("Numero client introuvable.");
        }
        String brut = numeroDestination.trim().replace(" ", "");
        if (brut.startsWith("+")) {
            return brut;
        }
        String chiffres = brut.replaceAll("[^0-9]", "");
        if (chiffres.startsWith("00")) {
            return "+" + chiffres.substring(2);
        }
        if (chiffres.startsWith("216") && chiffres.length() >= 11) {
            return "+" + chiffres;
        }
        if (chiffres.length() == 8) {
            return "+216" + chiffres;
        }
        throw new IllegalStateException("Numero client non conforme au format international.");
    }
}
