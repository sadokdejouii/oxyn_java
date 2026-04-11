package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.planning.PlanningUi;
import org.example.planning.PlanningViewModel;
import org.example.services.CurrentSession;

import java.net.URL;
import java.util.ResourceBundle;

public class PlanningPageController implements Initializable {

    @FXML
    private VBox dynamicContainer;

    @FXML
    private Label heroSub;

    @FXML
    private Label roleBadge;

    @FXML
    private Label avatarInitials;

    private final PlanningViewModel viewModel = new PlanningViewModel();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        heroSub.textProperty().bind(viewModel.heroSubtitleProperty());
        roleBadge.textProperty().bind(viewModel.roleBadgeTextProperty());
        applyRoleBadgeStyle(viewModel.roleBadgeVariant());
        if (avatarInitials != null) {
            avatarInitials.setText(initialsFromDisplayName(CurrentSession.context().getDisplayName()));
        }
        dynamicContainer.getChildren().clear();

        var ctx = CurrentSession.context();
        if (ctx.isEncadrant()) {
            buildEncadrantView();
        } else if (ctx.isAdmin()) {
            buildAdminView();
        } else {
            buildClientView();
        }
    }

    private void applyRoleBadgeStyle(String variant) {
        roleBadge.getStyleClass().removeIf(c -> c.startsWith("planning-role-badge--"));
        if (variant != null && !variant.isBlank()) {
            roleBadge.getStyleClass().add("planning-role-badge--" + variant);
        }
    }

    @FXML
    private void handleOpenDiscussion() {
        CurrentSession.context().openDiscussionFromPlanning();
    }

    private void buildClientView() {
        var ctx = CurrentSession.context();
        if (!ctx.hasDbUser()) {
            VBox req = new VBox(PlanningUi.hintLabel(
                    "Connexion requise : connectez-vous avec un compte reconnu par l’application (e-mail enregistré)."));
            dynamicContainer.getChildren().add(PlanningUi.card("Compte requis", null, null, wrapGrow(req)));
            return;
        }
        new PlanningClientController(dynamicContainer, ctx.getUserId()).refresh();
    }

    private void buildEncadrantView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/pages/EncadrantPlanningHub.fxml"));
            Parent root = loader.load();
            EncadrantPlanningHubController c = loader.getController();
            c.setup();
            dynamicContainer.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);
        } catch (Exception e) {
            Throwable t = e.getCause() != null ? e.getCause() : e;
            String msg = t.getMessage() != null ? t.getMessage() : t.toString();
            VBox err = new VBox(PlanningUi.hintLabel("Impossible de charger la vue encadrant : " + msg));
            dynamicContainer.getChildren().add(PlanningUi.card("Erreur", null, null, wrapGrow(err)));
        }
    }

    private void buildAdminView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/pages/AdminPlanningDashboard.fxml"));
            Parent root = loader.load();
            AdminPlanningDashboardController c = loader.getController();
            c.setup();
            dynamicContainer.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);
        } catch (Exception e) {
            Throwable t = e.getCause() != null ? e.getCause() : e;
            String msg = t.getMessage() != null ? t.getMessage() : t.toString();
            VBox err = new VBox(PlanningUi.hintLabel("Impossible de charger la vue admin Planning : " + msg));
            dynamicContainer.getChildren().add(PlanningUi.card("Erreur", null, null, wrapGrow(err)));
        }
    }

    private static Region wrapGrow(Region r) {
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    private static String initialsFromDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "?";
        }
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) {
            String p = parts[0];
            return p.length() <= 1 ? p.toUpperCase() : p.substring(0, Math.min(2, p.length())).toUpperCase();
        }
        String a = parts[0].substring(0, 1);
        String b = parts[parts.length - 1].substring(0, 1);
        return (a + b).toUpperCase();
    }
}
