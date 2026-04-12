package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.services.EquipmentService;
import org.example.services.SalleService;
import org.example.services.SubscriptionOfferService;

import java.io.IOException;
import java.net.URL;
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
    private VBox   activeTab  = null;
    private String currentKey = "SALLES";

    private final SalleService             salleService = new SalleService();
    private final EquipmentService         equipService = new EquipmentService();
    private final SubscriptionOfferService subsService  = new SubscriptionOfferService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeTab = tabSalles;
        // Load badges in background to avoid blocking UI thread
        javafx.concurrent.Task<int[]> task = new javafx.concurrent.Task<>() {
            @Override protected int[] call() throws Exception {
                int s = 0, e = 0, o = 0;
                try { s = salleService.afficher().size(); } catch (Exception ignored) {}
                try { e = equipService.afficher().size(); } catch (Exception ignored) {}
                try { o = subsService.afficher().size(); } catch (Exception ignored) {}
                return new int[]{s, e, o};
            }
        };
        task.setOnSucceeded(ev -> {
            int[] counts = task.getValue();
            badgeSalles.setText(String.valueOf(counts[0]));
            badgeEquip.setText(String.valueOf(counts[1]));
            badgeSubs.setText(String.valueOf(counts[2]));
        });
        task.setOnFailed(ev -> {
            badgeSalles.setText("?"); badgeEquip.setText("?"); badgeSubs.setText("?");
        });
        new Thread(task, "badge-loader").start();
        // Load first tab content
        showTab("SALLES");
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
        javafx.concurrent.Task<int[]> task = new javafx.concurrent.Task<>() {
            @Override protected int[] call() throws Exception {
                int s = 0, e = 0, o = 0;
                try { s = salleService.afficher().size(); } catch (Exception ignored) {}
                try { e = equipService.afficher().size(); } catch (Exception ignored) {}
                try { o = subsService.afficher().size(); } catch (Exception ignored) {}
                return new int[]{s, e, o};
            }
        };
        task.setOnSucceeded(ev -> {
            int[] counts = task.getValue();
            badgeSalles.setText(String.valueOf(counts[0]));
            badgeEquip.setText(String.valueOf(counts[1]));
            badgeSubs.setText(String.valueOf(counts[2]));
            showTab(currentKey);
        });
        new Thread(task, "badge-refresh").start();
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
        deactivate(tabSalles, badgeSalles, indSalles);
        deactivate(tabEquip,  badgeEquip,  indEquip);
        deactivate(tabSubs,   badgeSubs,   indSubs);

        tab.getStyleClass().add("hub-tab-active");
        Label badge = switch (key) { case "EQUIP" -> badgeEquip; case "SUBS" -> badgeSubs; default -> badgeSalles; };
        badge.getStyleClass().remove("hub-badge");
        badge.getStyleClass().add("hub-badge-active");
        Region ind = switch (key) { case "EQUIP" -> indEquip; case "SUBS" -> indSubs; default -> indSalles; };
        ind.getStyleClass().remove("hub-tab-indicator");
        ind.getStyleClass().add("hub-tab-indicator-active");
        activeTab = tab;

        switch (key) {
            case "SALLES", "EQUIP", "SUBS" -> {} // subtitle removed
        }
    }

    private void deactivate(VBox tab, Label badge, Region ind) {
        tab.getStyleClass().remove("hub-tab-active");
        badge.getStyleClass().remove("hub-badge-active");
        if (!badge.getStyleClass().contains("hub-badge")) badge.getStyleClass().add("hub-badge");
        ind.getStyleClass().remove("hub-tab-indicator-active");
        if (!ind.getStyleClass().contains("hub-tab-indicator")) ind.getStyleClass().add("hub-tab-indicator");
    }
}