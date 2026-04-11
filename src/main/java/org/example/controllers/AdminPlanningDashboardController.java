package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.example.model.planning.admin.AdminPlanningModuleStats;
import org.example.model.planning.admin.AdminPlanningUserRow;
import org.example.services.AdminPlanningDashboardService;

import java.sql.SQLException;
import java.util.List;

/**
 * Vue admin Planning — KPI + grille de cartes utilisateurs (sans tableau).
 */
public final class AdminPlanningDashboardController {

    @FXML
    private Label lblStatUsers;
    @FXML
    private Label lblStatFiches;
    @FXML
    private Label lblStatProg;
    @FXML
    private Label lblStatTasks;
    @FXML
    private Label lblStatConv;
    @FXML
    private Label lblStatMsg;
    @FXML
    private FlowPane userCardsHost;
    @FXML
    private Button btnRefresh;
    @FXML
    private StackPane detailOverlay;
    @FXML
    private Region detailDim;
    @FXML
    private Label detailAvatarLetter;
    @FXML
    private Label detailName;
    @FXML
    private Label detailEmail;
    @FXML
    private Label detailRole;
    @FXML
    private Label detailFiche;
    @FXML
    private Label detailProg;
    @FXML
    private Label detailTaches;
    @FXML
    private Label detailExtra;
    @FXML
    private Button btnDetailClose;

    private final AdminPlanningDashboardService service = new AdminPlanningDashboardService();
    private boolean overlayWired;

    public void setup() {
        wireDetailOverlay();
        btnRefresh.setOnAction(e -> loadAll());
        loadAll();
    }

    private void wireDetailOverlay() {
        if (overlayWired) {
            return;
        }
        overlayWired = true;
        btnDetailClose.setOnAction(e -> hideDetail());
        detailDim.setOnMouseClicked(e -> hideDetail());
    }

    private void showDetail(AdminPlanningUserRow row) {
        detailName.setText(row.displayName());
        detailEmail.setText(row.email() != null ? row.email() : "—");
        detailRole.setText("Rôle : " + row.roleSummary());
        detailFiche.setText("Fiche santé : genre " + dash(row.genre())
                + " · âge " + (row.age() != null ? row.age() + " ans" : "—")
                + " · poids " + (row.poids() != null ? row.poids() + " kg" : "—"));
        detailProg.setText("Programme généré : " + (row.programmeGenere() ? "Oui" : "Non"));
        detailTaches.setText("Tâches enregistrées (total) : " + row.nbTaches());
        detailExtra.setText("Objectif : " + dash(row.objectif()) + "\nActivité : " + dash(row.niveauActivite()));
        detailAvatarLetter.setText(initials(row.displayName()));
        detailOverlay.setManaged(true);
        detailOverlay.setVisible(true);
        detailOverlay.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(180), detailOverlay);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void hideDetail() {
        FadeTransition ft = new FadeTransition(Duration.millis(140), detailOverlay);
        ft.setFromValue(detailOverlay.getOpacity());
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            detailOverlay.setVisible(false);
            detailOverlay.setManaged(false);
        });
        ft.play();
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

    private void loadAll() {
        try {
            AdminPlanningModuleStats s = service.loadModuleStats();
            lblStatFiches.setText(String.valueOf(s.fichesSante()));
            lblStatProg.setText(String.valueOf(s.programmesGeneres()));
            lblStatTasks.setText(String.valueOf(s.tachesQuotidiennesTotal()));
            lblStatConv.setText(String.valueOf(s.conversationsSuivi()));
            lblStatMsg.setText(String.valueOf(s.messagesSuivi()));

            List<AdminPlanningUserRow> rows = service.listUsersWithPlanningActivity();
            lblStatUsers.setText(String.valueOf(rows.size()));
            rebuildUserCards(rows);
        } catch (SQLException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Planning admin");
            a.setHeaderText(null);
            a.setContentText(ex.getMessage() != null ? ex.getMessage() : ex.toString());
            a.showAndWait();
        }
    }

    private void rebuildUserCards(List<AdminPlanningUserRow> rows) {
        userCardsHost.getChildren().clear();
        for (AdminPlanningUserRow row : rows) {
            userCardsHost.getChildren().add(buildUserCard(row));
        }
    }

    private VBox buildUserCard(AdminPlanningUserRow row) {
        VBox card = new VBox(12);
        card.getStyleClass().add("apd-user-card");
        card.setOnMouseClicked(e -> showDetail(row));

        HBox head = new HBox(12);
        head.setAlignment(Pos.CENTER_LEFT);
        StackPane av = new StackPane();
        av.getStyleClass().add("apd-user-card-avatar");
        av.setMinSize(44, 44);
        av.setMaxSize(44, 44);
        Label avL = new Label(initials(row.displayName()));
        avL.getStyleClass().add("apd-user-card-avatar-text");
        av.getChildren().add(avL);

        VBox titles = new VBox(4);
        HBox.setHgrow(titles, Priority.ALWAYS);
        Label nm = new Label(row.displayName());
        nm.getStyleClass().add("apd-user-card-name");
        nm.setWrapText(true);
        Label em = new Label(row.email() != null && !row.email().isBlank() ? row.email() : "—");
        em.getStyleClass().add("apd-user-card-email");
        em.setWrapText(true);
        titles.getChildren().addAll(nm, em);
        head.getChildren().addAll(av, titles);

        Label badge = new Label(row.roleSummary());
        badge.getStyleClass().add("apd-user-card-badge");

        Label st = new Label("Genre " + dash(row.genre())
                + " · " + (row.age() != null ? row.age() + " ans" : "— ans")
                + " · " + (row.poids() != null ? row.poids() + " kg" : "— kg"));
        st.setWrapText(true);
        st.getStyleClass().add("apd-user-card-stats");

        Label st2 = new Label("Objectif : " + dash(row.objectif()));
        st2.setWrapText(true);
        st2.getStyleClass().add("apd-user-card-stats");

        Label st3 = new Label("Activité : " + dash(row.niveauActivite())
                + " · Programme : " + (row.programmeGenere() ? "oui" : "non")
                + " · Tâches : " + row.nbTaches());
        st3.setWrapText(true);
        st3.getStyleClass().add("apd-user-card-stats");

        FontIcon chev = new FontIcon();
        chev.setIconLiteral("fas-chevron-right");
        chev.setIconSize(12);
        chev.getStyleClass().add("apd-user-card-chevron");

        HBox foot = new HBox(8);
        foot.setAlignment(Pos.CENTER_RIGHT);
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        foot.getChildren().addAll(grow, chev);

        card.getChildren().addAll(head, badge, st, st2, st3, foot);
        return card;
    }

    private static String dash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

}
