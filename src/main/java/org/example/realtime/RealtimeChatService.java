package org.example.realtime;

import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Consumer;

/**
 * Pont temps réel pour la discussion d'une conversation donnée.
 *
 * <p>Côté émetteur : {@link #publishNewMessage(int, int, int, String, int, String)}
 * publie un évènement {@code message-created} sur
 * {@code /chat/conversation/{id}}.</p>
 *
 * <p>Côté récepteur : {@link #subscribeToConversation(int, Consumer)} ajoute le
 * topic dynamique au {@link RealtimeService} et abonne un consumer au
 * dispatcher. {@link #unsubscribeFromConversation(int, String)} fait le
 * miroir et retire proprement le topic.</p>
 */
public final class RealtimeChatService {

    private static final RealtimeChatService INSTANCE = new RealtimeChatService();

    private final RealtimeService realtime = RealtimeService.getInstance();
    private final RealtimeEventDispatcher dispatcher = RealtimeEventDispatcher.getInstance();

    private RealtimeChatService() {
    }

    public static RealtimeChatService getInstance() {
        return INSTANCE;
    }

    /**
     * Publie un évènement "nouveau message" sur le topic de la conversation.
     */
    public void publishNewMessage(int conversationId, int messageId, int senderUserId, String contenu,
                                  int recipientUserId, String type) {
        if (conversationId <= 0 || messageId <= 0) {
            return;
        }
        JsonObject payload = new JsonObject();
        String topic = "/chat/conversation/" + conversationId;
        payload.addProperty("topic", topic);
        payload.addProperty("eventType", "message-created");
        payload.addProperty("conversationId", conversationId);
        payload.addProperty("messageId", messageId);
        payload.addProperty("senderId", senderUserId);
        payload.addProperty("recipientId", recipientUserId);
        payload.addProperty("type", type == null ? "MESSAGE" : type);
        if (contenu != null) {
            payload.addProperty("contenu", contenu);
        }
        payload.addProperty("createdAtEpoch",
                LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000L);
        realtime.publish(topic, payload.toString());
    }

    /**
     * Abonne un consumer au topic conversation et ajoute ce topic au flux SSE.
     * Renvoie l'identifiant de souscription (à passer à
     * {@link #unsubscribeFromConversation(int, String)}).
     */
    public String subscribeToConversation(int conversationId, Consumer<RealtimeEvent> handler) {
        if (conversationId <= 0 || handler == null) {
            return null;
        }
        String topic = "/chat/conversation/" + conversationId;
        realtime.addTopic(topic);
        return dispatcher.subscribe(topic, handler);
    }

    public void unsubscribeFromConversation(int conversationId, String subscriptionId) {
        dispatcher.unsubscribe(subscriptionId);
        if (conversationId > 0) {
            realtime.removeTopic("/chat/conversation/" + conversationId);
        }
    }
}
