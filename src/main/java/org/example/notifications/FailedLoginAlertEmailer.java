package org.example.notifications;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.*;
import com.sendgrid.helpers.mail.objects.*;

import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Envoie une alerte e-mail avec photo (JPEG en pièce jointe base64)
 * lorsque 3 tentatives de connexion échouent pour un compte.
 */
public final class FailedLoginAlertEmailer {

    private FailedLoginAlertEmailer() {}

    /**
     * Envoie l'alerte en arrière-plan (non bloquant).
     * @param toEmail   adresse du compte ciblé
     * @param photoJpeg bytes JPEG capturés par la webcam (peut être null)
     */
    public static void sendAlertAsync(String toEmail, byte[] photoJpeg) {
        if (toEmail == null || toEmail.isBlank()) return;
        CompletableFuture.runAsync(() -> {
            try {
                send(toEmail.trim(), photoJpeg);
            } catch (Exception e) {
                System.err.println("[FailedLoginAlert] Erreur SendGrid: " + e.getMessage());
            }
        });
    }

    private static void send(String toEmail, byte[] photoJpeg) throws Exception {
        SendGridConfig.Cfg cfg = SendGridConfig.load();
        String apiKey = cfg.apiKey();
        String from   = cfg.fromEmail();
        if (apiKey == null || apiKey.isBlank() || from == null || from.isBlank()) {
            System.err.println("[FailedLoginAlert] SendGrid non configuré — alerte ignorée.");
            return;
        }

        String now     = Instant.now().toString();
        String subject = "⚠️ Tentatives de connexion suspectes sur votre compte";
        String body    = String.format(Locale.ROOT,
            "Bonjour,\n\n" +
            "3 tentatives de connexion échouées ont été détectées sur votre compte (%s).\n\n" +
            "Date/heure : %s\n\n" +
            "%s\n\n" +
            "Si ce n'était pas vous, changez immédiatement votre mot de passe.\n\n— OXYN Sécurité",
            toEmail,
            now,
            (photoJpeg != null && photoJpeg.length > 0)
                ? "Une photo de la personne présente devant l'écran est jointe à cet e-mail."
                : "Aucune image n'a pu être capturée."
        );

        Mail mail = new Mail();
        mail.setFrom(new Email(from.trim()));
        mail.setSubject(subject);
        mail.addContent(new Content("text/plain", body));

        Personalization p = new Personalization();
        p.addTo(new Email(toEmail));
        mail.addPersonalization(p);

        // Joindre la photo si disponible
        if (photoJpeg != null && photoJpeg.length > 0) {
            Attachments att = new Attachments();
            att.setContent(Base64.getEncoder().encodeToString(photoJpeg));
            att.setType("image/jpeg");
            att.setFilename("intrus.jpg");
            att.setDisposition("attachment");
            mail.addAttachments(att);
        }

        SendGrid sg  = new SendGrid(apiKey.trim());
        Request  req = new Request();
        req.setMethod(Method.POST);
        req.setEndpoint("mail/send");
        req.setBody(mail.build());

        Response resp = sg.api(req);
        int code = resp.getStatusCode();
        if (code < 200 || code >= 300) {
            System.err.println("[FailedLoginAlert] SendGrid HTTP " + code + ": " + resp.getBody());
        } else {
            System.out.println("[FailedLoginAlert] Alerte envoyée à " + toEmail);
        }
    }
}

