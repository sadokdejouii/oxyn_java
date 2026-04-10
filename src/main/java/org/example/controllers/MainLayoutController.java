package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.services.SessionContext;
import org.example.utils.PageLoader;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MainLayoutController implements Initializable {

    private static final String PAGE_ADMIN_DASH = "/FXML/pages/AdminDashboard.fxml";
    private static final String PAGE_STOCK = "/FXML/pages/StockManagement.fxml";
    private static final String PAGE_EVENTS_ADMIN = "/FXML/pages/EventManagement.fxml";
    private static final String PAGE_USERS = "/FXML/pages/UserManagement.fxml";
    private static final String PAGE_REPORTS = "/FXML/pages/ReportsStatistics.fxml";

    private static final String PAGE_CLIENT_HOME = "/FXML/pages/ClientHome.fxml";
    private static final String PAGE_CLIENT_EVENTS = "/FXML/pages/ClientEvents.fxml";
    private static final String PAGE_PLANNING = "/FXML/pages/PlanningPage.fxml";
    private static final String PAGE_PROFILE = "/FXML/pages/ProfilePage.fxml";

    @FXML
    private VBox sidebar;

    @FXML
    private Button brandHomeButton;

    @FXML
    private VBox brandTextColumn;

    @FXML
    private Button toggleButton;

    @FXML
    private Label toggleText;

    @FXML
    private Label shellModeLabel;

    @FXML
    private VBox adminNavGroup;

    @FXML
    private VBox clientNavGroup;

    @FXML
    private VBox sidebarActionsBox;

    @FXML
    private Label footerText;

    @FXML
    private StackPane contentArea;

    @FXML
    private Label topbarPageTitle;

    @FXML
    private TextField topbarSearchField;

    @FXML
    private Label topbarUserName;

    @FXML
    private Label topbarUserRole;

    @FXML
    private Label userAvatarLabel;

    @FXML
    private Button adminDashboardBtn;

    @FXML
    private Button stockMgmtBtn;

    @FXML
    private Button eventMgmtBtn;

    @FXML
    private Button userMgmtBtn;

    @FXML
    private Button reportsBtn;

    @FXML
    private Button clientHomeBtn;

    @FXML
    private Button clientEventsBtn;

    @FXML
    private Button clientPlanningBtn;

    @FXML
    private Button profileBtn;

    private final List<Button> mainNavButtons = new ArrayList<>();

    private Button activeNavButton;

    private boolean sidebarCompact;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SessionContext ctx = SessionContext.getInstance();
        topbarUserName.setText(ctx.getDisplayName());
        topbarUserRole.setText(ctx.getRole().displayLabel());
        userAvatarLabel.setText(initials(ctx.getDisplayName()));

        mainNavButtons.clear();
        mainNavButtons.add(adminDashboardBtn);
        mainNavButtons.add(stockMgmtBtn);
        mainNavButtons.add(eventMgmtBtn);
        mainNavButtons.add(userMgmtBtn);
        mainNavButtons.add(reportsBtn);
        mainNavButtons.add(clientHomeBtn);
        mainNavButtons.add(clientEventsBtn);
        mainNavButtons.add(clientPlanningBtn);
        mainNavButtons.add(profileBtn);

        applyRoleShell(ctx);
        wireSearch();
        if (ctx.isAdmin()) {
            navigate(PAGE_ADMIN_DASH, "Dashboard", adminDashboardBtn);
        } else {
            navigate(PAGE_CLIENT_HOME, "Home", clientHomeBtn);
        }
    }

    private void applyRoleShell(SessionContext ctx) {
        boolean admin = ctx.isAdmin();
        adminNavGroup.setVisible(admin);
        adminNavGroup.setManaged(admin);
        clientNavGroup.setVisible(!admin);
        clientNavGroup.setManaged(!admin);
        shellModeLabel.setText(admin ? "ADMINISTRATOR" : "CLIENT");
        topbarUserRole.setText(ctx.getRole().displayLabel());
        footerText.setText(admin ? "Back office session" : "Front office session");
    }

    private void wireSearch() {
        if (topbarSearchField == null) {
            return;
        }
        topbarSearchField.setOnAction(e -> {
            String q = topbarSearchField.getText() == null ? "" : topbarSearchField.getText().trim();
            info("Search", q.isEmpty() ? "Type a query to search (UI hook)." : "Searching for: " + q);
        });
    }

    private void navigate(String classpath, String title, Button navButton) {
        try {
            PageLoader.show(contentArea, classpath);
            topbarPageTitle.setText(title);
            setActiveNav(navButton);
        } catch (Exception e) {
            e.printStackTrace();
            if (footerText != null) {
                footerText.setText("Page load failed");
            }
            info("Navigation error", e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private void setActiveNav(Button button) {
        for (Button b : mainNavButtons) {
            if (b != null) {
                b.getStyleClass().remove("active");
            }
        }
        activeNavButton = button;
        if (button != null && !button.getStyleClass().contains("active")) {
            button.getStyleClass().add("active");
        }
    }

    @FXML
    private void handleToggleMenu() {
        sidebarCompact = !sidebarCompact;
        double w = sidebarCompact ? 88 : 272;
        sidebar.setPrefWidth(w);
        sidebar.setMinWidth(w);
        sidebar.setMaxWidth(sidebarCompact ? 96 : 288);
        if (toggleText != null) {
            toggleText.setText(sidebarCompact ? "Expand" : "Collapse sidebar");
        }
        applyNavTextVisible(adminNavGroup, !sidebarCompact);
        applyNavTextVisible(clientNavGroup, !sidebarCompact);
        applyNavTextVisible(sidebarActionsBox, !sidebarCompact);
        applySectionTitlesVisible(!sidebarCompact);
        if (brandTextColumn != null) {
            brandTextColumn.setVisible(!sidebarCompact);
            brandTextColumn.setManaged(!sidebarCompact);
        }
    }

    /**
     * Logo / brand row: return to the default dashboard for the current role.
     */
    @FXML
    private void handleBrandHome() {
        SessionContext ctx = SessionContext.getInstance();
        if (ctx.isAdmin()) {
            navigate(PAGE_ADMIN_DASH, "Dashboard", adminDashboardBtn);
        } else {
            navigate(PAGE_CLIENT_HOME, "Home", clientHomeBtn);
        }
    }

    private static void applyNavTextVisible(VBox group, boolean showLabels) {
        if (group == null) {
            return;
        }
        for (Node n : group.getChildren()) {
            if (n instanceof Button b && b.getGraphic() instanceof HBox h) {
                if (h.getChildren().size() > 1) {
                    Node labelNode = h.getChildren().get(1);
                    labelNode.setVisible(showLabels);
                    labelNode.setManaged(showLabels);
                }
            }
        }
    }

    private void applySectionTitlesVisible(boolean show) {
        if (adminNavGroup != null) {
            for (Node n : adminNavGroup.getChildren()) {
                if (n instanceof Label) {
                    n.setVisible(show);
                    n.setManaged(show);
                }
            }
        }
        if (clientNavGroup != null) {
            for (Node n : clientNavGroup.getChildren()) {
                if (n instanceof Label) {
                    n.setVisible(show);
                    n.setManaged(show);
                }
            }
        }
        if (shellModeLabel != null) {
            shellModeLabel.setVisible(show);
            shellModeLabel.setManaged(show);
        }
    }

    @FXML
    private void handleNotifications() {
        info("Notifications", "You have no unread notifications (demo).");
    }

    @FXML
    private void handleAdminDashboard() {
        navigate(PAGE_ADMIN_DASH, "Dashboard", adminDashboardBtn);
    }

    @FXML
    private void handleStockManagement() {
        navigate(PAGE_STOCK, "Stock management", stockMgmtBtn);
    }

    @FXML
    private void handleEventManagement() {
        navigate(PAGE_EVENTS_ADMIN, "Event management", eventMgmtBtn);
    }

    @FXML
    private void handleUserManagement() {
        navigate(PAGE_USERS, "User management", userMgmtBtn);
    }

    @FXML
    private void handleReports() {
        navigate(PAGE_REPORTS, "Reports / statistics", reportsBtn);
    }

    @FXML
    private void handleClientHome() {
        navigate(PAGE_CLIENT_HOME, "Home", clientHomeBtn);
    }

    @FXML
    private void handleClientEvents() {
        navigate(PAGE_CLIENT_EVENTS, "Events", clientEventsBtn);
    }

    @FXML
    private void handlePlanning() {
        navigate(PAGE_PLANNING, "Planning", clientPlanningBtn);
    }

    @FXML
    private void handleProfile() {
        navigate(PAGE_PROFILE, "Profile", profileBtn);
    }

    @FXML
    private void handleLogout() {
        try {
            SessionContext.getInstance().logout();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/Login.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root, 1080, 720));
            stage.setTitle("OXYN — Sign in");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            Platform.exit();
        }
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private static void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
