package org.example.realtime;

import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pont temps réel pour la discussion d'une conversation donnée.
 *
 * <p>Côté émetteur : {@link #publishNewMessage(int, int, int, String, int, String)}
 * publie un évènement {@code message-created} sur
 * {@code /chat/conversation/{id}}.</p>
 *
 * <p>Côté récepteur : {@link #subscribeToConversation(int, Consumer, String)} ajoute les topics
 * {@code /chat/conversation/…}, {@code /typing/conversation/…} et optionnellement présence du pair,
 * en une seule reconnexion SSE. {@link #unsubscribeFromConversation(int, String, String)} retire le tout.</p>
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
     * Abonne un consumer aux évènements « nouveau message » et souscrit en une fois au chat,
     * au flux typing de la même conversation, et optionnellement à la présence du pair.
     *
     * @param peerPresenceTopic ex. {@code /presence/user/42} ou {@code null}
     */
    public String subscribeToConversation(int conversationId, Consumer<RealtimeEvent> handler,
                                          String peerPresenceTopic) {
        if (conversationId <= 0 || handler == null) {
            return null;
        }
        String chatTopic = "/chat/conversation/" + conversationId;
        String typingTopic = "/typing/conversation/" + conversationId;
        List<String> topics = new ArrayList<>(3);
        topics.add(chatTopic);
        topics.add(typingTopic);
        if (peerPresenceTopic != null && !peerPresenceTopic.isBlank()) {
            topics.add(peerPresenceTopic.trim());
        }
        realtime.addTopics(topics);
        return dispatcher.subscribe(chatTopic, handler);
    }

    public void unsubscribeFromConversation(int conversationId, String subscriptionId,
                                            String peerPresenceTopic) {
        dispatcher.unsubscribe(subscriptionId);
        if (conversationId <= 0) {
            return;
        }
        List<String> topics = new ArrayList<>(3);
        topics.add("/chat/conversation/" + conversationId);
        topics.add("/typing/conversation/" + conversationId);
        if (peerPresenceTopic != null && !peerPresenceTopic.isBlank()) {
            topics.add(peerPresenceTopic.trim());
        }
        realtime.removeTopics(topics);
    }
}
