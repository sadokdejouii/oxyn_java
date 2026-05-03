package org.example.controllers;



import javafx.fxml.FXMLLoader;

import javafx.scene.control.Alert;

import javafx.scene.control.Label;

import javafx.scene.Parent;

import javafx.scene.layout.Priority;

import javafx.scene.layout.VBox;

import org.example.model.planning.FicheSanteFormData;

import org.example.model.planning.PlanningClientDashboardData;

import org.example.planning.PlanningUi;

import org.example.planning.form.FicheSanteFormView;

import org.example.services.PlanningClientService;



import java.sql.SQLException;



/**

 * UI Planning — rôle client : formulaire fiche, dashboard, édition, régénération programme.

 */

public final class PlanningClientController {



    private final VBox container;

    private final int userId;

    private final PlanningClientService service = new PlanningClientService();



    public PlanningClientController(VBox container, int userId) {

        this.container = container;

        this.userId = userId;

    }



    public void refresh() {

        container.getChildren().clear();

        try {

            PlanningClientDashboardData data = service.loadDashboard(userId);

            if (data.fiche().isEmpty()) {

                renderNewFicheForm();

            } else {

                renderDashboard(data);

            }

        } catch (SQLException e) {

            container.getChildren().add(PlanningUi.card("Erreur", null, null,

                    wrap(new VBox(PlanningUi.hintLabel(e.getMessage() != null ? e.getMessage() : e.toString())))));

        }

    }



    private void renderNewFicheForm() {

        Label intro = PlanningUi.hintLabel(

                "Aucune fiche santé en base pour votre compte. Complétez le formulaire ci-dessous : "

                        + "votre programme personnalisé sera généré automatiquement à l’enregistrement.");

        intro.getStyleClass().add("planning-client-intro");



        FicheSanteFormView form = FicheSanteFormView.forCreation(FicheSanteFormData.empty());

        form.setOnSubmit(data -> {

            try {

                service.createFicheAndGenerateProgram(userId, data);

                refresh();

            } catch (SQLException ex) {

                alertSql(ex);

            }

        });



        container.getChildren().addAll(
                sectionShell("Bienvenue", intro),
                wrap(form.getRoot())
        );

    }



    private void renderDashboard(PlanningClientDashboardData data) {

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/pages/PlanningClientDashboard.fxml"));

            Parent root = loader.load();

            PlanningClientDashboardController c = loader.getController();

            c.setup(userId, service, data, this::refresh);

            container.getChildren().add(root);

            VBox.setVgrow(root, Priority.ALWAYS);

        } catch (Exception e) {

            Throwable cause = e;

            if (e.getCause() != null) {

                cause = e.getCause();

            }

            String msg = cause.getMessage() != null ? cause.getMessage() : cause.toString();

            container.getChildren().add(PlanningUi.card("Erreur", null, null,

                    wrap(new VBox(PlanningUi.hintLabel("Impossible d’afficher le dashboard : " + msg)))));

        }

    }



    private static VBox sectionShell(String title, javafx.scene.layout.Region body) {

        VBox v = new VBox(8);

        Label t = new Label(title);

        t.getStyleClass().add("planning-client-section-title");

        v.getChildren().addAll(t, body);

        return v;

    }



    private static javafx.scene.layout.Region wrap(javafx.scene.layout.Region r) {

        VBox.setVgrow(r, Priority.ALWAYS);

        return r;

    }



    private static void alertSql(SQLException ex) {

        Alert a = new Alert(Alert.AlertType.ERROR);

        a.setTitle("Enregistrement");

        a.setHeaderText("Impossible d’enregistrer les données.");

        a.setContentText(ex.getMessage() != null ? ex.getMessage() : ex.toString());

        a.getDialogPane().setMinWidth(420);

        a.showAndWait();

    }

}

