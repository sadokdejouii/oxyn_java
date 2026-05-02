package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.example.entities.Equipment;
import org.example.entities.Salle;
import org.example.entities.SubscriptionOffer;
import org.example.services.AdminFormValidation;
import org.example.services.EquipmentService;
import org.example.services.SalleService;
import org.example.services.SubscriptionOfferService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    @FXML private TextArea  fieldDescription;
    @FXML private TextField fieldAdresse;
    @FXML private TextField fieldTelephone;
    @FXML private TextField fieldEmail;
    @FXML private TextField fieldYoutubeUrl;

    // Photo
    @FXML private StackPane previewPane;
    @FXML private ImageView previewImage;
    @FXML private Button removeImageBtn;
    @FXML private VBox dropZone;

    private final SalleService service = new SalleService();
    private final EquipmentService equipmentService = new EquipmentService();
    private Salle salleEnEdition = null;
    private String selectedImagePath = null; // chemin local du fichier choisi

    // Modals Équipements / Abonnements (par salle)
    @FXML private StackPane equipOverlay;
    @FXML private VBox equipCard;
    @FXML private Label equipTitle;
    @FXML private TableView<Equipement> equipementsTable;
    @FXML private TableColumn<Equipement, String> colNomEquipement;
    @FXML private TableColumn<Equipement, Integer> colQuantiteEquipement;
    @FXML private TableColumn<Equipement, Void> colActionsEquipement;
    @FXML private TextField nomEquipementField;
    @FXML private TextField quantiteEquipementField;
    @FXML private Label equipError;

    @FXML private StackPane subsOverlay;
    @FXML private VBox subsCard;
    @FXML private Label subsTitle;
    @FXML private TableView<Abonnement> abonnementsTable;
    @FXML private TableColumn<Abonnement, String> colNomAbonnement;
    @FXML private TableColumn<Abonnement, Double> colPrixAbonnement;
    @FXML private TableColumn<Abonnement, Void> colActionsAbonnement;
    @FXML private TextField nomAbonnementField;
    @FXML private TextField prixAbonnementField;
    @FXML private Label subsError;

    private Salle currentSalleModal = null;
    private SubscriptionOfferService subscriptionOfferService = new SubscriptionOfferService();
    private final Set<Integer> equipToDelete = new HashSet<>();
    private final Map<Integer, Equipment> equipToUpdate = new LinkedHashMap<>();
    private final List<Equipment> equipToAdd = new ArrayList<>();

    private final Set<Integer> subsToDelete = new HashSet<>();
    private final Map<Integer, SubscriptionOffer> subsToUpdate = new LinkedHashMap<>();
    private final List<SubscriptionOffer> subsToAdd = new ArrayList<>();

    // Méthode utilitaire pour getText() safe
    private String safeGetText(TextField field) {
        return field != null && field.getText() != null ? field.getText().trim() : "";
    }

    private String safeGetText(TextArea field) {
        if (field == null) {
            System.err.println("WARNING: TextArea field is null!");
            return "";
        }
        String text = field.getText();
        return text != null ? text.trim() : "";
    }

    // Dossier de stockage des images
    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/oxyn_uploads/salles/";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new File(UPLOAD_DIR).mkdirs();
        setupDropZone();
        if (equipementsTable != null) equipementsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if (abonnementsTable != null) abonnementsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupEquipTable();
        setupSubsTable();
        loadSalles();
    }

    private void setupEquipTable() {
        if (colNomEquipement != null) colNomEquipement.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (colQuantiteEquipement != null) colQuantiteEquipement.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        if (colActionsEquipement != null) {
            colActionsEquipement.setCellFactory(col -> new TableCell<>() {
                private final Button btnEdit = new Button("Modifier");
                private final Button btnDel  = new Button("Supprimer");
                private final HBox box = new HBox(8, btnEdit, btnDel);

                {
                    box.setAlignment(Pos.CENTER_RIGHT);
                    btnEdit.getStyleClass().addAll("equip-action-btn", "equip-action-btn--edit");
                    btnDel.getStyleClass().addAll("equip-action-btn", "equip-action-btn--delete");

                    btnEdit.setOnAction(e -> {
                        Equipement eq = getTableView().getItems().get(getIndex());
                        if (eq == null) return;
                        if (nomEquipementField != null) nomEquipementField.setText(eq.getNom());
                        if (quantiteEquipementField != null) quantiteEquipementField.setText(String.valueOf(eq.getQuantite()));
                        getTableView().getItems().remove(eq);
                        if (nomEquipementField != null) nomEquipementField.requestFocus();
                    });

                    btnDel.setOnAction(e -> {
                        Equipement eq = getTableView().getItems().get(getIndex());
                        if (eq != null) getTableView().getItems().remove(eq);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    setGraphic(box);
                }
            });
        }
    }

    private void setupSubsTable() {
        if (colNomAbonnement != null) colNomAbonnement.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (colPrixAbonnement != null) colPrixAbonnement.setCellValueFactory(new PropertyValueFactory<>("prix"));
        if (colActionsAbonnement != null) {
            colActionsAbonnement.setCellFactory(col -> new TableCell<>() {
                private final Button btnEdit = new Button("Modifier");
                private final Button btnDel  = new Button("Supprimer");
                private final HBox box = new HBox(8, btnEdit, btnDel);

                {
                    box.setAlignment(Pos.CENTER_RIGHT);
                    btnEdit.getStyleClass().addAll("equip-action-btn", "equip-action-btn--edit");
                    btnDel.getStyleClass().addAll("equip-action-btn", "equip-action-btn--delete");

                    btnEdit.setOnAction(e -> {
                        Abonnement ab = getTableView().getItems().get(getIndex());
                        if (ab == null) return;
                        if (nomAbonnementField != null) nomAbonnementField.setText(ab.getNom());
                        if (prixAbonnementField != null) prixAbonnementField.setText(String.valueOf(ab.getPrix()));
                        getTableView().getItems().remove(ab);
                        if (nomAbonnementField != null) nomAbonnementField.requestFocus();
                    });

                    btnDel.setOnAction(e -> {
                        Abonnement ab = getTableView().getItems().get(getIndex());
                        if (ab != null) getTableView().getItems().remove(ab);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    setGraphic(box);
                }
            });
        }
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
            List<Salle> salles = service.getAll();
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

        int equipCount = 0;
        int subsCount = 0;
        try { equipCount = equipmentService.countParSalle(s.getId()); } catch (Exception ignored) {}
        try { subsCount = subscriptionOfferService.countParSalle(s.getId()); } catch (Exception ignored) {}

        Button btnEquip = buildActionButtonWithBadge("🏋  Équipements", equipCount);
        btnEquip.setOnAction(e -> openEquipModal(s));

        Button btnSubs = buildActionButtonWithBadge("💳  Abonnements", subsCount);
        btnSubs.setOnAction(e -> openSubsModal(s));

        Button btnEdit = new Button("✏  Modifier");
        btnEdit.getStyleClass().addAll("salle-btn-chip", "salle-btn-edit");
        btnEdit.setOnAction(e -> openEditDialog(s));

        Button btnDel = new Button("🗑  Supprimer");
        btnDel.getStyleClass().addAll("salle-btn-chip", "salle-btn-delete");
        btnDel.setOnAction(e -> handleSupprimer(s));

        FlowPane actions = new FlowPane(8, 8);
        actions.getStyleClass().add("salle-card-actions");
        actions.getChildren().addAll(btnEquip, btnSubs, btnEdit, btnDel);

        content.getChildren().addAll(infos, ratingBox, divider, actions);
        card.getChildren().addAll(photoPane, content);
        return card;
    }

    private Button buildActionButtonWithBadge(String label, int count) {
        Label text = new Label(label);
        text.getStyleClass().add("salle-chip-text");

        Label badge = new Label(String.valueOf(Math.max(0, count)));
        badge.getStyleClass().add("salle-chip-badge");

        HBox box = new HBox(8, text, badge);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("salle-chip-inner");

        Button btn = new Button();
        btn.setGraphic(box);
        btn.getStyleClass().add("salle-btn-chip");
        return btn;
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
        if (fieldDescription != null) fieldDescription.setText(s.getDescription());
        fieldAdresse.setText(s.getAddress() != null ? s.getAddress() : "");
        fieldTelephone.setText(s.getPhone());
        fieldEmail.setText(s.getEmail());
        fieldYoutubeUrl.setText(s.getYoutubeUrl() != null ? s.getYoutubeUrl() : "");
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
        String adresse = safeGetText(fieldAdresse);
        if (adresse.isEmpty()) {
            dialogError.setText("L'adresse est obligatoire.");
            return;
        }
        String[] parts = adresse.split(",", 2);
        if (parts.length < 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
            dialogError.setText("Format invalide. Utilisez : Ville, Pays — ex : Sousse, Tunisie");
            return;
        }
        String errNom = AdminFormValidation.validateAdminDisplayName(safeGetText(fieldNom), "Le nom de la salle", 2, 150);
        if (errNom != null) { dialogError.setText(errNom); return; }
        String errDesc = AdminFormValidation.validateOptionalDescription(safeGetText(fieldDescription), 4000, "La description");
        if (errDesc != null) { dialogError.setText(errDesc); return; }
        String errTel = org.example.services.AuthValidation.validateTelephone(safeGetText(fieldTelephone), false);
        if (errTel != null) { dialogError.setText(errTel); return; }
        String errEmail = AdminFormValidation.validateEmailIfPresent(safeGetText(fieldEmail));
        if (errEmail != null) { dialogError.setText(errEmail); return; }
        dialogError.setText("");

        String nom = safeGetText(fieldNom);
        try {
            String finalImagePath = null;
            if (selectedImagePath != null) {
                if (selectedImagePath.startsWith(UPLOAD_DIR)) {
                    finalImagePath = selectedImagePath;
                } else {
                    finalImagePath = saveImageFile(selectedImagePath);
                }
            }
            if (salleEnEdition == null) {
                Salle s = new Salle(nom,
                    safeGetText(fieldDescription),
                    adresse,
                    safeGetText(fieldTelephone),
                    safeGetText(fieldEmail));
                s.setImageUrl(finalImagePath);
                s.setYoutubeUrl(safeGetText(fieldYoutubeUrl));
                service.ajouter(s);
            } else {
                salleEnEdition.setName(nom);
                salleEnEdition.setDescription(safeGetText(fieldDescription));
                salleEnEdition.setAddress(adresse);
                salleEnEdition.setPhone(safeGetText(fieldTelephone));
                salleEnEdition.setEmail(safeGetText(fieldEmail));
                salleEnEdition.setImageUrl(finalImagePath);
                salleEnEdition.setYoutubeUrl(safeGetText(fieldYoutubeUrl));
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
        if (fieldDescription != null) fieldDescription.clear();
        fieldAdresse.clear();
        fieldTelephone.clear();
        fieldEmail.clear();
        fieldYoutubeUrl.clear();
        dialogError.setText("");
        selectedImagePath = null;
        previewImage.setImage(null);
        showPreview(false);
    }

    // ── MODALS ÉQUIPEMENTS / ABONNEMENTS ────────────────────────────────────

// ...
    @FXML
    private void handleOverlayClick(MouseEvent e) {
        Object src = e.getSource();
        Object target = e.getTarget();
        if (src == equipOverlay && target == equipOverlay) closeEquipModal();
        if (src == subsOverlay && target == subsOverlay) closeSubsModal();
    }

    private void openEquipModal(Salle s) {
        currentSalleModal = s;
        equipTitle.setText("Équipements — " + s.getName());
        equipError.setText("");
        if (nomEquipementField != null) nomEquipementField.clear();
        if (quantiteEquipementField != null) quantiteEquipementField.clear();
        if (equipementsTable != null) equipementsTable.getItems().clear();
        equipToDelete.clear();
        equipToUpdate.clear();
        equipToAdd.clear();

        try {
            List<Equipment> list = equipmentService.afficherParSalle(s.getId());
            for (Equipment eq : list) {
                if (equipementsTable != null) equipementsTable.getItems().add(new Equipement(eq.getName(), eq.getQuantity()));
            }
        } catch (Exception ex) {
            equipError.setText("Impossible de charger les équipements.");
        }

        showEquip(true);
        if (nomEquipementField != null) nomEquipementField.requestFocus();
    }

    private void showEquip(boolean show) {
        equipOverlay.setVisible(show);
        equipOverlay.setManaged(show);
    }

    @FXML
    private void handleAjouterEquipement() {
        String nom = nomEquipementField == null || nomEquipementField.getText() == null ? "" : nomEquipementField.getText().trim();
        String qteRaw = quantiteEquipementField == null || quantiteEquipementField.getText() == null ? "" : quantiteEquipementField.getText().trim();

        if (nom.isEmpty() || qteRaw.isEmpty()) {
            equipError.setText("Veuillez saisir le nom et la quantité.");
            return;
        }

        int qte;
        try {
            qte = Integer.parseInt(qteRaw);
        } catch (NumberFormatException ex) {
            equipError.setText("Quantité invalide.");
            return;
        }

        equipError.setText("");
        if (equipementsTable != null) equipementsTable.getItems().add(new Equipement(nom, qte));
        if (nomEquipementField != null) nomEquipementField.clear();
        if (quantiteEquipementField != null) quantiteEquipementField.clear();
        if (nomEquipementField != null) nomEquipementField.requestFocus();
    }

    @FXML
    private void handleEquipCancel() {
        closeEquipModal();
    }

    @FXML
    private void handleEquipSave() {
        if (currentSalleModal == null) { closeEquipModal(); return; }
        try {
            for (Integer id : equipToDelete) equipmentService.supprimer(id);
            for (Equipment e : equipToUpdate.values()) equipmentService.modifier(e);
            for (Equipment e : equipToAdd) equipmentService.add(e);  // ← add() au lieu de ajouter()
            closeEquipModal();
            loadSalles();
        } catch (Exception ex) {
            equipError.setText("Erreur d’enregistrement : " + ex.getMessage());
        }
    }

    private void closeEquipModal() {
        showEquip(false);
        currentSalleModal = null;
        equipToDelete.clear();
        equipToUpdate.clear();
        equipToAdd.clear();
        if (equipementsTable != null) equipementsTable.getItems().clear();
        equipError.setText("");
        if (nomEquipementField != null) nomEquipementField.clear();
        if (quantiteEquipementField != null) quantiteEquipementField.clear();
    }

    private void openSubsModal(Salle s) {
        currentSalleModal = s;
        subsTitle.setText("Abonnements — " + s.getName());
        subsError.setText("");
        if (nomAbonnementField != null) nomAbonnementField.clear();
        if (prixAbonnementField != null) prixAbonnementField.clear();
        if (abonnementsTable != null) abonnementsTable.getItems().clear();
        subsToDelete.clear();
        subsToUpdate.clear();
        subsToAdd.clear();

        try {
            List<SubscriptionOffer> list = subscriptionOfferService.getByGym(s.getId());
            for (SubscriptionOffer o : list) {
                if (abonnementsTable != null) abonnementsTable.getItems().add(new Abonnement(o.getName(), o.getPrice()));
            }
        } catch (Exception ex) {
            subsError.setText("Impossible de charger les abonnements.");
        }

        showSubs(true);
        if (nomAbonnementField != null) nomAbonnementField.requestFocus();
    }

    private void showSubs(boolean show) {
        subsOverlay.setVisible(show);
        subsOverlay.setManaged(show);
    }

    @FXML
    private void handleAjouterAbonnement() {
        String nom = nomAbonnementField == null || nomAbonnementField.getText() == null ? "" : nomAbonnementField.getText().trim();
        String prixRaw = prixAbonnementField == null || prixAbonnementField.getText() == null ? "" : prixAbonnementField.getText().trim().replace(",", ".");

        if (nom.isEmpty() || prixRaw.isEmpty()) {
            subsError.setText("Veuillez saisir le nom et le prix.");
            return;
        }

        double prix;
        try {
            prix = Double.parseDouble(prixRaw);
        } catch (NumberFormatException ex) {
            subsError.setText("Prix invalide.");
            return;
        }

        subsError.setText("");
        if (abonnementsTable != null) abonnementsTable.getItems().add(new Abonnement(nom, prix));
        if (nomAbonnementField != null) nomAbonnementField.clear();
        if (prixAbonnementField != null) prixAbonnementField.clear();
        if (nomAbonnementField != null) nomAbonnementField.requestFocus();
    }

    @FXML
    private void handleSubsCancel() {
        closeSubsModal();
    }

    @FXML
    private void handleSubsSave() {
        if (currentSalleModal == null) { closeSubsModal(); return; }
        try {
            for (Integer id : subsToDelete) subscriptionOfferService.delete(id);
            for (SubscriptionOffer o : subsToUpdate.values()) subscriptionOfferService.update(o);
            for (SubscriptionOffer o : subsToAdd) subscriptionOfferService.add(o);
            closeSubsModal();
            loadSalles();
        } catch (Exception ex) {
            subsError.setText("Erreur d’enregistrement : " + ex.getMessage());
        }
    }

    private void closeSubsModal() {
        showSubs(false);
        currentSalleModal = null;
        subsToDelete.clear();
        subsToUpdate.clear();
        subsToAdd.clear();
        if (abonnementsTable != null) abonnementsTable.getItems().clear();
        subsError.setText("");
        if (nomAbonnementField != null) nomAbonnementField.clear();
        if (prixAbonnementField != null) prixAbonnementField.clear();
    }

    // ── Modèle simple pour la TableView Équipements ────────────────────────

    public static class Equipement {
        private final String nom;
        private final Integer quantite;

        public Equipement(String nom, Integer quantite) {
            this.nom = nom;
            this.quantite = quantite;
        }

        public String getNom() { return nom; }
        public Integer getQuantite() { return quantite; }
    }

    public static class Abonnement {
        private final String nom;
        private final Double prix;

        public Abonnement(String nom, Double prix) {
            this.nom = nom;
            this.prix = prix;
        }

        public String getNom() { return nom; }
        public Double getPrix() { return prix; }
    }

    public static class SubsRow {
        private final Integer id;
        private String name;
        private double price;
        private boolean editing = false;
        private String editName;
        private String editPriceRaw;
        private final SubscriptionOffer base;

        private SubsRow(Integer id, String name, double price, SubscriptionOffer base) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.base = base;
            this.editName = name;
            this.editPriceRaw = String.format("%.2f", price);
        }

        public static SubsRow from(SubscriptionOffer o) { return new SubsRow(o.getId(), o.getName(), o.getPrice(), o); }
        public static SubsRow fromNew(SubscriptionOffer o) { return new SubsRow(null, o.getName(), o.getPrice(), o); }

        public Integer getId() { return id; }
        public String getName() { return name; }
        public Double getPrice() { return price; }
        public String getActions() { return ""; }
        public boolean isEditing() { return editing; }
        public String getEditName() { return editName; }
        public String getEditPriceRaw() { return editPriceRaw; }

        public void startEdit() {
            editing = true;
            editName = name;
            editPriceRaw = String.format("%.2f", price);
        }
        public void cancelEdit() { editing = false; editName = name; editPriceRaw = String.format("%.2f", price); }

        public String commitEdit() {
            String n = editName == null ? "" : editName.trim();
            if (n.isEmpty()) return "Le nom de l’offre est requis.";
            double p;
            try { p = Double.parseDouble((editPriceRaw == null ? "" : editPriceRaw.trim()).replace(",", ".")); }
            catch (Exception ex) { return "Prix invalide."; }
            if (p < 0) return "Le prix doit être positif.";
            name = n;
            price = p;
            editing = false;
            return null;
        }

        public SubscriptionOffer toOffer(Integer gymId) {
            SubscriptionOffer o = base != null ? base : new SubscriptionOffer();
            o.setName(name);
            o.setPrice(price);
            if (gymId != null) o.setGymnasiumId(gymId);
            if (o.getDurationMonths() <= 0) o.setDurationMonths(1);
            if (o.getDescription() == null) o.setDescription("");
            return o;
        }
    }
}
