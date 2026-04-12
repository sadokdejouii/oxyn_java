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

    private static final String SORT_NOM    = "Nom (A-Z)";
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
        Label badge = new Label("Actif"); badge.getStyleClass().add("cl-badge-active");
        topRow.getChildren().addAll(name, badge);

        Label rating = new Label("* " + String.format("%.1f", s.getRating()) + "  (" + s.getRatingCount() + " avis)");
        rating.getStyleClass().add("cl-card-rating");

        VBox infos = new VBox(4);
        infos.getChildren().addAll(
            infoRow("[Adresse]", s.getAddress()),
            infoRow("[Tel]", s.getPhone()),
            infoRow("[Email]", s.getEmail())
        );

        Separator sep = new Separator();

        Button btnVoir = new Button("Voir cette salle");
        btnVoir.getStyleClass().add("cl-btn-voir");
        btnVoir.setMaxWidth(Double.MAX_VALUE);
        btnVoir.setOnAction(e -> openDetail(s));

        content.getChildren().addAll(topRow, rating, infos, sep, btnVoir);
        card.getChildren().addAll(photoPane, content);
        return card;
    }

    private HBox infoRow(String icon, String value) {
        HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icon); ico.setStyle("-fx-font-size:12px;");
        Label val = new Label(value != null && !value.isBlank() ? value : "—");
        val.getStyleClass().add("cl-card-info"); val.setWrapText(true); HBox.setHgrow(val, Priority.ALWAYS);
        row.getChildren().addAll(ico, val); return row;
    }

    private void openDetail(Salle salle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/pages/ClientSalleDetail.fxml"));
            javafx.scene.Node detail = loader.load();
            ClientSalleDetailController ctrl = loader.getController();
            ctrl.setSalle(salle);

            // Walk up the parent chain to find contentArea StackPane
            javafx.scene.Node node = sallesGrid;
            while (node != null) {
                if (node instanceof javafx.scene.layout.StackPane sp
                        && "contentArea".equals(sp.getId())) {
                    sp.getChildren().setAll(detail);
                    return;
                }
                node = node.getParent();
            }
            // Fallback: scene-level lookup
            javafx.scene.Node found = sallesGrid.getScene().getRoot().lookup("#contentArea");
            if (found instanceof javafx.scene.layout.StackPane sp) {
                sp.getChildren().setAll(detail);
            } else {
                System.err.println("[ClientSalleList] contentArea not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
