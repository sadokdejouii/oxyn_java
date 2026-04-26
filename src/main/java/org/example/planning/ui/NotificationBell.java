package org.example.planning.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import org.example.realtime.RealtimeNotificationService;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;
import org.example.services.NotificationService;
import org.example.services.NotificationService.UnreadNotification;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cloche de notifications du module Planning : icône + badge compteur + menu
 * déroulant listant les conversations comportant des messages non lus.
 *
 * <p>Un clic sur un item invoque {@code onOpenConversation} ; un clic à côté de
 * l'info-bulle la referme. La récupération se fait via {@link NotificationService}
 * et un rafraîchissement automatique toutes les 5 secondes est programmé.</p>
 */
public final class NotificationBell extends StackPane {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM");
    private static final Duration REFRESH_INTERVAL = Duration.seconds(5);

    private final int userId;
    private final NotificationService service;
    private final Consumer<UnreadNotification> onOpenConversation;

    private final Label badgeLabel;
    private final StackPane badgeWrap;
    private final FontIcon bellIcon;
    private final Popup popup = new Popup();
    private final VBox dropdownRoot;
    private final VBox notificationList;
    private final Label emptyHint;

    private final Timeline refreshTimer;
    private String realtimeSubId;
    private int lastUnreadTotal = 0;

    public NotificationBell(int userId,
                            NotificationService service,
                            Consumer<UnreadNotification> onOpenConversation) {
        this.userId = userId;
        this.service = service;
        this.onOpenConversation = onOpenConversation;

        getStyleClass().add("planning-notif-bell");
        setPickOnBounds(true);
        setMinSize(42, 42);
        setPrefSize(42, 42);
        setMaxSize(42, 42);
        setAlignment(Pos.CENTER);

        bellIcon = new FontIcon("fas-bell");
        bellIcon.setIconSize(18);
        bellIcon.getStyleClass().add("planning-notif-bell-icon");

        badgeLabel = new Label("0");
        badgeLabel.getStyleClass().add("planning-notif-badge");

        badgeWrap = new StackPane(badgeLabel);
        badgeWrap.getStyleClass().add("planning-notif-badge-wrap");
        badgeWrap.setVisible(false);
        badgeWrap.setManaged(false);
        badgeWrap.setTranslateX(11);
        badgeWrap.setTranslateY(-11);
        badgeWrap.setMouseTransparent(true);

        getChildren().addAll(bellIcon, badgeWrap);

        dropdownRoot = new VBox();
        dropdownRoot.getStyleClass().add("planning-notif-dropdown");
        dropdownRoot.setMinWidth(360);
        dropdownRoot.setMaxWidth(380);
        dropdownRoot.setPrefWidth(380);

        Label title = new Label("Notifications");
        title.getStyleClass().add("planning-notif-dropdown-title");

        Label sub = new Label("Messages non lus");
        sub.getStyleClass().add("planning-notif-dropdown-sub");

        VBox headerBox = new VBox(2, title, sub);
        headerBox.getStyleClass().add("planning-notif-dropdown-header");

        Button markAllBtn = new Button("Tout marquer comme lu");
        markAllBtn.getStyleClass().add("planning-notif-mark-all");
        markAllBtn.setOnAction(e -> {
            service.markAllAsRead(userId);
            refreshNow();
        });

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.getChildren().addAll(headerBox, spacer(), markAllBtn);
        headerRow.getStyleClass().add("planning-notif-dropdown-header-row");

        notificationList = new VBox();
        notificationList.getStyleClass().add("planning-notif-list");

        emptyHint = new Label("Vous êtes à jour. Aucun message non lu.");
        emptyHint.getStyleClass().add("planning-notif-empty");
        emptyHint.setWrapText(true);

        ScrollPane scroll = new ScrollPane(notificationList);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportHeight(360);
        scroll.setMaxHeight(360);
        scroll.getStyleClass().add("planning-notif-scroll");

        dropdownRoot.getChildren().addAll(headerRow, scroll);

        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.setHideOnEscape(true);
        popup.getContent().add(dropdownRoot);

        setOnMouseClicked(e -> togglePopup());

        refreshTimer = new Timeline(new KeyFrame(REFRESH_INTERVAL, e -> refreshNow()));
        refreshTimer.setCycleCount(Timeline.INDEFINITE);

        sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene == null) {
                refreshTimer.stop();
                popup.hide();
                if (realtimeSubId != null) {
                    RealtimeNotificationService.getInstance().unsubscribe(realtimeSubId);
                    realtimeSubId = null;
                }
            } else {
                refreshNow();
                refreshTimer.play();
                if (realtimeSubId == null) {
                    realtimeSubId = RealtimeNotificationService.getInstance()
                            .subscribeForCurrentUser(ev -> refreshNow());
                }
            }
        });
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Force le rafraîchissement immédiat depuis le contrôleur parent (ex. après envoi). */
    public void refresh() {
        refreshNow();
    }

    /** Arrête le rafraîchissement et ferme le menu (à appeler lors de la destruction de la vue). */
    public void dispose() {
        refreshTimer.stop();
        popup.hide();
        if (realtimeSubId != null) {
            RealtimeNotificationService.getInstance().unsubscribe(realtimeSubId);
            realtimeSubId = null;
        }
    }

    private void togglePopup() {
        if (popup.isShowing()) {
            popup.hide();
            return;
        }
        refreshNow();
        Window w = getScene() != null ? getScene().getWindow() : null;
        if (w == null) {
            return;
        }
        Bounds b = localToScreen(getBoundsInLocal());
        if (b == null) {
            return;
        }
        double x = b.getMaxX() - 380;
        double y = b.getMaxY() + 8;
        popup.show(w, x, y);
    }

    /**
     * Requête synchrone sur le JAT : la connexion JDBC du projet n'étant pas
     * partagée via un pool, on évite les accès concurrents côté threads d'I/O.
     */
    private void refreshNow() {
        List<UnreadNotification> data;
        try {
            data = service.getUnreadMessages(userId);
        } catch (Exception ex) {
            data = List.of();
        }
        int total = 0;
        for (UnreadNotification n : data) {
            total += n.unreadCount();
        }
        updateBadge(total);
        if (popup.isShowing()) {
            rebuildDropdown(data);
        }
    }

    private void updateBadge(int total) {
        if (total <= 0) {
            badgeWrap.setVisible(false);
            badgeWrap.setManaged(false);
            lastUnreadTotal = 0;
            return;
        }
        boolean increased = total > lastUnreadTotal;
        badgeWrap.setVisible(true);
        badgeWrap.setManaged(true);
        badgeLabel.setText(total > 99 ? "99+" : String.valueOf(total));
        lastUnreadTotal = total;
        if (increased) {
            playPulseAnimation();
        }
    }

    private void playPulseAnimation() {
        // 1) Pulse du badge (rouge) — "pop" rapide
        Timeline badgePulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(badgeWrap.scaleXProperty(), 1.0),
                        new KeyValue(badgeWrap.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(140),
                        new KeyValue(badgeWrap.scaleXProperty(), 1.35),
                        new KeyValue(badgeWrap.scaleYProperty(), 1.35)),
                new KeyFrame(Duration.millis(320),
                        new KeyValue(badgeWrap.scaleXProperty(), 1.0),
                        new KeyValue(badgeWrap.scaleYProperty(), 1.0))
        );
        badgePulse.play();

        // 2) "Ring" de la cloche — rotations type sonnerie (-15° / +15° amorti)
        if (bellIcon != null) {
            Timeline ring = new Timeline(
                    new KeyFrame(Duration.ZERO,    new KeyValue(bellIcon.rotateProperty(), 0)),
                    new KeyFrame(Duration.millis(80),  new KeyValue(bellIcon.rotateProperty(), -15)),
                    new KeyFrame(Duration.millis(160), new KeyValue(bellIcon.rotateProperty(), 12)),
                    new KeyFrame(Duration.millis(240), new KeyValue(bellIcon.rotateProperty(), -8)),
                    new KeyFrame(Duration.millis(320), new KeyValue(bellIcon.rotateProperty(), 5)),
                    new KeyFrame(Duration.millis(400), new KeyValue(bellIcon.rotateProperty(), 0))
            );
            ring.play();
        }
    }

    private void rebuildDropdown(List<UnreadNotification> data) {
        notificationList.getChildren().clear();
        if (data == null || data.isEmpty()) {
            VBox wrap = new VBox(emptyHint);
            wrap.setAlignment(Pos.CENTER);
            wrap.setPadding(new Insets(32, 24, 32, 24));
            notificationList.getChildren().add(wrap);
            return;
        }
        for (UnreadNotification n : data) {
            notificationList.getChildren().add(buildItem(n));
        }
    }

    private Node buildItem(UnreadNotification n) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("planning-notif-item");
        row.setPadding(new Insets(12, 16, 12, 16));

        StackPane avatar = buildAvatar(n.otherUserName());

        VBox info = new VBox(3);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(n.otherUserName());
        name.getStyleClass().add("planning-notif-item-name");
        name.setMaxWidth(190);
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Label time = new Label(formatTime(n.lastMessageAt()));
        time.getStyleClass().add("planning-notif-item-time");
        topRow.getChildren().addAll(name, grow, time);

        Label preview = new Label(n.lastMessagePreview() == null || n.lastMessagePreview().isBlank()
                ? "Nouveau message"
                : n.lastMessagePreview());
        preview.getStyleClass().add("planning-notif-item-preview");
        preview.setWrapText(true);
        preview.setMaxWidth(260);

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label email = new Label(n.otherUserEmail() == null ? "" : n.otherUserEmail());
        email.getStyleClass().add("planning-notif-item-email");
        email.setMaxWidth(220);
        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);
        Label unreadPill = new Label(n.unreadCount() + " non lu" + (n.unreadCount() > 1 ? "s" : ""));
        unreadPill.getStyleClass().add("planning-notif-item-pill");
        metaRow.getChildren().addAll(email, metaSpacer, unreadPill);

        info.getChildren().addAll(topRow, preview, metaRow);

        row.getChildren().addAll(avatar, info);

        row.setOnMouseClicked(e -> {
            popup.hide();
            service.markConversationAsRead(userId, n.conversationId());
            if (onOpenConversation != null) {
                onOpenConversation.accept(n);
            }
            refreshNow();
        });
        return row;
    }

    private static StackPane buildAvatar(String displayName) {
        StackPane wrap = new StackPane();
        wrap.getStyleClass().add("planning-notif-avatar");
        wrap.setMinSize(44, 44);
        wrap.setPrefSize(44, 44);
        wrap.setMaxSize(44, 44);

        Circle ring = new Circle(21);
        ring.getStyleClass().add("planning-notif-avatar-ring");
        Circle fill = new Circle(18);
        fill.getStyleClass().add("planning-notif-avatar-fill");
        Label initials = new Label(initialsFrom(displayName));
        initials.getStyleClass().add("planning-notif-avatar-text");

        wrap.getChildren().addAll(ring, fill, initials);
        return wrap;
    }

    private static String initialsFrom(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            String p = parts[0];
            return p.length() <= 1 ? p.toUpperCase() : p.substring(0, 2).toUpperCase();
        }
        String a = parts[0].substring(0, 1);
        String b = parts[parts.length - 1].substring(0, 1);
        return (a + b).toUpperCase();
    }

    private static String formatTime(LocalDateTime dt) {
        if (dt == null) {
            return "";
        }
        if (dt.toLocalDate().equals(LocalDate.now())) {
            return TIME_FMT.format(dt);
        }
        return DATE_FMT.format(dt);
    }
}
