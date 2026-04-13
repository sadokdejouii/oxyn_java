package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.example.entities.Salle;
import org.example.services.SalleService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

public class SalleManagementController implements Initializable {

    @FXML private FlowPane sallesGrid;
    @FXML private Label countLabel;

    // Dialog
    @FXML private StackPane dialogOverlay;
    @FXML private Label dialogTitle;
    @FXML private Label dialogError;
    @FXML private TextField fieldNom;
    @FXML private TextArea fieldDescription;
    @FXML private TextField fieldAdresse;
    @FXML private TextField fieldTelephone;
    @FXML private TextField fieldEmail;

    // Photo
    @FXML private StackPane previewPane;
    @FXML private ImageView previewImage;
    @FXML private Button removeImageBtn;
    @FXML private VBox dropZone;

    private final SalleService service = new SalleService();
    private Salle salleEnEdition = null;
    private String selectedImagePath = null; // chemin local du fichier choisi

    // Dossier de stockage des images
    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/oxyn_uploads/salles/";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new File(UPLOAD_DIR).mkdirs();
        setupDropZone();
        loadSalles();
    }

    // ── DRAG & DROP ─────────────────────────────────────────────────────────

    private void setupDropZone() {
        dropZone.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
                dropZone.getStyleClass().add("salle-drop-zone-hover");
            }
            e.consume();
        });
        dropZone.setOnDragExited(e -> dropZone.getStyleClass().remove("salle-drop-zone-hover"));
        dropZone.setOnDragDropped(e -> {
            List<File> files = e.getDragboard().getFiles();
            if (!files.isEmpty()) applyImageFile(files.get(0));
            e.setDropCompleted(true);
            e.consume();
        });
    }

    @FXML
    private void handlePickImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp")
        );
        File file = fc.showOpenDialog(dropZone.getScene().getWindow());
        if (file != null) applyImageFile(file);
    }

    private void applyImageFile(File file) {
        try {
            Image img = new Image(file.toURI().toString(), 444, 160, false, true);
            previewImage.setImage(img);
            selectedImagePath = file.getAbsolutePath();
            showPreview(true);
        } catch (Exception e) {
            dialogError.setText("Impossible de charger l'image.");
        }
    }

    @FXML
    private void handleRemoveImage() {
        selectedImagePath = null;
        previewImage.setImage(null);
        showPreview(false);
    }

    private void showPreview(boolean show) {
        previewPane.setVisible(show);
        previewPane.setManaged(show);
        dropZone.setVisible(!show);
        dropZone.setManaged(!show);
    }

    // ── Copie l'image dans UPLOAD_DIR et retourne le chemin destination ──────

    private String saveImageFile(String sourcePath) throws IOException {
        if (sourcePath == null) return null;
        Path src = Paths.get(sourcePath);
        String ext = sourcePath.contains(".") ? sourcePath.substring(sourcePath.lastIndexOf('.')) : ".jpg";
        String filename = UUID.randomUUID() + ext;
        Path dest = Paths.get(UPLOAD_DIR + filename);
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.toAbsolutePath().toString();
    }

    // ── CHARGEMENT ──────────────────────────────────────────────────────────

    private void loadSalles() {
        sallesGrid.getChildren().clear();
        try {
            List<Salle> salles = service.afficher();
            countLabel.setText(salles.size() + " salle(s)");
            for (Salle s : salles) sallesGrid.getChildren().add(buildCard(s));
        } catch (SQLException e) {
            countLabel.setText("Erreur de chargement");
            e.printStackTrace();
        }
    }

    // ── CARD BUILDER ────────────────────────────────────────────────────────

    private VBox buildCard(Salle s) {
        VBox card = new VBox(0);
        card.setPrefWidth(320);
        card.setMaxWidth(380);
        card.getStyleClass().add("salle-card");

        // ── Zone photo avec badge statut en overlay ──
        StackPane photoPane = new StackPane();
        photoPane.setPrefHeight(160);
        photoPane.setMinHeight(160);
        photoPane.setMaxHeight(160);
        photoPane.getStyleClass().add("salle-card-photo-wrap");

        String imgUrl = s.getImageUrl();
        if (imgUrl != null && !imgUrl.isBlank() && new File(imgUrl).exists()) {
            ImageView iv = new ImageView(new Image("file:///" + imgUrl.replace("\\", "/"), 320, 160, false, true));
            iv.setFitWidth(320);
            iv.setFitHeight(160);
            iv.setPreserveRatio(false);
            iv.getStyleClass().add("salle-card-photo");
            photoPane.getChildren().add(iv);
        } else {
            // Placeholder stylisé avec icône caméra
            VBox placeholder = new VBox(6);
            placeholder.setAlignment(Pos.CENTER);
            placeholder.getStyleClass().add("salle-card-placeholder");
            Label camIcon = new Label("📷");
            camIcon.getStyleClass().add("salle-card-placeholder-icon");
            Label camText = new Label("Aucune photo");
            camText.getStyleClass().add("salle-card-placeholder-text");
            placeholder.getChildren().addAll(camIcon, camText);
            photoPane.getChildren().add(placeholder);
        }

        // Badge statut en overlay top-right
        boolean actif = s.isActive();
        Label badge = new Label(actif ? "Actif" : "Inactif");
        badge.getStyleClass().add(actif ? "salle-badge-active" : "salle-badge-inactive");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(10, 10, 0, 0));
        photoPane.getChildren().add(badge);

        // ── Contenu ──
        VBox content = new VBox(10);
        content.setPadding(new Insets(14, 16, 16, 16));
        content.getStyleClass().add("salle-card-body");

        // Nom
        Label nom = new Label(s.getName());
        nom.getStyleClass().add("salle-card-title");
        nom.setWrapText(true);
        nom.setMaxWidth(Double.MAX_VALUE);

        // Description
        if (s.getDescription() != null && !s.getDescription().isBlank()) {
            Label desc = new Label(s.getDescription());
            desc.getStyleClass().add("salle-card-desc");
            desc.setWrapText(true);
            desc.setMaxWidth(Double.MAX_VALUE);
            content.getChildren().addAll(nom, desc);
        } else {
            content.getChildren().add(nom);
        }

        // Infos compactes
        VBox infos = new VBox(5);
        infos.getChildren().addAll(
            infoRow("📍", s.getAddress()),
            infoRow("📞", s.getPhone()),
            infoRow("✉", s.getEmail())
        );

        // Rating avec étoiles dorées
        HBox ratingBox = new HBox(6);
        ratingBox.setAlignment(Pos.CENTER_LEFT);
        double rating = s.getRating();
        int fullStars = (int) rating;
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) stars.append(i < fullStars ? "★" : "☆");
        Label starsLbl = new Label(stars.toString());
        starsLbl.getStyleClass().add("salle-card-stars");
        Label avisLbl = new Label(String.format("%.1f  (%d avis)", rating, s.getRatingCount()));
        avisLbl.getStyleClass().add("salle-card-avis");
        ratingBox.getChildren().addAll(starsLbl, avisLbl);

        // Séparateur fin
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.getStyleClass().add("salle-card-divider");

        // Boutons pleine largeur
        Button btnEdit = new Button("✏  Modifier");
        btnEdit.getStyleClass().add("salle-btn-edit");
        btnEdit.setMaxWidth(Double.MAX_VALUE);
        btnEdit.setOnAction(e -> openEditDialog(s));
        HBox.setHgrow(btnEdit, Priority.ALWAYS);

        Button btnDel = new Button("🗑  Supprimer");
        btnDel.getStyleClass().add("salle-btn-delete");
        btnDel.setMaxWidth(Double.MAX_VALUE);
        btnDel.setOnAction(e -> handleSupprimer(s));
        HBox.setHgrow(btnDel, Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.getChildren().addAll(btnEdit, btnDel);

        content.getChildren().addAll(infos, ratingBox, divider, actions);
        card.getChildren().addAll(photoPane, content);
        return card;
    }

    private HBox infoRow(String icon, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icon);
        ico.getStyleClass().add("salle-card-info-icon");
        Label val = new Label(value != null && !value.isBlank() ? value : "—");
        val.getStyleClass().add("salle-card-info");
        val.setWrapText(false);
        row.getChildren().addAll(ico, val);
        return row;
    }

    // ── DIALOG ──────────────────────────────────────────────────────────────

    @FXML
    public void handleAjouter() {
        salleEnEdition = null;
        dialogTitle.setText("Nouvelle salle");
        clearDialog();
        showDialog(true);
    }

    private void openEditDialog(Salle s) {
        salleEnEdition = s;
        dialogTitle.setText("Modifier la salle");
        fieldNom.setText(s.getName());
        fieldDescription.setText(s.getDescription());
        fieldAdresse.setText(s.getAddress());
        fieldTelephone.setText(s.getPhone());
        fieldEmail.setText(s.getEmail());
        dialogError.setText("");

        // Charger la photo existante
        String imgUrl = s.getImageUrl();
        if (imgUrl != null && !imgUrl.isBlank() && new File(imgUrl).exists()) {
            selectedImagePath = imgUrl;
            previewImage.setImage(new Image("file:///" + imgUrl.replace("\\", "/"), 444, 160, false, true));
            showPreview(true);
        } else {
            selectedImagePath = null;
            showPreview(false);
        }

        showDialog(true);
    }

    @FXML
    private void handleDialogSave() {
        String nom = fieldNom.getText().trim();
        if (nom.isEmpty()) {
            dialogError.setText("Le nom de la salle est obligatoire.");
            return;
        }
        try {
            // Copier l'image si c'est un nouveau fichier (pas déjà dans UPLOAD_DIR)
            String finalImagePath = null;
            if (selectedImagePath != null) {
                if (selectedImagePath.startsWith(UPLOAD_DIR)) {
                    finalImagePath = selectedImagePath; // déjà sauvegardée
                } else {
                    finalImagePath = saveImageFile(selectedImagePath);
                }
            }

            if (salleEnEdition == null) {
                Salle s = new Salle(nom,
                    fieldDescription.getText().trim(),
                    fieldAdresse.getText().trim(),
                    fieldTelephone.getText().trim(),
                    fieldEmail.getText().trim());
                s.setImageUrl(finalImagePath);
                service.ajouter(s);
            } else {
                salleEnEdition.setName(nom);
                salleEnEdition.setDescription(fieldDescription.getText().trim());
                salleEnEdition.setAddress(fieldAdresse.getText().trim());
                salleEnEdition.setPhone(fieldTelephone.getText().trim());
                salleEnEdition.setEmail(fieldEmail.getText().trim());
                salleEnEdition.setImageUrl(finalImagePath);
                service.modifier(salleEnEdition);
            }
            showDialog(false);
            loadSalles();
        } catch (SQLException | IOException e) {
            dialogError.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void handleDialogCancel() {
        showDialog(false);
    }

    private void handleSupprimer(Salle s) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer \"" + s.getName() + "\" ?");
        confirm.setContentText("La salle sera désactivée (suppression logique).");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.supprimer(s.getId());
                    loadSalles();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadSalles();
    }

    private void showDialog(boolean show) {
        dialogOverlay.setVisible(show);
        dialogOverlay.setManaged(show);
    }

    private void clearDialog() {
        fieldNom.clear();
        fieldDescription.clear();
        fieldAdresse.clear();
        fieldTelephone.clear();
        fieldEmail.clear();
        dialogError.setText("");
        selectedImagePath = null;
        previewImage.setImage(null);
        showPreview(false);
    }
}
