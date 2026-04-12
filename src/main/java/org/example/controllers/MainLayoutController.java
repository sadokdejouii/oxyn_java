package org.example.controllers;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.services.SessionContext;
import org.example.utils.PageLoader;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class MainLayoutController implements Initializable {

    private static final String PAGE_ADMIN_DASH = "/FXML/pages/AdminDashboard.fxml";
    private static final String PAGE_PLACEHOLDER = "/FXML/pages/PlaceholderPage.fxml";
    private static final String PAGE_PLANNING = "/FXML/pages/PlanningPage.fxml";
    private static final String PAGE_DISCUSSION = "/FXML/pages/DiscussionPage.fxml";

    @FXML
    private BorderPane shellRoot;

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
    private Button adminEvenementsBtn;

    @FXML
    private Button adminSalleBtn;

    @FXML
    private Button adminPlanningBtn;

    @FXML
    private Button adminBoutiqueBtn;

    @FXML
    private Button adminForumBtn;

    @FXML
    private Button homeBtn;

    @FXML
    private Button evenementsBtn;

    @FXML
    private Button salleBtn;

    @FXML
    private Button planningBtn;

    @FXML
    private Button boutiqueBtn;

    @FXML
    private Button forumBtn;

    @FXML
    private Button profileBtn;

    private final List<Button> mainNavButtons = new ArrayList<>();

    private Button activeNavButton;

    private boolean sidebarCompact;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Runnable installIcons = () -> installIkonliFontIcons(shellRoot);
        installIcons.run();
        Platform.runLater(installIcons);

        SessionContext ctx = SessionContext.getInstance();
        topbarUserName.textProperty().bind(ctx.displayNameProperty());
        topbarUserRole.setText(ctx.getRole().displayLabel());
        userAvatarLabel.textProperty().bind(Bindings.createStringBinding(
                () -> initials(ctx.getDisplayName()),
                ctx.displayNameProperty()));

        mainNavButtons.clear();
        mainNavButtons.add(adminDashboardBtn);
        mainNavButtons.add(adminEvenementsBtn);
        mainNavButtons.add(adminSalleBtn);
        mainNavButtons.add(adminPlanningBtn);
        mainNavButtons.add(adminBoutiqueBtn);
        mainNavButtons.add(adminForumBtn);
        mainNavButtons.add(homeBtn);
        mainNavButtons.add(evenementsBtn);
        mainNavButtons.add(salleBtn);
        mainNavButtons.add(planningBtn);
        mainNavButtons.add(boutiqueBtn);
        mainNavButtons.add(forumBtn);
        mainNavButtons.add(profileBtn);

        applyRoleShell(ctx);
        wireSearch();
        registerDiscussionFromPlanningHook(ctx);

        if (ctx.isAdmin()) {
            navigate(PAGE_ADMIN_DASH, "Dashboard", adminDashboardBtn);
        } else if (ctx.isEncadrant()) {
            navigate(PAGE_PLANNING, "Planning", planningBtn);
        } else {
            navigate(PAGE_PLANNING, "Planning", planningBtn);
        }
    }

    private void applyRoleShell(SessionContext ctx) {
        boolean admin = ctx.isAdmin();
        adminNavGroup.setVisible(admin);
        adminNavGroup.setManaged(admin);
        clientNavGroup.setVisible(!admin);
        clientNavGroup.setManaged(!admin);
        shellModeLabel.setText(admin ? "ADMINISTRATOR" : ctx.isEncadrant() ? "ENCADRANT" : "CLIENT");
        topbarUserRole.setText(ctx.getRole().displayLabel());
        if (admin) {
            footerText.setText("Back office session");
        } else if (ctx.isEncadrant()) {
            footerText.setText("Encadrant — planning");
        } else {
            footerText.setText("Front office session");
        }
    }

    private void registerDiscussionFromPlanningHook(SessionContext ctx) {
        ctx.setOpenDiscussionFromPlanningAction(() -> {
            try {
                Button nav = ctx.isAdmin() ? adminPlanningBtn : planningBtn;
                PageLoader.show(contentArea, PAGE_DISCUSSION);
                topbarPageTitle.setText("Discussion");
                setActiveNav(nav);
            } catch (Exception e) {
                e.printStackTrace();
                if (footerText != null) {
                    footerText.setText("Page load failed");
                }
                info("Navigation error", e.getMessage() != null ? e.getMessage() : e.toString());
            }
        });
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

    private void navigatePlaceholder(String moduleTitle, Button navButton) {
        try {
            PlaceholderContext.setModuleLabel(moduleTitle);
            PageLoader.show(contentArea, PAGE_PLACEHOLDER);
            topbarPageTitle.setText(moduleTitle);
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

    @FXML
    private void handleBrandHome() {
        SessionContext ctx = SessionContext.getInstance();
        if (ctx.isAdmin()) {
            navigate(PAGE_ADMIN_DASH, "Dashboard", adminDashboardBtn);
        } else if (ctx.isEncadrant()) {
            navigate(PAGE_PLANNING, "Planning", planningBtn);
        } else {
            navigate(PAGE_PLANNING, "Planning", planningBtn);
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
    private void handleAdminEvenements() {
        navigatePlaceholder("Événements", adminEvenementsBtn);
    }

    @FXML
    private void handleAdminSalle() {
        navigatePlaceholder("Salle de sport", adminSalleBtn);
    }

    @FXML
    private void handleAdminPlanning() {
        navigate(PAGE_PLANNING, "Planning", adminPlanningBtn);
    }

    @FXML
    private void handleAdminBoutique() {
        navigatePlaceholder("Boutique", adminBoutiqueBtn);
    }

    @FXML
    private void handleAdminForum() {
        navigatePlaceholder("Forum", adminForumBtn);
    }

    @FXML
    private void handleHome() {
        navigate(PAGE_PLANNING, "Planning", homeBtn);
    }

    @FXML
    private void handleEvenements() {
        navigatePlaceholder("Événements", evenementsBtn);
    }

    @FXML
    private void handleSalle() {
        navigatePlaceholder("Salle de sport", salleBtn);
    }

    @FXML
    private void handlePlanning() {
        navigate(PAGE_PLANNING, "Planning", planningBtn);
    }

    @FXML
    private void handleBoutique() {
        navigatePlaceholder("Boutique", boutiqueBtn);
    }

    @FXML
    private void handleForum() {
        navigatePlaceholder("Forum", forumBtn);
    }

    @FXML
    private void handleProfile() {
        navigatePlaceholder("Profil", profileBtn);
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

    /** Remplace les {@code Region} Ikonli dans l’arbre (FXML / Scene Builder). */
    private static void installIkonliFontIcons(Parent root) {
        if (root == null) {
            return;
        }
        Deque<Node> queue = new ArrayDeque<>();
        Set<Node> seen = new HashSet<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            Node n = queue.removeFirst();
            if (!seen.add(n)) {
                continue;
            }
            if (n instanceof Region r && r.getStyleClass().contains("ikonli-slot")) {
                FontIcon icon = ikonliPlaceholderToFontIcon(r);
                if (icon != null && r.getParent() instanceof Pane pane) {
                    int i = pane.getChildren().indexOf(r);
                    if (i >= 0) {
                        pane.getChildren().set(i, icon);
                    }
                }
                continue;
            }
            if (n instanceof Labeled labeled && labeled.getGraphic() != null) {
                queue.addLast(labeled.getGraphic());
            }
            if (n instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
                queue.addLast(scrollPane.getContent());
            }
            if (n instanceof Parent p) {
                queue.addAll(p.getChildrenUnmodifiable());
            }
        }
    }

    private static FontIcon ikonliPlaceholderToFontIcon(Region r) {
        String literal = null;
        int size = 18;
        List<String> styles = new ArrayList<>();
        for (String c : r.getStyleClass()) {
            if ("ikonli-slot".equals(c)) {
                continue;
            }
            if (c.startsWith("fas-") || c.startsWith("far-") || c.startsWith("fab-")) {
                literal = c;
                continue;
            }
            if (c.startsWith("sz-")) {
                try {
                    size = Integer.parseInt(c.substring(3));
                } catch (NumberFormatException ignored) {
                }
                continue;
            }
            styles.add(c);
        }
        if (literal == null) {
            return null;
        }
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        icon.getStyleClass().addAll(styles);
        return icon;
    }
}
