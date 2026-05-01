package org.example.notifications;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class LoginEmailNotifier {

    private LoginEmailNotifier() {
    }

    public static void notifyLoginAsync(String toEmail, String displayName) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                send(toEmail.trim(), displayName);
            } catch (Exception e) {
                // Notification best-effort: ne doit jamais casser le login.
                SendGridConfig.Cfg cfg = SendGridConfig.load();
                debug(cfg, "SendGrid error: " + e);
            }
        });
    }

    private static void send(String toEmail, String displayName) throws Exception {
        SendGridConfig.Cfg cfg = SendGridConfig.load();
        String apiKey = cfg.apiKey();
        String from = cfg.fromEmail();
        if (apiKey == null || apiKey.isBlank() || from == null || from.isBlank()) {
            debug(cfg, "SendGrid disabled (missing apiKey/from). Set env or " + SendGridConfig.userHomePropsPath());
            return;
        }
        String appName = env("APP_NAME", "OXYN");
        String subject = "Connexion détectée";

        String who = (displayName == null || displayName.isBlank()) ? toEmail : displayName.trim();
        String now = Instant.now().toString();
        String body = String.format(Locale.ROOT,
                "Bonjour %s,\n\nUne connexion à votre compte a été détectée (%s).\n\nSi ce n’était pas vous, changez votre mot de passe et désactivez les accès.\n\n— %s",
                who, now, appName);

        Email fromEmail = new Email(from.trim());
        Email to = new Email(toEmail);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail();
        mail.setFrom(fromEmail);
        mail.setSubject(subject);
        mail.addContent(content);

        // Destinataire (Personalization unique)
        Personalization p = new Personalization();
        p.addTo(to);
        mail.addPersonalization(p);

        SendGrid sg = new SendGrid(apiKey.trim());
        Request req = new Request();
        req.setMethod(Method.POST);
        req.setEndpoint("mail/send");
        req.setBody(mail.build());
        Response resp = sg.api(req);
        int code = resp.getStatusCode();
        if (code < 200 || code >= 300) {
            debug(cfg, "SendGrid non-2xx: " + code + " body=" + safe(resp.getBody()));
        } else {
            debug(cfg, "SendGrid sent: " + code + " to=" + toEmail);
        }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    private static void debug(SendGridConfig.Cfg cfg, String msg) {
        if (cfg == null || !cfg.debug()) {
            return;
        }
        System.out.println("[SendGrid] " + msg);
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.length() > 800 ? s.substring(0, 800) + "…" : s;
    }
}

