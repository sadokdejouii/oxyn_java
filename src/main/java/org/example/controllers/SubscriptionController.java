package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.entities.Salle;
import org.example.entities.SubscriptionOffer;
import org.example.services.AdminFormValidation;
import org.example.services.SalleService;
import org.example.services.SubscriptionOfferService;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SubscriptionController implements Initializable {

    @FXML private Label statTotal, statActives, statPrix, countLabel;
    @FXML private FlowPane offersGrid;
    @FXML private ToggleButton filterAll, filterActive, filterInactive;
    @FXML private StackPane dialogOverlay, confirmOverlay, ordersOverlay;
    @FXML private Label dialogTitle, dialogError, confirmMsg;
    @FXML private TextField fieldName, fieldDuration, fieldPrice;
    @FXML private TextArea fieldDesc;
    @FXML private ComboBox<Salle> fieldSalle;
    @FXML private TableView<String[]> ordersTable;
    @FXML private TableColumn<String[], String> colOrderId, colUserId, colQty, colUnitPrice, colTotal, colStatus, colDate;

    private final SubscriptionOfferService service = new SubscriptionOfferService();
    private final SalleService salleService = new SalleService();
    private List<SubscriptionOffer> allItems = List.of();
    private SubscriptionOffer editing = null, toDelete = null;
    private String currentFilter = "ALL";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadCombo(); setupTable(); load();
    }

    private void loadCombo() {
        Salle none = new Salle(); none.setId(0); none.setName("Aucune salle");
        javafx.util.StringConverter<Salle> conv = new javafx.util.StringConverter<>() {
            @Override public String toString(Salle s) { return s == null ? "" : s.getName(); }
            @Override public Salle fromString(String s) { return null; }
        };
        fieldSalle.getItems().clear(); fieldSalle.getItems().add(none);
        try { fieldSalle.getItems().addAll(salleService.afficher()); } catch (Exception ignored) {}
        fieldSalle.getSelectionModel().selectFirst(); fieldSalle.setConverter(conv);
    }

    private void setupTable() {
        colOrderId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        colUserId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        colQty.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));
        colUnitPrice.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[3]));
        colTotal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[4]));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[5]));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[6]));
    }

    private void load() {
        allItems = service.getAll();
        statTotal.setText(String.valueOf(allItems.size()));
        statActives.setText(String.valueOf(allItems.stream().filter(SubscriptionOffer::isActive).count()));
        statPrix.setText(String.format("%.2f", allItems.stream().mapToDouble(SubscriptionOffer::getPrice).average().orElse(0)));
        applyFilter();
    }

    private void applyFilter() {
        List<SubscriptionOffer> f = allItems.stream().filter(o -> {
            if ("ACTIVE".equals(currentFilter) && !o.isActive()) return false;
            if ("INACTIVE".equals(currentFilter) && o.isActive()) return false;
            return true;
        }).collect(Collectors.toList());
        offersGrid.getChildren().clear();
        countLabel.setText(f.size() + " offre(s)");
        f.forEach(o -> offersGrid.getChildren().add(buildCard(o)));
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

    private VBox buildCard(SubscriptionOffer o) {
        VBox card = new VBox(0); card.setPrefWidth(320); card.setMaxWidth(400); card.getStyleClass().add("sess-card");
        HBox header = new HBox(10); header.getStyleClass().add("sess-card-header"); header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("💳  " + o.getName()); title.getStyleClass().add("sess-card-type"); title.setWrapText(true); HBox.setHgrow(title, Priority.ALWAYS);
        Label badge = new Label(o.isActive() ? "Active" : "Inactive"); badge.getStyleClass().add(o.isActive() ? "sess-badge-planifiee" : "sess-badge-inactive");
        header.getChildren().addAll(title, badge);
        VBox body = new VBox(6); body.getStyleClass().add("sess-card-body");
        body.getChildren().addAll(row("🏢", o.getGymnasiumName() != null ? o.getGymnasiumName() : "—"), row("📅", o.getDurationMonths() + " mois"), row("💰", String.format("%.2f TND", o.getPrice())));
        if (o.getDescription() != null && !o.getDescription().isBlank()) { Label d = new Label(o.getDescription()); d.getStyleClass().add("sess-card-notes"); d.setWrapText(true); body.getChildren().add(d); }
        HBox actions = new HBox(8); actions.getStyleClass().add("sess-card-actions"); actions.setAlignment(Pos.CENTER_LEFT);
        Button bEdit = new Button("Modifier"); bEdit.getStyleClass().add("sess-btn-edit"); bEdit.setOnAction(ev -> openEdit(o));
        Button bToggle = new Button(o.isActive() ? "Desactiver" : "Activer"); bToggle.getStyleClass().add(o.isActive() ? "sess-btn-delete" : "sess-btn-edit"); bToggle.setOnAction(ev -> toggle(o));
        Button bOrders = new Button("Commandes"); bOrders.getStyleClass().add("sess-btn-edit"); bOrders.setOnAction(ev -> showOrders(o));
        Button bDel = new Button("Supprimer"); bDel.getStyleClass().add("sess-btn-delete"); bDel.setOnAction(ev -> openConfirm(o));
        actions.getChildren().addAll(bEdit, bToggle, bOrders, bDel);
        card.getChildren().addAll(header, body, actions); return card;
    }

    private HBox row(String icon, String value) {
        HBox r = new HBox(8); r.setAlignment(Pos.CENTER_LEFT);
        Label i = new Label(icon); i.setStyle("-fx-font-size:13px;");
        Label v = new Label(value != null ? value : "—"); v.getStyleClass().add("sess-card-info"); v.setWrapText(true); HBox.setHgrow(v, Priority.ALWAYS);
        r.getChildren().addAll(i, v); return r;
    }

    @FXML public void handleAjouter() { editing = null; dialogTitle.setText("Nouvelle offre"); clearDialog(); showDialog(true); }

    private void openEdit(SubscriptionOffer o) {
        editing = o; dialogTitle.setText("Modifier l'offre");
        fieldName.setText(o.getName()); fieldDuration.setText(String.valueOf(o.getDurationMonths())); fieldPrice.setText(String.format("%.2f", o.getPrice()));
        fieldDesc.setText(o.getDescription() != null ? o.getDescription() : "");
        if (o.getGymnasiumId() != null) fieldSalle.getItems().stream().filter(s -> s.getId() == o.getGymnasiumId()).findFirst().ifPresent(fieldSalle::setValue);
        else fieldSalle.getSelectionModel().selectFirst();
        dialogError.setText(""); showDialog(true);
    }

    @FXML private void handleSave() {
        Salle s = fieldSalle.getValue();
        Integer gymId = (s != null && s.getId() > 0) ? s.getId() : null;
        String err = AdminFormValidation.validateSubscriptionOfferForm(
                fieldName.getText(),
                fieldDuration.getText(),
                fieldPrice.getText(),
                fieldDesc.getText(),
                gymId);
        if (err != null) {
            dialogError.setText(err);
            return;
        }
        String name = fieldName.getText().trim();
        int dur = Integer.parseInt(fieldDuration.getText().trim());
        BigDecimal priceBd = AdminFormValidation.parsePriceTnd(fieldPrice.getText());
        double price = priceBd.doubleValue();
        try {
            if (editing == null) {
                service.add(new SubscriptionOffer(gymId, name, dur, price, fieldDesc.getText().trim()));
            } else {
                editing.setName(name);
                editing.setDurationMonths(dur);
                editing.setPrice(price);
                editing.setDescription(fieldDesc.getText().trim());
                editing.setGymnasiumId(gymId);
                service.update(editing);
            }
            showDialog(false); load();
        } catch (Exception e) { dialogError.setText("Erreur : " + e.getMessage()); }
    }

    @FXML private void handleCancel() { showDialog(false); }
    private void toggle(SubscriptionOffer o) { service.toggleActive(o.getId(), !o.isActive()); load(); }
    private void showOrders(SubscriptionOffer o) { 
    List<String[]> orders = service.getOrdersByOffer(o.getId());
    ordersTable.setItems(FXCollections.observableArrayList(orders)); 
    ordersOverlay.setVisible(true); 
    ordersOverlay.setManaged(true); 
    }
    @FXML private void handleCloseOrders() { ordersOverlay.setVisible(false); ordersOverlay.setManaged(false); }
    private void openConfirm(SubscriptionOffer o) { toDelete = o; confirmMsg.setText("\"" + o.getName() + "\" sera supprimee."); confirmOverlay.setVisible(true); confirmOverlay.setManaged(true); }
    @FXML private void handleConfirmDelete() { if (toDelete == null) return; service.delete(toDelete.getId()); load(); confirmOverlay.setVisible(false); confirmOverlay.setManaged(false); toDelete = null; }
    @FXML private void handleConfirmCancel() { confirmOverlay.setVisible(false); confirmOverlay.setManaged(false); toDelete = null; }
    private void showDialog(boolean show) { dialogOverlay.setVisible(show); dialogOverlay.setManaged(show); }
    private void clearDialog() { fieldName.clear(); fieldDuration.clear(); fieldPrice.clear(); fieldDesc.clear(); fieldSalle.getSelectionModel().selectFirst(); dialogError.setText(""); }
}
