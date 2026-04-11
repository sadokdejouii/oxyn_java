package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.model.planning.encadrant.EncadrantClientCardRow;
import org.example.services.EncadrantClientPlanningService;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.SQLException;
import java.util.List;

/**
 * Grille de clients avec fiche + vue détail inline (sans changement de page).
 */
public final class EncadrantPlanningHubController {

    @FXML
    private VBox listLayer;
    @FXML
    private VBox detailLayer;
    @FXML
    private FlowPane clientsGrid;
    @FXML
    private Button btnRefresh;
    @FXML
    private Button btnBack;
    @FXML
    private Label lblDetailTitle;
    @FXML
    private StackPane detailMount;

    private final EncadrantClientPlanningService service = new EncadrantClientPlanningService();

    public void setup() {
        btnRefresh.setOnAction(e -> loadGrid());
        btnBack.setOnAction(e -> showList());
        loadGrid();
    }

    private void loadGrid() {
        clientsGrid.getChildren().clear();
        try {
            List<EncadrantClientCardRow> rows = service.listActiveClientsWithFicheCards();
            if (rows.isEmpty()) {
                Label empty = new Label("Aucun client actif avec fiche santé. Créez une fiche côté client ou activez un compte ROLE_CLIENT.");
                empty.setWrapText(true);
                empty.getStyleClass().add("eph-empty");
                clientsGrid.getChildren().add(empty);
                return;
            }
            for (EncadrantClientCardRow row : rows) {
                clientsGrid.getChildren().add(buildClientCard(row));
            }
        } catch (SQLException ex) {
            Label err = new Label("Erreur chargement : " + (ex.getMessage() != null ? ex.getMessage() : ex));
            err.setWrapText(true);
            err.getStyleClass().add("eph-empty");
            clientsGrid.getChildren().add(err);
        }
    }

    private VBox buildClientCard(EncadrantClientCardRow row) {
        VBox card = new VBox(14);
        card.getStyleClass().add("eph-client-card");
        card.setOnMouseClicked(e -> openDetail(row));

        HBox head = new HBox(14);
        head.setAlignment(Pos.CENTER_LEFT);
        StackPane av = new StackPane();
        av.getStyleClass().add("eph-card-avatar");
        av.setMinSize(52, 52);
        av.setMaxSize(52, 52);
        Label avL = new Label(initials(row.displayName()));
        avL.getStyleClass().add("eph-card-avatar-text");
        av.getChildren().add(avL);

        VBox who = new VBox(4);
        HBox.setHgrow(who, Priority.ALWAYS);
        Label nm = new Label(row.displayName());
        nm.getStyleClass().add("eph-card-name");
        nm.setWrapText(true);
        Label em = new Label(row.email() != null && !row.email().isBlank() ? row.email() : "—");
        em.getStyleClass().add("eph-card-email");
        em.setWrapText(true);
        who.getChildren().addAll(nm, em);
        head.getChildren().addAll(av, who);

        Label badge = new Label(row.statusBadge());
        badge.getStyleClass().add("eph-card-badge");

        Label obj = new Label("Objectif : " + dash(row.objectif()));
        obj.getStyleClass().add("eph-card-line");
        obj.setWrapText(true);
        Label act = new Label("Activité : " + dash(row.niveauActivite()));
        act.getStyleClass().add("eph-card-line");
        act.setWrapText(true);

        ProgressBar bar = new ProgressBar();
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("eph-card-progress");
        double p = row.progressPct();
        if (p < 0) {
            bar.setProgress(0);
            bar.getStyleClass().add("eph-card-progress--na");
        } else {
            bar.setProgress(p / 100.0);
            bar.getStyleClass().add("eph-card-progress--ok");
        }
        Label pct = new Label(p < 0 ? "Progression : —" : String.format("Progression : %.0f %%", p));
        pct.getStyleClass().add("eph-card-pct");

        HBox foot = new HBox(8);
        foot.setAlignment(Pos.CENTER_RIGHT);
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        FontIcon chev = new FontIcon();
        chev.setIconLiteral("fas-chevron-right");
        chev.setIconSize(14);
        chev.getStyleClass().add("eph-card-chevron");
        foot.getChildren().addAll(grow, chev);

        card.getChildren().addAll(head, badge, obj, act, pct, bar, foot);
        return card;
    }

    private void openDetail(EncadrantClientCardRow row) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/pages/EncadrantPlanningDashboard.fxml"));
            Parent root = loader.load();
            EncadrantPlanningDashboardController c = loader.getController();
            c.setup(row.userId());
            detailMount.getChildren().setAll(root);
            lblDetailTitle.setText(row.displayName());
            listLayer.setVisible(false);
            listLayer.setManaged(false);
            detailLayer.setVisible(true);
            detailLayer.setManaged(true);
        } catch (Exception ex) {
            Throwable t = ex.getCause() != null ? ex.getCause() : ex;
            Label err = new Label("Impossible d’ouvrir le détail : " + (t.getMessage() != null ? t.getMessage() : t));
            err.setWrapText(true);
            err.getStyleClass().add("eph-empty");
            detailMount.getChildren().setAll(err);
            lblDetailTitle.setText("Erreur");
            listLayer.setVisible(false);
            listLayer.setManaged(false);
            detailLayer.setVisible(true);
            detailLayer.setManaged(true);
        }
    }

    private void showList() {
        detailMount.getChildren().clear();
        detailLayer.setVisible(false);
        detailLayer.setManaged(false);
        listLayer.setVisible(true);
        listLayer.setManaged(true);
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

    private static String dash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
