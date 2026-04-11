package org.example.planning.form;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.model.planning.FicheSanteFormData;

import java.util.List;
import java.util.function.Consumer;

/**
 * Vue JavaFX du formulaire fiche santé (création / édition). {@code user_id} et horodatages : hors UI.
 */
public final class FicheSanteFormView {

    private final VBox root;
    private final ComboBox<String> genreCombo = new ComboBox<>();
    private final Spinner<Integer> ageSpinner = new Spinner<>();
    private final Spinner<Integer> tailleSpinner = new Spinner<>();
    private final Spinner<Double> poidsSpinner = new Spinner<>();
    private final ComboBox<String> objectifCombo = new ComboBox<>();
    private final ComboBox<String> niveauCombo = new ComboBox<>();
    private final VBox errorBox = new VBox(6);
    private final Label errorTitle = new Label();
    private Consumer<FicheSanteFormData> onSubmit;

    public FicheSanteFormView(FicheSanteFormMode mode, FicheSanteFormData initial) {
        this(mode, initial, null);
    }

    /**
     * @param onCancel si non null, affiche un bouton « Annuler » (ex. fermeture dialogue).
     */
    public FicheSanteFormView(FicheSanteFormMode mode, FicheSanteFormData initial, Runnable onCancel) {
        this.root = new VBox(18);
        root.getStyleClass().add("planning-fiche-form-root");
        root.setPadding(new Insets(4, 0, 8, 0));

        Label headline = new Label(mode == FicheSanteFormMode.CREATION
                ? "Création de votre fiche santé"
                : "Modifier votre fiche santé");
        headline.getStyleClass().add("planning-fiche-form-headline");

        Label sub = new Label(mode == FicheSanteFormMode.CREATION
                ? "Les champs marqués d’une astérisque (*) sont obligatoires. Après enregistrement, votre programme sera généré automatiquement."
                : "La validation régénère votre programme personnalisé à partir de ces données.");
        sub.setWrapText(true);
        sub.getStyleClass().add("planning-fiche-form-sub");

        setupGenre(initial.genre());
        setupAge(initial.age());
        setupTaille(initial.tailleCm());
        setupPoids(initial.poidsKg());
        setupObjectif(initial.objectif());
        setupNiveau(initial.niveauActivite());

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(14);
        grid.getStyleClass().add("planning-fiche-form-grid");

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(160);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setMinWidth(220);
        grid.getColumnConstraints().addAll(c0, c1);

        int r = 0;
        grid.add(rowLabel("Genre *"), 0, r);
        grid.add(wrapField(genreCombo), 1, r++);
        grid.add(rowLabel("Âge *"), 0, r);
        grid.add(wrapField(ageSpinner), 1, r++);
        grid.add(rowLabel("Taille (cm) *"), 0, r);
        grid.add(wrapField(tailleSpinner), 1, r++);
        grid.add(rowLabel("Poids (kg) *"), 0, r);
        grid.add(wrapField(poidsSpinner), 1, r++);
        grid.add(rowLabel("Objectif *"), 0, r);
        grid.add(wrapField(objectifCombo), 1, r++);
        grid.add(rowLabel("Niveau d’activité *"), 0, r);
        grid.add(wrapField(niveauCombo), 1, r++);

        errorBox.setVisible(false);
        errorBox.setManaged(false);
        errorBox.getStyleClass().add("planning-form-error-box");
        errorTitle.getStyleClass().add("planning-form-error-title");
        errorTitle.setText("Veuillez corriger les points suivants :");
        errorTitle.setVisible(false);
        errorTitle.setManaged(false);

        Button submit = new Button(mode == FicheSanteFormMode.CREATION ? "Enregistrer" : "Mettre à jour");
        submit.getStyleClass().addAll("planning-primary-btn", "planning-fiche-submit");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(e -> handleSubmit());

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getChildren().add(submit);
        if (onCancel != null) {
            Button cancel = new Button("Annuler");
            cancel.getStyleClass().add("planning-fiche-cancel");
            cancel.setOnAction(e -> onCancel.run());
            actions.getChildren().add(cancel);
        }
        HBox.setHgrow(submit, Priority.ALWAYS);

        root.getChildren().addAll(headline, sub, grid, errorBox, actions);
    }

    public Region getRoot() {
        return root;
    }

    public void setOnSubmit(Consumer<FicheSanteFormData> handler) {
        this.onSubmit = handler;
    }

    private void handleSubmit() {
        clearErrors();
        FicheSanteFormData draft = readFromControls();
        FicheSanteFormValidator.Result res = FicheSanteFormValidator.validate(draft);
        if (!res.ok()) {
            showErrors(res.errors());
            return;
        }
        if (onSubmit != null) {
            onSubmit.accept(res.data());
        }
    }

    private FicheSanteFormData readFromControls() {
        return new FicheSanteFormData(
                genreCombo.getValue(),
                ageSpinner.getValue(),
                tailleSpinner.getValue(),
                poidsSpinner.getValue(),
                objectifCombo.getValue(),
                niveauCombo.getValue()
        );
    }

    private void showErrors(List<String> errors) {
        errorBox.getChildren().clear();
        errorBox.getChildren().add(errorTitle);
        errorTitle.setVisible(true);
        errorTitle.setManaged(true);
        for (String msg : errors) {
            Label line = new Label("• " + msg);
            line.setWrapText(true);
            line.getStyleClass().add("planning-form-error-line");
            errorBox.getChildren().add(line);
        }
        errorBox.setVisible(true);
        errorBox.setManaged(true);
    }

    private void clearErrors() {
        errorBox.getChildren().clear();
        errorBox.setVisible(false);
        errorBox.setManaged(false);
        errorTitle.setVisible(false);
        errorTitle.setManaged(false);
    }

    private static Label rowLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("planning-fiche-form-label");
        l.setMinHeight(Region.USE_PREF_SIZE);
        l.setAlignment(Pos.CENTER_LEFT);
        return l;
    }

    private static HBox wrapField(Region field) {
        HBox h = new HBox(field);
        h.setAlignment(Pos.CENTER_LEFT);
        field.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(field, Priority.ALWAYS);
        return h;
    }

    private void setupGenre(String initialGenre) {
        genreCombo.getItems().addAll("M", "F");
        genreCombo.getStyleClass().add("planning-fiche-combo");
        genreCombo.setMaxWidth(Double.MAX_VALUE);
        String g = initialGenre != null ? initialGenre.trim().toUpperCase() : "M";
        if (!"M".equals(g) && !"F".equals(g)) {
            g = "M";
        }
        genreCombo.setValue(g);
    }

    private void setupAge(Integer initialAge) {
        int v = initialAge != null ? initialAge : 30;
        v = Math.max(12, Math.min(110, v));
        ageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(12, 110, v, 1));
        ageSpinner.setEditable(true);
        ageSpinner.getStyleClass().add("planning-fiche-spinner");
        ageSpinner.setMaxWidth(Double.MAX_VALUE);
    }

    private void setupTaille(Integer initialCm) {
        int v = initialCm != null ? initialCm : 170;
        v = Math.max(120, Math.min(230, v));
        tailleSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(120, 230, v, 1));
        tailleSpinner.setEditable(true);
        tailleSpinner.getStyleClass().add("planning-fiche-spinner");
        tailleSpinner.setMaxWidth(Double.MAX_VALUE);
    }

    private void setupPoids(Double initialKg) {
        double v = initialKg != null ? initialKg : 70.0;
        v = Math.max(35.0, Math.min(220.0, v));
        poidsSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(35.0, 220.0, v, 0.5));
        poidsSpinner.setEditable(true);
        poidsSpinner.getStyleClass().add("planning-fiche-spinner");
        poidsSpinner.setMaxWidth(Double.MAX_VALUE);
    }

    private void setupObjectif(String code) {
        objectifCombo.getItems().addAll("perte_poids", "gain_poids", "devenir_muscle", "maintien");
        objectifCombo.setCellFactory(lv -> objectifCell());
        objectifCombo.setButtonCell(objectifCell());
        objectifCombo.getStyleClass().add("planning-fiche-combo");
        objectifCombo.setMaxWidth(Double.MAX_VALUE);
        String c = (code != null && objectifCombo.getItems().contains(code)) ? code : "perte_poids";
        objectifCombo.setValue(c);
    }

    private void setupNiveau(String codeFromDb) {
        niveauCombo.getItems().addAll("sedentaire", "peu_actif", "moderement_actif", "tres_actif");
        niveauCombo.setCellFactory(lv -> niveauCell());
        niveauCombo.setButtonCell(niveauCell());
        niveauCombo.getStyleClass().add("planning-fiche-combo");
        niveauCombo.setMaxWidth(Double.MAX_VALUE);
        String c = normalizeNiveauFromDb(codeFromDb);
        if (!niveauCombo.getItems().contains(c)) {
            c = "peu_actif";
        }
        niveauCombo.setValue(c);
    }

    private static ListCell<String> objectifCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(labelObjectif(item));
                }
            }
        };
    }

    private static ListCell<String> niveauCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(labelNiveau(item));
                }
            }
        };
    }

    private static String labelObjectif(String code) {
        return switch (code) {
            case "perte_poids" -> "perte_poids — Perte de poids";
            case "gain_poids" -> "gain_poids — Prise de masse";
            case "devenir_muscle" -> "devenir_muscle — Musculation / tonification";
            case "maintien" -> "maintien — Maintien";
            default -> code;
        };
    }

    private static String labelNiveau(String code) {
        return switch (code) {
            case "sedentaire" -> "sedentaire — Sédentaire";
            case "peu_actif" -> "peu_actif — Peu actif";
            case "moderement_actif" -> "moderement_actif — Modérément actif";
            case "tres_actif" -> "tres_actif — Très actif";
            default -> code;
        };
    }

    /**
     * Valeurs historiques base / Symfony → valeurs formulaire.
     */
    public static String normalizeNiveauFromDb(String db) {
        if (db == null || db.isBlank()) {
            return "peu_actif";
        }
        String k = db.trim().toLowerCase();
        return switch (k) {
            case "sedentaire", "sédentaire" -> "sedentaire";
            case "peu_actif" -> "peu_actif";
            case "modere", "modéré", "moderement_actif", "modérément_actif" -> "moderement_actif";
            case "actif" -> "moderement_actif";
            case "tres_actif", "très_actif" -> "tres_actif";
            default -> {
                if (List.of("sedentaire", "peu_actif", "moderement_actif", "tres_actif").contains(k)) {
                    yield k;
                }
                yield "peu_actif";
            }
        };
    }

    public void applyInitial(FicheSanteFormData data) {
        if (data.genre() != null && genreCombo.getItems().contains(data.genre())) {
            genreCombo.setValue(data.genre());
        }
        if (data.age() != null) {
            ageSpinner.getValueFactory().setValue(clamp(data.age(), 12, 110));
        }
        if (data.tailleCm() != null) {
            tailleSpinner.getValueFactory().setValue(clamp(data.tailleCm(), 120, 230));
        }
        if (data.poidsKg() != null) {
            double p = Math.max(35, Math.min(220, data.poidsKg()));
            poidsSpinner.getValueFactory().setValue(p);
        }
        if (data.objectif() != null && objectifCombo.getItems().contains(data.objectif())) {
            objectifCombo.setValue(data.objectif());
        }
        if (data.niveauActivite() != null) {
            String n = normalizeNiveauFromDb(data.niveauActivite());
            if (niveauCombo.getItems().contains(n)) {
                niveauCombo.setValue(n);
            }
        }
        clearErrors();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
