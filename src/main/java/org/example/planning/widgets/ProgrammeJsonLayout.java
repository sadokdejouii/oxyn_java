package org.example.planning.widgets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Rendu structuré des JSON programme (entraînement hebdo + repas) — même logique que le dashboard client.
 */
public final class ProgrammeJsonLayout {

    private ProgrammeJsonLayout() {
    }

    public static void fillTraining(FlowPane trainingFlow, String json) {
        trainingFlow.getChildren().clear();
        if (json == null || json.isBlank()) {
            trainingFlow.getChildren().add(hint("Aucune donnée d'entraînement."));
            return;
        }
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            String[] jours = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
            boolean any = false;
            for (String jour : jours) {
                if (!o.has(jour) || !o.get(jour).isJsonArray()) {
                    continue;
                }
                any = true;
                VBox dayCard = new VBox(8);
                dayCard.getStyleClass().add("w-day-card");
                Label jl = new Label(capitalize(jour));
                jl.getStyleClass().add("w-day-title");
                dayCard.getChildren().add(jl);
                JsonArray arr = o.getAsJsonArray(jour);
                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) {
                        continue;
                    }
                    JsonObject ex = el.getAsJsonObject();
                    String nom = str(ex, "nom");
                    if (nom.isEmpty()) {
                        nom = "Exercice";
                    }
                    Label name = new Label("• " + nom);
                    name.getStyleClass().add("w-ex-line");
                    name.setWrapText(true);
                    StringBuilder meta = new StringBuilder();
                    if (ex.has("duree") && !ex.get("duree").isJsonNull()) {
                        meta.append(ex.get("duree").getAsString());
                    }
                    if (ex.has("repetitions") && !ex.get("repetitions").isJsonNull()) {
                        if (meta.length() > 0) {
                            meta.append(" · ");
                        }
                        meta.append(ex.get("repetitions").getAsString());
                    }
                    if (ex.has("intensite") && !ex.get("intensite").isJsonNull()) {
                        if (meta.length() > 0) {
                            meta.append(" · ");
                        }
                        meta.append("Intensité : ").append(ex.get("intensite").getAsString());
                    }
                    int cal = ex.has("calories") && ex.get("calories").isJsonPrimitive() ? ex.get("calories").getAsInt() : 0;
                    if (cal > 0) {
                        if (meta.length() > 0) {
                            meta.append(" · ");
                        }
                        meta.append("~").append(cal).append(" kcal");
                    }
                    Label metaLb = new Label(meta.length() > 0 ? meta.toString() : "—");
                    metaLb.getStyleClass().add("w-ex-meta");
                    metaLb.setWrapText(true);
                    dayCard.getChildren().addAll(name, metaLb);
                }
                trainingFlow.getChildren().add(dayCard);
            }
            if (!any) {
                trainingFlow.getChildren().add(hint("Structure JSON sans jours reconnus."));
            }
        } catch (Exception e) {
            trainingFlow.getChildren().add(hint("Lecture du JSON entraînement impossible."));
        }
    }

    /**
     * Entraînement en timeline verticale (cartes par jour) — UI client motivante.
     */
    public static void fillTrainingTimeline(VBox trainingRoot, String json) {
        trainingRoot.getChildren().clear();
        if (json == null || json.isBlank()) {
            trainingRoot.getChildren().add(hint("Aucune donnée d'entraînement."));
            return;
        }
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            String[] jours = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
            boolean any = false;
            int idx = 0;
            for (String jour : jours) {
                if (!o.has(jour) || !o.get(jour).isJsonArray()) {
                    continue;
                }
                any = true;
                idx++;
                HBox row = new HBox(14);
                row.setAlignment(Pos.TOP_LEFT);
                row.getStyleClass().add("pcd-timeline-row");

                VBox rail = new VBox(0);
                rail.setMinWidth(36);
                rail.setAlignment(Pos.TOP_CENTER);
                Label step = new Label(String.valueOf(idx));
                step.setMinSize(30, 30);
                step.setMaxSize(30, 30);
                step.setAlignment(Pos.CENTER);
                step.getStyleClass().add("pcd-timeline-step");
                Region spacer = new Region();
                VBox.setVgrow(spacer, Priority.ALWAYS);
                spacer.getStyleClass().add("pcd-timeline-spacer");
                rail.getChildren().addAll(step, spacer);

                VBox dayCard = new VBox(12);
                dayCard.getStyleClass().add("pcd-timeline-day-card");
                dayCard.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(dayCard, Priority.ALWAYS);
                Label jl = new Label(capitalize(jour));
                jl.getStyleClass().add("pcd-timeline-day-title");
                dayCard.getChildren().add(jl);
                JsonArray arr = o.getAsJsonArray(jour);
                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) {
                        continue;
                    }
                    JsonObject ex = el.getAsJsonObject();
                    String nom = str(ex, "nom");
                    if (nom.isEmpty()) {
                        nom = "Exercice";
                    }
                    Label name = new Label(nom);
                    name.getStyleClass().add("pcd-timeline-ex-name");
                    name.setWrapText(true);
                    StringBuilder meta = new StringBuilder();
                    if (ex.has("duree") && !ex.get("duree").isJsonNull()) {
                        meta.append(ex.get("duree").getAsString());
                    }
                    if (ex.has("repetitions") && !ex.get("repetitions").isJsonNull()) {
                        if (meta.length() > 0) {
                            meta.append(" · ");
                        }
                        meta.append(ex.get("repetitions").getAsString());
                    }
                    if (ex.has("intensite") && !ex.get("intensite").isJsonNull()) {
                        if (meta.length() > 0) {
                            meta.append(" · ");
                        }
                        meta.append(ex.get("intensite").getAsString());
                    }
                    int cal = ex.has("calories") && ex.get("calories").isJsonPrimitive() ? ex.get("calories").getAsInt() : 0;
                    if (cal > 0) {
                        if (meta.length() > 0) {
                            meta.append(" · ");
                        }
                        meta.append("~").append(cal).append(" kcal");
                    }
                    Label metaLb = new Label(meta.length() > 0 ? meta.toString() : "—");
                    metaLb.getStyleClass().add("pcd-timeline-ex-meta");
                    metaLb.setWrapText(true);
                    VBox exBox = new VBox(4);
                    exBox.getStyleClass().add("pcd-timeline-ex-block");
                    exBox.getChildren().addAll(name, metaLb);
                    dayCard.getChildren().add(exBox);
                }
                row.getChildren().addAll(rail, dayCard);
                trainingRoot.getChildren().add(row);
            }
            if (!any) {
                trainingRoot.getChildren().add(hint("Structure JSON sans jours reconnus."));
            }
        } catch (Exception e) {
            trainingRoot.getChildren().add(hint("Lecture du JSON entraînement impossible."));
        }
    }

    /** Nutrition en colonne (cartes repas empilées) — UI client. */
    public static void fillNutritionVertical(VBox nutritionRoot, String json) {
        nutritionRoot.getChildren().clear();
        if (json == null || json.isBlank()) {
            nutritionRoot.getChildren().add(hint("Aucun plan repas."));
            return;
        }
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            String[][] keys = {
                    {"petit_dejeuner", "Petit-déjeuner"},
                    {"dejeuner", "Déjeuner"},
                    {"collation", "Collation"},
                    {"diner", "Dîner"}
            };
            boolean any = false;
            for (String[] kv : keys) {
                if (!o.has(kv[0]) || !o.get(kv[0]).isJsonObject()) {
                    continue;
                }
                any = true;
                JsonObject bloc = o.getAsJsonObject(kv[0]);
                int cal = bloc.has("calories") ? bloc.get("calories").getAsInt() : 0;
                VBox meal = new VBox(10);
                meal.getStyleClass().add("pcd-meal-card-v");
                Label head = new Label(kv[1] + " · " + cal + " kcal cible");
                head.getStyleClass().add("pcd-meal-head-v");
                meal.getChildren().add(head);
                if (bloc.has("exemples") && bloc.get("exemples").isJsonArray()) {
                    for (JsonElement exEl : bloc.getAsJsonArray("exemples")) {
                        if (!exEl.isJsonObject()) {
                            continue;
                        }
                        JsonObject ex = exEl.getAsJsonObject();
                        String nom = str(ex, "nom");
                        Label ln = new Label(nom.isEmpty() ? "—" : nom);
                        ln.getStyleClass().add("pcd-meal-nom-v");
                        ln.setWrapText(true);
                        meal.getChildren().add(ln);
                        if (ex.has("ingredients") && ex.get("ingredients").isJsonArray()) {
                            StringBuilder sb = new StringBuilder();
                            for (JsonElement ing : ex.getAsJsonArray("ingredients")) {
                                if (sb.length() > 0) {
                                    sb.append(", ");
                                }
                                sb.append(ing.getAsString());
                            }
                            Label li = new Label(sb.toString());
                            li.setWrapText(true);
                            li.getStyleClass().add("pcd-meal-ing-v");
                            meal.getChildren().add(li);
                        }
                    }
                }
                nutritionRoot.getChildren().add(meal);
            }
            if (!any) {
                nutritionRoot.getChildren().add(hint("Aucun repas structuré dans le JSON."));
            }
        } catch (Exception e) {
            nutritionRoot.getChildren().add(hint("Lecture du JSON repas impossible."));
        }
    }

    public static void fillNutrition(FlowPane nutritionFlow, String json) {
        nutritionFlow.getChildren().clear();
        if (json == null || json.isBlank()) {
            nutritionFlow.getChildren().add(hint("Aucun plan repas."));
            return;
        }
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            String[][] keys = {
                    {"petit_dejeuner", "Petit-déjeuner"},
                    {"dejeuner", "Déjeuner"},
                    {"collation", "Collation"},
                    {"diner", "Dîner"}
            };
            boolean any = false;
            for (String[] kv : keys) {
                if (!o.has(kv[0]) || !o.get(kv[0]).isJsonObject()) {
                    continue;
                }
                any = true;
                JsonObject bloc = o.getAsJsonObject(kv[0]);
                int cal = bloc.has("calories") ? bloc.get("calories").getAsInt() : 0;
                VBox meal = new VBox(8);
                meal.getStyleClass().add("w-meal-card");
                Label head = new Label(kv[1] + " · " + cal + " kcal cible");
                head.getStyleClass().add("w-meal-head");
                meal.getChildren().add(head);
                if (bloc.has("exemples") && bloc.get("exemples").isJsonArray()) {
                    for (JsonElement exEl : bloc.getAsJsonArray("exemples")) {
                        if (!exEl.isJsonObject()) {
                            continue;
                        }
                        JsonObject ex = exEl.getAsJsonObject();
                        String nom = str(ex, "nom");
                        Label ln = new Label(nom.isEmpty() ? "—" : nom);
                        ln.getStyleClass().add("w-meal-nom");
                        ln.setWrapText(true);
                        meal.getChildren().add(ln);
                        if (ex.has("ingredients") && ex.get("ingredients").isJsonArray()) {
                            StringBuilder sb = new StringBuilder();
                            for (JsonElement ing : ex.getAsJsonArray("ingredients")) {
                                if (sb.length() > 0) {
                                    sb.append(", ");
                                }
                                sb.append(ing.getAsString());
                            }
                            Label li = new Label(sb.toString());
                            li.setWrapText(true);
                            li.getStyleClass().add("w-meal-ing");
                            meal.getChildren().add(li);
                        }
                    }
                }
                nutritionFlow.getChildren().add(meal);
            }
            if (!any) {
                nutritionFlow.getChildren().add(hint("Aucun repas structuré dans le JSON."));
            }
        } catch (Exception e) {
            nutritionFlow.getChildren().add(hint("Lecture du JSON repas impossible."));
        }
    }

    private static Label hint(String t) {
        Label l = new Label(t);
        l.setWrapText(true);
        l.getStyleClass().add("w-empty-hint");
        return l;
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
