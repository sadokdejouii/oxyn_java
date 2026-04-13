package org.example.controllers;



import javafx.fxml.FXML;

import javafx.fxml.FXMLLoader;

import javafx.fxml.Initializable;

import javafx.scene.Node;

import javafx.scene.control.Label;

import javafx.scene.input.MouseEvent;

import javafx.scene.layout.Region;

import javafx.scene.layout.StackPane;

import javafx.scene.layout.VBox;

import org.example.services.EquipmentService;

import org.example.services.SalleService;

import org.example.services.SubscriptionOfferService;



import java.net.URL;

import java.sql.SQLException;

import java.util.HashMap;

import java.util.Map;

import java.util.ResourceBundle;



public class AdminSalleHubController implements Initializable {



    @FXML private StackPane hubContent;

    @FXML private VBox      tabSalles, tabEquip, tabSubs;

    @FXML private Label     badgeSalles, badgeEquip, badgeSubs;

    @FXML private Region    indSalles, indEquip, indSubs;



    private static final String PATH_SALLES = "/FXML/pages/SalleManagement.fxml";

    private static final String PATH_EQUIP  = "/FXML/pages/EquipmentManagement.fxml";

    private static final String PATH_SUBS   = "/FXML/pages/SubscriptionManagement.fxml";



    private final Map<String, Node>   cache       = new HashMap<>();

    private final Map<String, Object> controllers = new HashMap<>();

    private String currentKey = "SALLES";



    private final SalleService             salleService = new SalleService();

    private final EquipmentService         equipService = new EquipmentService();

    private final SubscriptionOfferService subsService  = new SubscriptionOfferService();



    @Override

    public void initialize(URL location, ResourceBundle resources) {

        loadBadges();

        showTab("SALLES");

        activateTab(tabSalles, "SALLES");

    }



    private void loadBadges() {

        try { badgeSalles.setText(String.valueOf(salleService.afficher().size())); } catch (SQLException e) { badgeSalles.setText("?"); }

        try { badgeEquip.setText(String.valueOf(equipService.afficher().size())); }  catch (SQLException e) { badgeEquip.setText("?"); }

        try { badgeSubs.setText(String.valueOf(subsService.afficher().size())); }    catch (SQLException e) { badgeSubs.setText("?"); }

    }



    @FXML

    private void handleTab(MouseEvent e) {

        VBox src = (VBox) e.getSource();

        String key;

        if (src == tabEquip)      key = "EQUIP";

        else if (src == tabSubs)  key = "SUBS";

        else                      key = "SALLES";

        activateTab(src, key);

        showTab(key);

    }



    @FXML

    private void handleRefresh() {

        cache.remove(currentKey);

        controllers.remove(currentKey);

        loadBadges();

        showTab(currentKey);

    }



    private void showTab(String key) {

        currentKey = key;

        if (!cache.containsKey(key)) {

            String path = switch (key) {

                case "EQUIP" -> PATH_EQUIP;

                case "SUBS"  -> PATH_SUBS;

                default      -> PATH_SALLES;

            };

            try {

                FXMLLoader loader = new FXMLLoader(getClass().getResource(path));

                Node node = loader.load();

                cache.put(key, node);

                controllers.put(key, loader.getController());

            } catch (Exception ex) {

                ex.printStackTrace();

                Label err = new Label("Erreur : " + ex.getMessage());

                err.setStyle("-fx-text-fill:#f85149;-fx-font-size:13px;-fx-padding:20px;");

                hubContent.getChildren().setAll(err);

                return;

            }

        }

        hubContent.getChildren().setAll(cache.get(key));

    }



    private void activateTab(VBox tab, String key) {

        tabSalles.getStyleClass().removeAll("hub-tab-active", "sh-tab-active");

        tabEquip.getStyleClass().removeAll("hub-tab-active", "sh-tab-active");

        tabSubs.getStyleClass().removeAll("hub-tab-active", "sh-tab-active");



        resetIndicator(indSalles);

        resetIndicator(indEquip);

        resetIndicator(indSubs);



        tab.getStyleClass().addAll("hub-tab-active", "sh-tab-active");

        Region ind = switch (key) {

            case "EQUIP" -> indEquip;

            case "SUBS"  -> indSubs;

            default      -> indSalles;

        };

        ind.getStyleClass().remove("sh-tab-indicator");

        ind.getStyleClass().add("sh-tab-indicator-active");

    }



    private static void resetIndicator(Region ind) {

        ind.getStyleClass().remove("sh-tab-indicator-active");

        if (!ind.getStyleClass().contains("sh-tab-indicator")) {

            ind.getStyleClass().add("sh-tab-indicator");

        }

    }

}

