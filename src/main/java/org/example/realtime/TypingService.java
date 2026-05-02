package org.example.realtime;

import com.google.gson.JsonObject;
import javafx.application.Platform;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Indicateur "est en train d'écrire…" via Mercure.
 *
 * <p><strong>Côté émetteur</strong> ({@link #notifyLocalTyping(int)}) :</p>
 * <ul>
 *   <li>premier appel : publie {@code typing=true} sur
 *       {@code /typing/conversation/{id}} ;</li>
 *   <li>tant que l'utilisateur saisit, on (re)programme un timer 2 s ;</li>
 *   <li>au bout de 2 s sans nouvelle saisie : publie {@code typing=false}.</li>
 * </ul>
 *
 * <p><strong>Côté récepteur</strong> : abonné global au préfixe
 * {@code /typing/conversation/}. Notifie les listeners métier sur le JAT
 * (typiquement le {@code DiscussionPageController} pour afficher / masquer
 * le label "X est en train d'écrire…").</p>
 *
 * <p>Auto-extinction côté récepteur : si on reçoit {@code typing=true} sans
 * recevoir le {@code false} correspondant dans les 5 s, on bascule offline
 * tout seul pour éviter qu'un indicateur reste affiché à vie.</p>
 */
public final class TypingService {

    private static final TypingService INSTANCE = new TypingService();

    private static final long DEBOUNCE_STOP_MILLIS = 2_000L;
    private static final long REMOTE_TIMEOUT_MILLIS = 5_000L;

    private final RealtimeService realtime = RealtimeService.getInstance();
    private final RealtimeEventDispatcher dispatcher = RealtimeEventDispatcher.getInstance();

    private final AtomicInteger currentUserId = new AtomicInteger(-1);

    /** Pour chaque conversation : timestamp dernier publish typing=true (déduplication). */
    private final Map<Integer, Long> lastTypingPublishMs = new ConcurrentHashMap<>();
    /** Pour chaque conversation : ScheduledFuture qui publiera typing=false au bout de 2s. */
    private final Map<Integer, ScheduledFuture<?>> stopTimers = new ConcurrentHashMap<>();

    /** Côté récepteur : timestamp dernier "typing=true" reçu, par (conversationId, fromUserId). */
    private final Map<Key, Instant> remoteTypingSince = new ConcurrentHashMap<>();
    private final Map<Key, ScheduledFuture<?>> remoteTimeouts = new ConcurrentHashMap<>();

    private final java.util.List<BiConsumer<TypingEvent, Boolean>> listeners =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    private ScheduledExecutorService scheduler;
    private String dispatcherSubId;

    private TypingService() {
    }

    public static TypingService getInstance() {
        return INSTANCE;
    }

    public synchronized void start(int userId) {
        if (userId <= 0) {
            return;
        }
        if (currentUserId.get() == userId && scheduler != null && !scheduler.isShutdown()) {
            return;
        }
        stop();
        currentUserId.set(userId);
        dispatcherSubId = dispatcher.subscribe("/typing/conversation/", this::onTypingEvent);
        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "typing-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void stop() {
        // Émettre typing=false partout pour libérer les pairs
        for (Integer convId : lastTypingPublishMs.keySet()) {
            publishTyping(convId, false);
        }
        lastTypingPublishMs.clear();
        cancelAll(stopTimers);
        cancelAll(remoteTimeouts);
        remoteTypingSince.clear();
        if (dispatcherSubId != null) {
            dispatcher.unsubscribe(dispatcherSubId);
            dispatcherSubId = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        currentUserId.set(-1);
    }

    /**
     * À appeler à chaque frappe dans le champ message. Publie {@code typing=true}
     * (déduplication 1 s) puis programme {@code typing=false} après 2 s d'inactivité.
     */
    public void notifyLocalTyping(int conversationId) {
        if (conversationId <= 0 || currentUserId.get() <= 0 || scheduler == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastTypingPublishMs.get(conversationId);
        if (last == null || now - last > 1_000L) {
            lastTypingPublishMs.put(conversationId, now);
            publishTyping(conversationId, true);
        }
        // Replace the stop timer
        ScheduledFuture<?> existing = stopTimers.remove(conversationId);
        if (existing != null) {
            existing.cancel(false);
        }
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            publishTyping(conversationId, false);
            lastTypingPublishMs.remove(conversationId);
            stopTimers.remove(conversationId);
        }, DEBOUNCE_STOP_MILLIS, TimeUnit.MILLISECONDS);
        stopTimers.put(conversationId, future);
    }

    /**
     * Force un {@code typing=false} immédiat (ex. au moment d'envoyer le message).
     */
    public void notifyLocalStopped(int conversationId) {
        if (conversationId <= 0) {
            return;
        }
        ScheduledFuture<?> existing = stopTimers.remove(conversationId);
        if (existing != null) {
            existing.cancel(false);
        }
        if (lastTypingPublishMs.remove(conversationId) != null) {
            publishTyping(conversationId, false);
        }
    }

    public void addListener(BiConsumer<TypingEvent, Boolean> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(BiConsumer<TypingEvent, Boolean> listener) {
        listeners.remove(listener);
    }

    /**
     * Renvoie {@code true} si on sait qu'un autre utilisateur est en train de
     * taper dans la conversation.
     */
    public boolean isRemoteTypingActive(int conversationId) {
        Instant cutoff = Instant.now().minusMillis(REMOTE_TIMEOUT_MILLIS);
        for (Map.Entry<Key, Instant> e : remoteTypingSince.entrySet()) {
            if (e.getKey().conversationId == conversationId
                    && e.getKey().userId != currentUserId.get()
                    && e.getValue() != null
                    && e.getValue().isAfter(cutoff)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------

    private void publishTyping(int conversationId, boolean active) {
        int me = currentUserId.get();
        if (me <= 0 || !realtime.isEnabled()) {
            return;
        }
        JsonObject payload = new JsonObject();
        String topic = "/typing/conversation/" + conversationId;
        payload.addProperty("topic", topic);
        payload.addProperty("conversationId", conversationId);
        payload.addProperty("userId", me);
        payload.addProperty("typing", active);
        payload.addProperty("ts", System.currentTimeMillis());
        realtime.publish(topic, payload.toString());
    }

    private void onTypingEvent(RealtimeEvent event) {
        if (event == null) {
            return;
        }
        Integer convId = intField(event, "conversationId");
        Integer fromUserId = intField(event, "userId");
        if (convId == null) {
            convId = parseTrailingInt(event.topic());
        }
        if (convId == null || fromUserId == null) {
            return;
        }
        if (fromUserId == currentUserId.get()) {
            return; // echo
        }
        Boolean typing = event.asObject()
                .filter(o -> o.has("typing") && !o.get("typing").isJsonNull())
                .map(o -> o.get("typing").getAsBoolean())
                .orElse(Boolean.FALSE);
        Key key = new Key(convId, fromUserId);
        if (typing) {
            remoteTypingSince.put(key, Instant.now());
            // Auto-extinction si on ne reçoit pas typing=false dans 5s
            ScheduledFuture<?> previous = remoteTimeouts.remove(key);
            if (previous != null) {
                previous.cancel(false);
            }
            if (scheduler != null) {
                ScheduledFuture<?> future = scheduler.schedule(() -> {
                    remoteTypingSince.remove(key);
                    remoteTimeouts.remove(key);
                    notifyListeners(new TypingEvent(key.conversationId, key.userId), false);
                }, REMOTE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                remoteTimeouts.put(key, future);
            }
            notifyListeners(new TypingEvent(convId, fromUserId), true);
        } else {
            ScheduledFuture<?> previous = remoteTimeouts.remove(key);
            if (previous != null) {
                previous.cancel(false);
            }
            if (remoteTypingSince.remove(key) != null) {
                notifyListeners(new TypingEvent(convId, fromUserId), false);
            }
        }
    }

    private static Integer intField(RealtimeEvent ev, String key) {
        return ev.asObject()
                .filter(o -> o.has(key) && !o.get(key).isJsonNull())
                .map(o -> o.get(key).getAsInt())
                .orElse(null);
    }

    private static Integer parseTrailingInt(String topic) {
        if (topic == null) return null;
        int idx = topic.lastIndexOf('/');
        if (idx < 0 || idx >= topic.length() - 1) return null;
        try {
            return Integer.parseInt(topic.substring(idx + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void notifyListeners(TypingEvent ev, boolean active) {
        Runnable r = () -> {
            for (BiConsumer<TypingEvent, Boolean> l : listeners) {
                try {
                    l.accept(ev, active);
                } catch (RuntimeException ignored) {
                }
            }
        };
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    private static void cancelAll(Map<?, ScheduledFuture<?>> map) {
        for (ScheduledFuture<?> f : map.values()) {
            if (f != null) f.cancel(false);
        }
        map.clear();
    }

    /** Évènement métier émis aux listeners. */
    public record TypingEvent(int conversationId, int userId) {
    }

    private record Key(int conversationId, int userId) {
    }
}
