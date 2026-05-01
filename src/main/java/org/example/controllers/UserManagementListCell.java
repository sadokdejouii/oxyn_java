package org.example.controllers;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Ligne « tableau manuel » pour la liste des comptes (style dashboard SaaS).
 */
public final class UserManagementListCell extends ListCell<UserManagementController.UserRow> {

    private final HBox root = new HBox(14);
    private final Label monogram = new Label();
    private final VBox emailCol = new VBox(4);
    private final Label email = new Label();
    private final Label fullName = new Label();
    private final Region spacer = new Region();
    private final Label roleBadge = new Label();
    private final Label statusBadge = new Label();

    public UserManagementListCell() {
        root.setAlignment(Pos.CENTER_LEFT);
        root.getStyleClass().add("um-list-row");
        root.setMinHeight(56);
        root.setPrefHeight(56);

        monogram.setMinSize(40, 40);
        monogram.setMaxSize(40, 40);
        monogram.setAlignment(Pos.CENTER);
        monogram.getStyleClass().add("um-list-mono");

        email.getStyleClass().add("um-list-email");
        email.setTextOverrun(OverrunStyle.ELLIPSIS);
        fullName.getStyleClass().add("um-list-name");
        fullName.setTextOverrun(OverrunStyle.ELLIPSIS);
        emailCol.getChildren().addAll(email, fullName);
        HBox.setHgrow(emailCol, Priority.ALWAYS);
        emailCol.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(spacer, Priority.SOMETIMES);
        spacer.setMinWidth(8);

        roleBadge.getStyleClass().add("um-table-badge");
        roleBadge.setMinWidth(88);
        roleBadge.setAlignment(Pos.CENTER);

        statusBadge.getStyleClass().add("um-table-badge");
        statusBadge.setMinWidth(72);
        statusBadge.setAlignment(Pos.CENTER);

        root.getChildren().addAll(monogram, emailCol, spacer, roleBadge, statusBadge);
    }

    @Override
    protected void updateItem(UserManagementController.UserRow item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }
        email.setText(item.getEmail());
        String nm = item.getName().trim();
        fullName.setText(nm.isEmpty() ? "—" : nm);
        monogram.setText(initials(item));

        String role = item.getRole();
        roleBadge.setText(role);
        roleBadge.getStyleClass().removeIf(s ->
                "um-role-admin".equals(s) || "um-role-coach".equals(s) || "um-role-client".equals(s));
        if ("Admin".equals(role)) {
            roleBadge.getStyleClass().add("um-role-admin");
        } else if ("Encadrant".equals(role)) {
            roleBadge.getStyleClass().add("um-role-coach");
        } else if ("Client".equals(role)) {
            roleBadge.getStyleClass().add("um-role-client");
        }

        String st = item.getStatus();
        statusBadge.setText(st);
        statusBadge.getStyleClass().removeIf(s -> "um-status-on".equals(s) || "um-status-off".equals(s));
        if ("Actif".equals(st)) {
            statusBadge.getStyleClass().add("um-status-on");
        } else {
            statusBadge.getStyleClass().add("um-status-off");
        }

        setText(null);
        setGraphic(root);
    }

    private static String initials(UserManagementController.UserRow row) {
        String n = row.getUser().getNom();
        String p = row.getUser().getPrenom();
        if (p != null && !p.isBlank() && n != null && !n.isBlank()) {
            return (p.substring(0, 1) + n.substring(0, 1)).toUpperCase();
        }
        if (p != null && !p.isBlank()) {
            return p.substring(0, Math.min(2, p.length())).toUpperCase();
        }
        if (n != null && !n.isBlank()) {
            return n.substring(0, Math.min(2, n.length())).toUpperCase();
        }
        String mail = row.getEmail();
        if (mail != null && !mail.isBlank()) {
            return mail.substring(0, Math.min(2, mail.length())).toUpperCase();
        }
        return "?";
    }
}
