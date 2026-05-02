package org.example.realtime;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestrateur central du temps réel.
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li>démarrer / arrêter la connexion Mercure en fonction du cycle de vie
 *       utilisateur (login / logout / fermeture de l'app) ;</li>
 *   <li>maintenir la liste des topics « par défaut » d'un user (notifications
 *       perso, planning perso, présence) et accepter l'ajout dynamique d'autres
 *       topics (ex. conversation ouverte) ;</li>
 *   <li>exposer un {@link Status} observable pour l'UI (bandeau « mode dégradé »).</li>
 * </ul>
 *
 * <p>Un singleton — un seul user à la fois côté client lourd JavaFX.</p>
 */
public final class RealtimeService {

    public enum Status {
        OFFLINE,
        CONNECTING,
        ONLINE,
        FALLBACK
    }

    private static final RealtimeService INSTANCE = new RealtimeService();

    private final RealtimeConfig config;
    private final MercureClientService mercure;
    private final ReadOnlyObjectWrapper<Status> statusProperty =
            new ReadOnlyObjectWrapper<>(Status.OFFLINE);

    private final Set<String> baseTopics = new LinkedHashSet<>();
    private final Set<String> dynamicTopics = new LinkedHashSet<>();
    private final AtomicInteger sessionUserId = new AtomicInteger(-1);

    private RealtimeService() {
        this.config = RealtimeConfig.getInstance();
        this.mercure = new MercureClientService(config, RealtimeEventDispatcher.getInstance());
        this.mercure.addStatusListener(this::onMercureStatusChanged);
    }

    public static RealtimeService getInstance() {
        return INSTANCE;
    }

    public ReadOnlyObjectProperty<Status> statusProperty() {
        return statusProperty.getReadOnlyProperty();
    }

    public Status status() {
        return statusProperty.get();
    }

    public boolean isEnabled() {
        return config.enabled();
    }

    public int currentUserId() {
        return sessionUserId.get();
    }

    /**
     * Démarre la connexion temps réel pour un utilisateur DB.
     * Appelé après login réussi.
     *
     * @param userId identifiant base de données du user (>0). -1 pour ignorer.
     */
    public synchronized void startForUser(int userId) {
        if (userId <= 0) {
            return;
        }
        if (!config.enabled()) {
            System.out.println("[Realtime] Module désactivé via realtime.enabled=false");
            return;
        }
        sessionUserId.set(userId);
        baseTopics.clear();
        baseTopics.add("/notifications/user/" + userId);
        baseTopics.add("/planning/user/" + userId);
        baseTopics.add("/presence/user/" + userId);
        // Les topics de base couvrent les besoins transverses de planning + discussion.
        statusProperty.set(Status.CONNECTING);
        mercure.start(combinedTopics());
        try {
            PresenceService.getInstance().start(userId);
            TypingService.getInstance().start(userId);
            ToastNotificationService.getInstance().start();
        } catch (Exception e) {
            System.err.println("[Realtime] presence/typing/toast start failed : " + e.getMessage());
        }
    }

    /**
     * Stoppe proprement la connexion (logout ou shutdown).
     */
    public synchronized void stop() {
        try {
            ToastNotificationService.getInstance().stop();
            TypingService.getInstance().stop();
            PresenceService.getInstance().stop();
        } catch (Exception ignored) {
        }
        sessionUserId.set(-1);
        baseTopics.clear();
        dynamicTopics.clear();
        mercure.stop();
        statusProperty.set(Status.OFFLINE);
    }

    /**
     * Ajoute un topic supplémentaire (ex. conversation ouverte) et reconnecte.
     */
    public synchronized void addTopic(String topic) {
        if (topic == null || topic.isBlank() || sessionUserId.get() <= 0) {
            return;
        }
        addTopics(List.of(topic));
    }

    /**
     * Ajoute plusieurs topics dynamiques en une seule reconnexion Mercure (évite une fenêtre
     * où le SSE serait abonné au chat mais pas encore au typing, etc.).
     */
    public synchronized void addTopics(Collection<String> topics) {
        if (topics == null || sessionUserId.get() <= 0) {
            return;
        }
        boolean changed = false;
        for (String topic : topics) {
            if (topic != null && !topic.isBlank() && dynamicTopics.add(topic)) {
                changed = true;
            }
        }
        if (changed) {
            mercure.start(combinedTopics());
        }
    }

    /**
     * Retire un topic dynamique précédemment ajouté.
     */
    public synchronized void removeTopic(String topic) {
        if (topic == null || sessionUserId.get() <= 0) {
            return;
        }
        removeTopics(List.of(topic));
    }

    /**
     * Retire plusieurs topics en une seule reconnexion.
     */
    public synchronized void removeTopics(Collection<String> topics) {
        if (topics == null || sessionUserId.get() <= 0) {
            return;
        }
        boolean changed = false;
        for (String topic : topics) {
            if (topic != null && !topic.isBlank() && dynamicTopics.remove(topic)) {
                changed = true;
            }
        }
        if (changed) {
            mercure.start(combinedTopics());
        }
    }

    /**
     * Publie un évènement JSON sur un topic. Sans-effet si le module est
     * désactivé ou si Mercure est indisponible — la persistance DB et la
     * logique métier doivent rester opérantes en parallèle.
     */
    public boolean publish(String topic, String jsonPayload) {
        if (!config.enabled() || sessionUserId.get() <= 0) {
            return false;
        }
        return mercure.publish(topic, jsonPayload);
    }

    /**
     * Indique si le module fonctionne en repli (polling 5s).
     */
    public boolean isFallback() {
        return statusProperty.get() == Status.FALLBACK;
    }

    public List<String> subscribedTopics() {
        return Collections.unmodifiableList(new ArrayList<>(combinedTopics()));
    }

    // ------------------------------------------------------------------

    private List<String> combinedTopics() {
        Set<String> all = new LinkedHashSet<>(baseTopics);
        all.addAll(dynamicTopics);
        return new ArrayList<>(all);
    }

    private void onMercureStatusChanged(MercureClientService.Status mercureStatus) {
        // Adaptateur Mercure -> statut UI uniforme pour badges/indicateurs JavaFX.
        Status mapped = switch (mercureStatus) {
            case DISCONNECTED -> sessionUserId.get() > 0 ? Status.OFFLINE : Status.OFFLINE;
            case CONNECTING -> Status.CONNECTING;
            case CONNECTED -> Status.ONLINE;
            case FALLBACK -> Status.FALLBACK;
        };
        if (javafx.application.Platform.isFxApplicationThread()) {
            statusProperty.set(mapped);
        } else {
            javafx.application.Platform.runLater(() -> statusProperty.set(mapped));
        }
    }
}
