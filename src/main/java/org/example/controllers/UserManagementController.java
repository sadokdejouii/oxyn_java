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
import javafx.scene.control.TextField;
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
import java.util.Optional;
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

    private final ObservableList<UserRow> masterData = FXCollections.observableArrayList();
    private FilteredList<UserRow> filteredData;
    private SortedList<UserRow> sortedData;

    private final ObjectProperty<SortColumn> sortColumn = new SimpleObjectProperty<>(SortColumn.EMAIL);
    private final BooleanProperty sortAscending = new SimpleBooleanProperty(true);

    private final UserService userService = new UserService();

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
