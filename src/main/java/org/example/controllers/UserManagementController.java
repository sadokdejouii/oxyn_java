package org.example.controllers;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entities.Admin;
import org.example.entities.Client;
import org.example.entities.Coach;
import org.example.entities.User;
import org.example.services.UserService;
import org.example.utils.UserDialogHelper;

import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;

public class UserManagementController implements Initializable {

    private enum SortColumn {
        EMAIL, NAME, ROLE, STATUS
    }

    public static final class UserRow {
        private final User user;

        public UserRow(User user) {
            this.user = user;
        }

        public User getUser() {
            return user;
        }

        public String getEmail() {
            return user.getEmail();
        }

        public String getName() {
            return (user.getNom() != null ? user.getNom() : "") + " "
                    + (user.getPrenom() != null ? user.getPrenom() : "");
        }

        public String getRole() {
            return roleLabel(user);
        }

        public String getStatus() {
            return user.isActive() ? "Actif" : "Inactif";
        }
    }

    private static String roleLabel(User u) {
        if (u instanceof Admin) {
            return "Admin";
        }
        if (u instanceof Coach) {
            return "Encadrant";
        }
        if (u instanceof Client) {
            return "Client";
        }
        return "?";
    }

    private static User copyUserWithActive(User source, boolean active) {
        if (source instanceof Admin a) {
            return new Admin(a.getId(), a.getEmail(), a.getPassword(), a.getNom(), a.getPrenom(), a.getTelephone(), active);
        }
        if (source instanceof Coach c) {
            return new Coach(c.getId(), c.getEmail(), c.getPassword(), c.getNom(), c.getPrenom(), c.getTelephone(), active);
        }
        if (source instanceof Client cl) {
            return new Client(cl.getId(), cl.getEmail(), cl.getPassword(), cl.getNom(), cl.getPrenom(), cl.getTelephone(), active);
        }
        throw new IllegalStateException("Type utilisateur non géré");
    }

    @FXML
    private TextField searchField;

    @FXML
    private ListView<UserRow> usersList;

    @FXML
    private Button sortEmailBtn;

    @FXML
    private Button sortNameBtn;

    @FXML
    private Button sortRoleBtn;

    @FXML
    private Button sortStatusBtn;

    @FXML
    private Label statTotalValue;

    @FXML
    private Label statActiveValue;

    @FXML
    private Label statInactiveValue;

    @FXML
    private Label statAdminValue;

    @FXML
    private Label statCoachValue;

    @FXML
    private Label statClientValue;

    @FXML
    private Label tableHintLabel;

    @FXML
    private GridPane heatmapGrid;

    @FXML
    private Label heatmapHint;

    @FXML
    private Canvas rolesDonutCanvas;

    @FXML
    private Label rolesHint;

    @FXML
    private ProgressBar pbRetention;
    @FXML
    private ProgressBar pbEngagement;
    @FXML
    private ProgressBar pbNps;
    @FXML
    private ProgressBar pbChurn;
    @FXML
    private Label lblRetention;
    @FXML
    private Label lblEngagement;
    @FXML
    private Label lblNps;
    @FXML
    private Label lblChurn;

    @FXML
    private Canvas sparklinesCanvas;

    @FXML
    private Label systemHint;

    private final ObservableList<UserRow> masterData = FXCollections.observableArrayList();
    private FilteredList<UserRow> filteredData;
    private SortedList<UserRow> sortedData;

    private final ObjectProperty<SortColumn> sortColumn = new SimpleObjectProperty<>(SortColumn.EMAIL);
    private final BooleanProperty sortAscending = new SimpleBooleanProperty(true);

    private final UserService userService = new UserService();

    private final Random demoRng = new Random();
    private KpiSnapshotDemo lastDemoKpis;

    private Stage dialogOwner() {
        if (usersList == null || usersList.getScene() == null) {
            return null;
        }
        return (Stage) usersList.getScene().getWindow();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filteredData = new FilteredList<>(masterData, row -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(Bindings.createObjectBinding(
                () -> comparatorFor(sortColumn.get(), sortAscending.get()),
                sortColumn, sortAscending));
        usersList.setItems(sortedData);

        usersList.setCellFactory(lv -> new UserManagementListCell());
        usersList.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                handleEdit();
            }
        });

        Label emptyTitle = new Label("Aucun utilisateur");
        emptyTitle.getStyleClass().add("um-empty-title");
        Label emptySub = new Label("Les comptes apparaîtront ici après chargement depuis la base.");
        emptySub.getStyleClass().add("um-empty-sub");
        VBox emptyBox = new VBox(8, emptyTitle, emptySub);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.getStyleClass().add("um-empty-box");
        usersList.setPlaceholder(emptyBox);

        updateSortButtonLabels();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                applySearchFilter(newVal);
                refreshSummaryLabels();
            });
        }

        loadFromDatabase();
        generateDemoAnalytics(true);
    }

    private static Comparator<UserRow> comparatorFor(SortColumn col, boolean asc) {
        Comparator<UserRow> c = switch (col) {
            case EMAIL -> Comparator.comparing(UserRow::getEmail, String.CASE_INSENSITIVE_ORDER);
            case NAME -> Comparator.comparing(r -> r.getName().trim(), String.CASE_INSENSITIVE_ORDER);
            case ROLE -> Comparator.comparing(UserRow::getRole, String.CASE_INSENSITIVE_ORDER);
            case STATUS -> Comparator.comparing(UserRow::getStatus, String.CASE_INSENSITIVE_ORDER);
        };
        return asc ? c : c.reversed();
    }

    private void applySort(SortColumn col) {
        if (sortColumn.get() == col) {
            sortAscending.set(!sortAscending.get());
        } else {
            sortColumn.set(col);
            sortAscending.set(true);
        }
        updateSortButtonLabels();
    }

    private void updateSortButtonLabels() {
        SortColumn active = sortColumn.get();
        boolean asc = sortAscending.get();
        setSortBtn(sortEmailBtn, "E-mail", SortColumn.EMAIL, active, asc);
        setSortBtn(sortNameBtn, "Nom complet", SortColumn.NAME, active, asc);
        setSortBtn(sortRoleBtn, "Rôle", SortColumn.ROLE, active, asc);
        setSortBtn(sortStatusBtn, "Statut", SortColumn.STATUS, active, asc);
    }

    private static void setSortBtn(Button btn, String title, SortColumn col, SortColumn active, boolean asc) {
        if (btn == null) {
            return;
        }
        if (active != col) {
            btn.setText(title + "  ↕");
        } else {
            btn.setText(title + "  " + (asc ? "↑" : "↓"));
        }
    }

    @FXML
    private void handleSortEmail() {
        applySort(SortColumn.EMAIL);
    }

    @FXML
    private void handleSortName() {
        applySort(SortColumn.NAME);
    }

    @FXML
    private void handleSortRole() {
        applySort(SortColumn.ROLE);
    }

    @FXML
    private void handleSortStatus() {
        applySort(SortColumn.STATUS);
    }

    private void refreshSummaryLabels() {
        int total = masterData.size();
        long active = masterData.stream().filter(r -> r.getUser().isActive()).count();
        long admins = masterData.stream().filter(r -> r.getUser() instanceof Admin).count();
        long coaches = masterData.stream().filter(r -> r.getUser() instanceof Coach).count();
        long clients = masterData.stream().filter(r -> r.getUser() instanceof Client).count();
        if (statTotalValue != null) {
            statTotalValue.setText(String.valueOf(total));
        }
        if (statActiveValue != null) {
            statActiveValue.setText(String.valueOf(active));
        }
        if (statInactiveValue != null) {
            statInactiveValue.setText(String.valueOf(Math.max(0, total - active)));
        }
        if (statAdminValue != null) {
            statAdminValue.setText(String.valueOf(admins));
        }
        if (statCoachValue != null) {
            statCoachValue.setText(String.valueOf(coaches));
        }
        if (statClientValue != null) {
            statClientValue.setText(String.valueOf(clients));
        }
        if (tableHintLabel != null) {
            if (total == 0) {
                tableHintLabel.setText("");
            } else {
                int shown = filteredData != null ? filteredData.size() : total;
                if (shown == total) {
                    tableHintLabel.setText(total + " compte" + (total > 1 ? "s" : ""));
                } else {
                    tableHintLabel.setText(shown + " affiché" + (shown > 1 ? "s" : "") + " sur " + total);
                }
            }
        }
    }

    private void applySearchFilter(String query) {
        if (filteredData == null) {
            return;
        }
        if (query == null || query.isBlank()) {
            filteredData.setPredicate(row -> true);
            return;
        }
        String q = query.trim().toLowerCase();
        filteredData.setPredicate(row ->
                containsIgnoreCase(row.getEmail(), q)
                        || containsIgnoreCase(row.getName(), q)
                        || containsIgnoreCase(row.getRole(), q)
                        || containsIgnoreCase(row.getStatus(), q));
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    @FXML
    private void handleClearSearch() {
        if (searchField != null) {
            searchField.clear();
        }
        applySearchFilter("");
        refreshSummaryLabels();
    }

    private void loadFromDatabase() {
        try {
            masterData.setAll(userService.getAllUsers().stream().map(UserRow::new).toList());
            refreshSummaryLabels();
            if (searchField != null) {
                applySearchFilter(searchField.getText());
                refreshSummaryLabels();
            }
        } catch (SQLException e) {
            UserDialogHelper.showMessage(dialogOwner(), "Chargement des utilisateurs",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        }
    }

    @FXML
    private void handleGenerateDemo() {
        generateDemoUsers(24 + demoRng.nextInt(60));
        generateDemoAnalytics(false);
        refreshSummaryLabels();
    }

    private void generateDemoUsers(int count) {
        List<String> first = List.of("Amine", "Yassine", "Sara", "Nour", "Adam", "Lina", "Hiba", "Omar", "Rania", "Mehdi", "Aya", "Youssef");
        List<String> last = List.of("Benali", "El Idrissi", "Ait Lahcen", "Boukari", "Saidi", "Mernissi", "Zahraoui", "Khalfi", "Berrada", "Haddad");
        String[] roles = {"Admin", "Manager", "Editor", "Viewer"};
        masterData.clear();
        for (int i = 0; i < count; i++) {
            String fn = first.get(demoRng.nextInt(first.size()));
            String ln = last.get(demoRng.nextInt(last.size()));
            String email = (fn + "." + ln + (100 + demoRng.nextInt(900)) + "@oxyn.demo").toLowerCase();
            boolean active = demoRng.nextDouble() > 0.14;
            String role = roles[weightedRoleIndex(demoRng)];

            User u;
            // map our demo roles to existing subclasses for styling + existing logic
            if ("Admin".equals(role)) {
                u = new Admin(i + 1, email, "", ln, fn, "", active);
            } else if ("Manager".equals(role)) {
                u = new Coach(i + 1, email, "", ln, fn, "", active);
            } else {
                u = new Client(i + 1, email, "", ln, fn, "", active);
            }
            masterData.add(new UserRow(u));
        }
        if (searchField != null) {
            applySearchFilter(searchField.getText());
        }
    }

    private static int weightedRoleIndex(Random r) {
        double x = r.nextDouble();
        if (x < 0.12) return 0; // Admin
        if (x < 0.30) return 1; // Manager
        if (x < 0.62) return 2; // Editor
        return 3; // Viewer
    }

    private void generateDemoAnalytics(boolean initial) {
        int total = Math.max(1, masterData.size());
        long active = masterData.stream().filter(r -> r.getUser().isActive()).count();
        double avgScore = 62 + demoRng.nextGaussian() * 9;
        avgScore = Math.max(15, Math.min(96, avgScore));
        double sessionsPerDay = 1.2 + demoRng.nextDouble() * 3.8;

        KpiSnapshotDemo now = new KpiSnapshotDemo(total, (int) active, avgScore, sessionsPerDay);
        applyKpiDeltas(now, initial ? null : lastDemoKpis);
        lastDemoKpis = now;

        drawHeatmap12Weeks();
        drawRolesDonut();
        drawHealthScores();
        drawSparklines();
    }

    private void applyKpiDeltas(KpiSnapshotDemo now, KpiSnapshotDemo prev) {
        // Reuse existing KPI labels: TOTAL/ACTIFS/INACTIFS + roles counts.
        // Add deltas into tableHintLabel (quick win without redesigning FXML).
        if (tableHintLabel != null) {
            String delta = prev == null ? "" : String.format(java.util.Locale.ROOT,
                    "Δ total %+d · actifs %+d · score %+,.1f · sessions/jour %+.2f",
                    (now.total - prev.total),
                    (now.active - prev.active),
                    (now.avgScore - prev.avgScore),
                    (now.sessionsPerDay - prev.sessionsPerDay));
            tableHintLabel.setText(delta);
        }
    }

    private void drawHeatmap12Weeks() {
        if (heatmapGrid == null) return;
        heatmapGrid.getChildren().clear();
        int weeks = 12;
        int days = 7;
        int max = 0;
        int[][] v = new int[weeks][days];
        for (int w = 0; w < weeks; w++) {
            for (int d = 0; d < days; d++) {
                int val = (int) Math.max(0, Math.round(6 + demoRng.nextGaussian() * 7));
                if (demoRng.nextDouble() < 0.18) val = (int) (val * 0.25);
                v[w][d] = val;
                max = Math.max(max, val);
            }
        }
        for (int d = 0; d < days; d++) {
            for (int w = 0; w < weeks; w++) {
                javafx.scene.shape.Rectangle r = new javafx.scene.shape.Rectangle(12, 12);
                r.setArcWidth(4);
                r.setArcHeight(4);
                double t = max == 0 ? 0 : (double) v[w][d] / (double) max;
                r.setFill(heatColor(t));
                heatmapGrid.add(r, w, d);
            }
        }
        if (heatmapHint != null) {
            heatmapHint.setText("Intensité d’activité (style GitHub) · " + weeks + " semaines");
        }
    }

    private static Color heatColor(double t) {
        // dark → cyan
        t = Math.max(0, Math.min(1, t));
        Color base = Color.rgb(13, 27, 62);
        Color hi = Color.rgb(0, 191, 255);
        return base.interpolate(hi, 0.15 + 0.85 * t).deriveColor(0, 1.0, 1.0, 0.92);
    }

    private void drawRolesDonut() {
        if (rolesDonutCanvas == null) return;
        Map<String, Integer> counts = new HashMap<>();
        counts.put("Admin", 0);
        counts.put("Manager", 0);
        counts.put("Editor", 0);
        counts.put("Viewer", 0);
        for (UserRow r : masterData) {
            String label = roleLabel(r.getUser());
            if ("Admin".equals(label)) counts.compute("Admin", (k, v) -> v + 1);
            else if ("Encadrant".equals(label)) counts.compute("Manager", (k, v) -> v + 1);
            else counts.compute("Editor", (k, v) -> v + 1);
        }
        // Derive viewer as remainder-ish
        int known = counts.get("Admin") + counts.get("Manager") + counts.get("Editor");
        counts.put("Viewer", Math.max(0, masterData.size() - known));

        GraphicsContext g = rolesDonutCanvas.getGraphicsContext2D();
        double w = rolesDonutCanvas.getWidth();
        double h = rolesDonutCanvas.getHeight();
        g.clearRect(0, 0, w, h);
        double cx = w / 2.0;
        double cy = h / 2.0;
        double r = Math.min(w, h) * 0.38;
        double inner = r * 0.62;

        int total = Math.max(1, counts.values().stream().mapToInt(Integer::intValue).sum());
        double start = -90;
        List<Slice> slices = List.of(
                new Slice("Admin", counts.get("Admin"), Color.rgb(156, 39, 176)),
                new Slice("Manager", counts.get("Manager"), Color.rgb(100, 181, 246)),
                new Slice("Editor", counts.get("Editor"), Color.rgb(79, 195, 247)),
                new Slice("Viewer", counts.get("Viewer"), Color.rgb(160, 180, 204))
        );
        for (Slice s : slices) {
            if (s.value <= 0) continue;
            double ang = 360.0 * s.value / total;
            g.setFill(s.color.deriveColor(0, 1, 1, 0.35));
            g.fillArc(cx - r, cy - r, r * 2, r * 2, start, ang, javafx.scene.shape.ArcType.ROUND);
            start += ang;
        }
        // hole
        g.setFill(Color.rgb(13, 27, 62, 0.92));
        g.fillOval(cx - inner, cy - inner, inner * 2, inner * 2);
        g.setFill(Color.rgb(232, 238, 247, 0.92));
        g.fillText("Rôles", cx - 18, cy + 4);

        if (rolesHint != null) {
            rolesHint.setText("Admin " + counts.get("Admin") + " · Manager " + counts.get("Manager") + " · Editor " + counts.get("Editor") + " · Viewer " + counts.get("Viewer"));
        }
    }

    private void drawHealthScores() {
        double retention = clamp01(0.35 + demoRng.nextDouble() * 0.55);
        double engagement = clamp01(0.25 + demoRng.nextDouble() * 0.65);
        double nps = clamp01(0.15 + demoRng.nextDouble() * 0.75);
        double churn = clamp01(0.15 + demoRng.nextDouble() * 0.65);

        setPb(pbRetention, lblRetention, retention);
        setPb(pbEngagement, lblEngagement, engagement);
        setPb(pbNps, lblNps, nps);
        setPb(pbChurn, lblChurn, churn);
    }

    private static void setPb(ProgressBar pb, Label lbl, double v) {
        if (pb != null) pb.setProgress(v);
        if (lbl != null) lbl.setText(String.format(java.util.Locale.ROOT, "%.0f%%", v * 100));
    }

    private void drawSparklines() {
        if (sparklinesCanvas == null) return;
        GraphicsContext g = sparklinesCanvas.getGraphicsContext2D();
        double w = sparklinesCanvas.getWidth();
        double h = sparklinesCanvas.getHeight();
        g.clearRect(0, 0, w, h);

        double[] a = randomWalk(18, 60, 8);
        double[] b = randomWalk(18, 45, 6);
        double[] c = randomWalk(18, 30, 10);

        drawSpark(g, a, 10, 22, w - 20, 50, Color.rgb(0, 191, 255));
        drawSpark(g, b, 10, 92, w - 20, 50, Color.rgb(105, 240, 174));
        drawSpark(g, c, 10, 162, w - 20, 50, Color.rgb(255, 183, 77));

        if (systemHint != null) {
            systemHint.setText("Uptime 99." + (40 + demoRng.nextInt(60)) + "% · Latence p95 " + (90 + demoRng.nextInt(120)) + "ms · Requêtes/min " + (120 + demoRng.nextInt(340)));
        }
    }

    private static void drawSpark(GraphicsContext g, double[] v, double x, double y, double w, double h, Color c) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (double d : v) { min = Math.min(min, d); max = Math.max(max, d); }
        if (max <= min) { max = min + 1; }
        g.setStroke(c.deriveColor(0, 1, 1, 0.85));
        g.setLineWidth(2.0);
        for (int i = 0; i < v.length - 1; i++) {
            double x1 = x + (w * i / (v.length - 1));
            double x2 = x + (w * (i + 1) / (v.length - 1));
            double y1 = y + h - (h * (v[i] - min) / (max - min));
            double y2 = y + h - (h * (v[i + 1] - min) / (max - min));
            g.strokeLine(x1, y1, x2, y2);
        }
    }

    private double[] randomWalk(int n, double start, double step) {
        double[] v = new double[n];
        double cur = start;
        for (int i = 0; i < n; i++) {
            cur += demoRng.nextGaussian() * step;
            v[i] = cur;
        }
        return v;
    }

    private static double clamp01(double x) {
        return Math.max(0, Math.min(1, x));
    }

    private record KpiSnapshotDemo(int total, int active, double avgScore, double sessionsPerDay) {}
    private record Slice(String name, int value, Color color) {}

    @FXML
    private void handleAdd() {
        Optional<User> draft = UserDialogHelper.showUserForm(dialogOwner(), null);
        draft.ifPresent(u -> {
            try {
                userService.addUser(u);
                loadFromDatabase();
            } catch (SQLException e) {
                UserDialogHelper.showMessage(dialogOwner(), "Ajout utilisateur",
                        e.getMessage() != null ? e.getMessage() : e.toString(), true);
            }
        });
    }

    @FXML
    private void handleEdit() {
        UserRow row = usersList.getSelectionModel().getSelectedItem();
        if (row == null) {
            UserDialogHelper.showMessage(dialogOwner(), "Modifier",
                    "Sélectionnez un utilisateur dans la liste.", false);
            return;
        }
        Optional<User> draft = UserDialogHelper.showUserForm(dialogOwner(), row.getUser());
        draft.ifPresent(u -> {
            try {
                userService.updateUser(u);
                loadFromDatabase();
            } catch (SQLException e) {
                UserDialogHelper.showMessage(dialogOwner(), "Mise à jour utilisateur",
                        e.getMessage() != null ? e.getMessage() : e.toString(), true);
            }
        });
    }

    @FXML
    private void handleDelete() {
        UserRow row = usersList.getSelectionModel().getSelectedItem();
        if (row == null) {
            UserDialogHelper.showMessage(dialogOwner(), "Suppression",
                    "Sélectionnez un utilisateur dans la liste.", false);
            return;
        }
        boolean ok = UserDialogHelper.showConfirm(dialogOwner(), "Supprimer l'utilisateur",
                "Supprimer définitivement l'utilisateur « " + row.getEmail() + " » ?",
                "Supprimer", true);
        if (!ok) {
            return;
        }
        try {
            userService.deleteUser(row.getUser().getId());
            loadFromDatabase();
        } catch (SQLException e) {
            UserDialogHelper.showMessage(dialogOwner(), "Suppression utilisateur",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        }
    }

    @FXML
    private void handleToggleActive() {
        UserRow row = usersList.getSelectionModel().getSelectedItem();
        if (row == null) {
            UserDialogHelper.showMessage(dialogOwner(), "Activer / désactiver",
                    "Sélectionnez un utilisateur dans la liste.", false);
            return;
        }
        User u = row.getUser();
        boolean next = !u.isActive();
        String action = next ? "activer" : "désactiver";
        boolean ok = UserDialogHelper.showConfirm(dialogOwner(), "Compte utilisateur",
                "Voulez-vous " + action + " le compte « " + u.getEmail() + " » ?",
                next ? "Activer" : "Désactiver", false);
        if (!ok) {
            return;
        }
        try {
            userService.updateUser(copyUserWithActive(u, next));
            loadFromDatabase();
        } catch (SQLException e) {
            UserDialogHelper.showMessage(dialogOwner(), "Mise à jour du statut",
                    e.getMessage() != null ? e.getMessage() : e.toString(), true);
        }
    }

    @FXML
    private void handleRefresh() {
        loadFromDatabase();
    }
}
