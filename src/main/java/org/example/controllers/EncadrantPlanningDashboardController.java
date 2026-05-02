package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.entities.FicheSanteRow;
import org.example.entities.ObjectifRow;
import org.example.model.planning.encadrant.EncadrantClientPlanningSnapshot;
import org.example.model.planning.task.TacheEtat;
import org.example.model.planning.task.TacheQuotidienne;
import org.example.model.planning.task.WeeklyTaskSummary;
import org.example.planning.widgets.StructuredProgrammePane;
import org.example.services.EncadrantClientPlanningService;
import org.example.services.ObjectifClientService;
import org.example.model.planning.objectif.ObjectifClientRow;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Vue Planning encadrant : lecture du client suivi + observation persistée si objectif hebdo présent.
 */
public final class EncadrantPlanningDashboardController {

    @FXML
    private Label lblClientName;
    @FXML
    private Label lblClientEmail;
    @FXML
    private Label lblClientStatut;
    @FXML
    private Label lblGenre;
    @FXML
    private Label lblAge;
    @FXML
    private Label lblTaille;
    @FXML
    private Label lblPoids;
    @FXML
    private Label lblObjectif;
    @FXML
    private Label lblActivite;
    @FXML
    private VBox programmeStructuredHost;
    @FXML
    private Label lblDashCalories;
    @FXML
    private Label lblDashObjectifProg;
    @FXML
    private Label lblDashActivite;
    @FXML
    private Label lblDashProgressPct;
    @FXML
    private Label lblDashProgressDetail;
    @FXML
    private ProgressBar progressDashSummary;
    @FXML
    private VBox tasksContainer;
    @FXML
    private TextArea txtIa;
    @FXML
    private Label lblObsHint;
    @FXML
    private Label lblObsValidation;
    @FXML
    private TextArea txtObservation;
    @FXML
    private CheckBox chkEfforts;
    @FXML
    private Button btnRefresh;
    @FXML
    private Button btnSaveObs;
    @FXML
    private Label lblObjLibreClient;
    @FXML
    private TextArea txtReponseIaObjLibre;
    @FXML
    private TextArea txtInterventionObjLibre;
    @FXML
    private Button btnSendObjLibre;

    private final EncadrantClientPlanningService service = new EncadrantClientPlanningService();
    private final ObjectifClientService objectifClientService = new ObjectifClientService();
    private int clientUserId;

    public void setup(int clientUserId) {
        this.clientUserId = clientUserId;
        lblClientName.setText("Chargement…");
        lblClientEmail.setText("—");
        lblClientStatut.setText("—");
        btnRefresh.setOnAction(e -> loadAll());
        btnSaveObs.setOnAction(e -> saveObservation());
        txtObservation.textProperty().addListener((o, a, b) -> clearObsValidation());
        loadAll();
    }

    private void loadAll() {
        try {
            EncadrantClientPlanningSnapshot s = service.loadSnapshot(clientUserId);
            apply(s);
        } catch (SQLException ex) {
            alert(Alert.AlertType.ERROR, "Chargement", ex.getMessage() != null ? ex.getMessage() : ex.toString());
        }
    }

    private void apply(EncadrantClientPlanningSnapshot s) {
        clearObsValidation();
        applyClientHeader(s);

        if (s.fiche().isEmpty()) {
            lblGenre.setText("—");
            lblAge.setText("—");
            lblTaille.setText("—");
            lblPoids.setText("—");
            lblObjectif.setText("—");
            lblActivite.setText("—");
            lblDashActivite.setText("—");
        } else {
            FicheSanteRow f = s.fiche().get();
            lblGenre.setText(dash(f.genre()));
            lblAge.setText(f.age() != null ? f.age() + " ans" : "—");
            lblTaille.setText(f.tailleCm() != null ? f.tailleCm() + " cm" : "—");
            lblPoids.setText(f.poidsKg() != null ? f.poidsKg() + " kg" : "—");
            lblObjectif.setText(dash(f.objectif()));
            lblActivite.setText(dash(f.niveauActivite()));
            lblDashActivite.setText(dash(f.niveauActivite()));
        }

        programmeStructuredHost.getChildren().clear();
        if (s.programme().isEmpty()) {
            Label empty = new Label(s.programmeTextPreview());
            empty.setWrapText(true);
            empty.getStyleClass().add("w-empty-message");
            programmeStructuredHost.getChildren().add(empty);
        } else {
            programmeStructuredHost.getChildren().add(StructuredProgrammePane.build(s.programme().get()));
        }
        txtIa.setText(s.conseilsIaSynthese());

        if (s.programme().isPresent()) {
            var pg = s.programme().get();
            lblDashCalories.setText(pg.caloriesParJour() != null ? pg.caloriesParJour() + " kcal" : "—");
            lblDashObjectifProg.setText(dash(pg.objectifPrincipal()));
        } else {
            lblDashCalories.setText("—");
            lblDashObjectifProg.setText("—");
        }

        WeeklyTaskSummary w = s.taskSummary();
        lblDashProgressPct.getStyleClass().removeIf(c -> c.startsWith("epd-progress-pct--"));
        if (w.total() == 0) {
            lblDashProgressPct.setText("—");
            lblDashProgressDetail.setText("Aucune tâche enregistrée pour la semaine ISO courante.");
            progressDashSummary.setProgress(0);
            applyEncadrantProgressStyle(progressDashSummary, -1);
            lblDashProgressPct.getStyleClass().add("epd-progress-pct--na");
        } else {
            double t = w.tauxCompletionPct();
            lblDashProgressPct.setText(String.format("%.0f %%", t));
            lblDashProgressDetail.setText(String.format("%d / %d tâches · %d en cours", w.fait(), w.total(), w.enCours()));
            double p = Math.min(1, Math.max(0, t / 100.0));
            progressDashSummary.setProgress(p);
            applyEncadrantProgressStyle(progressDashSummary, t);
            if (t >= 75) {
                lblDashProgressPct.getStyleClass().add("epd-progress-pct--good");
            } else if (t >= 40) {
                lblDashProgressPct.getStyleClass().add("epd-progress-pct--warn");
            } else {
                lblDashProgressPct.getStyleClass().add("epd-progress-pct--bad");
            }
        }

        tasksContainer.getChildren().clear();
        if (s.tachesSemaine().isEmpty()) {
            Label empty = new Label("Aucune tâche enregistrée pour la semaine ISO courante (le client n’a peut‑être pas encore ouvert son planning).");
            empty.getStyleClass().add("w-empty-hint");
            empty.setWrapText(true);
            tasksContainer.getChildren().add(empty);
        } else {
            for (TacheQuotidienne t : s.tachesSemaine()) {
                tasksContainer.getChildren().add(taskRow(t));
            }
        }

        if (s.objectifSemaine().isPresent()) {
            ObjectifRow o = s.objectifSemaine().get();
            txtObservation.setText(o.messageEncadrant() != null ? o.messageEncadrant() : "");
            chkEfforts.setSelected(Boolean.TRUE.equals(o.effortsValides()));
            lblObsHint.setText("Les observations sont enregistrées sur l’objectif hebdomadaire (semaine "
                    + o.weekNumber() + " / " + o.year() + ").");
            btnSaveObs.setDisable(false);
            chkEfforts.setDisable(false);
        } else {
            txtObservation.setText("");
            lblObsHint.setText("Aucun objectif hebdomadaire pour cette semaine : l’enregistrement est désactivé "
                    + "jusqu’à ce qu’une ligne existe côté base (ex. après activité client).");
            btnSaveObs.setDisable(true);
            chkEfforts.setDisable(true);
        }
        applyObjectifLibre();
    }

    private void applyObjectifLibre() {
        if (lblObjLibreClient == null || txtReponseIaObjLibre == null) {
            return;
        }
        try {
            Optional<ObjectifClientRow> opt = objectifClientService.findLatestForUser(clientUserId);
            if (opt.isEmpty()) {
                lblObjLibreClient.setText("Aucune saisie « objectif libre » (assistant IA boutique) pour ce client.");
                txtReponseIaObjLibre.clear();
                if (txtInterventionObjLibre != null) {
                    txtInterventionObjLibre.clear();
                }
                return;
            }
            ObjectifClientRow r = opt.get();
            lblObjLibreClient.setText(r.texteObjectif());
            txtReponseIaObjLibre.setText(r.reponseIa() != null ? r.reponseIa() : "");
            if (txtInterventionObjLibre != null) {
                txtInterventionObjLibre.setText(r.interventionEncadrant() != null ? r.interventionEncadrant() : "");
            }
        } catch (SQLException ex) {
            lblObjLibreClient.setText("— Impossible de charger (vérifiez la table objectifs_hebdomadaires) : "
                    + (ex.getMessage() != null ? ex.getMessage() : ex));
            txtReponseIaObjLibre.clear();
        }
    }

    private void saveObjectifLibreIntervention() {
        if (txtInterventionObjLibre == null) {
            return;
        }
        try {
            objectifClientService.saveEncadrantInterventionForLatest(clientUserId, txtInterventionObjLibre.getText());
            alert(Alert.AlertType.INFORMATION, "Intervention", "Message enregistré — visible immédiatement côté client sur « Objectif / Assistant IA ».");
            loadAll();
        } catch (SQLException ex) {
            alert(Alert.AlertType.ERROR, "Enregistrement", ex.getMessage() != null ? ex.getMessage() : ex.toString());
        }
    }

    private void applyClientHeader(EncadrantClientPlanningSnapshot s) {
        String label = s.clientLabel() != null ? s.clientLabel() : "";
        String name = "—";
        String email = "—";
        if (label.contains(" · ")) {
            int i = label.indexOf(" · ");
            name = label.substring(0, i).trim();
            email = label.substring(i + 3).trim();
        } else if (label.contains("@")) {
            email = label.trim();
        } else if (!label.isBlank()) {
            name = label.trim();
        }
        lblClientName.setText(name.isBlank() ? "—" : name);
        lblClientEmail.setText(email.isBlank() ? "—" : email);
        if (s.objectifSemaine().isPresent()) {
            lblClientStatut.setText(dash(s.objectifSemaine().get().statut()));
        } else {
            lblClientStatut.setText("Sans objectif hebdomadaire");
        }
    }

    /**
     * Barre de progression encadrant : vert (≥ 75 %), orange (40–74 %), rouge (&lt; 40 %), gris si N/A.
     */
    private static void applyEncadrantProgressStyle(ProgressBar bar, double tauxPct) {
        bar.getStyleClass().removeIf(x -> x.startsWith("epd-pbar--"));
        if (!bar.getStyleClass().contains("epd-pbar")) {
            bar.getStyleClass().add("epd-pbar");
        }
        if (tauxPct < 0) {
            bar.getStyleClass().add("epd-pbar--na");
            return;
        }
        if (tauxPct >= 75) {
            bar.getStyleClass().add("epd-pbar--good");
        } else if (tauxPct >= 40) {
            bar.getStyleClass().add("epd-pbar--warn");
        } else {
            bar.getStyleClass().add("epd-pbar--bad");
        }
    }

    private static HBox taskRow(TacheQuotidienne t) {
        HBox row = new HBox(12);
        row.getStyleClass().addAll("w-task-row", "epd-task-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        String when = (t.date() != null ? t.date() + " · " : "") + dash(t.jourSemaine());
        Label l1 = new Label(when);
        l1.getStyleClass().add("w-task-when");
        Label l2 = new Label(t.description());
        l2.setWrapText(true);
        l2.getStyleClass().add("w-task-desc");
        HBox.setHgrow(l2, Priority.ALWAYS);
        Label badge = new Label(etatLabel(t.etat()));
        badge.getStyleClass().addAll("w-mini-badge", badgeClass(t.etat()));
        row.getChildren().addAll(l1, l2, badge);
        return row;
    }

    private static String etatLabel(TacheEtat e) {
        return switch (e) {
            case FAIT -> "FAIT";
            case EN_COURS -> "EN_COURS";
            case NON_FAIT -> "NON_FAIT";
        };
    }

    private static String badgeClass(TacheEtat e) {
        return switch (e) {
            case FAIT -> "w-mini-badge--fait";
            case EN_COURS -> "w-mini-badge--encours";
            case NON_FAIT -> "w-mini-badge--non";
        };
    }

    private void saveObservation() {
        String msg = txtObservation.getText() == null ? "" : txtObservation.getText().trim();
        if (msg.isEmpty()) {
            showObsValidation("Saisissez un message avant d’enregistrer.");
            return;
        }
        clearObsValidation();
        try {
            service.saveObservation(clientUserId, msg, chkEfforts.isSelected());
            loadAll();
        } catch (SQLException ex) {
            alert(Alert.AlertType.ERROR, "Enregistrement", ex.getMessage() != null ? ex.getMessage() : ex.toString());
        }
    }

    private void clearObsValidation() {
        if (lblObsValidation != null) {
            lblObsValidation.setText("");
            lblObsValidation.setVisible(false);
            lblObsValidation.setManaged(false);
        }
    }

    private void showObsValidation(String message) {
        if (lblObsValidation == null) {
            return;
        }
        lblObsValidation.setText(message);
        lblObsValidation.setVisible(true);
        lblObsValidation.setManaged(true);
    }

    private static void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.getDialogPane().setMinWidth(420);
        a.showAndWait();
    }

    private static String dash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
