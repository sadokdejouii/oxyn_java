package org.example.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.discussion.ui.ChatBubble;
import org.example.discussion.ui.ChatHeader;
import org.example.discussion.ui.MessageInput;
import org.example.discussion.ui.UserListItemCell;
import org.example.entities.ConversationInboxItem;
import org.example.entities.MessageRow;
import org.example.services.DiscussionService;
import org.example.services.SessionContext;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class DiscussionPageController implements Initializable {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @FXML
    private Label subtitleLabel;

    @FXML
    private VBox conversationSidebar;

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

    private final DiscussionService discussionService = new DiscussionService();
    private int activeConversationId = -1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mountChatChrome();

        SessionContext ctx = SessionContext.getInstance();
        if (ctx.isAdmin()) {
            subtitleLabel.setText("Suivi des conversations — lecture seule.");
            messageField.setDisable(true);
            sendButton.setDisable(true);
            updateThreadHeader("Équipe", "", "Sélectionnez une discussion à gauche.");
            wireInboxSelection(false);
            loadConversationList();
            statusLabel.setText("");
            return;
        }
        if (ctx.isEncadrant()) {
            subtitleLabel.setText("Échangez avec vos clients — réponses enregistrées comme messages d’accompagnement.");
            updateThreadHeader("Messagerie", "", "Choisissez un contact pour afficher la conversation.");
            wireInboxSelection(true);
            loadConversationList();
            statusLabel.setText("");
        } else {
            subtitleLabel.setText("Échangez en direct avec votre encadrant.");
            updateThreadHeaderForClient(ctx);
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
        try {
            List<ConversationInboxItem> items = discussionService.listConversationInbox();
            ObservableList<ConversationInboxItem> obs = FXCollections.observableArrayList(items);
            conversationList.setItems(obs);
            if (!items.isEmpty() && conversationList.getSelectionModel().getSelectedItem() == null) {
                conversationList.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            statusLabel.setText("Impossible de charger la liste des discussions.");
        }
    }

    private void onSelectConversation(ConversationInboxItem item, boolean assignEncadrantOnSelect) {
        SessionContext ctx = SessionContext.getInstance();
        try {
            if (assignEncadrantOnSelect && !ctx.isAdmin()) {
                discussionService.assignEncadrantToConversation(item.conversationId(), ctx.getUserId());
            }
            activeConversationId = item.conversationId();
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
            for (MessageRow m : rows) {
                boolean mine = ctx.hasDbUser() && m.senderId() == ctx.getUserId();
                String timeStr = m.createdAt() != null ? TIME.format(m.createdAt()) : "";
                String peerIni = initials(m.senderName());
                messagesBox.getChildren().add(
                        ChatBubble.createRow(m, mine, timeStr, peerIni, messagesBox.widthProperty()));
            }
            Platform.runLater(() -> messagesScroll.setVvalue(1.0));
        } catch (Exception ignored) {
            appendSystem("Impossible de charger les messages pour le moment. Réessayez dans un instant.");
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
        wrap.setPadding(new javafx.geometry.Insets(10, 20, 10, 20));
        Label l = new Label(s);
        l.setWrapText(true);
        l.getStyleClass().add("msg-system-line");
        wrap.getChildren().add(l);
        messagesBox.getChildren().add(wrap);
    }

    private static void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setContentText(msg);
        a.showAndWait();
    }
}
