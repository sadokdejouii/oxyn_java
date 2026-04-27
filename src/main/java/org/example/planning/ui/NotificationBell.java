package org.example.planning.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Duration;
import org.example.discussion.ui.MessageContent;
import org.example.realtime.RealtimeNotificationService;
import org.example.services.NotificationService;
import org.example.services.NotificationService.UnreadNotification;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dropdown notifications attaché à la cloche (overlay), style app moderne.
 * UI/UX uniquement : aucune modification backend ou DB.
 */
public final class NotificationBell extends StackPane {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM");
    private static final Duration REFRESH_INTERVAL = Duration.seconds(5);

    private static final double POPUP_WIDTH = 352d;
    private static final double POPUP_MAX_HEIGHT = 450d;
    private static final double LIST_VIEWPORT_HEIGHT = 336d;

    private final int userId;
    private final NotificationService service;
    private final Consumer<UnreadNotification> onOpenConversation;

    private final Label badgeLabel;
    private final StackPane badgeWrap;
    private final Circle badgeHalo;
    private final FontIcon bellIcon;
    private Timeline haloPulseTimeline;
    private Timeline idleBadgePulseTimeline;

    private final Popup popup = new Popup();
    private final VBox dropdownRoot;
    private final VBox notificationList;
    private final Label countSubtitle;
    private final Label tabAll;
    private final Label tabMessages;
    private final VBox emptyState;

    private enum Tab { ALL, MESSAGES }
    private Tab activeTab = Tab.ALL;

    private final Timeline refreshTimer;
    private String realtimeSubId;
    private int lastUnreadTotal = 0;
    private List<UnreadNotification> lastData = List.of();

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

        badgeHalo = new Circle(14);
        badgeHalo.getStyleClass().add("planning-notif-badge-halo");
        badgeHalo.setManaged(false);
        badgeHalo.setMouseTransparent(true);

        StackPane badgeStack = new StackPane(badgeHalo, badgeLabel);
        badgeWrap = new StackPane(badgeStack);
        badgeWrap.getStyleClass().add("planning-notif-badge-wrap");
        badgeWrap.setVisible(false);
        badgeWrap.setManaged(false);
        badgeWrap.setTranslateX(11);
        badgeWrap.setTranslateY(-11);
        badgeWrap.setMouseTransparent(true);

        getChildren().addAll(bellIcon, badgeWrap);

        dropdownRoot = new VBox();
        dropdownRoot.getStyleClass().add("planning-notif-dropdown");
        dropdownRoot.setMinWidth(POPUP_WIDTH);
        dropdownRoot.setPrefWidth(POPUP_WIDTH);
        dropdownRoot.setMaxWidth(POPUP_WIDTH);
        dropdownRoot.setMaxHeight(POPUP_MAX_HEIGHT);

        Label headerTitle = new Label("Notifications");
        headerTitle.getStyleClass().add("planning-notif-dropdown-title");
        countSubtitle = new Label("Tout est à jour");
        countSubtitle.getStyleClass().add("planning-notif-dropdown-sub");

        VBox headerText = new VBox(2, headerTitle, countSubtitle);

        Button markAllBtn = new Button("Tout lire");
        markAllBtn.getStyleClass().add("planning-notif-mark-all");
        FontIcon markIcon = new FontIcon("fas-check-double");
        markIcon.setIconSize(11);
        markAllBtn.setGraphic(markIcon);
        markAllBtn.setOnAction(e -> {
            service.markAllAsRead(userId);
            refreshNow();
            rebuildList();
        });

        HBox headerRow = new HBox(12, headerText, spacer(), markAllBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.getStyleClass().add("planning-notif-dropdown-header-row");

        tabAll = buildTab("Tout", Tab.ALL);
        tabMessages = buildTab("Messages", Tab.MESSAGES);
        HBox tabsRow = new HBox(8, tabAll, tabMessages);
        tabsRow.setAlignment(Pos.CENTER_LEFT);
        tabsRow.getStyleClass().add("planning-notif-tabs");

        VBox header = new VBox(headerRow, tabsRow);
        header.getStyleClass().add("planning-notif-header");

        notificationList = new VBox();
        notificationList.getStyleClass().add("planning-notif-list");

        FontIcon emptyIcon = new FontIcon("far-bell");
        emptyIcon.setIconSize(38);
        emptyIcon.getStyleClass().add("planning-notif-empty-icon");
        StackPane emptyIconWrap = new StackPane(emptyIcon);
        emptyIconWrap.getStyleClass().add("planning-notif-empty-icon-wrap");
        emptyIconWrap.setMinSize(72, 72);
        emptyIconWrap.setMaxSize(72, 72);

        Label emptyTitle = new Label("Tout est à jour");
        emptyTitle.getStyleClass().add("planning-notif-empty-title");
        Label emptySub = new Label("Aucune nouvelle notification.\nNous vous préviendrons dès qu'un message arrive.");
        emptySub.getStyleClass().add("planning-notif-empty-sub");
        emptySub.setWrapText(true);
        emptySub.setMaxWidth(260);
        emptySub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        emptyState = new VBox(12, emptyIconWrap, emptyTitle, emptySub);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(30, 20, 34, 20));
        emptyState.getStyleClass().add("planning-notif-empty");

        ScrollPane scroll = new ScrollPane(notificationList);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportHeight(LIST_VIEWPORT_HEIGHT);
        scroll.setMaxHeight(LIST_VIEWPORT_HEIGHT);
        scroll.getStyleClass().add("planning-notif-scroll");

        Label footer = new Label("Vous voyez les notifications non lues");
        footer.getStyleClass().add("planning-notif-footer");
        footer.setMaxWidth(Double.MAX_VALUE);
        footer.setAlignment(Pos.CENTER);

        dropdownRoot.getChildren().addAll(header, scroll, footer);

        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.setHideOnEscape(true);
        popup.getContent().add(dropdownRoot);

        setOnMouseClicked(e -> togglePopup());

        refreshTimer = new Timeline(new KeyFrame(REFRESH_INTERVAL, e -> refreshNow()));
        refreshTimer.setCycleCount(Timeline.INDEFINITE);

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                refreshTimer.stop();
                popup.hide();
                stopHaloPulse();
                stopIdleBadgePulse();
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

        applyActiveTab();
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public void refresh() {
        refreshNow();
    }

    public void dispose() {
        refreshTimer.stop();
        popup.hide();
        stopHaloPulse();
        stopIdleBadgePulse();
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

        Window window = getScene() != null ? getScene().getWindow() : null;
        if (window == null) {
            return;
        }
        Bounds bellBounds = localToScreen(getBoundsInLocal());
        if (bellBounds == null) {
            return;
        }

        double x = bellBounds.getMaxX() - POPUP_WIDTH;
        double y = bellBounds.getMaxY() + 8;

        Rectangle2D screenBounds = Screen.getScreensForRectangle(bellBounds.getMinX(), bellBounds.getMinY(), bellBounds.getWidth(), bellBounds.getHeight()).stream()
                .findFirst()
                .map(Screen::getVisualBounds)
                .orElse(Screen.getPrimary().getVisualBounds());
        double minX = screenBounds.getMinX() + 8;
        double maxX = screenBounds.getMaxX() - POPUP_WIDTH - 8;
        x = Math.max(minX, Math.min(maxX, x));
        y = Math.max(screenBounds.getMinY() + 8, y);

        popup.show(window, x, y);
        rebuildList();
        animatePopupOpen();
    }

    private void animatePopupOpen() {
        dropdownRoot.setOpacity(0);
        dropdownRoot.setTranslateY(-8);
        FadeTransition fade = new FadeTransition(Duration.millis(180), dropdownRoot);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(Duration.millis(220), dropdownRoot);
        slide.setFromY(-8);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

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
        lastData = data;
        updateBadge(total);
        countSubtitle.setText(total == 0 ? "Tout est à jour" : total + " non lu" + (total > 1 ? "s" : ""));

        if (popup.isShowing()) {
            rebuildList();
        }
    }

    private void updateBadge(int total) {
        if (total <= 0) {
            badgeWrap.setVisible(false);
            badgeWrap.setManaged(false);
            badgeHalo.setVisible(false);
            stopHaloPulse();
            stopIdleBadgePulse();
            lastUnreadTotal = 0;
            return;
        }

        boolean increased = total > lastUnreadTotal;
        badgeWrap.setVisible(true);
        badgeWrap.setManaged(true);
        badgeLabel.setText(total > 99 ? "99+" : String.valueOf(total));
        lastUnreadTotal = total;

        startHaloPulse();
        if (increased) {
            playPulseAnimation();
        } else {
            startIdleBadgePulse();
        }
    }

    private void playPulseAnimation() {
        stopIdleBadgePulse();
        Timeline badgePulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(badgeWrap.scaleXProperty(), 1.0),
                        new KeyValue(badgeWrap.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(140),
                        new KeyValue(badgeWrap.scaleXProperty(), 1.34),
                        new KeyValue(badgeWrap.scaleYProperty(), 1.34)),
                new KeyFrame(Duration.millis(320),
                        new KeyValue(badgeWrap.scaleXProperty(), 1.0),
                        new KeyValue(badgeWrap.scaleYProperty(), 1.0))
        );
        badgePulse.setOnFinished(e -> {
            if (badgeWrap.isVisible()) {
                startIdleBadgePulse();
            }
        });
        badgePulse.play();

        Timeline ring = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(bellIcon.rotateProperty(), 0)),
                new KeyFrame(Duration.millis(85), new KeyValue(bellIcon.rotateProperty(), -14)),
                new KeyFrame(Duration.millis(170), new KeyValue(bellIcon.rotateProperty(), 11)),
                new KeyFrame(Duration.millis(255), new KeyValue(bellIcon.rotateProperty(), -7)),
                new KeyFrame(Duration.millis(340), new KeyValue(bellIcon.rotateProperty(), 4)),
                new KeyFrame(Duration.millis(430), new KeyValue(bellIcon.rotateProperty(), 0))
        );
        ring.play();
    }

    private void startIdleBadgePulse() {
        if (idleBadgePulseTimeline != null) {
            return;
        }
        idleBadgePulseTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(badgeWrap.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(badgeWrap.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(900),
                        new KeyValue(badgeWrap.scaleXProperty(), 1.08, Interpolator.EASE_BOTH),
                        new KeyValue(badgeWrap.scaleYProperty(), 1.08, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(1800),
                        new KeyValue(badgeWrap.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(badgeWrap.scaleYProperty(), 1.0, Interpolator.EASE_BOTH))
        );
        idleBadgePulseTimeline.setCycleCount(Timeline.INDEFINITE);
        idleBadgePulseTimeline.play();
    }

    private void stopIdleBadgePulse() {
        if (idleBadgePulseTimeline != null) {
            idleBadgePulseTimeline.stop();
            idleBadgePulseTimeline = null;
        }
        badgeWrap.setScaleX(1.0);
        badgeWrap.setScaleY(1.0);
    }

    private void startHaloPulse() {
        if (haloPulseTimeline != null) {
            return;
        }
        badgeHalo.setVisible(true);
        badgeHalo.setOpacity(0.55);
        badgeHalo.setScaleX(1);
        badgeHalo.setScaleY(1);
        haloPulseTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(badgeHalo.opacityProperty(), 0.55, Interpolator.EASE_OUT),
                        new KeyValue(badgeHalo.scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(badgeHalo.scaleYProperty(), 1.0, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(1500),
                        new KeyValue(badgeHalo.opacityProperty(), 0.0, Interpolator.EASE_OUT),
                        new KeyValue(badgeHalo.scaleXProperty(), 1.85, Interpolator.EASE_OUT),
                        new KeyValue(badgeHalo.scaleYProperty(), 1.85, Interpolator.EASE_OUT))
        );
        haloPulseTimeline.setCycleCount(Timeline.INDEFINITE);
        haloPulseTimeline.play();
    }

    private void stopHaloPulse() {
        if (haloPulseTimeline != null) {
            haloPulseTimeline.stop();
            haloPulseTimeline = null;
        }
        badgeHalo.setOpacity(0);
        badgeHalo.setScaleX(1.0);
        badgeHalo.setScaleY(1.0);
    }

    private Label buildTab(String text, Tab tab) {
        Label l = new Label(text);
        l.getStyleClass().add("planning-notif-tab");
        l.setOnMouseClicked(e -> {
            activeTab = tab;
            applyActiveTab();
            rebuildList();
        });
        return l;
    }

    private void applyActiveTab() {
        tabAll.getStyleClass().remove("planning-notif-tab--active");
        tabMessages.getStyleClass().remove("planning-notif-tab--active");
        if (activeTab == Tab.ALL) {
            tabAll.getStyleClass().add("planning-notif-tab--active");
        } else {
            tabMessages.getStyleClass().add("planning-notif-tab--active");
        }
    }

    private void rebuildList() {
        notificationList.getChildren().clear();
        List<UnreadNotification> data = lastData == null ? List.of() : lastData;
        if (data.isEmpty()) {
            notificationList.getChildren().add(emptyState);
            return;
        }

        int index = 0;
        for (UnreadNotification n : data) {
            Node item = buildItem(n);
            notificationList.getChildren().add(item);
            playFadeIn(item, index++);
        }
    }

    private void playFadeIn(Node node, int index) {
        node.setOpacity(0);
        node.setTranslateX(-10);
        node.setScaleX(0.97);
        node.setScaleY(0.97);

        FadeTransition fade = new FadeTransition(Duration.millis(260), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(280), node);
        slide.setFromX(-10);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(Duration.millis(300), node);
        scale.setFromX(0.97);
        scale.setFromY(0.97);
        scale.setToX(1);
        scale.setToY(1);
        // JavaFX requires all SPLINE control points in [0,1].
        // Keep a smooth pop-in without risking runtime IllegalArgumentException.
        scale.setInterpolator(Interpolator.SPLINE(0.25, 0.10, 0.25, 1.0));

        ParallelTransition anim = new ParallelTransition(fade, slide, scale);
        anim.setDelay(Duration.millis(Math.min(index, 8) * 34L));
        anim.play();
    }

    private Node buildItem(UnreadNotification n) {
        StackPane avatar = buildAvatar(n.otherUserName());

        Label name = new Label(n.otherUserName() == null ? "Utilisateur" : n.otherUserName());
        name.getStyleClass().add("planning-notif-item-name");
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setMaxWidth(216);

        Label preview = new Label(buildPreview(n));
        preview.getStyleClass().add("planning-notif-item-preview");
        preview.setWrapText(true);
        preview.setMaxWidth(228);

        Label time = new Label(formatRelativeTime(n.lastMessageAt()));
        time.getStyleClass().add("planning-notif-item-time");

        VBox textCol = new VBox(3, name, preview, time);
        textCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        Circle unreadDot = new Circle(5);
        unreadDot.getStyleClass().add("planning-notif-unread-dot");
        VBox dotCol = new VBox(unreadDot);
        dotCol.setAlignment(Pos.CENTER);
        dotCol.setMinWidth(16);

        HBox row = new HBox(12, avatar, textCol, dotCol);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.getStyleClass().add("planning-notif-item");

        boolean unread = n.unreadCount() > 0;
        unreadDot.setVisible(unread);
        unreadDot.setManaged(unread);
        if (unread) {
            row.getStyleClass().add("planning-notif-item--unread");
        }

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

    private String buildPreview(UnreadNotification n) {
        String raw = n.lastMessagePreview();
        String clean = (raw == null || raw.isBlank())
                ? "Vous a envoyé un nouveau message"
                : MessageContent.preview(raw);
        if (n.unreadCount() > 1) {
            clean = clean + "  ·  " + n.unreadCount() + " messages";
        }
        return clean;
    }

    private static StackPane buildAvatar(String displayName) {
        StackPane wrap = new StackPane();
        wrap.getStyleClass().add("planning-notif-avatar");
        wrap.setMinSize(40, 40);
        wrap.setPrefSize(40, 40);
        wrap.setMaxSize(40, 40);

        Label initials = new Label(initialsFrom(displayName));
        initials.getStyleClass().add("planning-notif-avatar-text");
        wrap.getChildren().add(initials);
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
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private static String formatRelativeTime(LocalDateTime dt) {
        if (dt == null) {
            return "";
        }
        LocalDateTime now = LocalDateTime.now();
        long sec = ChronoUnit.SECONDS.between(dt, now);
        if (sec < 30) return "à l'instant";
        long min = sec / 60;
        if (min < 60) return min + " min";
        long hour = min / 60;
        if (hour < 24 && dt.toLocalDate().equals(LocalDate.now())) return TIME_FMT.format(dt);
        if (dt.toLocalDate().equals(LocalDate.now().minusDays(1))) return "hier";
        long days = ChronoUnit.DAYS.between(dt.toLocalDate(), LocalDate.now());
        if (days < 7) return days + " j";
        return DATE_FMT.format(dt);
    }
}
