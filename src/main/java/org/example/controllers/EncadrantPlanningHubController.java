package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.example.model.planning.encadrant.EncadrantClientCardRow;
import org.example.services.EncadrantClientPlanningService;
import org.example.services.SessionContext;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<EncadrantHubSort> sortCombo;
    @FXML
    private Label lblMatchHint;

    private final EncadrantClientPlanningService service = new EncadrantClientPlanningService();
    private final List<EncadrantClientCardRow> loadedRows = new ArrayList<>();

    private MainLayoutController mainLayoutController;

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    public void setup() {
        btnRefresh.setOnAction(e -> loadGrid());
        btnBack.setOnAction(e -> showList());
        wireSearchAndSort();
        loadGrid();
    }

    /**
     * Rafraîchissement public — déclenché par le bus temps réel
     * ({@code RealtimePlanningSyncService}) quand un évènement planning d'un client
     * arrive (intervention, objectif IA, tâche cochée). Recharge la grille sans
     * réinitialiser la recherche/tri courants.
     */
    public void refresh() {
        loadGrid();
    }

    private void wireSearchAndSort() {
        sortCombo.setItems(FXCollections.observableArrayList(EncadrantHubSort.values()));
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(EncadrantHubSort object) {
                return object == null ? "" : object.label;
            }

            @Override
            public EncadrantHubSort fromString(String string) {
                return null;
            }
        });
        searchField.textProperty().addListener((o, a, b) -> applySearchSortAndRender());
        sortCombo.valueProperty().addListener((o, a, b) -> applySearchSortAndRender());
    }

    private void loadGrid() {
        loadedRows.clear();
        try {
            loadedRows.addAll(service.listActiveClientsWithFicheCards());
        } catch (SQLException ex) {
            clientsGrid.getChildren().clear();
            Label err = new Label("Erreur chargement : " + (ex.getMessage() != null ? ex.getMessage() : ex));
            err.setWrapText(true);
            err.getStyleClass().add("eph-empty");
            clientsGrid.getChildren().add(err);
            updateMatchHint(0, 0);
            return;
        }
        applySearchSortAndRender();
    }

    private void applySearchSortAndRender() {
        String raw = searchField.getText() == null ? "" : searchField.getText().trim();
        String q = raw.toLowerCase(Locale.ROOT);
        EncadrantHubSort sort = sortCombo.getValue() != null ? sortCombo.getValue() : EncadrantHubSort.NAME_ASC;

        List<EncadrantClientCardRow> filtered = new ArrayList<>();
        for (EncadrantClientCardRow r : loadedRows) {
            if (matchesSearch(r, q)) {
                filtered.add(r);
            }
        }
        filtered.sort(sort.comparator);

        clientsGrid.getChildren().clear();
        int n = loadedRows.size();
        int m = filtered.size();
        updateMatchHint(n, m);

        if (n == 0) {
            Label empty = new Label("Aucun client actif avec fiche santé. Créez une fiche côté client ou activez un compte ROLE_CLIENT.");
            empty.setWrapText(true);
            empty.getStyleClass().add("eph-empty");
            clientsGrid.getChildren().add(empty);
            return;
        }
        if (m == 0) {
            Label none = new Label("Aucun client ne correspond à votre recherche. Essayez d’autres mots-clés.");
            none.setWrapText(true);
            none.getStyleClass().add("eph-empty");
            clientsGrid.getChildren().add(none);
            return;
        }
        for (EncadrantClientCardRow row : filtered) {
            clientsGrid.getChildren().add(buildClientCard(row));
        }
    }

    private void updateMatchHint(int total, int shown) {
        if (lblMatchHint == null) {
            return;
        }
        if (total == 0) {
            lblMatchHint.setText("");
            return;
        }
        if (shown == total) {
            lblMatchHint.setText(shown == 1 ? "1 client" : shown + " clients");
        } else {
            lblMatchHint.setText(shown + " sur " + total + " affichés");
        }
    }

    private static boolean matchesSearch(EncadrantClientCardRow r, String q) {
        if (q.isEmpty()) {
            return true;
        }
        return contains(r.displayName(), q)
                || contains(r.email(), q)
                || contains(r.objectif(), q)
                || contains(r.niveauActivite(), q)
                || contains(r.statusBadge(), q)
                || contains(r.objectifLibreExcerpt(), q)
                || contains(r.reponseIaLibreExcerpt(), q);
    }

    private static boolean contains(String field, String q) {
        if (field == null) {
            return false;
        }
        return field.toLowerCase(Locale.ROOT).contains(q);
    }

    private enum EncadrantHubSort {
        NAME_ASC("Nom (A → Z)", Comparator
                .comparing(EncadrantClientCardRow::displayName, String.CASE_INSENSITIVE_ORDER)),
        NAME_DESC("Nom (Z → A)", Comparator
                .comparing(EncadrantClientCardRow::displayName, String.CASE_INSENSITIVE_ORDER)
                .reversed()),
        PROGRESS_DESC("Progression (élevée d’abord)", EncadrantPlanningHubController::compareProgressDesc),
        PROGRESS_ASC("Progression (faible d’abord)", EncadrantPlanningHubController::compareProgressAsc),
        TASKS_DESC("Tâches semaine (↓)", Comparator
                .comparingInt(EncadrantClientCardRow::tasksTotalWeek).reversed()
                .thenComparing(EncadrantClientCardRow::displayName, String.CASE_INSENSITIVE_ORDER));

        private final String label;
        private final Comparator<EncadrantClientCardRow> comparator;

        EncadrantHubSort(String label, Comparator<EncadrantClientCardRow> comparator) {
            this.label = label;
            this.comparator = comparator;
        }
    }

    private static int compareProgressDesc(EncadrantClientCardRow a, EncadrantClientCardRow b) {
        return compareProgress(a, b, true);
    }

    private static int compareProgressAsc(EncadrantClientCardRow a, EncadrantClientCardRow b) {
        return compareProgress(a, b, false);
    }

    private static int compareProgress(EncadrantClientCardRow a, EncadrantClientCardRow b, boolean highFirst) {
        double pa = a.progressPct();
        double pb = b.progressPct();
        boolean na = pa < 0;
        boolean nb = pb < 0;
        if (na && nb) {
            return String.CASE_INSENSITIVE_ORDER.compare(
                    a.displayName() != null ? a.displayName() : "",
                    b.displayName() != null ? b.displayName() : "");
        }
        if (na) {
            return 1;
        }
        if (nb) {
            return -1;
        }
        int c = highFirst ? Double.compare(pb, pa) : Double.compare(pa, pb);
        if (c != 0) {
            return c;
        }
        return String.CASE_INSENSITIVE_ORDER.compare(
                a.displayName() != null ? a.displayName() : "",
                b.displayName() != null ? b.displayName() : "");
    }

    private VBox buildClientCard(EncadrantClientCardRow row) {
        VBox card = new VBox(14);
        card.getStyleClass().add("eph-client-card");

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

        VBox objLibreBox = new VBox(6);
        if (row.objectifLibreExcerpt() != null && !row.objectifLibreExcerpt().isBlank()) {
            Label h = new Label("Objectif libre (Planning)");
            h.getStyleClass().add("eph-card-kicker");
            Label t = new Label(dash(row.objectifLibreExcerpt()));
            t.getStyleClass().add("eph-card-line");
            t.setWrapText(true);
            objLibreBox.getChildren().addAll(h, t);
        }
        if (row.reponseIaLibreExcerpt() != null && !row.reponseIaLibreExcerpt().isBlank()) {
            Label h2 = new Label("Réponse IA (extrait)");
            h2.getStyleClass().add("eph-card-kicker");
            Label r = new Label(dash(row.reponseIaLibreExcerpt()));
            r.getStyleClass().add("eph-card-line");
            r.setWrapText(true);
            objLibreBox.getChildren().addAll(h2, r);
        }

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

        Button discussBtn = new Button("Ouvrir la discussion");
        discussBtn.setMnemonicParsing(false);
        discussBtn.getStyleClass().add("eph-btn-discussion");
        discussBtn.setMaxWidth(Double.MAX_VALUE);
        FontIcon discussIcon = new FontIcon();
        discussIcon.setIconLiteral("fas-comments");
        discussIcon.setIconSize(14);
        discussIcon.getStyleClass().add("eph-btn-discussion-icon");
        discussBtn.setGraphic(discussIcon);
        discussBtn.setOnAction(ev -> {
            ev.consume();
            openDiscussionForClient(row);
        });

        card.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            Node t = e.getPickResult().getIntersectedNode();
            if (t != null && isUnderNode(discussBtn, t)) {
                return;
            }
            openDetail(row);
        });

        HBox foot = new HBox(8);
        foot.setAlignment(Pos.CENTER_RIGHT);
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        FontIcon chev = new FontIcon();
        chev.setIconLiteral("fas-chevron-right");
        chev.setIconSize(14);
        chev.getStyleClass().add("eph-card-chevron");
        foot.getChildren().addAll(grow, chev);

        card.getChildren().addAll(head, badge, obj, act, pct, bar, discussBtn, foot);
        return card;
    }

    private void openDiscussionForClient(EncadrantClientCardRow row) {
        int uid = row.userId();
        if (uid <= 0) {
            return;
        }
        if (mainLayoutController != null) {
            mainLayoutController.openDiscussionForClientUser(uid);
            return;
        }
        SessionContext ctx = SessionContext.getInstance();
        ctx.setPendingDiscussionClientUserId(uid);
        ctx.openDiscussionFromPlanning();
    }

    private static boolean isUnderNode(Node ancestor, Node node) {
        for (Node n = node; n != null; n = n.getParent()) {
            if (n == ancestor) {
                return true;
            }
        }
        return false;
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
