package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.example.model.planning.admin.AdminPlanningModuleStats;
import org.example.model.planning.admin.AdminPlanningUserRow;
import org.example.services.AdminPlanningDashboardService;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
    private Label detailEyebrow;
    @FXML
    private Label detailAvatarLetter;
    @FXML
    private Label detailName;
    @FXML
    private Label detailEmail;
    @FXML
    private Label detailRoleChip;
    @FXML
    private Label detailGenreVal;
    @FXML
    private Label detailAgeVal;
    @FXML
    private Label detailPoidsVal;
    @FXML
    private Label detailProgrammeVal;
    @FXML
    private Label detailProgrammeHint;
    @FXML
    private Label detailTachesVal;
    @FXML
    private Label detailObjectifVal;
    @FXML
    private Label detailActiviteVal;
    @FXML
    private Button btnDetailClose;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<AdminPlanningSort> sortCombo;
    @FXML
    private Label lblMatchHint;

    private final AdminPlanningDashboardService service = new AdminPlanningDashboardService();
    private final List<AdminPlanningUserRow> loadedRows = new ArrayList<>();
    private boolean overlayWired;

    public void setup() {
        wireDetailOverlay();
        wireSearchAndSort();
        btnRefresh.setOnAction(e -> loadAll());
        loadAll();
    }

    private void wireSearchAndSort() {
        sortCombo.setItems(FXCollections.observableArrayList(AdminPlanningSort.values()));
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(AdminPlanningSort object) {
                return object == null ? "" : object.label;
            }

            @Override
            public AdminPlanningSort fromString(String string) {
                return null;
            }
        });
        searchField.textProperty().addListener((o, a, b) -> applySearchSortAndRender());
        sortCombo.valueProperty().addListener((o, a, b) -> applySearchSortAndRender());
    }

    private void wireDetailOverlay() {
        if (overlayWired) {
            return;
        }
        overlayWired = true;
        btnDetailClose.setOnAction(e -> hideDetail());
        detailDim.setOnMouseClicked(e -> hideDetail());
        btnDetailClose.setTooltip(new Tooltip("Fermer"));
    }

    private void showDetail(AdminPlanningUserRow row) {
        if (detailEyebrow != null) {
            detailEyebrow.setText("Planning · compte n° " + row.userId());
        }
        detailName.setText(row.displayName() != null && !row.displayName().isBlank() ? row.displayName() : "—");
        detailEmail.setText(row.email() != null && !row.email().isBlank() ? row.email() : "—");
        detailRoleChip.setText(row.roleSummary() != null ? row.roleSummary() : "—");
        detailGenreVal.setText(formatGenre(row.genre()));
        detailAgeVal.setText(row.age() != null ? row.age() + " ans" : "—");
        detailPoidsVal.setText(formatPoids(row.poids()));
        detailProgrammeVal.setText(row.programmeGenere() ? "Oui" : "Non");
        detailProgrammeVal.getStyleClass().removeAll("apd-detail-stat-v--yes", "apd-detail-stat-v--no");
        detailProgrammeVal.getStyleClass().add(row.programmeGenere() ? "apd-detail-stat-v--yes" : "apd-detail-stat-v--no");
        detailProgrammeHint.setText(row.programmeGenere()
                ? "Programme personnalisé présent en base."
                : "Aucun programme généré enregistré pour ce compte.");
        detailTachesVal.setText(String.valueOf(row.nbTaches()));
        detailObjectifVal.setText(formatObjectif(row.objectif()));
        detailActiviteVal.setText(formatNiveauActivite(row.niveauActivite()));
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

            loadedRows.clear();
            loadedRows.addAll(service.listUsersWithPlanningActivity());
            lblStatUsers.setText(String.valueOf(loadedRows.size()));
            applySearchSortAndRender();
        } catch (SQLException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Planning admin");
            a.setHeaderText(null);
            a.setContentText(ex.getMessage() != null ? ex.getMessage() : ex.toString());
            a.showAndWait();
        }
    }

    private void applySearchSortAndRender() {
        String raw = searchField.getText() == null ? "" : searchField.getText().trim();
        String q = raw.toLowerCase(Locale.ROOT);
        AdminPlanningSort sort = sortCombo.getValue() != null ? sortCombo.getValue() : AdminPlanningSort.NAME_ASC;

        List<AdminPlanningUserRow> filtered = new ArrayList<>();
        for (AdminPlanningUserRow r : loadedRows) {
            if (matchesSearch(r, q)) {
                filtered.add(r);
            }
        }
        filtered.sort(sort.comparator);

        int n = loadedRows.size();
        int m = filtered.size();
        updateMatchHint(n, m);

        userCardsHost.getChildren().clear();
        if (n == 0) {
            return;
        }
        if (m == 0) {
            Label none = new Label("Aucun utilisateur ne correspond à votre recherche.");
            none.setWrapText(true);
            none.getStyleClass().add("apd-empty-filter");
            userCardsHost.getChildren().add(none);
            return;
        }
        for (AdminPlanningUserRow row : filtered) {
            userCardsHost.getChildren().add(buildUserCard(row));
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
            lblMatchHint.setText(shown == 1 ? "1 utilisateur" : shown + " utilisateurs");
        } else {
            lblMatchHint.setText(shown + " sur " + total + " affichés");
        }
    }

    private static boolean matchesSearch(AdminPlanningUserRow r, String q) {
        if (q.isEmpty()) {
            return true;
        }
        return contains(r.displayName(), q)
                || contains(r.email(), q)
                || contains(r.roleSummary(), q)
                || contains(r.genre(), q)
                || contains(r.objectif(), q)
                || contains(r.niveauActivite(), q)
                || q.equals("oui") && r.programmeGenere()
                || q.equals("non") && !r.programmeGenere();
    }

    private static boolean contains(String field, String q) {
        if (field == null) {
            return false;
        }
        return field.toLowerCase(Locale.ROOT).contains(q);
    }

    private enum AdminPlanningSort {
        NAME_ASC("Nom (A → Z)", Comparator
                .comparing(AdminPlanningUserRow::displayName, String.CASE_INSENSITIVE_ORDER)),
        NAME_DESC("Nom (Z → A)", Comparator
                .comparing(AdminPlanningUserRow::displayName, String.CASE_INSENSITIVE_ORDER)
                .reversed()),
        TASKS_DESC("Tâches enregistrées (↓)", Comparator
                .comparingInt(AdminPlanningUserRow::nbTaches).reversed()
                .thenComparing(AdminPlanningUserRow::displayName, String.CASE_INSENSITIVE_ORDER)),
        TASKS_ASC("Tâches enregistrées (↑)", Comparator
                .comparingInt(AdminPlanningUserRow::nbTaches)
                .thenComparing(AdminPlanningUserRow::displayName, String.CASE_INSENSITIVE_ORDER)),
        PROGRAMME_FIRST("Programme généré d’abord", Comparator
                .comparing(AdminPlanningUserRow::programmeGenere).reversed()
                .thenComparing(AdminPlanningUserRow::displayName, String.CASE_INSENSITIVE_ORDER)),
        ROLE_ASC("Rôle (A → Z)", Comparator
                .comparing(AdminPlanningUserRow::roleSummary, String.CASE_INSENSITIVE_ORDER));

        private final String label;
        private final Comparator<AdminPlanningUserRow> comparator;

        AdminPlanningSort(String label, Comparator<AdminPlanningUserRow> comparator) {
            this.label = label;
            this.comparator = comparator;
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

    private static String formatPoids(Double kg) {
        if (kg == null) {
            return "—";
        }
        double v = kg;
        if (Math.abs(v - Math.rint(v)) < 1e-6) {
            return String.format(Locale.FRANCE, "%.0f kg", v);
        }
        return String.format(Locale.FRANCE, "%.1f kg", v);
    }

    private static String formatGenre(String g) {
        if (g == null || g.isBlank()) {
            return "—";
        }
        String x = g.trim();
        if (x.equalsIgnoreCase("M") || x.equalsIgnoreCase("H")) {
            return "Masculin";
        }
        if (x.equalsIgnoreCase("F")) {
            return "Féminin";
        }
        return x;
    }

    private static String formatObjectif(String raw) {
        if (raw == null || raw.isBlank()) {
            return "—";
        }
        String k = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        return switch (k) {
            case "perte_poids", "perte-de-poids" -> "Perte de poids";
            case "prise_masse", "prise-masse" -> "Prise de masse";
            case "maintien" -> "Maintien";
            case "forme", "remise_en_forme" -> "Remise en forme";
            case "endurance" -> "Endurance";
            default -> humanizeSnake(raw.trim());
        };
    }

    private static String formatNiveauActivite(String raw) {
        if (raw == null || raw.isBlank()) {
            return "—";
        }
        String k = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        return switch (k) {
            case "sedentaire", "sédentaire" -> "Sédentaire";
            case "peu_actif", "peu-actif" -> "Peu actif";
            case "modere", "modéré", "moyen" -> "Modéré";
            case "actif" -> "Actif";
            case "tres_actif", "très_actif", "tres-actif" -> "Très actif";
            default -> humanizeSnake(raw.trim());
        };
    }

    private static String humanizeSnake(String s) {
        String[] p = s.replace('-', '_').split("_");
        StringBuilder b = new StringBuilder();
        for (String part : p) {
            if (part.isBlank()) {
                continue;
            }
            if (b.length() > 0) {
                b.append(' ');
            }
            b.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                b.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return b.length() == 0 ? s : b.toString();
    }
}
