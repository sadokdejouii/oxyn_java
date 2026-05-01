package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.entities.Equipment;
import org.example.entities.Salle;
import org.example.services.AdminFormValidation;
import org.example.services.EquipmentService;
import org.example.services.SalleService;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class EquipmentController implements Initializable {

    @FXML private Label statTotal, statActifs, statQte, countLabel;
    @FXML private FlowPane equipGrid;
    @FXML private ToggleButton filterAll, filterActive, filterInactive;
    @FXML private ComboBox<Salle> filterSalle, fieldSalle;
    @FXML private StackPane dialogOverlay, confirmOverlay;
    @FXML private Label dialogTitle, dialogError, confirmMsg;
    @FXML private TextField fieldName, fieldQty;
    @FXML private TextArea fieldDesc;

    private final EquipmentService service = new EquipmentService();
    private final SalleService salleService = new SalleService();
    private List<Equipment> allItems = List.of();
    private Equipment editing = null, toDelete = null;
    private String currentFilter = "ALL";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadCombos(); filterSalle.setOnAction(e -> applyFilter()); load();
    }

    private void loadCombos() {
        javafx.util.StringConverter<Salle> conv = new javafx.util.StringConverter<>() {
            @Override public String toString(Salle s) { return s == null ? "" : s.getName(); }
            @Override public Salle fromString(String s) { return null; }
        };
        Salle all = new Salle(); all.setId(0); all.setName("Toutes les salles");
        Salle none = new Salle(); none.setId(-1); none.setName("Aucune salle");
        try {
            List<Salle> salles = salleService.afficher();
            filterSalle.getItems().setAll(); filterSalle.getItems().add(all); filterSalle.getItems().addAll(salles);
            filterSalle.getSelectionModel().selectFirst(); filterSalle.setConverter(conv);
            fieldSalle.getItems().setAll(); fieldSalle.getItems().add(none); fieldSalle.getItems().addAll(salles);
            fieldSalle.getSelectionModel().selectFirst(); fieldSalle.setConverter(conv);
        } catch (SQLException ignored) {}
    }

    private void load() {
        try { allItems = service.afficher(); } catch (SQLException e) { e.printStackTrace(); }
        statTotal.setText(String.valueOf(allItems.size()));
        statActifs.setText(String.valueOf(allItems.stream().filter(Equipment::isActive).count()));
        statQte.setText(String.valueOf(allItems.stream().mapToInt(Equipment::getQuantity).sum()));
        applyFilter();
    }

    private void applyFilter() {
        Salle sf = filterSalle.getValue();
        List<Equipment> f = allItems.stream().filter(e -> {
            if ("ACTIVE".equals(currentFilter) && !e.isActive()) return false;
            if ("INACTIVE".equals(currentFilter) && e.isActive()) return false;
            if (sf != null && sf.getId() > 0 && !Integer.valueOf(sf.getId()).equals(e.getGymnasiumId())) return false;
            return true;
        }).collect(Collectors.toList());
        equipGrid.getChildren().clear();
        countLabel.setText(f.size() + " equipement(s)");
        f.forEach(e -> equipGrid.getChildren().add(buildCard(e)));
    }

    @FXML private void handleFilter(javafx.event.ActionEvent ev) {
        ToggleButton src = (ToggleButton) ev.getSource();
        currentFilter = (String) src.getUserData();
        filterAll.setSelected("ALL".equals(currentFilter));
        filterActive.setSelected("ACTIVE".equals(currentFilter));
        filterInactive.setSelected("INACTIVE".equals(currentFilter));
        applyFilter();
    }

    @FXML private void handleRefresh() { load(); }

    private VBox buildCard(Equipment e) {
        VBox card = new VBox(0); card.setPrefWidth(320); card.setMaxWidth(380); card.getStyleClass().add("sess-card");
        HBox header = new HBox(10); header.getStyleClass().add("sess-card-header"); header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("🔧  " + e.getName()); title.getStyleClass().add("sess-card-type"); title.setWrapText(true); HBox.setHgrow(title, Priority.ALWAYS);
        Label badge = new Label(e.isActive() ? "Actif" : "Inactif");
        badge.getStyleClass().add(e.isActive() ? "sess-badge-planifiee" : "sess-badge-inactive");
        header.getChildren().addAll(title, badge);
        VBox body = new VBox(6); body.getStyleClass().add("sess-card-body");
        body.getChildren().addAll(row("🏢", e.getGymnasiumName() != null ? e.getGymnasiumName() : "—"), row("📦", "Qte : " + e.getQuantity()));
        if (e.getDescription() != null && !e.getDescription().isBlank()) { Label d = new Label(e.getDescription()); d.getStyleClass().add("sess-card-notes"); d.setWrapText(true); body.getChildren().add(d); }
        HBox actions = new HBox(8); actions.getStyleClass().add("sess-card-actions"); actions.setAlignment(Pos.CENTER_LEFT);
        Button bEdit = new Button("Modifier"); bEdit.getStyleClass().add("sess-btn-edit"); bEdit.setOnAction(ev -> openEdit(e));
        Button bToggle = new Button(e.isActive() ? "Desactiver" : "Activer"); bToggle.getStyleClass().add(e.isActive() ? "sess-btn-delete" : "sess-btn-edit"); bToggle.setOnAction(ev -> toggle(e));
        Button bDel = new Button("Supprimer"); bDel.getStyleClass().add("sess-btn-delete"); bDel.setOnAction(ev -> openConfirm(e));
        actions.getChildren().addAll(bEdit, bToggle, bDel);
        card.getChildren().addAll(header, body, actions); return card;
    }

    private HBox row(String icon, String value) {
        HBox r = new HBox(8); r.setAlignment(Pos.CENTER_LEFT);
        Label i = new Label(icon); i.setStyle("-fx-font-size:13px;");
        Label v = new Label(value != null ? value : "—"); v.getStyleClass().add("sess-card-info"); v.setWrapText(true); HBox.setHgrow(v, Priority.ALWAYS);
        r.getChildren().addAll(i, v); return r;
    }

    @FXML public void handleAjouter() { editing = null; dialogTitle.setText("Nouvel equipement"); clearDialog(); showDialog(true); }

    private void openEdit(Equipment e) {
        editing = e; dialogTitle.setText("Modifier l'equipement");
        fieldName.setText(e.getName()); fieldQty.setText(String.valueOf(e.getQuantity()));
        fieldDesc.setText(e.getDescription() != null ? e.getDescription() : "");
        if (e.getGymnasiumId() != null) fieldSalle.getItems().stream().filter(s -> s.getId() == e.getGymnasiumId()).findFirst().ifPresent(fieldSalle::setValue);
        else fieldSalle.getSelectionModel().selectFirst();
        dialogError.setText(""); showDialog(true);
    }

    @FXML private void handleSave() {
        Salle s = fieldSalle.getValue();
        Integer gymId = (s != null && s.getId() > 0) ? s.getId() : null;
        String err = AdminFormValidation.validateEquipmentAdminForm(
                fieldName.getText(), fieldDesc.getText(), fieldQty.getText(), gymId);
        if (err != null) {
            dialogError.setText(err);
            return;
        }
        int qty = Integer.parseInt(fieldQty.getText().trim());
        String name = fieldName.getText().trim();
        try {
            if (editing == null) service.ajouter(new Equipment(name, fieldDesc.getText().trim(), qty, gymId));
            else { editing.setName(name); editing.setDescription(fieldDesc.getText().trim()); editing.setQuantity(qty); editing.setGymnasiumId(gymId); service.modifier(editing); }
            showDialog(false); load();
        } catch (SQLException e) { dialogError.setText("Erreur : " + e.getMessage()); }
    }

    @FXML private void handleCancel() { showDialog(false); }
    private void toggle(Equipment e) { try { service.toggleActive(e.getId(), !e.isActive()); load(); } catch (SQLException ex) { ex.printStackTrace(); } }
    private void openConfirm(Equipment e) { toDelete = e; confirmMsg.setText("\"" + e.getName() + "\" sera supprime."); confirmOverlay.setVisible(true); confirmOverlay.setManaged(true); }
    @FXML private void handleConfirmDelete() { if (toDelete == null) return; try { service.supprimer(toDelete.getId()); load(); } catch (SQLException e) { e.printStackTrace(); } finally { confirmOverlay.setVisible(false); confirmOverlay.setManaged(false); toDelete = null; } }
    @FXML private void handleConfirmCancel() { confirmOverlay.setVisible(false); confirmOverlay.setManaged(false); toDelete = null; }
    private void showDialog(boolean show) { dialogOverlay.setVisible(show); dialogOverlay.setManaged(show); }
    private void clearDialog() { fieldName.clear(); fieldQty.clear(); fieldDesc.clear(); fieldSalle.getSelectionModel().selectFirst(); dialogError.setText(""); }
}
