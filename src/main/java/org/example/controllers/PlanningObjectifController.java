package org.example.controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.entities.PanierSession;
import org.example.entities.produits;
import org.example.model.planning.objectif.ObjectifClientRow;
import org.example.services.ObjectifClientService;
import org.example.utils.ProductImageStorage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Vue « Objectif / Problème + Assistant IA + Recommandations boutique » (client Planning).
 */
public final class PlanningObjectifController {

    private final int userId;
    private final Runnable onBack;
    private final ObjectifClientService service = new ObjectifClientService();

    private TextArea txtObjectif;
    private VBox analysisCard;
    private VBox productsCard;
    private Label lblAnalysis;
    private Label lblEncadrantMsg;
    private Label lblInputError;
    private Label lblToast;
    private FlowPane productsGrid;
    private List<produits> lastRecommended = new ArrayList<>();

    public PlanningObjectifController(int userId, Runnable onBack) {
        this.userId = userId;
        this.onBack = onBack;
    }

    public VBox buildRoot() {
        Button btnBack = new Button("← Retour au planning");
        btnBack.getStyleClass().add("planning-obj-back");
        btnBack.setOnAction(e -> onBack.run());

        Label title = new Label("Objectif / Assistant IA & boutique");
        title.getStyleClass().add("planning-obj-title");

        Label lead = new Label("Décrivez un objectif, une gêne ou un besoin : l’outil propose une analyse structurée "
                + "et des produits du catalogue OXYN (sans API externe).");
        lead.setWrapText(true);
        lead.getStyleClass().add("planning-obj-lead");

        txtObjectif = new TextArea();
        txtObjectif.setPromptText("Décrivez votre objectif ou problème… (ex. douleur au genou en montée, fatigue en fin de journée, manque de souffle à l’effort)");
        txtObjectif.setWrapText(true);
        txtObjectif.setPrefRowCount(5);
        txtObjectif.getStyleClass().add("planning-obj-input");

        lblInputError = new Label();
        lblInputError.getStyleClass().add("planning-obj-inline-error");
        lblInputError.setManaged(false);
        lblInputError.setVisible(false);

        Button btnAnalyze = new Button("Analyser avec IA");
        btnAnalyze.getStyleClass().addAll("planning-primary-btn", "planning-obj-analyze");
        btnAnalyze.setOnAction(e -> runAnalyze());

        VBox section1 = new VBox(12);
        section1.getStyleClass().addAll("planning-card", "planning-obj-card");
        section1.getChildren().addAll(
                sectionHeader("1 · Saisie objectif", "Décrivez votre objectif ou problème (ex : douleur genou, fatigue...)."),
                txtObjectif,
                lblInputError,
                btnAnalyze);

        lblAnalysis = new Label();
        lblAnalysis.setWrapText(true);
        lblAnalysis.setMaxWidth(Double.MAX_VALUE);
        lblAnalysis.getStyleClass().add("planning-obj-analysis-content");

        lblEncadrantMsg = new Label();
        lblEncadrantMsg.setWrapText(true);
        lblEncadrantMsg.getStyleClass().add("planning-obj-encadrant");
        lblEncadrantMsg.setManaged(false);
        lblEncadrantMsg.setVisible(false);

        analysisCard = new VBox(12);
        analysisCard.getStyleClass().addAll("planning-card", "planning-obj-card");
        analysisCard.setVisible(false);
        analysisCard.setManaged(false);
        analysisCard.getChildren().addAll(
                sectionHeader("2 · Analyse de votre situation", "Problème détecté · Explication · Conseils"),
                lblEncadrantMsg,
                lblAnalysis);

        productsGrid = new FlowPane();
        productsGrid.setHgap(16);
        productsGrid.setVgap(16);
        productsGrid.setPrefWrapLength(1080);
        productsGrid.getStyleClass().add("planning-obj-product-grid");

        productsCard = new VBox(12);
        productsCard.getStyleClass().addAll("planning-card", "planning-obj-card");
        productsCard.setVisible(false);
        productsCard.setManaged(false);
        productsCard.getChildren().addAll(
                sectionHeader("3 · Produits recommandés", "Ajoutez au panier ou ignorez une suggestion."),
                productsGrid);

        lblToast = new Label();
        lblToast.getStyleClass().add("planning-obj-toast");
        lblToast.setManaged(false);
        lblToast.setVisible(false);

        VBox root = new VBox(20);
        root.setPadding(new Insets(4, 4, 24, 4));
        root.setAlignment(Pos.TOP_CENTER);
        root.setMaxWidth(Double.MAX_VALUE);
        root.getStyleClass().add("planning-objectif-root");
        root.getChildren().addAll(btnBack, title, lead, section1, analysisCard, productsCard, lblToast);
        VBox.setVgrow(productsCard, Priority.ALWAYS);

        tryLoadLatest();
        return root;
    }

    private static VBox sectionHeader(String t, String sub) {
        Label a = new Label(t);
        a.getStyleClass().add("planning-card-title");
        Label b = new Label(sub);
        b.setWrapText(true);
        b.getStyleClass().add("planning-hint");
        return new VBox(4, a, b);
    }

    private void tryLoadLatest() {
        try {
            Optional<ObjectifClientRow> opt = service.findLatestForUser(userId);
            if (opt.isEmpty()) {
                return;
            }
            ObjectifClientRow r = opt.get();
            if (r.texteObjectif() != null && !r.texteObjectif().isBlank()
                    && !"Suivi généré automatiquement".equalsIgnoreCase(r.texteObjectif().trim())) {
                txtObjectif.setText(r.texteObjectif());
            }
        } catch (SQLException ex) {
            // ignore — table peut être absente au premier lancement
        }
    }

    private void runAnalyze() {
        hideToast();
        clearInputError();
        showAnalysisCard(false);
        showProductsCard(false);
        String text = txtObjectif.getText();
        if (text == null || text.isBlank()) {
            setInputError("Veuillez saisir un objectif avant analyse.");
            return;
        }
        lblEncadrantMsg.setVisible(false);
        lblEncadrantMsg.setManaged(false);
        try {
            ObjectifClientService.AnalyseResult res = service.analyzeAndSave(userId, text);
            lblAnalysis.setText(res.row().reponseIa());
            showAnalysisCard(true);
            if (res.row().interventionEncadrant() != null && !res.row().interventionEncadrant().isBlank()) {
                lblEncadrantMsg.setText("Message de votre encadrant :\n" + res.row().interventionEncadrant().trim());
                lblEncadrantMsg.setVisible(true);
                lblEncadrantMsg.setManaged(true);
            }
            List<produits> recommended = res.recommendedProducts() != null ? res.recommendedProducts() : List.of();
            if (!recommended.isEmpty()) {
                fillProductGrid(recommended);
                showProductsCard(true);
            } else {
                productsGrid.getChildren().clear();
                showProductsCard(false);
            }
        } catch (SQLException ex) {
            alert(Alert.AlertType.ERROR, "Analyse", ex.getMessage() != null ? ex.getMessage() : ex.toString());
        }
    }

    private void showAnalysisCard(boolean on) {
        analysisCard.setVisible(on);
        analysisCard.setManaged(on);
    }

    private void showProductsCard(boolean on) {
        productsCard.setVisible(on);
        productsCard.setManaged(on);
    }

    private void setInputError(String msg) {
        lblInputError.setText(msg);
        lblInputError.setVisible(true);
        lblInputError.setManaged(true);
    }

    private void clearInputError() {
        lblInputError.setText("");
        lblInputError.setVisible(false);
        lblInputError.setManaged(false);
    }

    private void showToast(String msg) {
        lblToast.setText(msg);
        lblToast.setVisible(true);
        lblToast.setManaged(true);
    }

    private void hideToast() {
        lblToast.setText("");
        lblToast.setVisible(false);
        lblToast.setManaged(false);
    }

    private VBox buildProductCard(produits p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("planning-obj-product-card");
        card.setPrefWidth(220);
        card.setMaxWidth(240);

        ImageView img = new ImageView();
        img.setFitWidth(200);
        img.setFitHeight(120);
        img.setPreserveRatio(true);
        ProductImageStorage.applyToImageView(img, p.getImage_produit());
        img.getStyleClass().add("planning-obj-product-img");

        Label nm = new Label(p.getNom_produit() != null ? p.getNom_produit() : "—");
        nm.setWrapText(true);
        nm.getStyleClass().add("planning-obj-product-name");

        String desc = p.getDescription_produit();
        if (desc != null && desc.length() > 140) {
            desc = desc.substring(0, 137) + "…";
        }
        Label ds = new Label(desc != null ? desc : "");
        ds.setWrapText(true);
        ds.getStyleClass().add("planning-obj-product-desc");

        Label pr = new Label(String.format(Locale.FRANCE, "%.2f TND", p.getPrix_produit()));
        pr.getStyleClass().add("planning-obj-product-price");

        Button add = new Button("Ajouter au panier");
        add.getStyleClass().add("planning-primary-btn");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(e -> {
            PanierSession.getInstance().ajouterProduit(p);
            showToast("Produit ajouté au panier");
        });
        card.getChildren().addAll(img, nm, ds, pr, add);
        return card;
    }

    private void fillProductGrid(List<produits> products) {
        lastRecommended = products != null ? new ArrayList<>(products) : new ArrayList<>();
        productsGrid.getChildren().clear();
        for (produits pr : lastRecommended) {
            productsGrid.getChildren().add(buildProductCard(pr));
        }
    }

    private static void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
