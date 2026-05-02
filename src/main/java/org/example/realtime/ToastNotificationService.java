package org.example.realtime;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.example.entities.User;
import org.example.realtime.ui.MessageToast;
import org.example.services.SessionContext;
import org.example.services.UserService;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Affiche un toast façon Messenger en bas-droite de l'écran à chaque
 * nouveau message {@code message-arrived} reçu sur
 * {@code /notifications/user/{me}}, sauf si la conversation concernée est
 * déjà ouverte au premier plan dans la page Discussion.
 *
 * <p>Démarré au login par {@link RealtimeService} (après que les services
 * Mercure soient prêts) ; arrêté au logout / shutdown.</p>
 *
 * <p>Chaîne d'évènements : un message arrive -> {@link RealtimeNotificationService}
 * dispatche -> ce service vérifie {@link SessionContext#getActiveDiscussionConversationId()}
 * -> affiche un {@link MessageToast} (5 s, cliquable pour ouvrir la
 * messagerie via {@link SessionContext#openDiscussionFromPlanning()}).</p>
 */
public final class ToastNotificationService {

    private static final ToastNotificationService INSTANCE = new ToastNotificationService();
    private static final int MAX_VISIBLE = 3;

    private final UserService userService = new UserService();

    private String subscriptionId;
    private boolean started;
    /** File des toasts visibles (FIFO) — sert à les empiler verticalement. */
    private final Deque<MessageToast> visibleStack = new ArrayDeque<>();

    private ToastNotificationService() {
    }

    public static ToastNotificationService getInstance() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        // S'assure d'être démarré côté JavaFX (les toasts créent des Stage).
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::start);
            return;
        }
        subscriptionId = RealtimeNotificationService.getInstance()
                .subscribeForCurrentUser(this::onIncomingNotification);
        started = subscriptionId != null;
    }

    public synchronized void stop() {
        if (!started) {
            return;
        }
        if (subscriptionId != null) {
            RealtimeNotificationService.getInstance().unsubscribe(subscriptionId);
            subscriptionId = null;
        }
        Runnable closeAll = () -> {
            for (MessageToast t : visibleStack) {
                try { t.close(); } catch (Exception ignored) { }
            }
            visibleStack.clear();
        };
        if (Platform.isFxApplicationThread()) {
            closeAll.run();
        } else {
            Platform.runLater(closeAll);
        }
        started = false;
    }

    // ------------------------------------------------------------------
    // Pipeline notification -> toast
    // ------------------------------------------------------------------

    private void onIncomingNotification(RealtimeEvent event) {
        if (event == null) {
            return;
        }
        // Filtre uniquement les "message-arrived" — on évitera de toaster des évènements
        // techniques (présence/planning) qui transiteraient par erreur sur ce topic.
        String type = event.stringField("type").orElse("");
        if (!"message-arrived".equalsIgnoreCase(type)) {
            return;
        }
        Integer conversationId = event.asObject()
                .filter(o -> o.has("conversationId") && !o.get("conversationId").isJsonNull())
                .map(o -> o.get("conversationId").getAsInt())
                .orElse(null);
        Integer fromUserId = event.asObject()
                .filter(o -> o.has("fromUserId") && !o.get("fromUserId").isJsonNull())
                .map(o -> o.get("fromUserId").getAsInt())
                .orElse(null);
        String preview = event.stringField("preview").orElse("");

        if (conversationId == null) {
            return;
        }

        SessionContext ctx = SessionContext.getInstance();
        // Si l'utilisateur regarde déjà cette conversation, pas de toast intrusif.
        if (ctx.getActiveDiscussionConversationId() == conversationId) {
            return;
        }
        // Echo de soi-même ? jamais notifier.
        if (fromUserId != null && fromUserId == ctx.getUserId()) {
            return;
        }

        // Résolution du nom du sender pour le titre du toast.
        String senderName = "Nouveau message";
        String initials = "?";
        if (fromUserId != null) {
            try {
                User u = userService.getUserById(fromUserId);
                if (u != null) {
                    String full = u.getFullName();
                    if (full != null && !full.isBlank()) {
                        senderName = full;
                        initials = computeInitials(full);
                    }
                }
            } catch (Exception ignored) { /* fallback générique */ }
        }

        final String finalSender = senderName;
        final String finalInitials = initials;
        final int finalConvId = conversationId;
        final String finalPreview = preview;

        Platform.runLater(() -> showToast(finalSender, finalPreview, finalInitials, finalConvId));
    }

    private void showToast(String senderName, String preview, String initials, int conversationId) {
        Window owner = pickOwner();
        if (owner == null) {
            // Pas de fenêtre prête (login à peine terminé) — on ignore plutôt qu'erreur.
            return;
        }
        // Conteneur tableau de taille 1 pour permettre la self-reference dans le callback.
        final MessageToast[] holder = new MessageToast[1];
        MessageToast toast = new MessageToast(
                owner,
                senderName,
                preview,
                initials,
                () -> openConversation(conversationId),
                () -> Platform.runLater(() -> visibleStack.remove(holder[0]))
        );
        holder[0] = toast;

        // Dépile le plus ancien si on dépasse le max.
        while (visibleStack.size() >= MAX_VISIBLE) {
            MessageToast oldest = visibleStack.pollFirst();
            if (oldest != null) {
                try { oldest.close(); } catch (Exception ignored) { }
            }
        }
        visibleStack.addLast(toast);
        // Position basée sur l'index dans la pile (toast le plus récent en bas).
        int indexFromBottom = visibleStack.size() - 1;
        toast.show(indexFromBottom * MessageToast.stackedHeight());
    }

    private void openConversation(int conversationId) {
        // Hook standard du projet : ouvre la page Discussion (planning -> messagerie).
        // On ne passe pas le conversationId : la page reprend la dernière conv ouverte
        // ou la sélectionne via pendingDiscussionClientUserId si défini.
        try {
            SessionContext.getInstance().openDiscussionFromPlanning();
        } catch (Exception ignored) {
        }
    }

    private Window pickOwner() {
        // 1) Une fenêtre focusée (la fenêtre principale active).
        for (Window w : Stage.getWindows()) {
            if (w.isShowing() && w.isFocused()) return w;
        }
        // 2) Sinon la première Stage visible.
        for (Window w : Stage.getWindows()) {
            if (w instanceof Stage && w.isShowing()) return w;
        }
        return null;
    }

    private static String computeInitials(String displayName) {
        if (displayName == null || displayName.isBlank()) return "?";
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) {
            String p = parts[0];
            return p.length() <= 1 ? p.toUpperCase() : p.substring(0, 2).toUpperCase();
        }
        String a = parts[0].substring(0, 1);
        String b = parts[parts.length - 1].substring(0, 1);
        return (a + b).toUpperCase();
    }
}
