package org.example.notifications;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Envoie un code de réinitialisation de mot de passe.
 */
public final class PasswordResetEmailer {

    private PasswordResetEmailer() {}

    public static void sendCodeAsync(String toEmail, String code, Duration ttl) {
        if (toEmail == null || toEmail.isBlank() || code == null || code.isBlank()) return;
        CompletableFuture.runAsync(() -> {
            try {
                sendCode(toEmail.trim(), code.trim(), ttl);
            } catch (Exception e) {
                System.err.println("[PasswordReset] Erreur SendGrid: " + e.getMessage());
            }
        });
    }

    private static void sendCode(String toEmail, String code, Duration ttl) throws Exception {
        SendGridConfig.Cfg cfg = SendGridConfig.load();
        String apiKey = cfg.apiKey();
        String from   = cfg.fromEmail();
        if (apiKey == null || apiKey.isBlank() || from == null || from.isBlank()) {
            System.err.println("[PasswordReset] SendGrid non configuré — e-mail ignoré.");
            return;
        }

        long minutes = (ttl != null) ? Math.max(1, ttl.toMinutes()) : 10;
        String subject = "Réinitialisation de votre mot de passe OXYN";
        String body = String.format(Locale.ROOT,
                "Bonjour,\n\n" +
                "Voici votre code de réinitialisation : %s\n\n" +
                "Ce code expire dans %d minute(s).\n\n" +
                "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet e-mail.\n\n— OXYN Sécurité",
                code, minutes);

        Mail mail = new Mail();
        mail.setFrom(new Email(from.trim()));
        mail.setSubject(subject);
        mail.addContent(new Content("text/plain", body));

        Personalization p = new Personalization();
        p.addTo(new Email(toEmail));
        mail.addPersonalization(p);

        SendGrid sg = new SendGrid(apiKey.trim());
        Request req = new Request();
        req.setMethod(Method.POST);
        req.setEndpoint("mail/send");
        req.setBody(mail.build());

        Response resp = sg.api(req);
        int codeResp = resp.getStatusCode();
        if (codeResp < 200 || codeResp >= 300) {
            System.err.println("[PasswordReset] SendGrid HTTP " + codeResp + ": " + resp.getBody());
        } else if (cfg.debug()) {
            System.out.println("[PasswordReset] Code envoyé à " + toEmail);
        }
    }
}

