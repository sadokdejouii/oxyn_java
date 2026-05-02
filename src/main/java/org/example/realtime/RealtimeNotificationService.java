package org.example.realtime;

import com.google.gson.JsonObject;
import javafx.application.Platform;

import java.util.function.Consumer;

/**
 * Pont temps réel pour la cloche de notifications du Planning.
 *
 * <p>Émission : appelé par {@code DiscussionPageController} après envoi d'un
 * message — publie un évènement {@code message-arrived} sur
 * {@code /notifications/user/{recipientId}} ; le destinataire, s'il a une
 * connexion Mercure active, reçoit le signal et raffraîchit sa cloche.</p>
 *
 * <p>Réception : permet au {@code NotificationBell} de s'abonner à son propre
 * topic et d'être déclenché en push (au lieu du polling 5 s) — le polling
 * reste en place comme filet de sécurité.</p>
 */
public final class RealtimeNotificationService {

    private static final RealtimeNotificationService INSTANCE = new RealtimeNotificationService();

    private final RealtimeService realtime = RealtimeService.getInstance();
    private final RealtimeEventDispatcher dispatcher = RealtimeEventDispatcher.getInstance();

    private RealtimeNotificationService() {
    }

    public static RealtimeNotificationService getInstance() {
        return INSTANCE;
    }

    /**
     * Publie un signal "nouveau message" pour {@code recipientUserId}.
     * Le payload reste léger : l'autre côté ira recharger via la DB pour
     * obtenir l'aperçu, le compteur, etc. (consistence avec NotificationService).
     */
    public void publishNewMessage(int recipientUserId, int conversationId, int senderUserId, String preview) {
        if (recipientUserId <= 0) {
            return;
        }
        JsonObject payload = new JsonObject();
        String topic = "/notifications/user/" + recipientUserId;
        payload.addProperty("topic", topic);
        payload.addProperty("type", "message-arrived");
        payload.addProperty("conversationId", conversationId);
        payload.addProperty("fromUserId", senderUserId);
        if (preview != null && !preview.isEmpty()) {
            payload.addProperty("preview", preview.length() > 120 ? preview.substring(0, 120) : preview);
        }
        payload.addProperty("ts", System.currentTimeMillis());
        realtime.publish(topic, payload.toString());
    }

    /**
     * Abonne un consumer aux notifications de l'utilisateur courant.
     * Le consumer est invoqué sur le JAT par le dispatcher.
     *
     * @return id de souscription (à passer à {@link #unsubscribe(String)})
     */
    public String subscribeForCurrentUser(Consumer<RealtimeEvent> handler) {
        int uid = realtime.currentUserId();
        if (uid <= 0 || handler == null) {
            return null;
        }
        return dispatcher.subscribe("/notifications/user/" + uid, handler);
    }

    public void unsubscribe(String subscriptionId) {
        dispatcher.unsubscribe(subscriptionId);
    }

    /** Garantit que tout callback métier passe sur le JAT (utilitaire public). */
    public static void runOnFx(Runnable r) {
        if (r == null) return;
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}
