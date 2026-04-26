package org.example.realtime;

import com.google.gson.JsonObject;

import java.util.function.Consumer;

/**
 * Synchronisation temps réel des changements Planning entre client et encadrant.
 *
 * <p>Topic unique par utilisateur : {@code /planning/user/{userId}}. Tout
 * changement qui concerne <em>la vue planning d'un user</em> (tâche cochée,
 * intervention encadrant, objectif IA modifié...) déclenche un publish sur
 * le topic du user concerné.</p>
 *
 * <p>Côté réception : les contrôleurs Planning (client / encadrant) s'abonnent
 * via {@link #subscribeForCurrentUser(Consumer)} et déclenchent un refresh
 * <em>ciblé</em> (jamais un reload complet de page) selon le {@code kind}
 * porté par le payload.</p>
 *
 * <h2>Format des payloads</h2>
 * <ul>
 *   <li>{@code kind=task-toggled}, {@code taskId}, {@code done(boolean)}, {@code userId}</li>
 *   <li>{@code kind=intervention-added}, {@code clientUserId}, {@code encadrantUserId}, {@code preview}</li>
 *   <li>{@code kind=objectif-updated}, {@code userId}, {@code objectifId}</li>
 * </ul>
 */
public final class RealtimePlanningSyncService {

    public static final String KIND_TASK_TOGGLED = "task-toggled";
    public static final String KIND_INTERVENTION_ADDED = "intervention-added";
    public static final String KIND_OBJECTIF_UPDATED = "objectif-updated";
    public static final String KIND_PROGRAMME_UPDATED = "programme-updated";

    private static final RealtimePlanningSyncService INSTANCE = new RealtimePlanningSyncService();

    private final RealtimeService realtime = RealtimeService.getInstance();
    private final RealtimeEventDispatcher dispatcher = RealtimeEventDispatcher.getInstance();

    private RealtimePlanningSyncService() {
    }

    public static RealtimePlanningSyncService getInstance() {
        return INSTANCE;
    }

    /**
     * Notifie le user concerné qu'une tâche a été cochée / décochée.
     */
    public void notifyTaskToggled(int userId, int taskId, boolean done) {
        if (userId <= 0) return;
        JsonObject p = baseEvent(userId, KIND_TASK_TOGGLED);
        p.addProperty("taskId", taskId);
        p.addProperty("done", done);
        publishForUser(userId, p);
    }

    /**
     * Notifie le client qu'une intervention encadrant vient d'être enregistrée.
     */
    public void notifyInterventionAdded(int clientUserId, int encadrantUserId, String preview) {
        if (clientUserId <= 0) return;
        JsonObject p = baseEvent(clientUserId, KIND_INTERVENTION_ADDED);
        p.addProperty("clientUserId", clientUserId);
        p.addProperty("encadrantUserId", encadrantUserId);
        if (preview != null && !preview.isEmpty()) {
            p.addProperty("preview", preview.length() > 160 ? preview.substring(0, 160) : preview);
        }
        publishForUser(clientUserId, p);
    }

    /**
     * Notifie le user qu'un objectif IA a été créé / modifié.
     */
    public void notifyObjectifUpdated(int userId, int objectifId) {
        if (userId <= 0) return;
        JsonObject p = baseEvent(userId, KIND_OBJECTIF_UPDATED);
        p.addProperty("objectifId", objectifId);
        publishForUser(userId, p);
    }

    /**
     * Notifie le user qu'un programme/regime a été mis à jour (assignation
     * encadrant, modification produit, etc.).
     */
    public void notifyProgrammeUpdated(int userId, String detail) {
        if (userId <= 0) return;
        JsonObject p = baseEvent(userId, KIND_PROGRAMME_UPDATED);
        if (detail != null && !detail.isEmpty()) {
            p.addProperty("detail", detail.length() > 160 ? detail.substring(0, 160) : detail);
        }
        publishForUser(userId, p);
    }

    /**
     * Abonne un consumer aux évènements planning de l'utilisateur courant.
     * Renvoie l'identifiant de souscription ({@link #unsubscribe(String)}).
     */
    public String subscribeForCurrentUser(Consumer<RealtimeEvent> handler) {
        int uid = realtime.currentUserId();
        if (uid <= 0 || handler == null) {
            return null;
        }
        return dispatcher.subscribe("/planning/user/" + uid, handler);
    }

    public void unsubscribe(String subscriptionId) {
        dispatcher.unsubscribe(subscriptionId);
    }

    /**
     * Helper pour les contrôleurs : récupère le {@code kind} d'un évènement reçu.
     */
    public static String kindOf(RealtimeEvent event) {
        if (event == null) return "";
        return event.stringField("kind").orElse("");
    }

    // ------------------------------------------------------------------

    private JsonObject baseEvent(int userId, String kind) {
        JsonObject p = new JsonObject();
        p.addProperty("topic", "/planning/user/" + userId);
        p.addProperty("kind", kind);
        p.addProperty("userId", userId);
        p.addProperty("ts", System.currentTimeMillis());
        return p;
    }

    private void publishForUser(int userId, JsonObject payload) {
        realtime.publish("/planning/user/" + userId, payload.toString());
    }
}
