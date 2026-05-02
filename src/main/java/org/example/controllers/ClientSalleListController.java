package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.example.entities.Salle;
import org.example.services.ClientSalleService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ClientSalleListController implements Initializable {

    @FXML private FlowPane  sallesGrid;
    @FXML private Label     countLabel, statSalles, statOffres, statSessions;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;

    private static final String SORT_NOM    = "Nom (A→Z)";
    private static final String SORT_NOTE   = "Meilleure note";
    private static final String SORT_AVIS   = "Plus d'avis";

    private final ClientSalleService service = new ClientSalleService();
    private List<Salle> allSalles = List.of();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sortCombo.getItems().addAll(SORT_NOM, SORT_NOTE, SORT_AVIS);
        sortCombo.setValue(SORT_NOM);
        // Live search — fires on every keystroke
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        load();
    }

    private void load() {
        try {
            allSalles = service.getSallesActives();
            statSalles.setText(String.valueOf(allSalles.size()));
            // Count offers and sessions across all salles
            int totalOffres = 0, totalSessions = 0;
            for (Salle s : allSalles) {
                try { totalOffres   += service.getOffres(s.getId()).size(); }   catch (SQLException ignored) {}
                try { totalSessions += service.getSessions(s.getId()).size(); } catch (SQLException ignored) {}
            }
            statOffres.setText(String.valueOf(totalOffres));
            statSessions.setText(String.valueOf(totalSessions));
        } catch (SQLException e) {
            e.printStackTrace();
            allSalles = List.of();
            if (statSalles != null) {
                statSalles.setText("0");
            }
            if (statOffres != null) {
                statOffres.setText("0");
            }
            if (statSessions != null) {
                statSessions.setText("0");
            }
            if (countLabel != null) {
                countLabel.setText("Erreur : " + (e.getMessage() != null ? e.getMessage() : "base de données"));
            }
        }
        applyFilter();
    }

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String sort = sortCombo.getValue();

        List<Salle> filtered = allSalles.stream()
            .filter(s -> q.isEmpty()
                || s.getName().toLowerCase().contains(q)
                || (s.getAddress() != null && s.getAddress().toLowerCase().contains(q)))
            .collect(Collectors.toList());

        // Sort
        Comparator<Salle> cmp = SORT_NOTE.equals(sort)
            ? Comparator.comparingDouble(Salle::getRating).reversed()
            : SORT_AVIS.equals(sort)
                ? Comparator.comparingInt(Salle::getRatingCount).reversed()
                : Comparator.comparing(s -> s.getName().toLowerCase());

        filtered.sort(cmp);

        sallesGrid.getChildren().clear();
        countLabel.setText(filtered.size() + " salle(s)");
        for (Salle s : filtered) sallesGrid.getChildren().add(buildCard(s));
    }

    @FXML private void handleRefresh() { load(); }

    private VBox buildCard(Salle s) {
        VBox card = new VBox(0);
        card.setPrefWidth(320);
        card.getStyleClass().add("cl-salle-card");

        // Photo
        StackPane photoPane = new StackPane();
        photoPane.setPrefHeight(160);
        photoPane.getStyleClass().add("cl-card-photo-wrap");
        String imgUrl = s.getImageUrl();
        if (imgUrl != null && !imgUrl.isBlank() && new File(imgUrl).exists()) {
            ImageView iv = new ImageView(new Image("file:///" + imgUrl.replace("\\", "/"), 320, 160, false, true));
            iv.setFitWidth(320); iv.setFitHeight(160); iv.setPreserveRatio(false);
            photoPane.getChildren().add(iv);
        } else {
            Label ph = new Label("Aucune photo"); ph.getStyleClass().add("cl-no-photo");
            photoPane.getChildren().add(ph);
        }

        // Content
        VBox content = new VBox(8);
        content.setPadding(new Insets(14, 16, 14, 16));

        HBox topRow = new HBox(8); topRow.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(s.getName()); name.getStyleClass().add("cl-card-title"); name.setWrapText(true); HBox.setHgrow(name, Priority.ALWAYS);
        Label badge = new Label("Active");
        badge.getStyleClass().addAll("front-status-chip", "front-status-active");
        topRow.getChildren().addAll(name, badge);

        Label rating = new Label("★ " + String.format("%.1f", s.getRating()) + "  (" + s.getRatingCount() + " avis)");
        rating.getStyleClass().add("cl-card-rating");

        VBox infos = new VBox(4);
        infos.getChildren().addAll(
            infoRow("📍", s.getAddress()),
            infoRow("📞", s.getPhone()),
            infoRow("✉", s.getEmail())
        );

        Separator sep = new Separator();
        sep.getStyleClass().add("client-salle-card-sep");

        Button btnVoir = new Button("Voir la fiche");
        btnVoir.getStyleClass().addAll("front-card-button", "client-salle-primary-btn");
        btnVoir.setMaxWidth(Double.MAX_VALUE);
        btnVoir.setOnAction(e -> {
            btnVoir.setDisable(true);  // ← empêche les clics multiples
            try {
                openDetail(s);
            } finally {
                btnVoir.setDisable(false);  // ← réactiver après
            }
        });

        content.getChildren().addAll(topRow, rating, infos, sep, btnVoir);
        card.getChildren().addAll(photoPane, content);
        return card;
    }

    private HBox infoRow(String icon, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("client-salle-meta-row");
        Label ico = new Label(icon);
        ico.getStyleClass().add("front-meta-icon");
        Label val = new Label(value != null && !value.isBlank() ? value : "—");
        val.getStyleClass().addAll("cl-card-info", "front-meta-text");
        val.setWrapText(true);
        HBox.setHgrow(val, Priority.ALWAYS);
        row.getChildren().addAll(ico, val);
        return row;
    }

    private void openDetail(Salle salle) {
        try {
            // Recharger la salle complète depuis BD pour inclure youtube_url
            Salle salleComplete = service.getById(salle.getId());
            if (salleComplete == null) {
                System.err.println("Erreur : salle non trouvée en BD pour ID=" + salle.getId());
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/pages/ClientSalleDetail.fxml"));
            javafx.scene.Node detail = loader.load();
            ClientSalleDetailController ctrl = loader.getController();
            ctrl.setSalle(salleComplete);

            // Récupérer le MainLayoutController et le passer au contrôleur de détail
            javafx.scene.Node root = sallesGrid.getScene().getRoot();
            System.out.println("🔍 DEBUG: Root class = " + root.getClass().getSimpleName());
            System.out.println("🔍 DEBUG: Root userData = " + root.getUserData());
            
            MainLayoutController mainLayoutCtrl = (MainLayoutController) root.getUserData();
            if (mainLayoutCtrl != null) {
                System.out.println("✅ DEBUG: MainLayoutController trouvé, passage au contrôleur détail");
                ctrl.setMainLayoutController(mainLayoutCtrl);
            } else {
                System.out.println("❌ DEBUG: MainLayoutController null - recherche alternative");
                // Alternative: chercher dans la hiérarchie
                mainLayoutCtrl = findMainLayoutController(root);
                if (mainLayoutCtrl != null) {
                    System.out.println("✅ DEBUG: MainLayoutController trouvé par recherche hiérarchique");
                    ctrl.setMainLayoutController(mainLayoutCtrl);
                } else {
                    System.out.println("❌ DEBUG: MainLayoutController introuvable - utilisation fallback");
                }
            }

            // Find contentArea StackPane in the scene
            javafx.scene.layout.StackPane contentArea =
                (javafx.scene.layout.StackPane) root.lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(detail);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Recherche le MainLayoutController dans la hiérarchie des nœuds
     */
    private MainLayoutController findMainLayoutController(javafx.scene.Node node) {
        if (node == null) return null;
        
        // Vérifier si le nœud lui-même est le contrôleur
        if (node.getUserData() instanceof MainLayoutController) {
            return (MainLayoutController) node.getUserData();
        }
        
        // Si c'est un parent, chercher dans ses enfants
        if (node instanceof javafx.scene.Parent) {
            javafx.scene.Parent parent = (javafx.scene.Parent) node;
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                MainLayoutController ctrl = findMainLayoutController(child);
                if (ctrl != null) return ctrl;
            }
        }
        
        return null;
    }
}
