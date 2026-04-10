package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class UserManagementController implements Initializable {

    public static final class UserRow {
        private final String email;
        private final String name;
        private final String role;
        private final String status;

        public UserRow(String email, String name, String role, String status) {
            this.email = email;
            this.name = name;
            this.role = role;
            this.status = status;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }

        public String getRole() {
            return role;
        }

        public String getStatus() {
            return status;
        }
    }

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

    private final ObservableList<UserRow> rows = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        loadDemo();
        usersTable.setItems(rows);
    }

    private void loadDemo() {
        rows.setAll(
                new UserRow("admin@agr.local", "Administrator", "Admin", "Active"),
                new UserRow("client@agr.local", "Jamie Client", "Client", "Active"),
                new UserRow("ops@agr.local", "Ops Bot", "Admin", "Invited")
        );
    }

    @FXML
    private void handleInvite() {
        info("Invite user", "Connect this action to your mail/SSO provisioning flow.");
    }

    @FXML
    private void handleResetRole() {
        UserRow row = usersTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            info("Role", "Select a user first.");
            return;
        }
        info("Change role", "Selected: " + row.getEmail());
    }

    @FXML
    private void handleRefresh() {
        loadDemo();
    }

    private static void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
