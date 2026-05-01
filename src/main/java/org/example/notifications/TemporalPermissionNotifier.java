package org.example.notifications;

import org.example.entities.User;
import org.example.temporal.TemporalPermissionGrant;
import org.example.temporal.TemporalPermissionDAO;
import org.example.temporal.TemporalTrustConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Notifications de renouvellement (permissions qui expirent bientôt).
 * Déclenché côté client (ex: au login).
 */
public final class TemporalPermissionNotifier {

    private TemporalPermissionNotifier() {}

    public static void notifyExpiringSoonAsync(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return;
        CompletableFuture.runAsync(() -> {
            try {
                notifyExpiringSoon(user);
            } catch (Exception e) {
                System.err.println("[TemporalTrust] notify error: " + e.getMessage());
            }
        });
    }

    private static void notifyExpiringSoon(User user) throws Exception {
        TemporalTrustConfig.Cfg cfg = TemporalTrustConfig.load();
        Duration within = cfg.notifyBeforeExpiry();

        TemporalPermissionDAO dao = new TemporalPermissionDAO();
        List<TemporalPermissionGrant> exp = dao.expiringSoonForUser(user.getId(), within, 10);
        if (exp.isEmpty()) return;

        // Anti-spam: si déjà notifié aujourd'hui pour le premier grant, on stop.
        TemporalPermissionGrant first = exp.get(0);
        if (first.lastNotifiedAt() != null) {
            Instant last = first.lastNotifiedAt();
            if (last.isAfter(Instant.now().minus(Duration.ofHours(18)))) {
                return;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Bonjour,\n\n");
        sb.append("Certaines de vos permissions temporaires vont expirer bientôt.\n\n");
        for (TemporalPermissionGrant g : exp) {
            sb.append("- ").append(g.permissionKey());
            if (g.scopeType() != null && g.scopeId() != null) {
                sb.append(" (").append(g.scopeType()).append("=").append(g.scopeId()).append(")");
            }
            sb.append(" → expire le ").append(g.expiresAt()).append("\n");
        }
        sb.append("\nSi vous avez besoin d'un renouvellement, contactez un administrateur.\n\n— OXYN Sécurité");

        // Envoi mail simple via SendGrid (utilise config existante)
        sendPlainEmail(user.getEmail(), "⏳ Permissions bientôt expirées (OXYN)", sb.toString());

        // Marquer notifié (au moins le 1er grant)
        try {
            dao.markNotified(first.id());
        } catch (Exception ignored) {
        }
    }

    private static void sendPlainEmail(String toEmail, String subject, String body) throws Exception {
        SendGridConfig.Cfg cfg = SendGridConfig.load();
        String apiKey = cfg.apiKey();
        String from = cfg.fromEmail();
        if (apiKey == null || apiKey.isBlank() || from == null || from.isBlank()) return;

        com.sendgrid.helpers.mail.Mail mail = new com.sendgrid.helpers.mail.Mail();
        mail.setFrom(new com.sendgrid.helpers.mail.objects.Email(from.trim()));
        mail.setSubject(subject);
        mail.addContent(new com.sendgrid.helpers.mail.objects.Content("text/plain", body));

        com.sendgrid.helpers.mail.objects.Personalization p = new com.sendgrid.helpers.mail.objects.Personalization();
        p.addTo(new com.sendgrid.helpers.mail.objects.Email(toEmail.trim()));
        mail.addPersonalization(p);

        com.sendgrid.SendGrid sg = new com.sendgrid.SendGrid(apiKey.trim());
        com.sendgrid.Request req = new com.sendgrid.Request();
        req.setMethod(com.sendgrid.Method.POST);
        req.setEndpoint("mail/send");
        req.setBody(mail.build());

        com.sendgrid.Response resp = sg.api(req);
        int code = resp.getStatusCode();
        if (code < 200 || code >= 300) {
            System.err.println(String.format(Locale.ROOT, "[TemporalTrust] SendGrid HTTP %d: %s", code, resp.getBody()));
        } else if (cfg.debug()) {
            System.out.println("[TemporalTrust] Expiry reminder sent to " + toEmail);
        }
    }
}

