package org.example.planning.widgets;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.entities.ProgrammeGenereRow;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Contenu structuré d’un {@link ProgrammeGenereRow} pour cartes « Programme généré » (scroll interne).
 */
public final class StructuredProgrammePane {

    private StructuredProgrammePane() {
    }

    public static VBox build(ProgrammeGenereRow p) {
        VBox root = new VBox(18);
        root.getStyleClass().add("w-programme-inner");
        root.setPadding(new Insets(4, 0, 8, 0));

        if (p.caloriesParJour() != null && p.caloriesParJour() > 0) {
            VBox hero = new VBox(6);
            hero.getStyleClass().add("w-programme-target-hero");
            Label eyebrow = new Label("Objectif énergétique quotidien");
            eyebrow.getStyleClass().add("w-programme-target-hero__eyebrow");
            String n = NumberFormat.getIntegerInstance(Locale.FRANCE).format(p.caloriesParJour());
            Label value = new Label(n + " kcal / jour");
            value.getStyleClass().add("w-programme-target-hero__value");
            Label hint = new Label("Apport recommandé en moyenne par jour pour soutenir l’objectif et le plan nutritionnel.");
            hint.getStyleClass().add("w-programme-target-hero__hint");
            hint.setWrapText(true);
            hero.getChildren().addAll(eyebrow, value, hint);
            root.getChildren().add(hero);
        }

        HBox stats = new HBox(14);
        stats.getStyleClass().add("w-stat-row");
        VBox c1 = statChip("Calories / jour",
                p.caloriesParJour() != null ? p.caloriesParJour() + " kcal" : "—", true);
        VBox c2 = statChip("Objectif principal",
                p.objectifPrincipal() != null && !p.objectifPrincipal().isBlank() ? p.objectifPrincipal() : "—", false);
        HBox.setHgrow(c2, Priority.ALWAYS);
        stats.getChildren().addAll(c1, c2);
        root.getChildren().add(stats);

        String conseils = p.conseilsGeneraux() != null ? p.conseilsGeneraux().trim() : "";
        if (!conseils.isBlank()) {
            Label sec = new Label("CONSEILS GÉNÉRAUX");
            sec.getStyleClass().add("w-section-title");
            TextArea ta = new TextArea(conseils);
            ta.setEditable(false);
            ta.setWrapText(true);
            ta.setPrefRowCount(5);
            ta.setMaxHeight(160);
            ta.getStyleClass().add("w-conseils-text");
            VBox.setVgrow(ta, Priority.NEVER);
            root.getChildren().addAll(sec, ta);
        }

        Label secT = new Label("ENTRAÎNEMENT HEBDOMADAIRE");
        secT.getStyleClass().add("w-section-title");
        FlowPane train = new FlowPane();
        train.setHgap(14);
        train.setVgap(14);
        train.setPrefWrapLength(1080);
        train.getStyleClass().add("w-flow");
        ProgrammeJsonLayout.fillTraining(train, p.exercicesHebdomadairesJson());
        root.getChildren().addAll(secT, train);

        Label secN = new Label("PLAN NUTRITION");
        secN.getStyleClass().add("w-section-title");
        FlowPane meals = new FlowPane();
        meals.setHgap(14);
        meals.setVgap(14);
        meals.setPrefWrapLength(1080);
        meals.getStyleClass().add("w-flow");
        ProgrammeJsonLayout.fillNutrition(meals, p.plansRepasJson());
        root.getChildren().addAll(secN, meals);

        return root;
    }

    private static VBox statChip(String k, String v, boolean bigValue) {
        VBox box = new VBox(6);
        box.getStyleClass().add("w-stat-chip");
        HBox.setHgrow(box, bigValue ? Priority.NEVER : Priority.ALWAYS);
        Label lk = new Label(k);
        lk.getStyleClass().add("w-stat-chip__k");
        Label lv = new Label(v);
        lv.setWrapText(true);
        lv.getStyleClass().add(bigValue ? "w-stat-chip__v-big" : "w-stat-chip__v");
        box.getChildren().addAll(lk, lv);
        return box;
    }
}
