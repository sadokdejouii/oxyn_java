package org.example.controllers;

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
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.example.discussion.ui.ChatBubble;
import org.example.discussion.ui.ChatHeader;
import org.example.discussion.ui.DiscussionInboxSortMode;
import org.example.discussion.ui.InlineMessageEditor;
import org.example.discussion.ui.MessageInput;
import org.example.discussion.ui.UserListItemCell;
import org.example.entities.ConversationInboxItem;
import org.example.entities.MessageRow;
import org.example.services.DiscussionService;
import org.example.services.SessionContext;

import javafx.scene.control.DialogPane;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class DiscussionPageController implements Initializable {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @FXML
    private Label subtitleLabel;

    @FXML
    private VBox conversationSidebar;

    @FXML
    private VBox inboxToolbarMount;

    @FXML
    private ListView<ConversationInboxItem> conversationList;

    @FXML
    private VBox chatHeaderMount;

    @FXML
    private ScrollPane messagesScroll;

    @FXML
    private VBox messagesBox;

    @FXML
    private VBox messageInputMount;

    @FXML
    private Label statusLabel;

    private TextField messageField;
    private Button sendButton;

    private ChatHeader chatHeader;

    private final ObservableList<ConversationInboxItem> inboxSource = FXCollections.observableArrayList();
    private FilteredList<ConversationInboxItem> inboxFiltered;
    private SortedList<ConversationInboxItem> inboxSorted;

    private TextField inboxSearchField;
    private ComboBox<DiscussionInboxSortMode> inboxSortCombo;
    private CheckBox inboxUnreadOnlyCheck;

    private final DiscussionService discussionService = new DiscussionService();
    private int activeConversationId = -1;
    /** Édition inline : id du message en cours, ou -1. */
    private int editingMessageId = -1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mountChatChrome();

        SessionContext ctx = SessionContext.getInstance();
        if (ctx.isAdmin()) {
            subtitleLabel.setText("Suivi des conversations — lecture seule.");
            messageField.setDisable(true);
            sendButton.setDisable(true);
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
        chatHeader = new ChatHeader();
        chatHeaderMount.getChildren().setAll(chatHeader);

        MessageInput mi = new MessageInput();
        messageInputMount.getChildren().setAll(mi);
        messageField = mi.textField();
        sendButton = mi.sendButton();
        sendButton.setOnAction(e -> handleSend());
        mi.refreshButton().setOnAction(e -> handleRefresh());

        messagesBox.prefWidthProperty().bind(messagesScroll.widthProperty());
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
            activeConversationId = cid;
            updateThreadHeaderForClient(ctx);
            statusLabel.setText("");
            reloadMessages();
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
            Platform.runLater(() -> restoreInboxSelection(prev));
        } catch (Exception e) {
            statusLabel.setText("Impossible de charger la liste des discussions.");
        }
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
            activeConversationId = item.conversationId();
            editingMessageId = -1;
            String sub = item.clientEmail() != null && !item.clientEmail().isBlank() ? item.clientEmail() : "";
            if (chatHeader != null) {
                chatHeader.update(item.clientName(), sub, item.presenceLabel(), initials(item.clientName()));
            }
            statusLabel.setText("");
            reloadMessages();
        } catch (Exception e) {
            statusLabel.setText("Action impossible pour le moment.");
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
            discussionService.sendMessage(
                    activeConversationId,
                    ctx.getUserId(),
                    text,
                    ctx.isEncadrant()
            );
            messageField.clear();
            reloadMessages();
            if (ctx.isEncadrant() || ctx.isAdmin()) {
                loadConversationList();
            }
        } catch (Exception e) {
            alert("L’envoi du message a échoué. Vérifiez votre connexion puis réessayez.");
        }
    }

    private void reloadMessages() {
        if (activeConversationId <= 0) {
            return;
        }
        messagesBox.getChildren().clear();
        try {
            List<MessageRow> rows = discussionService.loadMessages(activeConversationId, 0);
            SessionContext ctx = SessionContext.getInstance();
            Window owner = messagesScroll.getScene() != null ? messagesScroll.getScene().getWindow() : null;
            for (MessageRow m : rows) {
                boolean mine = ctx.hasDbUser() && m.senderId() == ctx.getUserId();
                String timeStr = m.createdAt() != null ? TIME.format(m.createdAt()) : "";
                String peerIni = initials(m.senderName());
                boolean showMenu = mine
                        && !ctx.isAdmin()
                        && ctx.hasDbUser()
                        && DiscussionService.messageTypeAllowsUserEdit(m.type());
                if (showMenu && m.id() == editingMessageId) {
                    messagesBox.getChildren().add(InlineMessageEditor.createMineRow(
                            m,
                            messagesBox.widthProperty(),
                            text -> commitInlineEdit(m, text),
                            this::cancelInlineEdit));
                    continue;
                }
                Runnable onEdit = showMenu ? () -> startInlineEdit(m) : null;
                Runnable onDelete = showMenu ? () -> offerDeleteMessage(owner, m) : null;
                messagesBox.getChildren().add(
                        ChatBubble.createRow(m, mine, timeStr, peerIni, messagesBox.widthProperty(), showMenu, onEdit, onDelete));
            }
            Platform.runLater(() -> messagesScroll.setVvalue(1.0));
        } catch (Exception ignored) {
            appendSystem("Impossible de charger les messages pour le moment. Réessayez dans un instant.");
        }
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
