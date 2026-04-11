package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
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
import java.util.Optional;
import java.util.ResourceBundle;

public class UserManagementController implements Initializable {

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
    private TableView<UserRow> usersTable;

    @FXML
    private TableColumn<UserRow, String> emailColumn;

    @FXML
    private TableColumn<UserRow, String> nameColumn;

    @FXML
    private TableColumn<UserRow, String> roleColumn;

    @FXML
    private TableColumn<UserRow, String> statusColumn;

    @FXML
    private Label statTotalValue;

    @FXML
    private Label statActiveValue;

    @FXML
    private Label statInactiveValue;

    @FXML
    private Label statAdminValue;

    @FXML
    private Label tableHintLabel;

    private final ObservableList<UserRow> masterData = FXCollections.observableArrayList();
    private FilteredList<UserRow> filteredData;

    private final UserService userService = new UserService();

    private Stage dialogOwner() {
        if (usersTable == null || usersTable.getScene() == null) {
            return null;
        }
        return (Stage) usersTable.getScene().getWindow();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filteredData = new FilteredList<>(masterData, row -> true);
        SortedList<UserRow> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(usersTable.comparatorProperty());
        usersTable.setItems(sortedData);

        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        configureRoleAndStatusCells();
        usersTable.setRowFactory(tv -> {
            TableRow<UserRow> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    usersTable.getSelectionModel().select(row.getItem());
                    handleEdit();
                }
            });
            return row;
        });
        Label emptyTitle = new Label("Aucun utilisateur");
        emptyTitle.getStyleClass().add("um-empty-title");
        Label emptySub = new Label("Les comptes apparaîtront ici après chargement depuis la base.");
        emptySub.getStyleClass().add("um-empty-sub");
        VBox emptyBox = new VBox(8, emptyTitle, emptySub);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.getStyleClass().add("um-empty-box");
        usersTable.setPlaceholder(emptyBox);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applySearchFilter(newVal));
        }

        loadFromDatabase();
    }

    private void configureRoleAndStatusCells() {
        roleColumn.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();

            {
                badge.getStyleClass().add("um-table-badge");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                badge.setText(item);
                badge.getStyleClass().removeIf(s ->
                        "um-role-admin".equals(s) || "um-role-coach".equals(s) || "um-role-client".equals(s));
                if ("Admin".equals(item)) {
                    badge.getStyleClass().add("um-role-admin");
                } else if ("Encadrant".equals(item)) {
                    badge.getStyleClass().add("um-role-coach");
                } else if ("Client".equals(item)) {
                    badge.getStyleClass().add("um-role-client");
                }
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(badge);
            }
        });
        statusColumn.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();

            {
                badge.getStyleClass().add("um-table-badge");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                badge.setText(item);
                badge.getStyleClass().removeIf(s -> "um-status-on".equals(s) || "um-status-off".equals(s));
                if ("Actif".equals(item)) {
                    badge.getStyleClass().add("um-status-on");
                } else {
                    badge.getStyleClass().add("um-status-off");
                }
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(badge);
            }
        });
    }

    private void refreshSummaryLabels() {
        int total = masterData.size();
        long active = masterData.stream().filter(r -> r.getUser().isActive()).count();
        long admins = masterData.stream().filter(r -> r.getUser() instanceof Admin).count();
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
        if (tableHintLabel != null) {
            if (total == 0) {
                tableHintLabel.setText("");
            } else {
                tableHintLabel.setText(total + " compte" + (total > 1 ? "s" : ""));
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
    }

    private void loadFromDatabase() {
        try {
            masterData.setAll(userService.getAllUsers().stream().map(UserRow::new).toList());
            refreshSummaryLabels();
            if (searchField != null) {
                applySearchFilter(searchField.getText());
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
        UserRow row = usersTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            UserDialogHelper.showMessage(dialogOwner(), "Modifier",
                    "Sélectionnez un utilisateur dans le tableau.", false);
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
        UserRow row = usersTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            UserDialogHelper.showMessage(dialogOwner(), "Suppression",
                    "Sélectionnez un utilisateur dans le tableau.", false);
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
        UserRow row = usersTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            UserDialogHelper.showMessage(dialogOwner(), "Activer / désactiver",
                    "Sélectionnez un utilisateur dans le tableau.", false);
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
