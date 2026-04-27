package org.example.controllers;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.example.discussion.ui.ChatBubble;
import org.example.discussion.ui.ChatHeader;
import org.example.discussion.ui.ConversationDetailsPanel;
import org.example.discussion.ui.DiscussionInboxSortMode;
import org.example.discussion.ui.InlineMessageEditor;
import org.example.discussion.ui.MessageContent;
import org.example.discussion.ui.MessageInput;
import org.example.discussion.ui.UserListItemCell;
import org.example.discussion.ui.VoiceRecorder;
import org.example.discussion.ui.VoiceStore;
import org.example.entities.ConversationInboxItem;
import org.example.entities.MessageRow;
import org.example.entities.User;
import org.example.realtime.PresenceService;
import org.example.realtime.RealtimeChatService;
import org.example.realtime.RealtimeService;
import org.example.realtime.TypingService;
import org.example.services.DiscussionService;
import org.example.services.NotificationService;
import org.example.services.SessionContext;
import org.example.services.TranslationService;
import org.example.services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

public class DiscussionPageController implements Initializable {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @FXML private Label subtitleLabel;
    @FXML private VBox chatColumn;
    @FXML private VBox conversationSidebar;
    @FXML private VBox inboxToolbarMount;
    @FXML private ListView<ConversationInboxItem> conversationList;
    @FXML private VBox chatHeaderMount;
    @FXML private ScrollPane messagesScroll;
    @FXML private VBox messagesBox;
    @FXML private VBox messageInputMount;
    @FXML private Label statusLabel;
    @FXML private ConversationDetailsPanel detailsPanel;

    private TextField messageField;
    private Button sendButton;
    private MessageInput messageInput;

    private ChatHeader chatHeader;

    private final ObservableList<ConversationInboxItem> inboxSource = FXCollections.observableArrayList();
    private FilteredList<ConversationInboxItem> inboxFiltered;
    private SortedList<ConversationInboxItem> inboxSorted;

    private TextField inboxSearchField;
    private ComboBox<DiscussionInboxSortMode> inboxSortCombo;
    private CheckBox inboxUnreadOnlyCheck;

    private final DiscussionService discussionService = new DiscussionService();
    private final NotificationService notificationService = new NotificationService();
    private final UserService userService = new UserService();
    private final RealtimeChatService realtimeChatService = RealtimeChatService.getInstance();
    private final PresenceService presenceService = PresenceService.getInstance();
    private final TypingService typingService = TypingService.getInstance();
    private final TranslationService translationService = TranslationService.getInstance();

    private int activeConversationId = -1;
    private int activePeerUserId = -1;
    private String activePeerDisplayName = "";
    private String activePeerEmail = "";
    private String chatSubscriptionId = null;
    private String activeTypingTopic = null;
    private String activePeerPresenceTopic = null;
    private BiConsumer<Integer, PresenceService.Presence> presenceListener;
    private BiConsumer<TypingService.TypingEvent, Boolean> typingListener;
    private int editingMessageId = -1;

    private VoiceRecorder voiceRecorder;

    private List<MessageRow> lastLoadedMessages = Collections.emptyList();

    /** Animation du scroll messages : annule la précédente pour éviter les à-coups. */
    private Timeline messagesScrollTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mountChatChrome();

        SessionContext ctx = SessionContext.getInstance();
        // L'ecran discussion adapte ses capacites par role:
        // - admin: lecture seule
        // - encadrant: inbox multi-clients
        // - client: conversation unique avec encadrant
        if (ctx.isAdmin()) {
            subtitleLabel.setText("Suivi des conversations — lecture seule.");
            messageField.setDisable(true);
            sendButton.setDisable(true);
            if (messageInput != null) {
                messageInput.micButton().setDisable(true);
                messageInput.emojiButton().setDisable(true);
            }
            updateThreadHeader("Équipe", "", "Sélectionnez une discussion à gauche.");
            wireInboxDataModel();
            mountInboxToolbar();
            wireInboxSelection(false);
            loadConversationList();
            statusLabel.setText("");
            return;
        }
        if (ctx.isEncadrant()) {
            subtitleLabel.setText("Échangez avec vos clients — réponses enregistrées comme messages d’accompagnement.");
            updateThreadHeader("Messagerie", "", "Choisissez un contact pour afficher la conversation.");
            wireInboxDataModel();
            mountInboxToolbar();
            wireInboxSelection(true);
            loadConversationList();
            statusLabel.setText("");
        } else {
            subtitleLabel.setText("Échangez en direct avec votre encadrant.");
            updateThreadHeaderForClient(ctx);
            if (inboxToolbarMount != null) {
                inboxToolbarMount.setManaged(false);
                inboxToolbarMount.setVisible(false);
            }
            if (conversationSidebar != null) {
                conversationSidebar.setVisible(false);
                conversationSidebar.setManaged(false);
            }
            if (!ctx.hasDbUser()) {
                statusLabel.setText("Connexion requise pour afficher la messagerie.");
            } else {
                statusLabel.setText("");
            }
            Platform.runLater(this::setupClientConversation);
        }
    }

    private void mountChatChrome() {
        if (chatColumn != null) {
            HBox.setHgrow(chatColumn, Priority.ALWAYS);
        }
        if (conversationList != null) {
            VBox.setVgrow(conversationList, Priority.ALWAYS);
        }
        if (messagesScroll != null) {
            VBox.setVgrow(messagesScroll, Priority.ALWAYS);
        }
        chatHeader = new ChatHeader();
        chatHeaderMount.getChildren().setAll(chatHeader);

        messageInput = new MessageInput();
        messageInputMount.getChildren().setAll(messageInput);
        messageField = messageInput.textField();
        sendButton = messageInput.sendButton();
        sendButton.setOnAction(e -> handleSend());
        messageField.setOnAction(e -> handleSend());
        messageInput.refreshButton().setOnAction(e -> handleRefresh());

        messageInput.setOnMicStart(this::startVoiceRecording);
        messageInput.setOnMicCancel(this::cancelVoiceRecording);
        messageInput.setOnMicConfirm(this::confirmVoiceRecording);

        messagesBox.prefWidthProperty().bind(messagesScroll.widthProperty());

        messageField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (activeConversationId > 0 && newVal != null && !newVal.isEmpty()) {
                typingService.notifyLocalTyping(activeConversationId);
            }
        });

        installRealtimeListeners();
        installSceneCleanupHook();
    }

    private void installRealtimeListeners() {
        // Presence: met a jour le header/side panel en quasi temps reel.
        presenceListener = (userId, presence) -> {
            if (userId != null && userId == activePeerUserId) {
                applyPresenceToHeader();
                refreshDetailsPanel();
            }
        };
        presenceService.addListener(presenceListener);

        // Typing: affiche "en train d'ecrire" pour l'interlocuteur actif.
        typingListener = (event, active) -> {
            if (event != null && event.conversationId() == activeConversationId
                    && event.userId() != SessionContext.getInstance().getUserId()) {
                if (chatHeader != null) {
                    chatHeader.setTypingActive(active, buildTypingLabel());
                }
            }
        };
        typingService.addListener(typingListener);
    }

    private void installSceneCleanupHook() {
        chatHeader.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                disposeRealtime();
            }
        });
    }

    private void disposeRealtime() {
        if (chatSubscriptionId != null) {
            realtimeChatService.unsubscribeFromConversation(activeConversationId, chatSubscriptionId);
            chatSubscriptionId = null;
        }
        if (presenceListener != null) {
            presenceService.removeListener(presenceListener);
            presenceListener = null;
        }
        if (typingListener != null) {
            typingService.removeListener(typingListener);
            typingListener = null;
        }
        if (activeConversationId > 0) {
            typingService.notifyLocalStopped(activeConversationId);
        }
        RealtimeService rt = RealtimeService.getInstance();
        if (activeTypingTopic != null) {
            rt.removeTopic(activeTypingTopic);
            activeTypingTopic = null;
        }
        if (activePeerPresenceTopic != null) {
            rt.removeTopic(activePeerPresenceTopic);
            activePeerPresenceTopic = null;
        }
        if (voiceRecorder != null && voiceRecorder.isRecording()) {
            voiceRecorder.cancel();
        }
        SessionContext.getInstance().setActiveDiscussionConversationId(-1);
    }

    private void wireInboxDataModel() {
        inboxFiltered = new FilteredList<>(inboxSource, p -> true);
        inboxSorted = new SortedList<>(inboxFiltered);
        inboxSorted.setComparator(DiscussionInboxSortMode.RECENT.comparator());
        conversationList.setItems(inboxSorted);
    }

    private void mountInboxToolbar() {
        if (inboxToolbarMount == null) {
            return;
        }
        VBox toolbar = new VBox(12);
        toolbar.getStyleClass().addAll("msg-inbox-toolbar", "planning-filter-toolbar", "planning-filter-panel");

        inboxSearchField = new TextField();
        inboxSearchField.setPromptText("Rechercher un contact, un e-mail ou un message…");
        inboxSearchField.getStyleClass().add("planning-search-premium");

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("planning-filter-row");
        Label tri = new Label("TRI");
        tri.getStyleClass().add("planning-filter-label");
        inboxSortCombo = new ComboBox<>(FXCollections.observableArrayList(DiscussionInboxSortMode.values()));
        inboxSortCombo.setValue(DiscussionInboxSortMode.RECENT);
        inboxSortCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(inboxSortCombo, Priority.ALWAYS);
        inboxSortCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(DiscussionInboxSortMode m) {
                return m == null ? "" : m.label();
            }

            @Override
            public DiscussionInboxSortMode fromString(String s) {
                for (DiscussionInboxSortMode m : DiscussionInboxSortMode.values()) {
                    if (m.label().equalsIgnoreCase(s)) {
                        return m;
                    }
                }
                return DiscussionInboxSortMode.RECENT;
            }
        });
        inboxSortCombo.getStyleClass().add("planning-select-premium");

        inboxUnreadOnlyCheck = new CheckBox("Non lus seulement");
        inboxUnreadOnlyCheck.getStyleClass().addAll("msg-inbox-filter-check", "planning-filter-check");

        row.getChildren().addAll(tri, inboxSortCombo, inboxUnreadOnlyCheck);
        toolbar.getChildren().addAll(inboxSearchField, row);
        inboxToolbarMount.getChildren().setAll(toolbar);

        inboxSearchField.textProperty().addListener((o, a, b) -> applyInboxFilter());
        inboxSortCombo.valueProperty().addListener((o, a, b) -> applyInboxSort());
        inboxUnreadOnlyCheck.selectedProperty().addListener((o, a, b) -> applyInboxFilter());
    }

    private void applyInboxFilter() {
        if (inboxFiltered == null) {
            return;
        }
        String raw = inboxSearchField == null ? "" : inboxSearchField.getText();
        String q = raw.trim().toLowerCase(Locale.ROOT);
        boolean unreadOnly = inboxUnreadOnlyCheck != null && inboxUnreadOnlyCheck.isSelected();
        inboxFiltered.setPredicate(item -> {
            if (unreadOnly && !item.awaitingStaffReply()) {
                return false;
            }
            if (q.isEmpty()) {
                return true;
            }
            return textContains(item.clientName(), q)
                    || textContains(item.clientEmail(), q)
                    || textContains(item.lastMessagePreview(), q);
        });
    }

    private static boolean textContains(String field, String q) {
        if (field == null) {
            return false;
        }
        return field.toLowerCase(Locale.ROOT).contains(q);
    }

    private void applyInboxSort() {
        if (inboxSorted == null) {
            return;
        }
        DiscussionInboxSortMode m = inboxSortCombo != null && inboxSortCombo.getValue() != null
                ? inboxSortCombo.getValue()
                : DiscussionInboxSortMode.RECENT;
        inboxSorted.setComparator(m.comparator());
    }

    private void updateThreadHeader(String title, String subtitle, String status) {
        if (chatHeader != null) {
            chatHeader.update(title, subtitle, status, initials(title));
        }
    }

    private void updateThreadHeaderForClient(SessionContext ctx) {
        if (chatHeader == null) {
            return;
        }
        String who = ctx.getDisplayName() != null && !ctx.getDisplayName().isBlank()
                ? ctx.getDisplayName()
                : "Vous";
        chatHeader.update("Votre conversation", "", "Messages avec votre encadrant", initials(who));
    }

    private void wireInboxSelection(boolean assignEncadrantOnSelect) {
        conversationList.setCellFactory(lv -> new UserListItemCell());
        conversationList.getSelectionModel().selectedItemProperty().addListener((o, a, sel) -> {
            if (sel != null) {
                onSelectConversation(sel, assignEncadrantOnSelect);
            }
        });
    }

    private void setupClientConversation() {
        SessionContext ctx = SessionContext.getInstance();
        if (!ctx.hasDbUser()) {
            appendSystem("Connexion requise pour afficher la messagerie.");
            return;
        }
        try {
            int cid = discussionService.ensureConversationForClient(ctx.getUserId(), -1);
            if (cid <= 0) {
                statusLabel.setText("Impossible de créer ou charger la conversation (base indisponible ?).");
                return;
            }
            int previous = activeConversationId;
            activeConversationId = cid;
            activePeerDisplayName = "";
            activePeerEmail = "";
            updateThreadHeaderForClient(ctx);
            statusLabel.setText("");
            reloadMessages();
            markActiveConversationAsRead(ctx);
            rebindRealtimeForActiveConversation(previous);
        } catch (Exception e) {
            statusLabel.setText("Impossible d’ouvrir la conversation. Réessayez plus tard.");
        }
    }

    private void loadConversationList() {
        if (inboxFiltered == null) {
            return;
        }
        try {
            int prev = activeConversationId;
            List<ConversationInboxItem> items = discussionService.listConversationInbox();
            inboxSource.setAll(items);
            applyInboxFilter();
            applyInboxSort();
            Platform.runLater(() -> restoreInboxSelectionAfterLoad(prev));
        } catch (Exception e) {
            statusLabel.setText("Impossible de charger la liste des discussions.");
        }
    }

    private void restoreInboxSelectionAfterLoad(int previousConversationId) {
        SessionContext ctx = SessionContext.getInstance();
        if (ctx.isEncadrant() && ctx.getPendingDiscussionClientUserId() > 0) {
            if (!trySelectPendingClientConversation()) {
                restoreInboxSelection(previousConversationId);
            }
            return;
        }
        restoreInboxSelection(previousConversationId);
    }

    private boolean trySelectPendingClientConversation() {
        SessionContext ctx = SessionContext.getInstance();
        int clientId = ctx.getPendingDiscussionClientUserId();
        if (clientId <= 0 || !ctx.isEncadrant() || inboxSorted == null || conversationList == null) {
            return false;
        }
        if (selectInboxByClientUserId(clientId)) {
            ctx.clearPendingDiscussionClientUserId();
            return true;
        }
        try {
            int cid = discussionService.ensureConversationForClient(clientId, ctx.getUserId());
            if (cid > 0) {
                discussionService.assignEncadrantToConversation(cid, ctx.getUserId());
            }
            List<ConversationInboxItem> items = discussionService.listConversationInbox();
            inboxSource.setAll(items);
            applyInboxFilter();
            applyInboxSort();
            if (selectInboxByClientUserId(clientId)) {
                ctx.clearPendingDiscussionClientUserId();
                return true;
            }
            ctx.clearPendingDiscussionClientUserId();
            statusLabel.setText("Actualisez la liste si la conversation n’apparaît pas.");
            return false;
        } catch (Exception e) {
            ctx.clearPendingDiscussionClientUserId();
            statusLabel.setText("Impossible d’ouvrir la discussion avec ce client.");
            return false;
        }
    }

    private boolean selectInboxByClientUserId(int clientUserId) {
        for (ConversationInboxItem it : inboxSorted) {
            if (it.clientId() == clientUserId) {
                conversationList.getSelectionModel().select(it);
                conversationList.scrollTo(it);
                return true;
            }
        }
        return false;
    }

    private void restoreInboxSelection(int previousConversationId) {
        if (inboxSorted == null || conversationList == null) {
            return;
        }
        if (previousConversationId > 0) {
            for (ConversationInboxItem it : inboxSorted) {
                if (it.conversationId() == previousConversationId) {
                    conversationList.getSelectionModel().select(it);
                    return;
                }
            }
        }
        if (!inboxSorted.isEmpty() && conversationList.getSelectionModel().getSelectedItem() == null) {
            conversationList.getSelectionModel().selectFirst();
        }
    }

    private void onSelectConversation(ConversationInboxItem item, boolean assignEncadrantOnSelect) {
        SessionContext ctx = SessionContext.getInstance();
        try {
            if (assignEncadrantOnSelect && !ctx.isAdmin()) {
                discussionService.assignEncadrantToConversation(item.conversationId(), ctx.getUserId());
            }
            int previous = activeConversationId;
            activeConversationId = item.conversationId();
            editingMessageId = -1;
            activePeerDisplayName = item.clientName() != null ? item.clientName().trim() : "";
            activePeerEmail = item.clientEmail() != null ? item.clientEmail().trim() : "";
            String sub = item.clientEmail() != null && !item.clientEmail().isBlank() ? item.clientEmail() : "";
            if (chatHeader != null) {
                chatHeader.update(item.clientName(), sub, item.presenceLabel(), initials(item.clientName()));
            }
            statusLabel.setText("");
            reloadMessages();
            markActiveConversationAsRead(ctx);
            rebindRealtimeForActiveConversation(previous);
        } catch (Exception e) {
            statusLabel.setText("Action impossible pour le moment.");
        }
    }

    private void markActiveConversationAsRead(SessionContext ctx) {
        if (activeConversationId <= 0 || ctx == null || !ctx.hasDbUser()) {
            return;
        }
        // API interne notifications: synchronise badge/liste lors de l'ouverture du thread.
        notificationService.markConversationAsRead(ctx.getUserId(), activeConversationId);
    }

    private void rebindRealtimeForActiveConversation(int oldConversationId) {
        RealtimeService rt = RealtimeService.getInstance();
        SessionContext.getInstance().setActiveDiscussionConversationId(activeConversationId);
        if (oldConversationId > 0 && chatSubscriptionId != null) {
            realtimeChatService.unsubscribeFromConversation(oldConversationId, chatSubscriptionId);
            chatSubscriptionId = null;
        }
        if (activeTypingTopic != null) {
            rt.removeTopic(activeTypingTopic);
            activeTypingTopic = null;
        }
        if (activePeerPresenceTopic != null) {
            rt.removeTopic(activePeerPresenceTopic);
            activePeerPresenceTopic = null;
        }
        if (activeConversationId <= 0) {
            activePeerUserId = -1;
            activePeerDisplayName = "";
            activePeerEmail = "";
            if (chatHeader != null) {
                chatHeader.clearPresence();
                chatHeader.setTypingActive(false, null);
            }
            if (detailsPanel != null) detailsPanel.showEmpty();
            return;
        }
        SessionContext ctx = SessionContext.getInstance();
        if (ctx.hasDbUser()) {
            try {
                activePeerUserId = discussionService.findOtherParticipantUserId(activeConversationId, ctx.getUserId());
            } catch (Exception e) {
                activePeerUserId = -1;
            }
            if (activePeerUserId > 0 && (activePeerDisplayName == null || activePeerDisplayName.isBlank())) {
                try {
                    User peer = userService.getUserById(activePeerUserId);
                    if (peer != null) {
                        String full = peer.getFullName();
                        activePeerDisplayName = full != null ? full.trim() : "";
                        if (peer.getEmail() != null) {
                            activePeerEmail = peer.getEmail();
                        }
                        if (chatHeader != null && !ctx.isAdmin()
                                && (peer.getNom() != null || peer.getPrenom() != null)
                                && !activePeerDisplayName.isEmpty()) {
                            chatHeader.update("Votre conversation",
                                    "Avec " + activePeerDisplayName,
                                    "", initials(activePeerDisplayName));
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            applyPresenceToHeader();
        } else {
            activePeerUserId = -1;
            activePeerDisplayName = "";
            activePeerEmail = "";
            if (chatHeader != null) {
                chatHeader.clearPresence();
            }
        }
        chatSubscriptionId = realtimeChatService.subscribeToConversation(
                activeConversationId, this::onIncomingChatEvent);

        activeTypingTopic = "/typing/conversation/" + activeConversationId;
        rt.addTopic(activeTypingTopic);

        if (activePeerUserId > 0) {
            activePeerPresenceTopic = "/presence/user/" + activePeerUserId;
            rt.addTopic(activePeerPresenceTopic);
        }
        if (chatHeader != null) {
            chatHeader.setTypingActive(false, null);
        }
        refreshDetailsPanel();
    }

    private String buildTypingLabel() {
        String first = firstName(activePeerDisplayName);
        if (first.isEmpty()) {
            return "En train d'écrire…";
        }
        return first + " est en train d'écrire…";
    }

    private static String firstName(String displayName) {
        if (displayName == null) {
            return "";
        }
        String trimmed = displayName.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        int sep = trimmed.indexOf(' ');
        return sep > 0 ? trimmed.substring(0, sep) : trimmed;
    }

    private void applyPresenceToHeader() {
        if (chatHeader == null) {
            return;
        }
        if (activePeerUserId <= 0) {
            chatHeader.clearPresence();
            return;
        }
        boolean online = presenceService.currentPresence(activePeerUserId) == PresenceService.Presence.ONLINE;
        String label = presenceService.formatPresenceLabel(activePeerUserId);
        chatHeader.updatePresence(online, label);
    }

    private void refreshDetailsPanel() {
        if (detailsPanel == null) return;
        if (activeConversationId <= 0) {
            detailsPanel.showEmpty();
            return;
        }
        boolean online = activePeerUserId > 0
                && presenceService.currentPresence(activePeerUserId) == PresenceService.Presence.ONLINE;
        String label = activePeerUserId > 0
                ? presenceService.formatPresenceLabel(activePeerUserId)
                : "";
        String name = activePeerDisplayName != null && !activePeerDisplayName.isBlank()
                ? activePeerDisplayName
                : "Conversation";
        detailsPanel.update(name, activePeerEmail, initials(name), online, label, lastLoadedMessages);
    }

    private void onIncomingChatEvent(org.example.realtime.RealtimeEvent event) {
        if (event == null || activeConversationId <= 0) {
            return;
        }
        // On ignore les events d'autres conversations: cette vue affiche uniquement le thread actif.
        Integer convId = event.asObject()
                .filter(o -> o.has("conversationId") && !o.get("conversationId").isJsonNull())
                .map(o -> o.get("conversationId").getAsInt())
                .orElse(activeConversationId);
        if (convId != activeConversationId) {
            return;
        }
        Integer senderId = event.asObject()
                .filter(o -> o.has("senderId") && !o.get("senderId").isJsonNull())
                .map(o -> o.get("senderId").getAsInt())
                .orElse(-1);
        if (senderId != null && senderId == SessionContext.getInstance().getUserId()) {
            return;
        }
        reloadMessages();
        markActiveConversationAsRead(SessionContext.getInstance());
        if (chatHeader != null) {
            chatHeader.setTypingActive(false, null);
        }
    }

    private void handleRefresh() {
        SessionContext ctx = SessionContext.getInstance();
        if (ctx.isEncadrant() || ctx.isAdmin()) {
            loadConversationList();
        }
        reloadMessages();
    }

    private void handleSend() {
        SessionContext ctx = SessionContext.getInstance();
        String text = messageField.getText() == null ? "" : messageField.getText().trim();
        if (text.isEmpty() || activeConversationId <= 0 || !ctx.hasDbUser()) {
            return;
        }
        try {
            int messageId = discussionService.sendMessage(
                    activeConversationId,
                    ctx.getUserId(),
                    text,
                    ctx.isEncadrant()
            );
            messageField.clear();
            typingService.notifyLocalStopped(activeConversationId);
            reloadMessages();
            if (ctx.isEncadrant() || ctx.isAdmin()) {
                loadConversationList();
            }
            publishMessageRealtime(messageId, text, ctx);
        } catch (Exception e) {
            alert("L’envoi du message a échoué. Vérifiez votre connexion puis réessayez.");
        }
    }

    // ----------------------------------------------------- Voice messages
    private void startVoiceRecording() {
        SessionContext ctx = SessionContext.getInstance();
        if (activeConversationId <= 0 || !ctx.hasDbUser() || ctx.isAdmin()) {
            return;
        }
        if (voiceRecorder == null) {
            voiceRecorder = new VoiceRecorder();
        }
        voiceRecorder.setOnError(t -> {
            messageInput.exitRecordingMode();
            alert("Impossible d'accéder au micro : " + (t.getMessage() == null ? "ligne audio indisponible" : t.getMessage()));
        });
        voiceRecorder.setOnLevel(level -> messageInput.updateRecordingFeedback(level,
                voiceRecorder.durationProperty().get()));
        voiceRecorder.setOnTick(secs -> messageInput.updateRecordingFeedback(
                voiceRecorder.levelProperty().get(), secs));
        try {
            voiceRecorder.start();
            messageInput.enterRecordingMode();
        } catch (IllegalStateException e) {
            // déjà en cours, ignore
        }
    }

    private void cancelVoiceRecording() {
        if (voiceRecorder != null && voiceRecorder.isRecording()) {
            voiceRecorder.cancel();
        }
        messageInput.exitRecordingMode();
    }

    private void confirmVoiceRecording() {
        if (voiceRecorder == null || !voiceRecorder.isRecording()) {
            messageInput.exitRecordingMode();
            return;
        }
        VoiceRecorder.RecordedVoice rec = voiceRecorder.stop();
        messageInput.exitRecordingMode();
        if (rec == null) {
            alert("Aucun son capté — réessayez en parlant un peu plus longtemps.");
            return;
        }
        SessionContext ctx = SessionContext.getInstance();
        String content = MessageContent.encodeVoice(rec.voiceId(), rec.durationSec());
        try {
            int messageId = discussionService.sendMessage(
                    activeConversationId,
                    ctx.getUserId(),
                    content,
                    ctx.isEncadrant());
            typingService.notifyLocalStopped(activeConversationId);
            reloadMessages();
            if (ctx.isEncadrant() || ctx.isAdmin()) {
                loadConversationList();
            }
            publishMessageRealtime(messageId, content, ctx);
        } catch (Exception e) {
            VoiceStore.delete(rec.voiceId());
            alert("L'envoi du message vocal a échoué. Réessayez plus tard.");
        }
    }

    private void publishMessageRealtime(int messageId, String text, SessionContext ctx) {
        if (messageId <= 0) {
            return;
        }
        int peerId = activePeerUserId;
        if (peerId <= 0) {
            try {
                peerId = discussionService.findOtherParticipantUserId(activeConversationId, ctx.getUserId());
                if (peerId > 0) {
                    activePeerUserId = peerId;
                }
            } catch (Exception ignored) {
            }
        }
        String type = ctx.isEncadrant() ? DiscussionService.TYPE_CONSEIL : DiscussionService.TYPE_MESSAGE;
        try {
            realtimeChatService.publishNewMessage(activeConversationId, messageId, ctx.getUserId(),
                    text, peerId, type);
            if (peerId > 0) {
                org.example.realtime.RealtimeNotificationService.getInstance()
                        .publishNewMessage(peerId, activeConversationId, ctx.getUserId(), text);
            }
        } catch (Exception ex) {
            System.err.println("[Realtime] publish message failed : " + ex.getMessage());
        }
    }

    private void reloadMessages() {
        if (activeConversationId <= 0) {
            return;
        }
        messagesBox.getChildren().clear();
        try {
            List<MessageRow> rows = discussionService.loadMessages(activeConversationId, 0);
            lastLoadedMessages = rows;
            SessionContext ctx = SessionContext.getInstance();
            Window owner = messagesScroll.getScene() != null ? messagesScroll.getScene().getWindow() : null;
            int animIndex = 0;
            for (MessageRow m : rows) {
                boolean mine = ctx.hasDbUser() && m.senderId() == ctx.getUserId();
                String timeStr = m.createdAt() != null ? TIME.format(m.createdAt()) : "";
                String peerIni = initials(m.senderName());
                boolean canEditDelete = mine
                        && !ctx.isAdmin()
                        && ctx.hasDbUser()
                        && DiscussionService.messageTypeAllowsUserEdit(m.type());
                if (canEditDelete && m.id() == editingMessageId
                        && !MessageContent.parse(m.contenu()).isVoice()) {
                    Node editorRow = InlineMessageEditor.createMineRow(
                            m,
                            messagesBox.widthProperty(),
                            text -> commitInlineEdit(m, text),
                            this::cancelInlineEdit);
                    messagesBox.getChildren().add(editorRow);
                    animateChatRowIn(editorRow, true, animIndex++);
                    continue;
                }
                ChatBubble.Row row = new ChatBubble.Row(m, mine, timeStr, peerIni,
                        messagesBox.widthProperty(), canEditDelete);
                if (canEditDelete) {
                    row.onEdit(() -> startInlineEdit(m));
                    row.onDelete(() -> offerDeleteMessage(owner, m));
                }
                row.onTranslate(toggle -> handleTranslate(m, toggle));
                Node bubbleRow = ChatBubble.createRow(row);
                messagesBox.getChildren().add(bubbleRow);
                animateChatRowIn(bubbleRow, mine, animIndex++);
            }
            scheduleSmoothScrollToBottom();
            refreshDetailsPanel();
        } catch (Exception ignored) {
            appendSystem("Impossible de charger les messages pour le moment. Réessayez dans un instant.");
        }
    }

    /**
     * Micro-interaction : apparition message (fade + slide horizontal selon l’auteur).
     * Délai en cascade pour un effet « timeline » discret.
     */
    private void animateChatRowIn(Node node, boolean mine, int staggerIndex) {
        double fromX = mine ? 26 : -26;
        node.setOpacity(0);
        node.setTranslateX(fromX);
        FadeTransition fade = new FadeTransition(Duration.millis(300), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(Duration.millis(340), node);
        slide.setFromX(fromX);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition intro = new ParallelTransition(fade, slide);
        intro.setDelay(Duration.millis(Math.min(staggerIndex, 14) * 32L));
        intro.play();
    }

    /** Scroll vertical animé vers le bas (remplace un saut brutal de {@code setVvalue(1)}). */
    private void scheduleSmoothScrollToBottom() {
        Platform.runLater(() -> {
            PauseTransition layoutWait = new PauseTransition(Duration.millis(48));
            layoutWait.setOnFinished(ev -> smoothScrollMessagesToEnd());
            layoutWait.play();
        });
    }

    private void smoothScrollMessagesToEnd() {
        if (messagesScroll == null) {
            return;
        }
        messagesScroll.applyCss();
        messagesScroll.layout();
        double start = messagesScroll.getVvalue();
        double target = 1.0;
        if (messagesScrollTimeline != null) {
            messagesScrollTimeline.stop();
            messagesScrollTimeline = null;
        }
        if (target - start < 0.03) {
            messagesScroll.setVvalue(target);
            return;
        }
        messagesScrollTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(messagesScroll.vvalueProperty(), start)),
                new KeyFrame(Duration.millis(450),
                        new KeyValue(messagesScroll.vvalueProperty(), target, Interpolator.EASE_BOTH))
        );
        messagesScrollTimeline.setOnFinished(e -> messagesScrollTimeline = null);
        messagesScrollTimeline.play();
    }

    private void handleTranslate(MessageRow m, ChatBubble.TranslationToggle toggle) {
        if (toggle.isVisible()) {
            toggle.hide();
            return;
        }
        MessageContent.Parsed parsed = MessageContent.parse(m.contenu());
        if (parsed.isVoice()) {
            return;
        }
        toggle.showLoading();
        // API externe (MyMemory) + fallback local geres dans TranslationService.
        translationService.translateToArabic(parsed.text())
                .thenAccept(arabic -> Platform.runLater(() ->
                        toggle.showResult(arabic == null || arabic.isBlank()
                                ? "(الترجمة غير متوفرة)"
                                : arabic)));
    }

    private void startInlineEdit(MessageRow m) {
        editingMessageId = m.id();
        reloadMessages();
    }

    private void cancelInlineEdit() {
        editingMessageId = -1;
        reloadMessages();
    }

    private void commitInlineEdit(MessageRow m, String newText) {
        try {
            discussionService.updateOwnMessageInConversation(
                    activeConversationId,
                    m.id(),
                    SessionContext.getInstance().getUserId(),
                    newText);
            editingMessageId = -1;
            reloadMessages();
            refreshStaffInbox();
        } catch (SQLException ex) {
            alert(ex.getMessage() != null ? ex.getMessage() : "Modification impossible.");
        }
    }

    private void offerDeleteMessage(Window owner, MessageRow m) {
        ButtonType supprimer = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType annuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "Ce message sera retiré définitivement de la conversation pour tous les participants.",
                annuler,
                supprimer);
        conf.initOwner(owner);
        conf.setTitle("Supprimer le message");
        conf.setHeaderText("Confirmer la suppression ?");
        DialogPane dp = conf.getDialogPane();
        dp.getStyleClass().add("msg-confirm-dialog");
        URL css = getClass().getResource("/css/planning-discussion-page.css");
        if (css != null) {
            dp.getStylesheets().add(css.toExternalForm());
        }
        Optional<ButtonType> r = conf.showAndWait();
        if (r.isEmpty() || r.get() != supprimer) {
            return;
        }
        try {
            // Pour les messages vocaux, supprime aussi le fichier local.
            MessageContent.Parsed parsed = MessageContent.parse(m.contenu());
            if (parsed.isVoice()) {
                VoiceStore.delete(parsed.voiceId());
            }
            discussionService.deleteOwnMessageInConversation(
                    activeConversationId,
                    m.id(),
                    SessionContext.getInstance().getUserId());
            reloadMessages();
            refreshStaffInbox();
        } catch (SQLException ex) {
            alert(ex.getMessage() != null ? ex.getMessage() : "Suppression impossible.");
        }
    }

    private void refreshStaffInbox() {
        SessionContext ctx = SessionContext.getInstance();
        if (ctx.isEncadrant() || ctx.isAdmin()) {
            loadConversationList();
        }
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) {
            return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase();
        }
        return (p[0].substring(0, 1) + p[p.length - 1].substring(0, 1)).toUpperCase();
    }

    private void appendSystem(String s) {
        HBox wrap = new HBox();
        wrap.setAlignment(Pos.CENTER);
        wrap.setPadding(new Insets(10, 20, 10, 20));
        Label l = new Label(s);
        l.setWrapText(true);
        l.getStyleClass().add("msg-system-line");
        wrap.getChildren().add(l);
        messagesBox.getChildren().add(wrap);
        animateSystemBannerIn(wrap);
        scheduleSmoothScrollToBottom();
    }

    private void animateSystemBannerIn(Node node) {
        node.setOpacity(0);
        node.setTranslateY(10);
        FadeTransition fade = new FadeTransition(Duration.millis(280), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(Duration.millis(320), node);
        slide.setFromY(10);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    private static void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setContentText(msg);
        URL css = DiscussionPageController.class.getResource("/css/planning-discussion-page.css");
        if (css != null) {
            a.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
        a.showAndWait();
    }
}
