package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.planning.PlanningUi;
import org.example.planning.PlanningViewModel;
import org.example.planning.ui.NotificationBell;
import org.example.realtime.RealtimePlanningSyncService;
import org.example.realtime.ui.RealtimeStatusBadge;
import org.example.services.CurrentSession;
import org.example.services.NotificationService;
import org.example.services.SessionContext;

import java.net.URL;
import java.util.ResourceBundle;

public class PlanningPageController implements Initializable {

    private MainLayoutController mainLayoutController;

    /** Référence pour réinjecter le layout après {@code setMainLayoutController} (appelé après {@code initialize}). */
    private EncadrantPlanningHubController encadrantHubController;

    @FXML
    private VBox dynamicContainer;

    @FXML
    private VBox planningMainShell;

    @FXML
    private StackPane planningPageStack;

    @FXML
    private ScrollPane planningPageScroll;

    @FXML
    private VBox planningRoot;

    @FXML
    private Label heroSub;

    @FXML
    private Label roleBadge;

    @FXML
    private Label avatarInitials;

    @FXML
    private HBox planningClientToolbar;
    @FXML
    private Button btnObjectifAssistant;
    @FXML
    private Button btnTaskCalendar;

    @FXML
    private StackPane notificationBellSlot;

    @FXML
    private StackPane realtimeStatusSlot;

    private NotificationBell notificationBell;
    private RealtimeStatusBadge realtimeStatusBadge;
    private final NotificationService notificationService = new NotificationService();
    private String planningSyncSubscriptionId;
    private long lastPlanningRefreshMs = 0L;
    private PlanningClientController activeClientController;
    private EncadrantPlanningHubController activeEncadrantHub;

    private final PlanningViewModel viewModel = new PlanningViewModel();

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
        if (encadrantHubController != null) {
            encadrantHubController.setMainLayoutController(mainLayoutController);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        heroSub.textProperty().bind(viewModel.heroSubtitleProperty());
        roleBadge.textProperty().bind(viewModel.roleBadgeTextProperty());
        applyRoleBadgeStyle(viewModel.roleBadgeVariant());
        if (avatarInitials != null) {
            avatarInitials.setText(initialsFromDisplayName(CurrentSession.context().getDisplayName()));
        }
        var ctx = CurrentSession.context();
        applyAdminPlanningDarkTheme(false);
        applyClientPlanningZen(false);
        encadrantHubController = null;
        dynamicContainer.getChildren().clear();

        mountNotificationBell(ctx);
        mountRealtimeStatusBadge();
        installPlanningRealtimeListener();

        if (ctx.isEncadrant()) {
            setPlanningClientToolbarVisible(false);
            buildEncadrantView();
        } else if (ctx.isAdmin()) {
            setPlanningClientToolbarVisible(false);
            buildAdminView();
        } else {
            buildClientView();
            setPlanningClientToolbarVisible(ctx.isClientUser() && ctx.hasDbUser());
        }
    }

    private void setPlanningClientToolbarVisible(boolean on) {
        if (planningClientToolbar != null) {
            planningClientToolbar.setVisible(on);
            planningClientToolbar.setManaged(on);
        }
    }

    @FXML
    private void handleOpenObjectifAssistant() {
        var ctx = CurrentSession.context();
        if (!ctx.isClientUser() || !ctx.hasDbUser()) {
            return;
        }
        dynamicContainer.getChildren().clear();
        PlanningObjectifController oc = new PlanningObjectifController(ctx.getUserId(), this::restoreClientPlanningDashboard);
        VBox root = oc.buildRoot();
        dynamicContainer.getChildren().add(root);
        VBox.setVgrow(root, Priority.ALWAYS);
        setPlanningClientToolbarVisible(false);
    }

    @FXML
    private void handleOpenTaskCalendar() {
        var ctx = CurrentSession.context();
        if (!ctx.isClientUser() || !ctx.hasDbUser()) {
            return;
        }
        dynamicContainer.getChildren().clear();
        PlanningCalendarController calendar = new PlanningCalendarController(ctx.getUserId(), this::restoreClientPlanningDashboard);
        VBox root = calendar.buildRoot();
        dynamicContainer.getChildren().add(root);
        VBox.setVgrow(root, Priority.ALWAYS);
        setPlanningClientToolbarVisible(false);
    }

    private void restoreClientPlanningDashboard() {
        buildClientView();
        var ctx = CurrentSession.context();
        setPlanningClientToolbarVisible(ctx.isClientUser() && ctx.hasDbUser());
    }

    private void applyRoleBadgeStyle(String variant) {
        roleBadge.getStyleClass().removeIf(c -> c.startsWith("planning-role-badge--"));
        if (variant != null && !variant.isBlank()) {
            roleBadge.getStyleClass().add("planning-role-badge--" + variant);
        }
    }

    @FXML
    private void handleOpenDiscussion() {
        CurrentSession.context().openDiscussionFromPlanning();
    }

    private void mountNotificationBell(SessionContext ctx) {
        if (notificationBellSlot == null) {
            return;
        }
        if (notificationBell != null) {
            notificationBell.dispose();
        }
        notificationBellSlot.getChildren().clear();
        if (!ctx.hasDbUser() || ctx.isAdmin()) {
            notificationBellSlot.setVisible(false);
            notificationBellSlot.setManaged(false);
            notificationBell = null;
            return;
        }
        notificationBellSlot.setVisible(true);
        notificationBellSlot.setManaged(true);
        notificationBell = new NotificationBell(
                ctx.getUserId(),
                notificationService,
                this::openConversationFromNotification);
        notificationBellSlot.getChildren().add(notificationBell);
    }

    private void installPlanningRealtimeListener() {
        if (planningSyncSubscriptionId != null) {
            RealtimePlanningSyncService.getInstance().unsubscribe(planningSyncSubscriptionId);
            planningSyncSubscriptionId = null;
        }
        planningSyncSubscriptionId = RealtimePlanningSyncService.getInstance()
                .subscribeForCurrentUser(this::onPlanningSyncEvent);
        if (planningPageStack != null) {
            planningPageStack.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null && planningSyncSubscriptionId != null) {
                    RealtimePlanningSyncService.getInstance().unsubscribe(planningSyncSubscriptionId);
                    planningSyncSubscriptionId = null;
                }
            });
        }
    }

    private void onPlanningSyncEvent(org.example.realtime.RealtimeEvent event) {
        long now = System.currentTimeMillis();
        if (now - lastPlanningRefreshMs < 500L) {
            return;
        }
        lastPlanningRefreshMs = now;
        var ctx = CurrentSession.context();
        if (ctx.isClientUser() && activeClientController != null) {
            try {
                activeClientController.refresh();
            } catch (Exception ignored) {
            }
        } else if (ctx.isEncadrant() && activeEncadrantHub != null) {
            try {
                activeEncadrantHub.refresh();
            } catch (Exception ignored) {
            }
        }
    }

    private void mountRealtimeStatusBadge() {
        if (realtimeStatusSlot == null) {
            return;
        }
        if (realtimeStatusBadge != null) {
            realtimeStatusBadge.dispose();
        }
        realtimeStatusSlot.getChildren().clear();
        realtimeStatusBadge = new RealtimeStatusBadge();
        realtimeStatusSlot.getChildren().add(realtimeStatusBadge);
    }

    private void openConversationFromNotification(NotificationService.UnreadNotification notification) {
        SessionContext ctx = CurrentSession.context();
        if (ctx.isEncadrant() && notification.otherUserId() > 0) {
            ctx.setPendingDiscussionClientUserId(notification.otherUserId());
        }
        ctx.openDiscussionFromPlanning();
    }

    private void buildClientView() {
        applyAdminPlanningDarkTheme(false);
        applyClientPlanningZen(true);
        var ctx = CurrentSession.context();
        if (!ctx.hasDbUser()) {
            VBox req = new VBox(PlanningUi.hintLabel(
                    "Connexion requise : connectez-vous avec un compte reconnu par l’application (e-mail enregistré)."));
            dynamicContainer.getChildren().add(PlanningUi.card("Compte requis", null, null, wrapGrow(req)));
            activeClientController = null;
            return;
        }
        activeClientController = new PlanningClientController(dynamicContainer, ctx.getUserId());
        activeClientController.refresh();
    }

    private void buildEncadrantView() {
        applyAdminPlanningDarkTheme(false);
        applyClientPlanningZen(true);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/pages/EncadrantPlanningHub.fxml"));
            Parent root = loader.load();
            EncadrantPlanningHubController c = loader.getController();
            encadrantHubController = c;
            activeEncadrantHub = c;
            if (mainLayoutController != null) {
                c.setMainLayoutController(mainLayoutController);
            }
            c.setup();
            dynamicContainer.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);
        } catch (Exception e) {
            encadrantHubController = null;
            activeEncadrantHub = null;
            Throwable t = e.getCause() != null ? e.getCause() : e;
            String msg = t.getMessage() != null ? t.getMessage() : t.toString();
            VBox err = new VBox(PlanningUi.hintLabel("Impossible de charger la vue encadrant : " + msg));
            dynamicContainer.getChildren().add(PlanningUi.card("Erreur", null, null, wrapGrow(err)));
        }
    }

    private void buildAdminView() {
        applyClientPlanningZen(false);
        applyAdminPlanningDarkTheme(true);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/pages/AdminPlanningDashboard.fxml"));
            Parent root = loader.load();
            AdminPlanningDashboardController c = loader.getController();
            c.setup();
            dynamicContainer.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);
        } catch (Exception e) {
            applyClientPlanningZen(false);
            applyAdminPlanningDarkTheme(true);
            Throwable t = e.getCause() != null ? e.getCause() : e;
            String msg = t.getMessage() != null ? t.getMessage() : t.toString();
            VBox err = new VBox(PlanningUi.hintLabel("Impossible de charger la vue admin Planning : " + msg));
            dynamicContainer.getChildren().add(PlanningUi.card("Erreur", null, null, wrapGrow(err)));
        }
    }

    private static final String ADMIN_PLANNING_SHELL = "planning-main-shell--admin-dark";
    private static final String ADMIN_PAGE_STACK = "planning-page-stack--admin-dark";
    private static final String ADMIN_PAGE_ROOT = "planning-page-root--admin-dark";
    private static final String ADMIN_PAGE_SCROLL = "planning-scroll--admin-dark";
    private static final String CLIENT_ZEN_ROOT = "planning-page-root--client-zen";
    private static final String CLIENT_ZEN_SHELL = "planning-main-shell--client-zen";
    private static final String CLIENT_ZEN_STACK = "planning-page-stack--client-zen";

    /** Aligne toute la vue Planning admin sur le shell sombre du Dashboard (.content-area). */
    private void applyAdminPlanningDarkTheme(boolean on) {
        toggleStyle(planningMainShell, ADMIN_PLANNING_SHELL, on);
        toggleStyle(planningPageStack, ADMIN_PAGE_STACK, on);
        toggleStyle(planningRoot, ADMIN_PAGE_ROOT, on);
        toggleStyle(planningPageScroll, ADMIN_PAGE_SCROLL, on);
    }

    /** Fond et coque clairs (#f4f6f9 + carte blanche), alignés sur le planning client — tous rôles. */
    private void applyClientPlanningZen(boolean on) {
        toggleStyle(planningPageStack, CLIENT_ZEN_STACK, on);
        toggleStyle(planningRoot, CLIENT_ZEN_ROOT, on);
        toggleStyle(planningMainShell, CLIENT_ZEN_SHELL, on);
    }

    private static void toggleStyle(Node node, String styleClass, boolean on) {
        if (node == null) {
            return;
        }
        if (on) {
            if (!node.getStyleClass().contains(styleClass)) {
                node.getStyleClass().add(styleClass);
            }
        } else {
            node.getStyleClass().remove(styleClass);
        }
    }

    private static Region wrapGrow(Region r) {
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    private static String initialsFromDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "?";
        }
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) {
            String p = parts[0];
            return p.length() <= 1 ? p.toUpperCase() : p.substring(0, Math.min(2, p.length())).toUpperCase();
        }
        String a = parts[0].substring(0, 1);
        String b = parts[parts.length - 1].substring(0, 1);
        return (a + b).toUpperCase();
    }
}
