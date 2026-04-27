package org.example.realtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Client SSE Mercure : ouvre une connexion HTTP long-lived vers le hub,
 * parse les évènements {@code text/event-stream} et les redistribue via
 * le {@link RealtimeEventDispatcher}.
 *
 * <ul>
 *   <li>Connexion gérée dans un thread dédié, jamais sur le JAT.</li>
 *   <li>Reconnexion automatique avec backoff exponentiel borné.</li>
 *   <li>Publication d'évènements sortants ({@link #publish(String, String)}).</li>
 *   <li>Détection « Mercure indisponible » -> notifie l'orchestrateur via
 *       {@link #addStatusListener(Consumer)} pour activer le polling de repli.</li>
 * </ul>
 */
public final class MercureClientService {

    public enum Status {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        FALLBACK
    }

    private final RealtimeConfig config;
    private final RealtimeEventDispatcher dispatcher;
    private final HttpClient httpClient;

    private final AtomicReference<Status> status = new AtomicReference<>(Status.DISCONNECTED);
    private final List<Consumer<Status>> statusListeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final List<String> subscribedTopics = new CopyOnWriteArrayList<>();
    private Thread sseThread;
    private volatile HttpURLConnection currentConnection;
    /**
     * Pool dédié à la publication HTTP — évite de bloquer le JavaFX Application
     * Thread quand on publie un évènement (typing à chaque frappe, présence, ...).
     */
    private final ExecutorService publishExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "mercure-publish");
        t.setDaemon(true);
        return t;
    });

    /** Évite d'inonder la console si le hub Mercure est injoignable. */
    private volatile long lastPublishErrorLogMs;

    public MercureClientService() {
        this(RealtimeConfig.getInstance(), RealtimeEventDispatcher.getInstance());
    }

    public MercureClientService(RealtimeConfig config, RealtimeEventDispatcher dispatcher) {
        this.config = config;
        this.dispatcher = dispatcher;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.httpConnectTimeoutSeconds()))
                .build();
    }

    public Status status() {
        return status.get();
    }

    public void addStatusListener(Consumer<Status> listener) {
        if (listener != null) {
            statusListeners.add(listener);
            try {
                listener.accept(status.get());
            } catch (RuntimeException ignored) {
            }
        }
    }

    public void removeStatusListener(Consumer<Status> listener) {
        statusListeners.remove(listener);
    }

    /**
     * Démarre l'écoute SSE pour la liste de topics donnée.
     * Idempotent — si déjà démarré, met simplement à jour la liste de topics
     * et reconnecte.
     */
    public synchronized void start(List<String> topics) {
        this.subscribedTopics.clear();
        if (topics != null) {
            this.subscribedTopics.addAll(topics);
        }
        if (running.compareAndSet(false, true)) {
            sseThread = new Thread(this::runLoop, "mercure-sse-client");
            sseThread.setDaemon(true);
            sseThread.start();
        } else {
            // Topics changed -> force reconnection.
            closeCurrentConnectionQuietly();
        }
    }

    /**
     * Arrête proprement la connexion SSE et le thread d'écoute.
     */
    public synchronized void stop() {
        running.set(false);
        closeCurrentConnectionQuietly();
        if (sseThread != null) {
            sseThread.interrupt();
            sseThread = null;
        }
        updateStatus(Status.DISCONNECTED);
    }

    /**
     * Publie un évènement sur un topic — exécution déportée sur un pool dédié
     * pour ne JAMAIS bloquer le JavaFX Application Thread (la publication peut
     * être appelée à chaque frappe via {@code TypingService}).
     *
     * @return {@code true} si la publication a bien été planifiée (le résultat
     *         HTTP réel sera loggé en cas d'échec).
     */
    public boolean publish(String topic, String jsonPayload) {
        if (topic == null || topic.isBlank() || jsonPayload == null) {
            return false;
        }
        publishExecutor.execute(() -> doPublish(topic, jsonPayload));
        return true;
    }

    private void doPublish(String topic, String jsonPayload) {
        try {
            String body = "topic=" + URLEncoder.encode(topic, StandardCharsets.UTF_8)
                    + "&data=" + URLEncoder.encode(jsonPayload, StandardCharsets.UTF_8);
            String token = MercureJwt.publisherToken(List.of("*"), config.jwtSecret());
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(config.publishUrl()))
                    .timeout(Duration.ofSeconds(config.httpConnectTimeoutSeconds() + 5))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                System.err.println("[Realtime] Publish failed (" + resp.statusCode() + ") on "
                        + topic + " : " + resp.body());
            }
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            if (now - lastPublishErrorLogMs >= 30_000L) {
                lastPublishErrorLogMs = now;
                String detail = e.getMessage();
                if (detail == null || detail.isBlank()) {
                    detail = e.getClass().getSimpleName();
                }
                System.err.println("[Realtime] Publication Mercure impossible (hub arrêté ou "
                        + "realtime.properties). " + detail + " — prochains échecs silencieux ~30s.");
            }
        }
    }

    // ------------------------------------------------------------------
    // SSE loop
    // ------------------------------------------------------------------

    private void runLoop() {
        int delay = Math.max(1, config.reconnectInitialDelaySeconds());
        int maxDelay = Math.max(delay, config.reconnectMaxDelaySeconds());
        boolean firstAttempt = true;
        while (running.get()) {
            updateStatus(Status.CONNECTING);
            try {
                connectAndStream();
                // si on revient ici sans exception -> connexion fermée proprement
                delay = Math.max(1, config.reconnectInitialDelaySeconds());
            } catch (Exception ex) {
                if (firstAttempt) {
                    System.err.println("[Realtime] Mercure indisponible, fallback polling activé"
                            + " (" + ex.getClass().getSimpleName() + " : " + ex.getMessage() + ")");
                }
                updateStatus(Status.FALLBACK);
            }
            firstAttempt = false;
            if (!running.get()) {
                break;
            }
            sleep(delay);
            delay = Math.min(maxDelay, delay * 2);
        }
        updateStatus(Status.DISCONNECTED);
    }

    private void connectAndStream() throws Exception {
        if (subscribedTopics.isEmpty()) {
            // Rien à écouter -> attendre proprement avant nouvelle vérification
            sleep(2);
            return;
        }
        String url = buildSubscribeUrl(subscribedTopics);
        URI uri = URI.create(url);
        String token = MercureJwt.subscriberToken(subscribedTopics, config.jwtSecret());

        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setConnectTimeout(config.httpConnectTimeoutSeconds() * 1000);
        conn.setReadTimeout(0); // long-lived
        conn.connect();

        int code = conn.getResponseCode();
        if (code != 200) {
            String msg = "Mercure HTTP " + code;
            try (InputStream err = conn.getErrorStream()) {
                if (err != null) {
                    msg += " : " + new String(err.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {
            }
            conn.disconnect();
            throw new IllegalStateException(msg);
        }

        currentConnection = conn;
        updateStatus(Status.CONNECTED);
        System.out.println("[Realtime] Connecté à Mercure (" + subscribedTopics.size() + " topics)");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String currentEventTopic = null;
            StringBuilder dataBuf = new StringBuilder();
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (dataBuf.length() > 0) {
                        deliverEvent(currentEventTopic, dataBuf.toString());
                    }
                    currentEventTopic = null;
                    dataBuf.setLength(0);
                    continue;
                }
                if (line.startsWith(":")) {
                    // commentaire / heartbeat Mercure
                    continue;
                }
                int sep = line.indexOf(':');
                if (sep < 0) {
                    continue;
                }
                String field = line.substring(0, sep);
                String value = line.substring(sep + 1).stripLeading();
                if ("data".equals(field)) {
                    if (dataBuf.length() > 0) {
                        dataBuf.append('\n');
                    }
                    dataBuf.append(value);
                } else if ("event".equals(field)) {
                    currentEventTopic = value;
                }
                // id / retry sont ignorés volontairement
            }
        } finally {
            currentConnection = null;
            try {
                conn.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    private void deliverEvent(String fallbackTopic, String rawData) {
        // Mercure place le topic concret dans le payload JSON sous "topic"
        // (champ ajouté par certains hubs). On retombe sur le 1er topic
        // souscrit si on ne trouve rien d'autre — c'est volontairement
        // tolérant pour ne jamais perdre un évènement.
        String topic = fallbackTopic;
        try {
            JsonElement el = JsonParser.parseString(rawData);
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("topic") && !obj.get("topic").isJsonNull()) {
                    topic = obj.get("topic").getAsString();
                } else if (obj.has("__topic") && !obj.get("__topic").isJsonNull()) {
                    topic = obj.get("__topic").getAsString();
                }
            }
        } catch (Exception ignored) {
            // sera converti en {"raw": ...} par fromRawPayload
        }
        if (topic == null && !subscribedTopics.isEmpty()) {
            topic = subscribedTopics.get(0);
        }
        if (topic == null) {
            return;
        }
        dispatcher.dispatch(RealtimeEvent.fromRawPayload(topic, rawData));
    }

    private String buildSubscribeUrl(List<String> topics) {
        StringBuilder sb = new StringBuilder(config.subscribeUrl());
        sb.append('?');
        for (int i = 0; i < topics.size(); i++) {
            if (i > 0) {
                sb.append('&');
            }
            sb.append("topic=").append(URLEncoder.encode(topics.get(i), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private void closeCurrentConnectionQuietly() {
        HttpURLConnection conn = currentConnection;
        if (conn != null) {
            try {
                conn.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    private void updateStatus(Status next) {
        Status prev = status.getAndSet(next);
        if (prev != next) {
            for (Consumer<Status> l : statusListeners) {
                try {
                    l.accept(next);
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    private static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Liste immutable des topics actuellement souscrits — utile aux services
     * qui veulent vérifier ce qui est écouté avant d'ajouter.
     */
    public List<String> currentTopics() {
        return Collections.unmodifiableList(new ArrayList<>(subscribedTopics));
    }
}
