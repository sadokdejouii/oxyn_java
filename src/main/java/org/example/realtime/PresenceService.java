package org.example.realtime;

import com.google.gson.JsonObject;
import javafx.application.Platform;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Présence en ligne / hors ligne via Mercure.
 *
 * <p><strong>Émission :</strong></p>
 * <ul>
 *   <li>au {@link #start(int)} (login) -> publish {@code online} sur {@code /presence/user/{userId}} ;</li>
 *   <li>heartbeat toutes les 20 s -> repeat {@code online} pour rafraîchir l'horodatage côté pairs ;</li>
 *   <li>au {@link #stop()} (logout / fermeture) -> publish {@code offline}.</li>
 * </ul>
 *
 * <p><strong>Réception :</strong> abonné global au préfixe {@code /presence/user/}.
 * Stocke le dernier {@link Instant} d'activité par {@code userId} et déclare un user
 * <em>offline</em> si plus de 60 s sans heartbeat.</p>
 *
 * <p>Singleton — un seul démarrage par session utilisateur.</p>
 */
public final class PresenceService {

    public enum Presence {
        ONLINE,
        OFFLINE
    }

    private static final PresenceService INSTANCE = new PresenceService();

    private static final long HEARTBEAT_SECONDS = 20L;
    private static final long OFFLINE_AFTER_SECONDS = 60L;

    private final RealtimeService realtime = RealtimeService.getInstance();
    private final RealtimeEventDispatcher dispatcher = RealtimeEventDispatcher.getInstance();

    private final Map<Integer, Instant> lastSeenByUser = new ConcurrentHashMap<>();
    private final Map<Integer, Presence> stateByUser = new ConcurrentHashMap<>();

    /** Listeners métier (typiquement le ChatHeader) : reçoivent userId + Presence sur le JAT. */
    private final java.util.List<BiConsumer<Integer, Presence>> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> heartbeatFuture;
    private ScheduledFuture<?> staleCheckFuture;
    private String dispatcherSubId;
    private int currentUserId = -1;

    private PresenceService() {
    }

    public static PresenceService getInstance() {
        return INSTANCE;
    }

    /**
     * Démarre la présence pour l'utilisateur courant : abonnement aux events
     * de présence + heartbeat périodique + check des stales.
     */
    public synchronized void start(int userId) {
        if (userId <= 0) {
            return;
        }
        if (this.currentUserId == userId && scheduler != null && !scheduler.isShutdown()) {
            return;
        }
        stop();
        this.currentUserId = userId;

        dispatcherSubId = dispatcher.subscribe("/presence/user/", this::onPresenceEvent);

        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "presence-scheduler");
            t.setDaemon(true);
            return t;
        });

        // Premier ONLINE immédiat puis heartbeat toutes les 20s
        publishPresence(Presence.ONLINE);
        heartbeatFuture = scheduler.scheduleAtFixedRate(
                () -> publishPresence(Presence.ONLINE),
                HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);

        // Check stales toutes les 10s -> bascule offline si dernier heartbeat > 60s
        staleCheckFuture = scheduler.scheduleAtFixedRate(this::sweepStaleUsers,
                10L, 10L, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (currentUserId > 0) {
            publishPresence(Presence.OFFLINE);
        }
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
            heartbeatFuture = null;
        }
        if (staleCheckFuture != null) {
            staleCheckFuture.cancel(true);
            staleCheckFuture = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (dispatcherSubId != null) {
            dispatcher.unsubscribe(dispatcherSubId);
            dispatcherSubId = null;
        }
        lastSeenByUser.clear();
        stateByUser.clear();
        currentUserId = -1;
    }

    /**
     * Renvoie l'état présence connu pour {@code userId} (par défaut OFFLINE).
     */
    public Presence currentPresence(int userId) {
        return stateByUser.getOrDefault(userId, Presence.OFFLINE);
    }

    /**
     * Renvoie l'instant du dernier signal présence reçu pour {@code userId}, ou null.
     */
    public Instant lastSeen(int userId) {
        return lastSeenByUser.get(userId);
    }

    /**
     * Construit un libellé prêt-à-afficher (ex. "En ligne", "Hors ligne depuis 3 min").
     */
    public String formatPresenceLabel(int userId) {
        Presence p = currentPresence(userId);
        if (p == Presence.ONLINE) {
            return "En ligne";
        }
        Instant last = lastSeen(userId);
        if (last == null) {
            return "Hors ligne";
        }
        long minutes = Math.max(1, Duration.between(last, Instant.now()).toMinutes());
        if (minutes < 60) {
            return "Hors ligne depuis " + minutes + " min";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return "Hors ligne depuis " + hours + " h";
        }
        long days = hours / 24;
        return "Hors ligne depuis " + days + " j";
    }

    public void addListener(BiConsumer<Integer, Presence> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(BiConsumer<Integer, Presence> listener) {
        listeners.remove(listener);
    }

    // ------------------------------------------------------------------

    private void publishPresence(Presence p) {
        if (currentUserId <= 0 || !realtime.isEnabled()) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("topic", "/presence/user/" + currentUserId);
        payload.addProperty("userId", currentUserId);
        payload.addProperty("status", p == Presence.ONLINE ? "online" : "offline");
        payload.addProperty("ts", Instant.now().toEpochMilli());
        realtime.publish("/presence/user/" + currentUserId, payload.toString());
    }

    private void onPresenceEvent(RealtimeEvent event) {
        if (event == null) {
            return;
        }
        Integer userId = event.asObject()
                .filter(o -> o.has("userId") && !o.get("userId").isJsonNull())
                .map(o -> o.get("userId").getAsInt())
                .orElse(null);
        if (userId == null) {
            // Topic au format /presence/user/{id} : fallback parsing
            String topic = event.topic();
            int idx = topic.lastIndexOf('/');
            if (idx >= 0 && idx < topic.length() - 1) {
                try {
                    userId = Integer.parseInt(topic.substring(idx + 1));
                } catch (NumberFormatException ignored) {
                    return;
                }
            } else {
                return;
            }
        }
        if (userId == currentUserId) {
            // ignorer son propre echo
            return;
        }
        String status = event.stringField("status").orElse("online");
        Presence p = "offline".equalsIgnoreCase(status) ? Presence.OFFLINE : Presence.ONLINE;
        lastSeenByUser.put(userId, Instant.now());
        Presence previous = stateByUser.put(userId, p);
        if (!Objects.equals(previous, p)) {
            notifyListeners(userId, p);
        }
    }

    private void sweepStaleUsers() {
        Instant cutoff = Instant.now().minusSeconds(OFFLINE_AFTER_SECONDS);
        for (Map.Entry<Integer, Instant> e : lastSeenByUser.entrySet()) {
            if (e.getValue() != null && e.getValue().isBefore(cutoff)) {
                Presence prev = stateByUser.put(e.getKey(), Presence.OFFLINE);
                if (prev == Presence.ONLINE) {
                    notifyListeners(e.getKey(), Presence.OFFLINE);
                }
            }
        }
    }

    private void notifyListeners(int userId, Presence p) {
        Runnable r = () -> {
            for (BiConsumer<Integer, Presence> l : listeners) {
                try {
                    l.accept(userId, p);
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
}
