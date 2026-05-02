package org.example.realtime;

import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Bus pub/sub thread-safe pour les évènements temps réel.
 *
 * <p>Toutes les fonctions {@link Consumer} enregistrées sont invoquées sur
 * le <strong>JavaFX Application Thread</strong> via {@link Platform#runLater(Runnable)}
 * — les contrôleurs UI peuvent donc manipuler la scène en toute sécurité
 * sans se préoccuper du thread d'origine de l'évènement.</p>
 *
 * <p>Le matching s'opère par préfixe : un abonné à {@code "/chat/"} reçoit
 * tous les évènements dont le topic commence par {@code "/chat/"}. Ça
 * permet aux services métiers d'être agnostiques à l'identifiant de
 * conversation/utilisateur dans l'URI.</p>
 */
public final class RealtimeEventDispatcher {

    private static final RealtimeEventDispatcher INSTANCE = new RealtimeEventDispatcher();

    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final List<Subscription> orderedSubs = new CopyOnWriteArrayList<>();

    private RealtimeEventDispatcher() {
    }

    public static RealtimeEventDispatcher getInstance() {
        return INSTANCE;
    }

    /**
     * Enregistre un abonné qui sera notifié pour tout évènement dont le topic
     * commence par {@code topicPrefix}.
     *
     * @return un identifiant à conserver pour {@link #unsubscribe(String)}.
     */
    public String subscribe(String topicPrefix, Consumer<RealtimeEvent> handler) {
        Objects.requireNonNull(topicPrefix, "topicPrefix");
        Objects.requireNonNull(handler, "handler");
        String id = UUID.randomUUID().toString();
        Subscription sub = new Subscription(id, topicPrefix, handler);
        subscriptions.put(id, sub);
        orderedSubs.add(sub);
        return id;
    }

    /**
     * Désinscrit un handler. Aucun effet si l'identifiant est inconnu.
     */
    public void unsubscribe(String subscriptionId) {
        if (subscriptionId == null) {
            return;
        }
        Subscription removed = subscriptions.remove(subscriptionId);
        if (removed != null) {
            orderedSubs.remove(removed);
        }
    }

    /**
     * Diffuse un évènement à tous les abonnés correspondants.
     * Les exceptions levées par un handler n'interrompent pas les autres.
     */
    public void dispatch(RealtimeEvent event) {
        if (event == null || event.topic() == null) {
            return;
        }
        String topic = event.topic();
        for (Subscription sub : orderedSubs) {
            if (topic.startsWith(sub.topicPrefix())) {
                runOnFx(() -> {
                    try {
                        sub.handler().accept(event);
                    } catch (RuntimeException ex) {
                        System.err.println("[Realtime] Handler error on " + sub.topicPrefix()
                                + " : " + ex.getMessage());
                    }
                });
            }
        }
    }

    /**
     * Vide tout le bus (utile en logout / arrêt application).
     */
    public void clear() {
        subscriptions.clear();
        orderedSubs.clear();
    }

    private static void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    private record Subscription(String id, String topicPrefix, Consumer<RealtimeEvent> handler) {
    }
}
