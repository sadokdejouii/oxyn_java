package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.entities.FicheSanteRow;
import org.example.entities.ObjectifRow;
import org.example.entities.ProgrammeGenereRow;
import org.example.model.planning.FicheSanteFormData;
import org.example.model.planning.PlanningClientDashboardData;
import org.example.model.planning.ai.FicheSante;
import org.example.model.planning.ai.ProgrammeGenere;
import org.example.model.planning.ai.WeeklyProgress;
import org.example.model.planning.task.TacheEtat;
import org.example.model.planning.task.TacheQuotidienne;
import org.example.model.planning.task.WeeklyTaskSummary;
import org.example.planning.form.FicheSanteFormMode;
import org.example.planning.form.FicheSanteFormView;
import org.example.planning.widgets.ProgrammeJsonLayout;
import org.example.services.CurrentSession;
import org.example.services.PlanningAiAdviceMapper;
import org.example.services.PlanningAiAdviceService;
import org.example.services.PlanningClientService;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dashboard Planning client — layout SaaS, modal fiche intégrée.
 */
public final class PlanningClientDashboardController {

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
    private Label lblNiveau;
    @FXML
    private Button btnEditFiche;
    @FXML
    private Label lblCalories;
    @FXML
    private Label lblObjectifPrincipal;
    @FXML
    private Label lblNoProgram;
    @FXML
    private VBox programmeKcalHero;
    @FXML
    private Label lblProgrammeDailyKcal;
    @FXML
    private ScrollPane programmeScroll;
    @FXML
    private VBox trainingTimeline;
    @FXML
    private VBox nutritionStack;
    @FXML
    private VBox chartSection;
    @FXML
    private BarChart<String, Number> weekProgressChart;
    @FXML
    private TextArea conseilsArea;
    @FXML
    private Button btnAiRegenerate;
    @FXML
    private Button btnAiCopy;
    @FXML
    private TextArea chatbotArea;
    @FXML
    private Label lblAiMotto;
    @FXML
    private Label lblDashActivite;
    @FXML
    private Label lblDashProgressPct;
    @FXML
    private Label lblDashProgressDetail;
    @FXML
    private VBox progressDashBlock;
    @FXML
    private ProgressBar progressWeek;
    @FXML
    private Label lblEncouragement;
    @FXML
    private Label lblNoObjectif;
    @FXML
    private VBox tasksBox;
    @FXML
    private StackPane ficheOverlay;
    @FXML
    private Region ficheDim;
    @FXML
    private StackPane ficheFormMount;
    @FXML
    private Button btnFicheModalClose;

    private int userId;
    private PlanningClientService service;
    private Runnable onRefresh;
    private FicheSanteRow ficheCourante;
    private PlanningClientDashboardData dashboardData;
    private int aiVariant;
    private final PlanningAiAdviceService planningAiAdviceService = new PlanningAiAdviceService();
    private boolean ficheOverlayHandlersWired;

    private static final String[] AI_MOTTOS = {
            "Chaque séance te rapproche de ton objectif — garde le cap.",
            "La constance bat l’intensité : un peu chaque jour, c’est gagnant.",
            "Ton corps s’adapte : fais-lui confiance et suis le plan.",
            "Progrès = répétition × patience. Tu es sur la bonne voie.",
            "Hydratation, sommeil, mouvement : les trois piliers du succès.",
            "Célébre les petites victoires — elles mènent aux grandes."
    };

    public void setup(int userId, PlanningClientService service, PlanningClientDashboardData data, Runnable onRefresh) {
        this.userId = userId;
        this.service = service;
        this.onRefresh = onRefresh;
        this.dashboardData = data;
        this.aiVariant = ThreadLocalRandom.current().nextInt(0, 512);

        wireFicheOverlayHandlers();

        FicheSanteRow f = data.fiche().orElseThrow();
        this.ficheCourante = f;


        lblGenre.setText(nullToDash(f.genre()));
        lblAge.setText(f.age() != null ? f.age() + " ans" : "—");
        lblTaille.setText(f.tailleCm() != null ? f.tailleCm() + " cm" : "—");
        lblPoids.setText(f.poidsKg() != null ? f.poidsKg() + " kg" : "—");
        lblObjectif.setText(nullToDash(f.objectif()));
        lblNiveau.setText(nullToDash(f.niveauActivite()));
        lblDashActivite.setText(nullToDash(f.niveauActivite()));

        btnEditFiche.setOnAction(e -> openFicheModal(ficheCourante));
        btnAiRegenerate.setOnAction(e -> {
            aiVariant++;
            applyLocalAiAdvice();
            refreshAiMotto();
        });
        btnAiCopy.setOnAction(e -> copyAiAdviceToClipboard());

        Optional<ProgrammeGenereRow> pg = data.programme();
        if (pg.isEmpty()) {
            lblNoProgram.setVisible(true);
            lblNoProgram.setManaged(true);
            lblNoProgram.setText(
                    "Aucun programme généré — utilisez « Modifier la fiche santé » puis enregistrez pour régénérer.");
            lblCalories.setText("—");
            lblObjectifPrincipal.setText("—");
            if (lblProgrammeDailyKcal != null) {
                lblProgrammeDailyKcal.setText("—");
            }
            hideProgrammeKcalHero();
            hideProgrammeSections();
            conseilsArea.setText("");
            applyLocalAiAdvice();
            fillProgression(data.objectifSemaine(), WeeklyTaskSummary.from(data.tachesSemaine()));
            fillTasks(data.tachesSemaine());
            refreshAiMotto();
            return;
        }

        lblNoProgram.setVisible(false);
        lblNoProgram.setManaged(false);
        showProgrammeSections();
        showProgrammeKcalHero();

        ProgrammeGenereRow p = pg.get();
        String kcalLine = formatDailyKcal(p.caloriesParJour());
        lblCalories.setText(formatKcalShort(p.caloriesParJour()));
        if (lblProgrammeDailyKcal != null) {
            lblProgrammeDailyKcal.setText(kcalLine);
        }
        lblObjectifPrincipal.setText(p.objectifPrincipal() != null && !p.objectifPrincipal().isBlank()
                ? p.objectifPrincipal()
                : "—");

        ProgrammeJsonLayout.fillTrainingTimeline(trainingTimeline, p.exercicesHebdomadairesJson());
        ProgrammeJsonLayout.fillNutritionVertical(nutritionStack, p.plansRepasJson());
        conseilsArea.setText(p.conseilsGeneraux() != null ? p.conseilsGeneraux() : "");
        applyLocalAiAdvice();

        fillProgression(data.objectifSemaine(), WeeklyTaskSummary.from(data.tachesSemaine()));
        fillTasks(data.tachesSemaine());
        refreshAiMotto();
    }

    private void wireFicheOverlayHandlers() {
        if (ficheOverlayHandlersWired) {
            return;
        }
        ficheOverlayHandlersWired = true;
        if (btnFicheModalClose != null) {
            btnFicheModalClose.setOnAction(e -> hideFicheModal());
        }
        if (ficheDim != null) {
            ficheDim.setOnMouseClicked(e -> hideFicheModal());
        }
    }

    private void showFicheModal() {
        if (ficheOverlay == null) {
            return;
        }
        ficheOverlay.setManaged(true);
        ficheOverlay.setVisible(true);
        ficheOverlay.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), ficheOverlay);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void hideFicheModal() {
        if (ficheOverlay == null) {
            return;
        }
        FadeTransition ft = new FadeTransition(Duration.millis(160), ficheOverlay);
        ft.setFromValue(ficheOverlay.getOpacity());
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            ficheOverlay.setVisible(false);
            ficheOverlay.setManaged(false);
            if (ficheFormMount != null) {
                ficheFormMount.getChildren().clear();
            }
        });
        ft.play();
    }

    private void refreshAiMotto() {
        if (lblAiMotto == null) {
            return;
        }
        lblAiMotto.setText(AI_MOTTOS[Math.floorMod(aiVariant, AI_MOTTOS.length)]);
    }

    private void applyLocalAiAdvice() {
        FicheSante fiche = PlanningAiAdviceMapper.fromFiche(dashboardData.fiche().orElseThrow());
        ProgrammeGenere programme = PlanningAiAdviceMapper.fromProgramme(dashboardData.programme());
        WeeklyProgress progress = PlanningAiAdviceMapper.fromObjectif(dashboardData.objectifSemaine());
        String text = planningAiAdviceService.generateAdvice(fiche, programme, progress, aiVariant);
        chatbotArea.setText(text);
    }

    private void copyAiAdviceToClipboard() {
        ClipboardContent cc = new ClipboardContent();
        cc.putString(chatbotArea.getText());
        Clipboard.getSystemClipboard().setContent(cc);
    }

    private void hideProgrammeSections() {
        programmeScroll.setVisible(false);
        programmeScroll.setManaged(false);
    }

    private void showProgrammeSections() {
        programmeScroll.setVisible(true);
        programmeScroll.setManaged(true);
    }

    private void hideProgrammeKcalHero() {
        if (programmeKcalHero == null) {
            return;
        }
        programmeKcalHero.setVisible(false);
        programmeKcalHero.setManaged(false);
    }

    private void showProgrammeKcalHero() {
        if (programmeKcalHero == null) {
            return;
        }
        programmeKcalHero.setVisible(true);
        programmeKcalHero.setManaged(true);
    }

    /** Libellé lisible pour le bandeau « kcal / jour » (objectif programme). */
    private static String formatDailyKcal(Integer kcal) {
        if (kcal == null || kcal <= 0) {
            return "—";
        }
        String n = NumberFormat.getIntegerInstance(Locale.FRANCE).format(kcal);
        return n + " kcal / jour";
    }

    private static String formatKcalShort(Integer kcal) {
        if (kcal == null || kcal <= 0) {
            return "—";
        }
        return NumberFormat.getIntegerInstance(Locale.FRANCE).format(kcal) + " kcal";
    }

    private static Label hintLabel(String t) {
        Label l = new Label(t);
        l.setWrapText(true);
        l.getStyleClass().add("w-empty-hint");
        return l;
    }

    private void fillProgression(Optional<ObjectifRow> obj, WeeklyTaskSummary tasks) {
        boolean hasTasks = tasks.total() > 0;
        if (!hasTasks && obj.isEmpty()) {
            progressDashBlock.setVisible(false);
            progressDashBlock.setManaged(false);
            lblNoObjectif.setVisible(true);
            lblNoObjectif.setManaged(true);
            lblNoObjectif.setText("Pas encore d’objectif ni de tâches pour cette semaine — elles seront créées automatiquement lorsque votre fiche est complète.");
            applyProgressHeroPctStyle(-1);
            if (weekProgressChart != null) {
                weekProgressChart.getData().clear();
            }
            if (chartSection != null) {
                chartSection.setVisible(false);
                chartSection.setManaged(false);
            }
            return;
        }
        if (chartSection != null) {
            chartSection.setVisible(true);
            chartSection.setManaged(true);
        }
        progressDashBlock.setVisible(true);
        progressDashBlock.setManaged(true);
        lblNoObjectif.setVisible(false);
        lblNoObjectif.setManaged(false);

        int total;
        int fait;
        int encours;
        double taux;
        if (hasTasks) {
            total = tasks.total();
            fait = tasks.fait();
            encours = tasks.enCours();
            taux = tasks.tauxCompletionPct();
        } else {
            ObjectifRow o = obj.get();
            total = o.tachesPrevues();
            fait = o.tachesRealisees();
            encours = 0;
            taux = o.tauxRealisation();
        }

        String pct = String.format(Locale.FRANCE, "%.0f %%", taux);
        lblDashProgressPct.setText(pct);
        String detail;
        if (hasTasks) {
            detail = String.format(Locale.FRANCE, "%d / %d tâches · %d en cours", fait, total, encours);
        } else if (obj.isPresent()) {
            ObjectifRow o = obj.get();
            detail = String.format(Locale.FRANCE, "Semaine %d / %d · %s · %d / %d tâches",
                    o.weekNumber(), o.year(), nullToDash(o.statut()), fait, total);
        } else {
            detail = "Semaine courante · suivi des tâches";
        }
        lblDashProgressDetail.setText(detail);
        applyProgressHeroPctStyle(taux);

        double p = Math.min(1, Math.max(0, taux / 100.0));
        progressWeek.setProgress(p);
        applyProgressBarStyle(taux);
        lblEncouragement.setText(encouragementForWeek(obj, taux));

        if (weekProgressChart != null) {
            weekProgressChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Semaine");
            if (hasTasks) {
                series.getData().add(new XYChart.Data<>("Réalisées", (double) fait));
                series.getData().add(new XYChart.Data<>("Restant", (double) Math.max(0, total - fait)));
            } else {
                series.getData().add(new XYChart.Data<>("Réalisées", (double) fait));
                series.getData().add(new XYChart.Data<>("Prévues", (double) Math.max(0, total)));
            }
            weekProgressChart.getData().add(series);
        }
    }

    private void applyProgressBarStyle(double taux) {
        progressWeek.getStyleClass().removeIf(s ->
                s.equals("w-progress-track--high") || s.equals("w-progress-track--mid") || s.equals("w-progress-track--low"));
        if (taux >= 75) {
            progressWeek.getStyleClass().add("w-progress-track--high");
        } else if (taux >= 40) {
            progressWeek.getStyleClass().add("w-progress-track--mid");
        } else {
            progressWeek.getStyleClass().add("w-progress-track--low");
        }
    }

    /** Couleur du gros pourcentage « Progression & semaine » (vert / orange / rouge). */
    private void applyProgressHeroPctStyle(double taux) {
        if (lblDashProgressPct == null) {
            return;
        }
        lblDashProgressPct.getStyleClass().removeIf(c -> c.startsWith("pcd-progress-hero-pct--"));
        if (taux < 0 || Double.isNaN(taux)) {
            lblDashProgressPct.getStyleClass().add("pcd-progress-hero-pct--na");
            return;
        }
        if (taux >= 75) {
            lblDashProgressPct.getStyleClass().add("pcd-progress-hero-pct--good");
        } else if (taux >= 40) {
            lblDashProgressPct.getStyleClass().add("pcd-progress-hero-pct--warn");
        } else {
            lblDashProgressPct.getStyleClass().add("pcd-progress-hero-pct--bad");
        }
    }

    private String encouragementForWeek(Optional<ObjectifRow> obj, double taux) {
        if (obj.isPresent()) {
            ObjectifRow o = obj.get();
            if (o.messageEncadrant() != null && !o.messageEncadrant().isBlank()) {
                return o.messageEncadrant();
            }
            if (o.messageIa() != null && !o.messageIa().isBlank()) {
                return o.messageIa();
            }
        }
        if (taux >= 80) {
            return "Excellent rythme : votre semaine est très bien maîtrisée. Continuez sur cette lancée.";
        }
        if (taux >= 45) {
            return "Bon équilibre : quelques tâches supplémentaires suffisent pour une semaine très solide.";
        }
        if (taux >= 15) {
            return "Vous avancez : privilégiez la régularité — un petit créneau chaque jour bat une grosse séance unique.";
        }
        return "Objectif doux : commencez par une seule tâche « FAIT » aujourd’hui pour relancer la dynamique.";
    }

    private void fillTasks(List<TacheQuotidienne> taches) {
        tasksBox.getChildren().clear();
        if (taches.isEmpty()) {
            Label empty = hintLabel("Aucune tâche pour cette semaine — elles seront générées automatiquement dès que votre fiche est prête.");
            tasksBox.getChildren().add(empty);
            return;
        }
        for (TacheQuotidienne t : taches) {
            VBox card = new VBox(10);
            card.getStyleClass().add("w-task-card");

            HBox head = new HBox(12);
            head.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            String when = (t.date() != null ? t.date() + " · " : "") + nullToDash(t.jourSemaine());
            Label whenLb = new Label(when);
            whenLb.getStyleClass().add("w-task-when");
            Region grow = new Region();
            HBox.setHgrow(grow, Priority.ALWAYS);
            Button badge = new Button(labelEtat(t.etat()));
            badge.getStyleClass().addAll("w-chip-etat", badgeStyleClass(t.etat()));
            badge.setFocusTraversable(false);
            int tacheId = t.id();
            badge.setOnAction(ev -> {
                try {
                    service.cycleTacheEtat(tacheId, userId);
                    onRefresh.run();
                } catch (SQLException ex) {
                    alertWarn(ex.getMessage());
                }
            });
            head.getChildren().addAll(whenLb, grow, badge);

            Label desc = new Label(t.description());
            desc.setWrapText(true);
            desc.getStyleClass().add("w-task-desc");

            Label hint = new Label("Cliquez sur le badge pour faire avancer l’état : NON_FAIT → EN_COURS → FAIT.");
            hint.getStyleClass().add("w-task-hint");
            hint.setWrapText(true);

            card.getChildren().addAll(head, desc, hint);
            tasksBox.getChildren().add(card);
        }
    }

    private static String labelEtat(TacheEtat e) {
        return switch (e) {
            case FAIT -> "FAIT";
            case EN_COURS -> "EN_COURS";
            case NON_FAIT -> "NON_FAIT";
        };
    }

    private static String badgeStyleClass(TacheEtat e) {
        return switch (e) {
            case FAIT -> "w-chip-etat--fait";
            case EN_COURS -> "w-chip-etat--encours";
            case NON_FAIT -> "w-chip-etat--nonfait";
        };
    }

    private void openFicheModal(FicheSanteRow f) {
        if (ficheFormMount == null) {
            return;
        }
        FicheSanteFormData init = new FicheSanteFormData(
                f.genre(), f.age(), f.tailleCm(), f.poidsKg(), f.objectif(), f.niveauActivite());

        ficheFormMount.getChildren().clear();
        FicheSanteFormView form = new FicheSanteFormView(FicheSanteFormMode.EDITION, init, this::hideFicheModal);
        form.setOnSubmit(data -> {
            try {
                service.updateFicheAndRegenerateProgram(userId, data);
                hideFicheModal();
                onRefresh.run();
            } catch (Exception ex) {
                alertSql(ex);
            }
        });
        ficheFormMount.getChildren().add(form.getRoot());
        showFicheModal();
    }

    private static void alertSql(Exception ex) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Enregistrement");
        a.setHeaderText("Impossible d'enregistrer les données.");
        a.setContentText(ex.getMessage() != null ? ex.getMessage() : ex.toString());
        a.getDialogPane().setMinWidth(420);
        a.showAndWait();
    }

    private static void alertWarn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
