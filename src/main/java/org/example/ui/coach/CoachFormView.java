package org.example.ui.coach;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.model.planning.FicheSanteFormData;
import org.example.ui.forms.FicheSanteFormMode;
import java.util.function.Consumer;

/**
 * Formulaire généraliste pour le coaching santé et bien-être.
 * Remplace l'ancien FicheSanteFormView spécifique à la salle de sport.
 */
public final class CoachFormView {

    private final VBox root;
    private final ToggleGroup genreGroup = new ToggleGroup();
    private final ToggleButton genreMale = new ToggleButton("Homme");
    private final ToggleButton genreFemale = new ToggleButton("Femme");
    private final Spinner<Integer> ageSpinner = new Spinner<>();
    private final Spinner<Integer> tailleSpinner = new Spinner<>();
    private final Spinner<Double> poidsSpinner = new Spinner<>();
    private final ComboBox<String> objectifCombo = new ComboBox<>();
    private final ComboBox<String> niveauCombo = new ComboBox<>();
    private Button submitButton;
    private Consumer<FicheSanteFormData> onSubmit;

    public CoachFormView(FicheSanteFormMode mode, FicheSanteFormData initial) {
        this.root = new VBox(20);
        root.getStyleClass().add("coach-form-shell");
        root.setPadding(new Insets(20));

        setupGenreToggles(initial.genre());
        setupAge(initial.age());
        setupTaille(initial.tailleCm());
        setupPoids(initial.poidsKg());
        setupObjectif(initial.objectif());
        setupNiveau(initial.niveauActivite());

        VBox header = buildHeader(mode);
        GridPane form = buildForm();

        submitButton = new Button(mode == FicheSanteFormMode.CREATION ? 
            "Démarrer mon programme" : "Mettre à jour mon programme");
        submitButton.getStyleClass().addAll("primary-btn", "coach-submit");
        submitButton.setMaxWidth(Double.MAX_VALUE);
        submitButton.setOnAction(e -> {
            if (onSubmit != null) {
                onSubmit.accept(getData());
            }
        });

        root.getChildren().addAll(header, form, submitButton);
    }

    private VBox buildHeader(FicheSanteFormMode mode) {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        
        Label title = new Label(mode == FicheSanteFormMode.CREATION ? 
            "Bienvenue dans votre programme personnalisé" : 
            "Mise à jour de votre programme");
        title.getStyleClass().add("coach-title");
        
        Label subtitle = new Label(mode == FicheSanteFormMode.CREATION ?
            "Complétez vos informations pour recevoir un programme adapté à vos objectifs" :
            "Ajustez vos informations pour optimiser votre programme");
        subtitle.getStyleClass().add("coach-subtitle");
        
        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private GridPane buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        // Genre
        Label genreLabel = new Label("Genre");
        genreLabel.getStyleClass().add("form-label");
        genreMale.setToggleGroup(genreGroup);
        genreFemale.setToggleGroup(genreGroup);
        genreMale.getStyleClass().add("toggle-btn");
        genreFemale.getStyleClass().add("toggle-btn");
        HBox genreBox = new HBox(10, genreMale, genreFemale);
        grid.add(genreLabel, 0, 0);
        grid.add(genreBox, 1, 0);

        // Âge
        Label ageLabel = new Label("Âge");
        ageLabel.getStyleClass().add("form-label");
        ageSpinner.getStyleClass().add("form-spinner");
        grid.add(ageLabel, 0, 1);
        grid.add(ageSpinner, 1, 1);

        // Taille
        Label tailleLabel = new Label("Taille (cm)");
        tailleLabel.getStyleClass().add("form-label");
        tailleSpinner.getStyleClass().add("form-spinner");
        grid.add(tailleLabel, 0, 2);
        grid.add(tailleSpinner, 1, 2);

        // Poids
        Label poidsLabel = new Label("Poids (kg)");
        poidsLabel.getStyleClass().add("form-label");
        poidsSpinner.getStyleClass().add("form-spinner");
        grid.add(poidsLabel, 0, 3);
        grid.add(poidsSpinner, 1, 3);

        // Objectif
        Label objectifLabel = new Label("Objectif principal");
        objectifLabel.getStyleClass().add("form-label");
        objectifCombo.getStyleClass().add("form-combo");
        grid.add(objectifLabel, 0, 4);
        grid.add(objectifCombo, 1, 4);

        // Niveau
        Label niveauLabel = new Label("Niveau d'activité");
        niveauLabel.getStyleClass().add("form-label");
        niveauCombo.getStyleClass().add("form-combo");
        grid.add(niveauLabel, 0, 5);
        grid.add(niveauCombo, 1, 5);

        return grid;
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
        ageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 100, v, 1));
        ageSpinner.setEditable(true);
    }

    private void setupTaille(Integer initialCm) {
        int v = initialCm != null ? initialCm : 170;
        tailleSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 250, v, 1));
        tailleSpinner.setEditable(true);
    }

    private void setupPoids(Double initialKg) {
        double v = initialKg != null ? initialKg : 70.0;
        poidsSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(30.0, 200.0, v, 0.5));
        poidsSpinner.setEditable(true);
    }

    private void setupObjectif(String code) {
        objectifCombo.getItems().addAll(
            "Perte de poids",
            "Prise de masse", 
            "Musculation",
            "Maintien",
            "Remise en forme"
        );
        objectifCombo.setMaxWidth(Double.MAX_VALUE);
        
        if (code != null) {
            objectifCombo.setValue(code);
        } else {
            objectifCombo.getSelectionModel().selectFirst();
        }
    }

    private void setupNiveau(String codeFromDb) {
        niveauCombo.getItems().addAll(
            "Sédentaire",
            "Peu actif", 
            "Modérément actif",
            "Très actif"
        );
        niveauCombo.setMaxWidth(Double.MAX_VALUE);
        
        String c = normalizeNiveau(codeFromDb);
        if (niveauCombo.getItems().contains(c)) {
            niveauCombo.setValue(c);
        } else {
            niveauCombo.setValue("Peu actif");
        }
    }

    private static String normalizeNiveau(String db) {
        if (db == null || db.isBlank()) {
            return "Peu actif";
        }
        String k = db.trim().toLowerCase();
        return switch (k) {
            case "sedentaire" -> "Sédentaire";
            case "peu_actif" -> "Peu actif";
            case "moderement_actif" -> "Modérément actif";
            case "tres_actif" -> "Très actif";
            default -> "Peu actif";
        };
    }

    public VBox getRoot() {
        return root;
    }

    public Button getSubmitButton() {
        return submitButton;
    }

    public void setOnSubmit(Consumer<FicheSanteFormData> handler) {
        this.onSubmit = handler;
    }

    public FicheSanteFormData getData() {
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
}
