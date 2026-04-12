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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.stage.Stage;
import org.example.services.SessionContext;
import org.example.utils.PageLoader;

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
    private static final String PAGE_STOCK = "/FXML/pages/AdminSalleHub.fxml";
    private static final String PAGE_EVENTS_ADMIN = "/FXML/pages/EventManagement.fxml";
    private static final String PAGE_USERS = "/FXML/pages/UserManagement.fxml";
    private static final String PAGE_REPORTS = "/FXML/pages/ReportsStatistics.fxml";
    private static final String PAGE_ADMIN_COMMANDES = "/FXML/pages/AdminCommandes.fxml";
    private static final String PAGE_BOUTIQUE = "/FXML/pages/BoutiquePage.fxml";
    private static final String PAGE_AJOUTER_PRODUIT = "/FXML/pages/AjouterProduitPage.fxml";
    private static final String PAGE_MODIFIER_PRODUIT = "/FXML/pages/ModifierProduitPage.fxml";

    private static final String PAGE_CLIENT_HOME   = "/FXML/pages/ClientHome.fxml";
    private static final String PAGE_CLIENT_SALLE  = "/FXML/pages/ClientSalleList.fxml";
    private static final String PAGE_CLIENT_EVENTS = "/FXML/pages/ClientEvents.fxml";
    private static final String PAGE_PLANNING = "/FXML/pages/PlanningPage.fxml";
    private static final String PAGE_CLIENT_BOUTIQUE = "/FXML/pages/ClientBoutique.fxml";
    private static final String PAGE_PROFILE = "/FXML/pages/ProfilePage.fxml";
    private static final String PAGE_FORUM = "/FXML/pages/Forum.fxml";
    private static final String PAGE_FORUM_BACKOFFICE = "/FXML/pages/ForumBackoffice.fxml";

    private static final String PAGE_ENC_HOME = "/FXML/pages/EncadrantHome.fxml";
    private static final String PAGE_ENC_GROUPE = "/FXML/pages/EncadrantGroupe.fxml";
    private static final String PAGE_ENC_PLANNING = "/FXML/pages/EncadrantPlanning.fxml";
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
    private VBox encadrantNavGroup;

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
    private Button adminUsersBtn;
  
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

    @FXML
    private Button encDashboardBtn;

    @FXML
    private Button encEvenementsBtn;

    @FXML
    private Button encGroupeBtn;

    @FXML
    private Button encSalleBtn;

    @FXML
    private Button encBoutiqueBtn;

    @FXML
    private Button encForumBtn;

    private final List<Button> mainNavButtons = new ArrayList<>();

    private Button activeNavButton;

    private boolean sidebarCompact;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Runnable installIcons = () -> installIkonliFontIcons(shellRoot);
        installIcons.run();
        // After skins attach button graphics / scroll content, placeholders may only appear on this pass.
        Platform.runLater(installIcons);

        SessionContext ctx = SessionContext.getInstance();
        topbarUserName.textProperty().bind(ctx.displayNameProperty());
        topbarUserRole.setText(ctx.getRole().displayLabel());
        userAvatarLabel.textProperty().bind(Bindings.createStringBinding(
                () -> initials(ctx.getDisplayName()),
                ctx.displayNameProperty()));

        mainNavButtons.clear();
        mainNavButtons.add(adminUsersBtn);
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
        mainNavButtons.add(encDashboardBtn);
        mainNavButtons.add(encEvenementsBtn);
        mainNavButtons.add(encGroupeBtn);
        mainNavButtons.add(encSalleBtn);
        mainNavButtons.add(encSalleBtn);
        mainNavButtons.add(encBoutiqueBtn);
        mainNavButtons.add(encForumBtn);

        applyRoleShell(ctx);
        wireSearch();
        if (ctx.isAdmin()) {
            navigate(PAGE_ADMIN_DASH, "Dashboard", adminDashboardBtn);
        } else if (ctx.isEncadrant()) {
            navigate(PAGE_ENC_PLANNING, "Sessions", encSalleBtn);
            navigate(PAGE_CLIENT_HOME, "Accueil encadrant", homeBtn);
        } else {
            navigate(PAGE_CLIENT_HOME, "Home", homeBtn);
        }
    }

    private void applyRoleShell(SessionContext ctx) {
        boolean admin = ctx.isAdmin();
        boolean encadrant = ctx.isEncadrant();
        boolean client = !admin && !encadrant;

        adminNavGroup.setVisible(admin);
        adminNavGroup.setManaged(admin);
        encadrantNavGroup.setVisible(encadrant);
        encadrantNavGroup.setManaged(encadrant);
        clientNavGroup.setVisible(client);
        clientNavGroup.setManaged(client);

        String modeLabel = admin ? "ADMINISTRATOR" : encadrant ? "ENCADRANT" : "CLIENT";
        shellModeLabel.setText(modeLabel);
        topbarUserRole.setText(ctx.getRole().displayLabel());
        footerText.setText(admin ? "Back office session" : encadrant ? "Encadrant session" : "Front office session");
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

    public void navigate(String classpath, String title, Button navButton) {
        try {
            PageLoader.show(contentArea, classpath, this);
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
        applyNavTextVisible(encadrantNavGroup, !sidebarCompact);
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
        } else if (ctx.isEncadrant()) {
            navigate(PAGE_ENC_PLANNING, "Sessions", encSalleBtn);
            navigate(PAGE_CLIENT_HOME, "Accueil encadrant", homeBtn);
        } else {
            navigate(PAGE_CLIENT_HOME, "Home", homeBtn);
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
                if (n instanceof Label) { n.setVisible(show); n.setManaged(show); }
            }
        }
        if (encadrantNavGroup != null) {
            for (Node n : encadrantNavGroup.getChildren()) {
                if (n instanceof Label) { n.setVisible(show); n.setManaged(show); }
            }
        }
        if (clientNavGroup != null) {
            for (Node n : clientNavGroup.getChildren()) {
                if (n instanceof Label) { n.setVisible(show); n.setManaged(show); }
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
        navigate(PAGE_EVENTS_ADMIN, "Evenements", adminEvenementsBtn);
    }

    @FXML
    private void handleAdminSalle() {
        navigate(PAGE_STOCK, "Salle", adminSalleBtn);
    }

    @FXML
    private void handleAdminPlanning() {
        navigate(PAGE_PLANNING, "Planning", adminPlanningBtn);
    }

    @FXML
    private void handleAdminBoutique() {
        navigate(PAGE_BOUTIQUE, "Boutique", adminBoutiqueBtn);
    }

    /**
     * Ouvert depuis la page Boutique : garde l’entrée « Boutique » active dans la barre latérale.
     */
    public void navigateToAdminCommandes() {
        navigate(PAGE_ADMIN_COMMANDES, "Commandes", adminBoutiqueBtn);
    }

    @FXML
    private void handleAdminUsers() {
        navigate(PAGE_USERS, "Utilisateurs", adminUsersBtn);
    }

    @FXML
    private void handleAdminForum() {
        navigate(PAGE_FORUM_BACKOFFICE, "Forum Management", adminForumBtn);
    }

    @FXML
    private void handleHome() {
        navigate(PAGE_CLIENT_HOME, "Home", homeBtn);
    }

    @FXML
    private void handleEvenements() {
        navigate(PAGE_CLIENT_EVENTS, "Evenements", evenementsBtn);
    }

    @FXML
    private void handleSalle() {
        navigate(PAGE_CLIENT_SALLE, "Salles", salleBtn);
    }

    @FXML
    private void handlePlanning() {
        navigate(PAGE_PLANNING, "Planning", planningBtn);
    }

    @FXML
    private void handleBoutique() {
        navigate(PAGE_CLIENT_BOUTIQUE, "Boutique", boutiqueBtn);
    }

    @FXML
    private void handleForum() {
        navigate(PAGE_FORUM, "Forum", forumBtn);
    }

    @FXML
    private void handleProfile() {
        navigate(PAGE_PROFILE, "Profile", profileBtn);
    }

    @FXML
    private void handleEncDashboard() {
        navigate(PAGE_ENC_HOME, "Dashboard", encDashboardBtn);
    }

    @FXML
    private void handleEncEvenements() {
        navigate(PAGE_CLIENT_EVENTS, "Evenements", encEvenementsBtn);
    }

    @FXML
    private void handleEncSalle() {
        navigate(PAGE_ENC_PLANNING, "Sessions", encSalleBtn);
    }

    @FXML
    private void handleEncPlanning() {
        navigate(PAGE_ENC_PLANNING, "Sessions", encSalleBtn);
    }

    @FXML
    private void handleEncBoutique() {
        navigate(PAGE_PLANNING, "Boutique", encBoutiqueBtn);
    }

    @FXML
    private void handleEncForum() {
        navigate(PAGE_PLANNING, "Forum", encForumBtn);
    }

    @FXML
    private void handleEncGroupe() {
        navigate(PAGE_ENC_GROUPE, "Mes Groupes", encGroupeBtn);
    }

    @FXML
    private void handleLogout() {
        try {
            SessionContext.getInstance().logout();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/Login.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root, 1080, 720));
            stage.setTitle("OXYN — Connexion");
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

    /**
     * FXML uses {@code Region} placeholders so Scene Builder can open the file without Ikonli on its classpath.
     * This replaces them with real {@link FontIcon} nodes at runtime.
     * <p>
     * Traversal must follow {@link Labeled#getGraphic()} and {@link ScrollPane#getContent()}, not only
     * {@link Parent#getChildrenUnmodifiable()}, or icons inside buttons / scroll areas are never reached.
     */
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