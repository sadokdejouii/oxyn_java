package org.example.digitalwill;

import org.example.dao.UserDAO;
import org.example.entities.User;
import org.example.notifications.SendGridConfig;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Workflow Digital Will (comptes inactifs / succession).
 *
 * Mode test: régler OXYN_DIGITAL_WILL_INACTIVE_AFTER=PT2M pour déclencher en quelques minutes.
 */
public final class DigitalWillService {

    private static volatile boolean maintenanceStarted = false;

    private final DigitalWillConfig.Cfg cfg = DigitalWillConfig.load();
    private final DigitalWillDAO dao = new DigitalWillDAO();
    private UserDAO userDAO;

    private UserDAO users() throws SQLException {
        if (userDAO == null) {
            userDAO = new UserDAO();
        }
        return userDAO;
    }

    public void startMaintenanceOnceAsync() {
        if (maintenanceStarted) return;
        maintenanceStarted = true;
        CompletableFuture.runAsync(() -> {
            try {
                runMaintenanceOnce();
            } catch (Exception e) {
                System.err.println("[DigitalWill] maintenance error: " + e.getMessage());
            } finally {
                // allow future runs after some time if needed; for now keep "once per app run"
            }
        });
    }

    /**
     * Cherche les comptes inactifs et applique le workflow configuré.
     */
    public void runMaintenanceOnce() throws SQLException {
        Instant cutoff = Instant.now().minus(cfg.inactiveAfter());
        List<User> inactive = users().findInactiveUsersBefore(cutoff, 200);
        for (User u : inactive) {
            applyWorkflow(u);
        }
    }

    private void applyWorkflow(User u) throws SQLException {
        switch (cfg.mode()) {
            case ARCHIVE -> archive(u);
            case LEGACY -> legacy(u);
            case TRANSFER -> transfer(u);
            case DELETE -> deleteGdpr(u);
        }
    }

    private void archive(User u) throws SQLException {
        // Archivage simple: désactiver le compte + event
        if (u.isActive()) {
            u.setActive(false);
            users().updateUser(u);
        }
        dao.addEvent(u.getId(), "ARCHIVED", "Compte archivé (inactivité)");
    }

    private void legacy(User u) throws SQLException {
        // Mode héritage: pour l'instant on journalise (les groupes seraient gérés dans une table dédiée)
        dao.addEvent(u.getId(), "LEGACY_MODE", "Mode héritage activé (inactivité)");
    }

    private void transfer(User u) throws SQLException {
        var contactOpt = dao.getTrustedContact(u.getId());
        if (contactOpt.isEmpty() || !contactOpt.get().enabled()) {
            dao.addEvent(u.getId(), "TRANSFER_SKIPPED", "Aucun contact de confiance configuré");
            return;
        }
        var c = contactOpt.get();

        String subject = "Succession numérique — compte inactif (OXYN)";
        String body = String.format(Locale.ROOT,
                "Bonjour%s,\n\n" +
                "Vous avez été désigné comme contact de confiance pour le compte OXYN associé à %s.\n" +
                "Ce compte est inactif depuis une période configurée (workflow Digital Will).\n\n" +
                "Action: transfert/accès aux données (mode DEMO).\n\n" +
                "— OXYN Sécurité",
                (c.name() != null && !c.name().isBlank()) ? (" " + c.name()) : "",
                u.getEmail()
        );

        sendPlainEmail(c.email(), subject, body);
        dao.addEvent(u.getId(), "TRANSFER_NOTIFIED", "Notification envoyée au contact: " + c.email());
    }

    private void deleteGdpr(User u) throws SQLException {
        // RGPD: suppression physique (dangereux) → on journalise avant suppression
        dao.addEvent(u.getId(), "DELETE_REQUESTED", "Suppression RGPD (inactivité)");
        users().deleteUser(u.getId());
    }

    private static void sendPlainEmail(String toEmail, String subject, String body) {
        try {
            SendGridConfig.Cfg cfg = SendGridConfig.load();
            String apiKey = cfg.apiKey();
            String from = cfg.fromEmail();
            if (apiKey == null || apiKey.isBlank() || from == null || from.isBlank()) return;

            Mail mail = new Mail();
            mail.setFrom(new Email(from.trim()));
            mail.setSubject(subject);
            mail.addContent(new Content("text/plain", body));

            Personalization p = new Personalization();
            p.addTo(new Email(toEmail.trim()));
            mail.addPersonalization(p);

            SendGrid sg = new SendGrid(apiKey.trim());
            Request req = new Request();
            req.setMethod(Method.POST);
            req.setEndpoint("mail/send");
            req.setBody(mail.build());
            Response resp = sg.api(req);
            int code = resp.getStatusCode();
            if (code < 200 || code >= 300) {
                System.err.println("[DigitalWill] SendGrid HTTP " + code + ": " + resp.getBody());
            } else if (cfg.debug()) {
                System.out.println("[DigitalWill] Notification sent to " + toEmail);
            }
        } catch (Exception e) {
            System.err.println("[DigitalWill] SendGrid error: " + e.getMessage());
        }
    }
}

