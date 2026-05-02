package org.example.planning.form;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.model.planning.FicheSanteFormData;
import org.example.ui.forms.FicheSanteFormMode;
import org.example.ui.forms.FicheSanteFormValidator;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Formulaire fiche santé — présentation type produit SaaS : hero, jalons visuels, 
 * cartes thématiques, IMC live. Adapté pour toute application nécessitant 
 * les informations de santé d'un utilisateur (fitness, bien-être, suivi médical, etc.).
 */
public final class FicheSanteFormView {

    private final VBox root;
    private final ToggleGroup genreGroup = new ToggleGroup();
    private final ToggleButton genreMale = new ToggleButton("Homme");
    private final ToggleButton genreFemale = new ToggleButton("Femme");
    private final Spinner<Integer> ageSpinner = new Spinner<>();
    private final Spinner<Integer> tailleSpinner = new Spinner<>();
    private final Spinner<Double> poidsSpinner = new Spinner<>();
    private final ComboBox<String> objectifCombo = new ComboBox<>();
    private final ComboBox<String> niveauCombo = new ComboBox<>();
    private final VBox errorBox = new VBox(6);
    private final Label errorTitle = new Label();
    private final Label bmiValue = new Label("—");
    private final Label bmiHint = new Label("Indice calculé à partir de votre taille et poids.");
    private Consumer<FicheSanteFormData> onSubmit;

    private HBox ageControlWrap;
    private final HBox ageMessageRow = new HBox(8);
    private final FontIcon ageFieldIcon = new FontIcon();
    private final Label ageFieldMessage = new Label();

    private HBox tailleControlWrap;
    private final HBox tailleMessageRow = new HBox(8);
    private final FontIcon tailleFieldIcon = new FontIcon();
    private final Label tailleFieldMessage = new Label();

    private HBox poidsControlWrap;
    private final HBox poidsMessageRow = new HBox(8);
    private final FontIcon poidsFieldIcon = new FontIcon();
    private final Label poidsFieldMessage = new Label();

    private Button submitButton;

    public FicheSanteFormView(FicheSanteFormMode mode, FicheSanteFormData initial) {
        this(mode, initial, null);
    }

    public FicheSanteFormView(FicheSanteFormMode mode, FicheSanteFormData initial, Runnable onCancel) {
        this.root = new VBox(0);
        root.getStyleClass().add("fs-form-shell");

        setupGenreToggles(initial.genre());
        setupAge(initial.age());
        setupTaille(initial.tailleCm());
        setupPoids(initial.poidsKg());
        setupObjectif(initial.objectif());
        setupNiveau(initial.niveauActivite());

        VBox hero = buildHero(mode);
        HBox steps = buildStepRail();
        HBox bmiStrip = buildBmiStrip();
        wireBmiUpdates();

        Region profileCard = buildProfileCard();
        Region bodyCard = buildBodyCard();
        HBox topRow = new HBox(18, profileCard, bodyCard);
        topRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(profileCard, Priority.ALWAYS);
        HBox.setHgrow(bodyCard, Priority.ALWAYS);
        profileCard.setMinWidth(280);
        bodyCard.setMinWidth(280);

        Region goalsCard = buildGoalsCard();

        errorBox.setVisible(false);
        errorBox.setManaged(false);
        errorBox.getStyleClass().add("planning-form-error-box");
        errorTitle.getStyleClass().add("planning-form-error-title");
        errorTitle.setText("Veuillez corriger les points suivants :");
        errorTitle.setVisible(false);
        errorTitle.setManaged(false);

        submitButton = new Button(mode == FicheSanteFormMode.CREATION ? "Enregistrer et générer mon programme" : "Mettre à jour et régénérer");
        submitButton.getStyleClass().addAll("planning-primary-btn", "planning-fiche-submit", "fs-form-submit");
        submitButton.setMaxWidth(Double.MAX_VALUE);
        submitButton.setOnAction(e -> handleSubmit());

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(8, 0, 0, 0));
        actions.getStyleClass().add("fs-form-actions");
        actions.getChildren().add(submitButton);
        if (onCancel != null) {
            Button cancel = new Button("Annuler");
            cancel.getStyleClass().add("planning-fiche-cancel");
            cancel.setOnAction(e -> onCancel.run());
            actions.getChildren().add(cancel);
        }
        HBox.setHgrow(submitButton, Priority.ALWAYS);

        VBox bodyStack = new VBox(20);
        bodyStack.setPadding(new Insets(0, 0, 8, 0));
        bodyStack.getStyleClass().add("fs-form-body-stack");
        bodyStack.getChildren().addAll(steps, bmiStrip, topRow, goalsCard, errorBox, actions);

        root.getChildren().addAll(hero, bodyStack);

        wireMetricValidationListeners();
        refreshMetricsValidation();
    }

    private VBox buildHero(FicheSanteFormMode mode) {
        VBox hero = new VBox(10);
        hero.getStyleClass().add("fs-form-hero");
        hero.setPadding(new Insets(22, 26, 22, 26));

        HBox heroTop = new HBox(16);
        heroTop.setAlignment(Pos.CENTER_LEFT);

        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().add("fs-form-hero-icon-wrap");
        FontIcon pulse = new FontIcon("fas-heartbeat");
        pulse.setIconSize(22);
        pulse.getStyleClass().add("fs-form-hero-icon");
        iconWrap.getChildren().add(pulse);

        VBox titles = new VBox(6);
        HBox.setHgrow(titles, Priority.ALWAYS);
        Label kicker = new Label(mode == FicheSanteFormMode.CREATION ? "Onboarding santé" : "Mise à jour profil");
        kicker.getStyleClass().add("fs-form-hero-kicker");
        Label headline = new Label(mode == FicheSanteFormMode.CREATION
                ? "Construisez votre fiche santé"
                : "Affinez votre fiche santé");
        headline.getStyleClass().add("fs-form-hero-title");
        headline.setWrapText(true);
        Label sub = new Label(mode == FicheSanteFormMode.CREATION
                ? "Quelques informations suffisent : nous calculons votre IMC, adaptons vos objectifs et générons un programme sur mesure à l'enregistrement."
                : "Les changements régénèrent votre programme personnalisé à partir de ces données.");
        sub.setWrapText(true);
        sub.getStyleClass().add("fs-form-hero-sub");
        titles.getChildren().addAll(kicker, headline, sub);

        Label chip = new Label("≈ 2 min");
        chip.getStyleClass().add("fs-form-hero-chip");
        heroTop.getChildren().addAll(iconWrap, titles, chip);

        hero.getChildren().add(heroTop);
        return hero;
    }

    private HBox buildStepRail() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("fs-form-steps");
        String[] labels = {"Profil", "Mesures", "Objectifs"};
        for (int i = 0; i < labels.length; i++) {
            HBox step = new HBox(8);
            step.setAlignment(Pos.CENTER_LEFT);
            step.getStyleClass().add("fs-form-step");
            Label num = new Label(String.valueOf(i + 1));
            num.getStyleClass().add("fs-form-step-num");
            Label tx = new Label(labels[i]);
            tx.getStyleClass().add("fs-form-step-label");
            step.getChildren().addAll(num, tx);
            row.getChildren().add(step);
            if (i < labels.length - 1) {
                Region dash = new Region();
                dash.getStyleClass().add("fs-form-step-connector");
                dash.setMinWidth(24);
                dash.setPrefHeight(2);
                HBox.setHgrow(dash, Priority.SOMETIMES);
                row.getChildren().add(dash);
            }
        }
        return row;
    }

    private HBox buildBmiStrip() {
        HBox strip = new HBox(16);
        strip.setAlignment(Pos.CENTER_LEFT);
        strip.getStyleClass().add("fs-form-bmi-strip");
        strip.setPadding(new Insets(14, 18, 14, 18));

        VBox left = new VBox(4);
        Label t = new Label("IMC estimé");
        t.getStyleClass().add("fs-form-bmi-title");
        bmiHint.getStyleClass().add("fs-form-bmi-hint");
        bmiHint.setWrapText(true);
        left.getChildren().addAll(t, bmiHint);
        HBox.setHgrow(left, Priority.ALWAYS);

        VBox right = new VBox(2);
        right.setAlignment(Pos.CENTER_RIGHT);
        bmiValue.getStyleClass().add("fs-form-bmi-value");
        right.getChildren().add(bmiValue);

        strip.getChildren().addAll(left, right);
        refreshBmi();
        return strip;
    }

    private void wireBmiUpdates() {
        tailleSpinner.valueProperty().addListener((o, a, b) -> refreshBmi());
        poidsSpinner.valueProperty().addListener((o, a, b) -> refreshBmi());
    }

    private void refreshBmi() {
        try {
            int cm = tailleSpinner.getValue();
            double kg = poidsSpinner.getValue();
            if (cm <= 0) {
                bmiValue.setText("—");
                return;
            }
            double m = cm / 100.0;
            double bmi = kg / (m * m);
            bmiValue.setText(String.format("%.1f", bmi));
            bmiHint.setText(bmiCategory(bmi) + " · mis à jour automatiquement.");
        } catch (Exception e) {
            bmiValue.setText("—");
        }
    }

    private static String bmiCategory(double bmi) {
        if (bmi < 18.5) {
            return "Profil maigreur";
        }
        if (bmi < 25) {
            return "Corpulence normale";
        }
        if (bmi < 30) {
            return "Surpoids modéré";
        }
        return "Obésité — suivi renforcé recommandé";
    }

    private Region buildProfileCard() {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("fs-form-card");
        card.setPadding(new Insets(18, 20, 20, 20));

        card.getChildren().add(cardHeader("fas-id-card", "Identité", "Genre et tranche d'âge"));

        Label gLab = fieldCaption("Genre");
        genreMale.setToggleGroup(genreGroup);
        genreFemale.setToggleGroup(genreGroup);
        genreMale.getStyleClass().addAll("fs-seg", "fs-seg-left");
        genreFemale.getStyleClass().addAll("fs-seg", "fs-seg-right");
        HBox seg = new HBox(0, genreMale, genreFemale);
        seg.setAlignment(Pos.CENTER_LEFT);
        seg.getStyleClass().add("fs-seg-row");

        Label aLab = fieldCaption("Âge");
        styleSpinner(ageSpinner);
        ageSpinner.setMaxWidth(Double.MAX_VALUE);

        ageControlWrap = new HBox();
        ageControlWrap.setAlignment(Pos.CENTER_LEFT);
        ageControlWrap.getStyleClass().add("fs-field-control-wrap");
        HBox.setHgrow(ageSpinner, Priority.ALWAYS);
        ageControlWrap.getChildren().add(ageSpinner);

        ageFieldIcon.setIconSize(12);
        ageFieldMessage.getStyleClass().add("fs-field-message");
        ageMessageRow.setAlignment(Pos.CENTER_LEFT);
        ageMessageRow.getChildren().addAll(ageFieldIcon, ageFieldMessage);
        ageMessageRow.getStyleClass().add("fs-field-message-row");
        ageMessageRow.setVisible(false);
        ageMessageRow.setManaged(false);

        card.getChildren().addAll(gLab, seg, aLab, ageControlWrap, ageMessageRow);
        return card;
    }

    private Region buildBodyCard() {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("fs-form-card");
        card.setPadding(new Insets(18, 20, 20, 20));

        card.getChildren().add(cardHeader("fas-ruler-combined", "Anthropométrie", "Taille & poids pour calibrer charge et nutrition"));

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(12);
        ColumnConstraints half = new ColumnConstraints();
        half.setPercentWidth(50);
        half.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(half, half);

        VBox c0 = buildMetricColumn("Taille", "cm", tailleSpinner, tailleMessageRow, tailleFieldIcon, tailleFieldMessage);
        VBox c1 = buildMetricColumn("Poids", "kg", poidsSpinner, poidsMessageRow, poidsFieldIcon, poidsFieldMessage);
        g.add(c0, 0, 0);
        g.add(c1, 1, 0);

        card.getChildren().add(g);
        return card;
    }

    private VBox buildMetricColumn(String title, String unit, Spinner<?> spinner,
                                   HBox messageRow, FontIcon icon, Label messageLabel) {
        VBox v = new VBox(6);
        HBox cap = new HBox(6);
        cap.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(title);
        t.getStyleClass().add("fs-form-field-title");
        Label u = new Label(unit);
        u.getStyleClass().add("fs-form-field-unit");
        cap.getChildren().addAll(t, u);

        HBox wrap = new HBox();
        wrap.setAlignment(Pos.CENTER_LEFT);
        wrap.getStyleClass().add("fs-field-control-wrap");
        styleSpinner(spinner);
        spinner.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(spinner, Priority.ALWAYS);
        wrap.getChildren().add(spinner);
        if ("Taille".equalsIgnoreCase(title)) {
            tailleControlWrap = wrap;
        } else {
            poidsControlWrap = wrap;
        }

        icon.setIconSize(12);
        messageLabel.getStyleClass().add("fs-field-message");
        messageRow.setAlignment(Pos.CENTER_LEFT);
        messageRow.getChildren().setAll(icon, messageLabel);
        messageRow.getStyleClass().add("fs-field-message-row");
        messageRow.setVisible(false);
        messageRow.setManaged(false);

        v.getChildren().addAll(cap, wrap, messageRow);
        return v;
    }

    private Region buildGoalsCard() {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("fs-form-card", "fs-form-card--wide");
        card.setPadding(new Insets(18, 20, 20, 20));

        card.getChildren().add(cardHeader("fas-bullseye", "Objectifs & rythme", "Nous adaptons volume d'entraînement et déficit calorique"));

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(14);
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(50);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        c0.setHgrow(Priority.ALWAYS);
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        VBox oBox = new VBox(6);
        oBox.getChildren().addAll(fieldCaption("Objectif principal"), wrapFull(objectifCombo));
        VBox nBox = new VBox(6);
        nBox.getChildren().addAll(fieldCaption("Niveau d'activité"), wrapFull(niveauCombo));
        grid.add(oBox, 0, 0);
        grid.add(nBox, 1, 0);

        card.getChildren().add(grid);
        return card;
    }

    private static HBox cardHeader(String iconLiteral, String title, String subtitle) {
        HBox h = new HBox(12);
        h.setAlignment(Pos.TOP_LEFT);
        FontIcon ic = new FontIcon(iconLiteral);
        ic.setIconSize(18);
        ic.getStyleClass().add("fs-form-card-icon");
        VBox tx = new VBox(4);
        HBox.setHgrow(tx, Priority.ALWAYS);
        Label t = new Label(title);
        t.getStyleClass().add("fs-form-card-heading");
        Label s = new Label(subtitle);
        s.setWrapText(true);
        s.getStyleClass().add("fs-form-card-sub");
        tx.getChildren().addAll(t, s);
        h.getChildren().addAll(ic, tx);
        return h;
    }

    private static Label fieldCaption(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("fs-form-field-caption");
        return l;
    }

    private static void styleSpinner(Spinner<?> sp) {
        sp.getStyleClass().add("planning-fiche-spinner");
        sp.setMaxWidth(Double.MAX_VALUE);
    }

    private static Region wrapFull(Region field) {
        HBox h = new HBox(field);
        h.setAlignment(Pos.CENTER_LEFT);
        field.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(field, Priority.ALWAYS);
        return h;
    }

    public Region getRoot() {
        return root;
    }

    public void setOnSubmit(Consumer<FicheSanteFormData> handler) {
        this.onSubmit = handler;
    }

    private void handleSubmit() {
        commitQuiet(ageSpinner);
        commitQuiet(tailleSpinner);
        commitQuiet(poidsSpinner);
        refreshMetricsValidation();

        Optional<String> ageErr = FormValidator.validateAge(ageSpinner);
        Optional<String> tailleErr = FormValidator.validateHeight(tailleSpinner);
        Optional<String> poidsErr = FormValidator.validateWeight(poidsSpinner);
        if (ageErr.isPresent()) {
            ageSpinner.requestFocus();
            return;
        }
        if (tailleErr.isPresent()) {
            tailleSpinner.requestFocus();
            return;
        }
        if (poidsErr.isPresent()) {
            poidsSpinner.requestFocus();
            return;
        }

        clearErrors();
        FicheSanteFormData draft = readFromControls();
        FicheSanteFormValidator.Result res = FicheSanteFormValidator.validate(draft);
        if (!res.ok()) {
            List<String> filtered = filterBackendPhysiqueErrors(res.errors());
            if (!filtered.isEmpty()) {
                showErrors(filtered);
            }
            return;
        }
        if (onSubmit != null) {
            onSubmit.accept(res.data());
        }
    }

    private FicheSanteFormData readFromControls() {
        commitQuiet(ageSpinner);
        commitQuiet(tailleSpinner);
        commitQuiet(poidsSpinner);
        String g = genreMale.isSelected() ? "M" : "F";
        return new FicheSanteFormData(
                g,
                ageSpinner.getValue(),
                tailleSpinner.getValue(),
                poidsSpinner.getValue(),
                objectifCombo.getValue(),
                niveauCombo.getValue()
        );
    }

    private static void commitQuiet(Spinner<?> spinner) {
        if (!spinner.isEditable()) {
            return;
        }
        try {
            spinner.commitValue();
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void wireMetricValidationListeners() {
        Runnable r = this::refreshMetricsValidation;
        Runnable onBlur = () -> {
            commitQuiet(ageSpinner);
            commitQuiet(tailleSpinner);
            commitQuiet(poidsSpinner);
            r.run();
        };
        ageSpinner.valueProperty().addListener((o, a, b) -> r.run());
        ageSpinner.getEditor().textProperty().addListener((o, a, b) -> r.run());
        ageSpinner.focusedProperty().addListener((o, ov, nv) -> {
            if (!nv) {
                onBlur.run();
            }
        });
        tailleSpinner.valueProperty().addListener((o, a, b) -> r.run());
        tailleSpinner.getEditor().textProperty().addListener((o, a, b) -> r.run());
        tailleSpinner.focusedProperty().addListener((o, ov, nv) -> {
            if (!nv) {
                onBlur.run();
            }
        });
        poidsSpinner.valueProperty().addListener((o, a, b) -> r.run());
        poidsSpinner.getEditor().textProperty().addListener((o, a, b) -> r.run());
        poidsSpinner.focusedProperty().addListener((o, ov, nv) -> {
            if (!nv) {
                onBlur.run();
            }
        });
    }

    private void refreshMetricsValidation() {
        Optional<String> ageErr = FormValidator.validateAge(ageSpinner);
        Optional<String> tailleErr = FormValidator.validateHeight(tailleSpinner);
        Optional<String> poidsErr = FormValidator.validateWeight(poidsSpinner);
        applyFieldState(ageControlWrap, ageMessageRow, ageFieldIcon, ageFieldMessage, ageErr);
        applyFieldState(tailleControlWrap, tailleMessageRow, tailleFieldIcon, tailleFieldMessage, tailleErr);
        applyFieldState(poidsControlWrap, poidsMessageRow, poidsFieldIcon, poidsFieldMessage, poidsErr);
        boolean metricsOk = ageErr.isEmpty() && tailleErr.isEmpty() && poidsErr.isEmpty();
        submitButton.setDisable(!metricsOk);
    }

    private static void applyFieldState(HBox wrap, HBox messageRow, FontIcon icon, Label message,
                                        Optional<String> err) {
        if (wrap == null || messageRow == null) {
            return;
        }
        message.getStyleClass().remove("fs-field-message--error");
        wrap.getStyleClass().removeAll("fs-field-control-wrap--error", "fs-field-control-wrap--success");
        icon.getStyleClass().removeAll("fs-field-status-icon", "fs-field-status-icon--error", "fs-field-status-icon--ok");
        if (err.isPresent()) {
            wrap.getStyleClass().add("fs-field-control-wrap--error");
            icon.setIconLiteral("fas-exclamation-triangle");
            icon.getStyleClass().addAll("fs-field-status-icon", "fs-field-status-icon--error");
            message.getStyleClass().add("fs-field-message--error");
            message.setText(err.get());
            messageRow.setVisible(true);
            messageRow.setManaged(true);
        } else {
            wrap.getStyleClass().add("fs-field-control-wrap--success");
            icon.setIconLiteral("fas-check");
            icon.getStyleClass().addAll("fs-field-status-icon", "fs-field-status-icon--ok");
            message.setText("");
            messageRow.setVisible(true);
            messageRow.setManaged(true);
        }
    }

    private static List<String> filterBackendPhysiqueErrors(List<String> errors) {
        List<String> out = new ArrayList<>();
        for (String line : errors) {
            if (line == null) {
                continue;
            }
            String x = line.toLowerCase(Locale.FRENCH);
            if (x.contains("âge") || x.contains("taille") || x.contains("poids")) {
                continue;
            }
            out.add(line);
        }
        return out;
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
        errorTitle.setVisible(false);
        errorTitle.setManaged(false);
    }

    private void setupGenreToggles(String initialGenre) {
        String g = initialGenre != null ? initialGenre.trim().toUpperCase() : "M";
        if ("F".equals(g)) {
            genreFemale.setSelected(true);
        } else {
            genreMale.setSelected(true);
        }
    }

    private void setupAge(Integer initialAge) {
        int v = initialAge != null ? initialAge : 30;
        v = Math.max(FicheSanteFormValidator.AGE_MIN, Math.min(FicheSanteFormValidator.AGE_MAX, v));
        ageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 200, v, 1));
        ageSpinner.setEditable(true);
    }

    private void setupTaille(Integer initialCm) {
        int v = initialCm != null ? initialCm : 170;
        v = Math.max(FicheSanteFormValidator.TAILLE_MIN_CM, Math.min(FicheSanteFormValidator.TAILLE_MAX_CM, v));
        tailleSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(50, 300, v, 1));
        tailleSpinner.setEditable(true);
    }

    private void setupPoids(Double initialKg) {
        double v = initialKg != null ? initialKg : 70.0;
        double minInit = FicheSanteFormValidator.POIDS_MIN_STRICTLY_ABOVE_KG + 0.5;
        v = Math.max(minInit, Math.min(FicheSanteFormValidator.POIDS_MAX_KG, v));
        poidsSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(1.0, 300.0, v, 0.5));
        poidsSpinner.setEditable(true);
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
            case "perte_poids" -> "Perte de poids";
            case "gain_poids" -> "Prise de masse";
            case "devenir_muscle" -> "Musculation / tonification";
            case "maintien" -> "Maintien";
            default -> code;
        };
    }

    private static String labelNiveau(String code) {
        return switch (code) {
            case "sedentaire" -> "Sédentaire";
            case "peu_actif" -> "Peu actif";
            case "moderement_actif" -> "Modérément actif";
            case "tres_actif" -> "Très actif";
            default -> code;
        };
    }

    public void applyInitial(FicheSanteFormData data) {
        if (data.genre() != null) {
            if ("F".equalsIgnoreCase(data.genre().trim())) {
                genreFemale.setSelected(true);
            } else {
                genreMale.setSelected(true);
            }
        }
        if (data.age() != null) {
            ageSpinner.getValueFactory().setValue(clamp(data.age(), FicheSanteFormValidator.AGE_MIN, FicheSanteFormValidator.AGE_MAX));
        }
        if (data.tailleCm() != null) {
            tailleSpinner.getValueFactory().setValue(clamp(data.tailleCm(), FicheSanteFormValidator.TAILLE_MIN_CM, FicheSanteFormValidator.TAILLE_MAX_CM));
        }
        if (data.poidsKg() != null) {
            double minInit = FicheSanteFormValidator.POIDS_MIN_STRICTLY_ABOVE_KG + 0.5;
            double p = Math.max(minInit, Math.min(FicheSanteFormValidator.POIDS_MAX_KG, data.poidsKg()));
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
        refreshBmi();
        refreshMetricsValidation();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

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
}
