package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.geometry.Pos;
import javafx.scene.web.WebView;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import org.example.utils.VideoPlayerHelper;
import org.example.services.YouTubeService;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.io.File;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import org.example.entities.Salle;
import org.example.entities.SubscriptionOffer;
import org.example.entities.Session;
import org.example.entities.GymRating;
import org.example.services.ClientSalleService;
import org.example.services.FlouciService;
import org.example.dao.GymRatingDAO;
import org.example.utils.CommandeClientResolver;
import org.example.utils.MyDataBase;
import org.example.utils.PageLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientSalleDetailController implements Initializable {

    @FXML private Button    btnBack;
    @FXML private Button    btnSubscribe;
    @FXML private Button    btnVoirPlanning;
    @FXML private StackPane photoPane;
    @FXML private ImageView sallePhoto;
    @FXML private Label     photoPlaceholder;
    @FXML private Label     salleName, salleDesc, salleAddr, sallePhone, salleEmail, salleRating;
    @FXML private FlowPane  offresGrid, sessionsGrid;
    @FXML private Label     noOffres, noSessions;
    @FXML private WebView  videoPlayer;
    @FXML private VBox     videoSection;
    @FXML private Label    noVideoLabel;
    @FXML private Label    videoLinkLabel;
    
    // Carte de localisation
    @FXML private WebView mapView;
    
    // Référence au contrôleur principal
    private MainLayoutController mainLayoutController;
    
    // Système de notation
    @FXML private HBox     starsContainer;
    @FXML private Label    ratingInfo;
    @FXML private TextArea commentArea;
    @FXML private Button   submitRatingBtn;
    @FXML private Label    ratingMessage;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ClientSalleService service = new ClientSalleService();
    private Salle salle;
    
    // Système de notation
    private GymRatingDAO ratingDAO;
    private List<Label> stars = new ArrayList<>();
    private int currentRating = 0;
    private GymRating existingRating = null;
    
    // ID du client connecté via le système d'authentification
    private final int currentUserId = CommandeClientResolver.idClientConnecte();
    
    // Flag pour éviter la double initialisation
    private boolean initialized = false;

    public void setSalle(Salle salle) {
        // Debug - vérifier les valeurs reçues
        System.out.println("=== SALLE REÇUE ===");
        System.out.println("ID       : " + salle.getId());
        System.out.println("Nom      : " + salle.getName());
        System.out.println("YouTube  : " + salle.getYoutubeUrl());
        System.out.println("===================");
        
        // ✅ Éviter la réinitialisation si même salle
        if (salle == null || salle.equals(this.salle)) {
            System.out.println("🔄 Même salle ou salle null - pas de réinitialisation");
            return;
        }
        
        this.salle = salle;
        
        // Forcer la réinitialisation complète pour nouvelle salle
        populateInfo();
        loadYoutubeVideo(salle.getYoutubeUrl());
        
        // Localisation avec fallback
        String location = salle.getAddress();
        if (location == null || location.isEmpty()) {
            System.out.println("⚠️ Localisation vide - utilisation du fallback Hammamet");
            location = "Hammamet, Tunisie"; // fallback par défaut
        } else {
            System.out.println("✅ Localisation trouvée: " + location);
        }
        loadMap(location);
        loadOffres();
        chargerSessions(salle.getId());
        initializeRatingSystem();
        
        initialized = true;
        System.out.println("✅ Salle initialisée avec succès");
    }

    /**
     * Définit le contrôleur principal pour la navigation
     */
    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        btnBack.setOnAction(e -> goBack());
        if (submitRatingBtn != null) {
            submitRatingBtn.setOnAction(e -> submitRating());
        } else {
            System.err.println("WARNING: submitRatingBtn not found in FXML - check fx:id");
        }
        
        // ✅ N'afficher le debug qu'une seule fois
        if (!initialized) {
            System.out.println("DEBUG: ClientSalleDetailController initialisé");
        }
    }
    
    /**
     * Retour à la liste des salles
     */
    private void goBack() {
        try {
            // Récupérer le conteneur principal (contentArea) depuis la scène
            javafx.scene.Node root = btnBack.getScene().getRoot();
            javafx.scene.layout.StackPane contentArea = 
                (javafx.scene.layout.StackPane) root.lookup("#contentArea");
            
            if (contentArea != null) {
                // ✅ Utiliser PageLoader comme les autres boutons de navigation
                PageLoader.show(contentArea, "/FXML/pages/ClientSalleList.fxml");
            } else {
                showInfo("Erreur", "Conteneur principal non trouvé");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Erreur", "Impossible de revenir à la liste: " + e.getMessage());
        }
    }

    private void populateInfo() {
        salleName.setText(salle.getName());
        salleDesc.setText(salle.getDescription() != null && !salle.getDescription().isBlank()
            ? salle.getDescription() : "Aucune description disponible.");
        salleAddr.setText(salle.getAddress() != null && !salle.getAddress().isBlank()
                ? salle.getAddress() : "Adresse non renseignée");
        sallePhone.setText(salle.getPhone() != null && !salle.getPhone().isBlank()
                ? salle.getPhone() : "Téléphone non renseigné");
        salleEmail.setText(salle.getEmail() != null && !salle.getEmail().isBlank()
                ? salle.getEmail() : "E-mail non renseigné");
        salleRating.setText(String.format("Note %.1f / 5", salle.getRating())
                + " · " + salle.getRatingCount() + " avis");

        String imgUrl = salle.getImageUrl();
        if (imgUrl != null && !imgUrl.isBlank() && new File(imgUrl).exists()) {
            sallePhoto.setImage(new Image("file:///" + imgUrl.replace("\\", "/"), 900, 220, false, true));
            photoPlaceholder.setVisible(false); photoPlaceholder.setManaged(false);
        } else {
            sallePhoto.setImage(null);
            photoPlaceholder.setVisible(true); photoPlaceholder.setManaged(true);
        }
    }

    private void loadMap(String adresse) {
        if (mapView == null) return;
        if (adresse == null || adresse.isBlank()) {
            mapView.setVisible(false);
            mapView.setManaged(false);
            return;
        }

        mapView.setVisible(true);
        mapView.setManaged(true);
        mapView.getEngine().setJavaScriptEnabled(true);

        String adresseUrl = adresse.replace(" ", "+").replace(",", "%2C");
        String adresseJs  = adresse.replace("'", "\\'");

        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'/>"
            + "<style>*{margin:0;padding:0;}html,body,#map{width:100%;height:100%;}</style>"
            + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"
            + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
            + "</head><body><div id='map'></div><script>"
            + "var map=L.map('map').setView([36.8,10.1],7);"
            + "L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',"
            + "{maxZoom:19,attribution:'© OSM'}).addTo(map);"
            + "fetch('https://nominatim.openstreetmap.org/search?q=" + adresseUrl
            + "&format=json&limit=1',{headers:{'Accept-Language':'fr'}})"
            + ".then(r=>r.json()).then(d=>{"
            + "if(d&&d.length>0){"
            + "var lt=parseFloat(d[0].lat),ln=parseFloat(d[0].lon);"
            + "map.setView([lt,ln],15);"
            + "L.marker([lt,ln]).addTo(map).bindPopup('<b>" + adresseJs + "</b>').openPopup();"
            + "}map.invalidateSize();});"
            + "setTimeout(function(){"
+ "  map.invalidateSize();"
+ "  setTimeout(function(){map.invalidateSize();},800);"
+ "},300);"
            + "</script></body></html>";

        mapView.getEngine().loadContent(html);
    }
    
    private void drawMap(javafx.scene.canvas.GraphicsContext gc,
                         double W, double H, String adresse) {

        // fond beige
        gc.setFill(javafx.scene.paint.Color.web("#f2efe9"));
        gc.fillRect(0, 0, W, H);

        // rues blanches horizontales
        gc.setStroke(javafx.scene.paint.Color.WHITE);
        gc.setLineWidth(8);
        for (int y = 40; y < H; y += 60) gc.strokeLine(0, y, W, y);
        for (int x = 60; x < W; x += 80) gc.strokeLine(x, 0, x, H);

        // rues secondaires
        gc.setLineWidth(3);
        gc.strokeLine(0, 70, W, 130);
        gc.strokeLine(0, 180, W, 210);
        gc.strokeLine(150, 0, 200, H);
        gc.strokeLine(500, 0, 480, H);

        // boulevard jaune
        gc.setStroke(javafx.scene.paint.Color.web("#ffd700"));
        gc.setLineWidth(12);
        gc.strokeLine(0, H / 2 + 20, W, H / 2 - 20);

        // zones vertes
        gc.setFill(javafx.scene.paint.Color.web("#c8e6c9"));
        gc.fillRoundRect(80, 30, 120, 80, 8, 8);
        gc.fillRoundRect(W - 200, 160, 100, 70, 8, 8);

        // bâtiments
        gc.setFill(javafx.scene.paint.Color.web("#ddd6c8"));
        int[][] b = {{20,20,50,40},{180,50,70,50},{320,20,60,35},
                     {420,80,80,45},{(int)(W-200),30,90,55},
                     {30,160,60,50},{160,180,75,45},{350,160,65,55}};
        for (int[] r : b) {
            gc.fillRoundRect(r[0], r[1], r[2], r[3], 4, 4);
            gc.setStroke(javafx.scene.paint.Color.web("#c4bdb0"));
            gc.setLineWidth(1);
            gc.strokeRoundRect(r[0], r[1], r[2], r[3], 4, 4);
        }

        // eau si ville côtière
        String low = adresse.toLowerCase();
        if (low.contains("hammamet") || low.contains("tunis")
         || low.contains("sfax")    || low.contains("sousse")) {
            gc.setFill(javafx.scene.paint.Color.web("#b3d9f5", 0.6));
            gc.fillRect(0, 0, 70, H);
        }

        // marqueur rouge
        double mx = W / 2, my = H / 2 - 10;
        gc.setFill(javafx.scene.paint.Color.web("#000000", 0.2));
        gc.fillOval(mx - 12, my + 22, 24, 8);
        gc.setFill(javafx.scene.paint.Color.web("#EA4335"));
        gc.beginPath();
        gc.moveTo(mx, my + 28);
        gc.bezierCurveTo(mx-18, my+10, mx-18, my-16, mx, my-20);
        gc.bezierCurveTo(mx+18, my-16, mx+18, my+10, mx, my+28);
        gc.closePath();
        gc.fill();
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillOval(mx - 7, my - 3, 14, 14);

        // popup adresse
        String txt = adresse.length() > 35 ? adresse.substring(0, 35) + "…" : adresse;
        double pw = Math.max(180, txt.length() * 8.5);
        double ph = 40, px = mx - pw / 2, py = my - 20 - ph - 12;
        gc.setFill(javafx.scene.paint.Color.web("#000000", 0.12));
        gc.fillRoundRect(px+3, py+3, pw, ph, 10, 10);
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillRoundRect(px, py, pw, ph, 10, 10);
        gc.setStroke(javafx.scene.paint.Color.web("#dddddd"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(px, py, pw, ph, 10, 10);
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.beginPath();
        gc.moveTo(mx-8, py+ph); gc.lineTo(mx+8, py+ph); gc.lineTo(mx, py+ph+10);
        gc.closePath(); gc.fill();
        gc.setFill(javafx.scene.paint.Color.web("#333333"));
        gc.setFont(javafx.scene.text.Font.font("System",
            javafx.scene.text.FontWeight.BOLD, 12));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText("📍 " + txt, mx, py + 26);

        // attribution
        gc.setFill(javafx.scene.paint.Color.web("#ffffff", 0.8));
        gc.fillRect(0, H - 18, W, 18);
        gc.setFill(javafx.scene.paint.Color.web("#666666"));
        gc.setFont(javafx.scene.text.Font.font("System", 9));
        gc.setTextAlign(javafx.scene.text.TextAlignment.RIGHT);
        gc.fillText("© OpenStreetMap contributors", W - 8, H - 5);
    }
    
    
    private void loadOffres() {
        offresGrid.getChildren().clear();
        try {
            List<SubscriptionOffer> offres = service.getOffres(salle.getId());
            if (offres.isEmpty()) {
                noOffres.setVisible(true); noOffres.setManaged(true);
            } else {
                noOffres.setVisible(false); noOffres.setManaged(false);
                for (SubscriptionOffer o : offres) offresGrid.getChildren().add(buildOffreCard(o));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSessions() {
        sessionsGrid.getChildren().clear();
        try {
            List<Session> sessions = service.getSessions(salle.getId());
            if (sessions.isEmpty()) {
                noSessions.setVisible(true); noSessions.setManaged(true);
            } else {
                noSessions.setVisible(false); noSessions.setManaged(false);
                for (Session s : sessions) sessionsGrid.getChildren().add(buildSessionCard(s));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void chargerSessions(int salleId) {
        // conserve l'API demandée côté UI tout en réutilisant la logique existante
        if (this.salle == null || this.salle.getId() != salleId) {
            this.salle = this.salle == null ? new Salle() : this.salle;
            this.salle.setId(salleId);
        }
        loadSessions();
    }

    private VBox buildOffreCard(SubscriptionOffer o) {
        VBox card = new VBox(10);
        card.setPrefWidth(320);
        card.getStyleClass().add("cl-offer-card");
        card.setPadding(new Insets(18));

        Label name = new Label(o.getName());
        name.getStyleClass().add("cl-card-title");
        name.setWrapText(true);
        Label dur = new Label("Durée : " + o.getDurationMonths() + " mois");
        dur.getStyleClass().add("cl-card-info");
        Label prix = new Label(String.format("%.2f TND", o.getPrice()));
        prix.getStyleClass().add("client-salle-price-tag");

        if (o.getDescription() != null && !o.getDescription().isBlank()) {
            Label desc = new Label(o.getDescription()); 
            desc.getStyleClass().add("cl-card-desc"); 
            desc.setWrapText(true);
            card.getChildren().addAll(name, dur, prix, desc);
        } else {
            card.getChildren().addAll(name, dur, prix);
        }

        Button btn = new Button("S'abonner");
        btn.getStyleClass().addAll("front-card-button", "client-salle-btn-success");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setUserData(o); // ✅ Stocker l'offre dans userData
        btn.setOnAction(this::handleSubscribe); // ✅ Utiliser la méthode handleSubscribe
        card.getChildren().add(btn);
        return card;
    }

    private VBox buildSessionCard(Session s) {
        VBox card = new VBox(10);
        card.setPrefWidth(320);
        card.getStyleClass().add("cl-session-card");
        card.setPadding(new Insets(18));

        Label title = new Label(s.getTitle());
        title.getStyleClass().add("cl-card-title");
        title.setWrapText(true);
        String horaire = s.getStartAt() != null ? s.getStartAt().format(FMT) : "—";
        if (s.getEndAt() != null) {
            horaire += " → " + s.getEndAt().format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        Label date = new Label(horaire);
        date.getStyleClass().add("cl-card-info");
        Label places = new Label(s.getCapacity() + " places");
        places.getStyleClass().add("cl-card-info");
        Label prix = new Label(String.format("%.2f TND", s.getPrice()));
        prix.getStyleClass().add("client-salle-price-tag");

        card.getChildren().addAll(title, date, places, prix);

        Button btn = new Button("Réserver");
        btn.getStyleClass().addAll("front-card-button", "client-salle-primary-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> showInfo("Reservation", "Fonctionnalite a connecter au module inscriptions."));
        card.getChildren().add(btn);
        return card;
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/FXML/pages/ClientSalleList.fxml"));
            Node list = loader.load();
            Node root = btnBack.getScene().getRoot();
            StackPane contentArea =
                (StackPane) root.lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(list);
        } catch (Exception ex) { 
            ex.printStackTrace(); 
            showInfo("Erreur", "Impossible de revenir à la liste: " + ex.getMessage());
        }
    }
    
    
    /**
     * Ouvre le planning hebdomadaire de la salle
     */
    @FXML
    private void handleVoirPlanning() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/FXML/pages/SallePlanningPage.fxml"));
            Parent root = loader.load();
            SallePlanningPageController ctrl = loader.getController();
            
            // Passer la salle au contrôleur
            ctrl.setSalle(salle);
            
            // Créer une nouvelle fenêtre pour le planning
            Stage stage = new Stage();
            stage.setTitle("Planning - " + salle.getName());
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            // Afficher une alerte en cas d'erreur
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir le planning");
            alert.setContentText("Une erreur est survenue lors de l'ouverture du planning de la salle.");
            alert.showAndWait();
        }
    }
    
    /**
     * Gestion des abonnements - Créer une commande et initier le paiement Flouci
     */
    @FXML
    private void handleSubscribe(ActionEvent event) {
        try {
            // Récupérer l'offre sélectionnée (bouton cliqué depuis la carte offre)
            SubscriptionOffer offre = (SubscriptionOffer) 
                ((Button) event.getSource()).getUserData();
            
            if (offre == null) {
                showInfo("Erreur", "Aucune offre sélectionnée");
                return;
            }
            
            // Confirmation avant paiement
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Paiement Flouci");
            confirm.setHeaderText("S'abonner : " + offre.getName());
            confirm.setContentText(
                "Durée : " + offre.getDurationMonths() + " mois\n" +
                "Prix  : " + String.format("%.2f", offre.getPrice()) + " TND\n\n" +
                "Vous allez être redirigé vers Flouci pour payer."
            );

            ButtonType btnPayer = new ButtonType("Payer avec Flouci");
            ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirm.getButtonTypes().setAll(btnPayer, btnAnnuler);

            confirm.showAndWait().ifPresent(result -> {
                if (result == btnPayer) {
                    try {
                        // Créer paiement Flouci
                        FlouciService flouci = new FlouciService();
                        FlouciService.FlouciPayment pay = flouci.createPayment(
                            offre.getPrice(),
                            "Abonnement " + offre.getName()
                        );

                        if (pay.success) {
                            // MODE DEMO - skip l'ouverture du navigateur
                            // Desktop.getDesktop().browse(new URI(pay.link));
                            showPaymentWaiting(pay.paymentId, offre);
                        } else {
                            showInfo("Erreur", "Impossible de créer le paiement Flouci");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        showInfo("Erreur", "Impossible de créer le paiement : " + e.getMessage());
                    }
                }
            });
            
        } catch (Exception ex) {
            ex.printStackTrace();
            showInfo("Erreur", "Impossible de traiter la demande: " + ex.getMessage());
        }
    }
    
    /**
     * Attendre confirmation paiement
     */
    private void showPaymentWaiting(String paymentId, SubscriptionOffer offre) {
        Alert waiting = new Alert(Alert.AlertType.CONFIRMATION);
        waiting.setTitle("En attente de paiement");
        waiting.setHeaderText("Complétez le paiement sur Flouci");
        waiting.setContentText(
            "Une fois le paiement effectué,\n" +
            "cliquez 'Vérifier' pour confirmer."
        );

        ButtonType btnVerifier = new ButtonType("Vérifier paiement");
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        waiting.getButtonTypes().setAll(btnVerifier, btnAnnuler);

        waiting.showAndWait().ifPresent(r -> {
            if (r == btnVerifier) {
                try {
                    FlouciService flouci = new FlouciService();

                    // Vérifier paiement
                    boolean paid = flouci.verifyPayment(paymentId);

                    if (paid) {
                        // Enregistrer abonnement
                        createSubscriptionOrder(
                            getCurrentUserId(),
                            offre.getId(),
                            offre.getPrice()
                        );
                        showInfo(
                            "Abonnement activé !",
                            "Paiement confirmé !\n" +
                            "Vous êtes abonné à " + offre.getName()
                        );
                    } else {
                        showInfo(
                            "Paiement non confirmé",
                            "Le paiement n'a pas encore été reçu.\n" +
                            "Réessayez dans quelques instants."
                        );
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showInfo("Erreur", "Erreur lors de la vérification: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Créer et enregistrer directement l'abonnement
     */
    private void createSubscriptionOrder(int userId, int offerId, double offerPrice) throws SQLException {
        // ✅ try-with-resources → ferme automatiquement
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO gym_subscription_orders " +
                     "(user_id, offer_id, quantity, " +
                     "unit_price, total_price, " +
                     "status, created_at) " +
                     "VALUES (?, ?, 1, ?, ?, 'active', NOW())")) {

            ps.setInt(1, userId);
            ps.setInt(2, offerId);
            ps.setDouble(3, offerPrice); // unit_price
            ps.setDouble(4, offerPrice); // total_price

            ps.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur createSubscriptionOrder: " + e.getMessage());
            throw e;
        }
        // connexion fermée automatiquement ici ✅
    }
    
        
    /**
     * Initier le paiement via l'API Flouci
     */
    private void initiateFlouciPayment(int orderId, double amount, String offerName) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            
            // Préparer les données pour l'API Flouci
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("amount", amount);
            paymentData.put("currency", "EUR");
            paymentData.put("description", "Abonnement " + offerName);
            paymentData.put("return_url", "http://localhost:8080/payment/success");
            paymentData.put("cancel_url", "http://localhost:8080/payment/cancel");
            paymentData.put("webhook_url", "http://localhost:8080/payment/webhook");
            paymentData.put("metadata", Map.of("order_id", orderId));
            
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(paymentData);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.flouci.com/v1/payment-sessions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer YOUR_FLOUCI_API_KEY") // Remplacer par votre clé API
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
                
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                ObjectMapper responseMapper = new ObjectMapper();
                Map<String, Object> responseData = responseMapper.readValue(response.body(), Map.class);
                
                String paymentUrl = (String) responseData.get("payment_url");
                String sessionId = (String) responseData.get("session_id");
                
                // Stocker le session_id pour la vérification future
                updateOrderWithSessionId(orderId, sessionId);
                
                // Ouvrir la page de paiement dans le navigateur par défaut
                Desktop.getDesktop().browse(URI.create(paymentUrl));
                
                showInfo("Paiement", "Redirection vers la page de paiement sécurisée...");
                
                // Lancer un thread pour vérifier le statut du paiement périodiquement
                startPaymentVerification(orderId, sessionId);
                
            } else {
                showInfo("Erreur", "Échec de la création de la session de paiement: " + response.statusCode());
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
            showInfo("Erreur", "Erreur lors de l'initialisation du paiement: " + ex.getMessage());
        }
    }
    
    /**
     * Vérifier périodiquement le statut du paiement
     */
    private Thread verificationThread = null; // Pour pouvoir l'arrêter
    
    private void startPaymentVerification(int orderId, String sessionId) {
        // Arrêter le thread précédent s'il existe
        stopPaymentVerification();
        
        verificationThread = new Thread(() -> {
            try {
                boolean paymentConfirmed = false;
                int attempts = 0;
                int maxAttempts = 30; // Vérifier pendant 5 minutes (30 * 10 secondes)
                
                while (!paymentConfirmed && attempts < maxAttempts && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(10000); // Attendre 10 secondes
                    
                    if (verifyFlouciPayment(sessionId)) {
                        // Paiement confirmé - mettre à jour le statut
                        updateOrderStatus(orderId, "payé");
                        activateSubscription(orderId);
                        paymentConfirmed = true;
                        
                        javafx.application.Platform.runLater(() -> {
                            showInfo("Succès", "Paiement confirmé ! Votre abonnement est maintenant actif.");
                        });
                    }
                    
                    attempts++;
                }
                
                if (!paymentConfirmed && !Thread.currentThread().isInterrupted()) {
                    // Timeout - marquer comme expiré
                    updateOrderStatus(orderId, "expiré");
                    javafx.application.Platform.runLater(() -> {
                        showInfo("Info", "Le délai de paiement a expiré. Veuillez réessayer.");
                    });
                }
                
            } catch (InterruptedException ex) {
                System.out.println("🛑 Thread de vérification paiement interrompu");
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "payment-verification-thread");
        
        verificationThread.setDaemon(true);
        verificationThread.start();
    }
    
    /**
     * Arrête le thread de vérification de paiement
     */
    private void stopPaymentVerification() {
        if (verificationThread != null && verificationThread.isAlive()) {
            verificationThread.interrupt();
            try {
                verificationThread.join(1000); // Attendre max 1 seconde
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("🛑 Thread de vérification paiement arrêté");
        }
    }
    
    /**
     * Vérifier le statut du paiement auprès de Flouci (API v2)
     */
    private boolean verifyFlouciPayment(String paymentId) {
        try {
            FlouciService flouciService = new FlouciService();
            boolean result = flouciService.verifyPayment(paymentId);
            
            System.out.println("🔍 Vérification paiement " + paymentId + " : " + (result ? "✅ SUCCÈS" : "❌ ÉCHEC"));
            
            return result;
            
        } catch (Exception ex) {
            System.err.println("❌ Erreur verifyFlouciPayment : " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }
    
    /**
     * Mettre à jour le statut de la commande
     */
    private void updateOrderStatus(int orderId, String status) {
        try (Connection conn = org.example.utils.MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE gym_subscription_orders SET status = ?, updated_at = NOW() WHERE id = ?")) {
            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Activer l'abonnement après paiement réussi
     */
    private void activateSubscription(int orderId) {
        try (Connection conn = org.example.utils.MyDataBase.getConnection()) {
            
            // Récupérer les détails de la commande
            try (PreparedStatement orderStmt = conn.prepareStatement("SELECT * FROM gym_subscription_orders WHERE id = ?")) {
                orderStmt.setInt(1, orderId);
                try (ResultSet orderRs = orderStmt.executeQuery()) {
                    
                    if (orderRs.next()) {
                        int userId = orderRs.getInt("user_id");
                        int offerId = orderRs.getInt("offer_id");
                        
                        // Récupérer les détails de l'offre
                        try (PreparedStatement offerStmt = conn.prepareStatement("SELECT * FROM gym_subscription_offers WHERE id = ?")) {
                            offerStmt.setInt(1, offerId);
                            try (ResultSet offerRs = offerStmt.executeQuery()) {
                                
                                if (offerRs.next()) {
                                    int durationMonths = offerRs.getInt("duration_months");
                                    
                                    // Calculer les dates d'abonnement
                                    java.time.LocalDate startDate = java.time.LocalDate.now();
                                    java.time.LocalDate endDate = startDate.plusMonths(durationMonths);
                                    
                                    // Insérer dans la table des abonnements actifs
                                    String insertSubscription = "INSERT INTO user_subscriptions " +
                                        "(user_id, offer_id, order_id, start_date, end_date, status, created_at) " +
                                        "VALUES (?, ?, ?, ?, ?, 'active', NOW())";
                                    
                                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSubscription)) {
                                        insertStmt.setInt(1, userId);
                                        insertStmt.setInt(2, offerId);
                                        insertStmt.setInt(3, orderId);
                                        insertStmt.setDate(4, java.sql.Date.valueOf(startDate));
                                        insertStmt.setDate(5, java.sql.Date.valueOf(endDate));
                                        insertStmt.executeUpdate();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Mettre à jour la commande avec l'ID de session Flouci
     */
    private void updateOrderWithSessionId(int orderId, String sessionId) {
        try (Connection conn = org.example.utils.MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE gym_subscription_orders SET flouci_session_id = ? WHERE id = ?")) {
            stmt.setString(1, sessionId);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Obtenir l'ID de l'utilisateur connecté (à adapter selon votre système d'authentification)
     */
    private int getCurrentUserId() {
        // Utiliser le système existant déjà défini dans la classe
        return currentUserId;
    }

    private void loadYoutubeVideo(String url) {
        System.out.println("🔧 DEBUG loadYoutubeVideo appelé avec URL: " + url);
        
        if (videoPlayer == null) {
            System.out.println("❌ DEBUG: videoPlayer est null");
            return;
        }
        
        if (videoLinkLabel == null) {
            System.out.println("❌ DEBUG: videoLinkLabel est null");
            return;
        }
        
        // ── Masquer le WebView complètement ───────────────────────────
        videoPlayer.setVisible(false);
        videoPlayer.setManaged(false);
        System.out.println("✅ DEBUG: WebView masqué");
        
        // ── null ou vide → masquer complètement ───────────────────────────
        if (url == null || url.isBlank()) {
            System.out.println("⚠️ DEBUG: URL vide - affichage noVideoLabel");
            if (noVideoLabel != null) {
                noVideoLabel.setVisible(true);
                noVideoLabel.setManaged(true);
            }
            videoLinkLabel.setVisible(false);
            videoLinkLabel.setManaged(false);
            return;
        }

        // ── Créer un bouton YouTube rouge classique ─────────────────────
        System.out.println("🎨 DEBUG: Création du bouton YouTube");
        
        videoLinkLabel.setText("▶️ Voir sur YouTube");
        String buttonStyle = 
            "-fx-background-color: #FF0000;" +
            "-fx-text-fill: WHITE;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 16px;" +
            "-fx-padding: 12px 24px;" +
            "-fx-background-radius: 8px;" +
            "-fx-cursor: hand;" +
            "-fx-border-color: #CC0000;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 4);";
        
        videoLinkLabel.setStyle(buttonStyle);
        videoLinkLabel.setVisible(true);
        videoLinkLabel.setManaged(true);
        
        System.out.println("✅ DEBUG: Bouton configuré - visible=" + videoLinkLabel.isVisible() + " managed=" + videoLinkLabel.isManaged());
        System.out.println("🎨 DEBUG: Style appliqué: " + buttonStyle);
        
        // Action au clic - ouvrir YouTube dans WebView intégré
        videoLinkLabel.setOnMouseClicked(e -> {
            try {
                System.out.println("🖱️ Clic sur bouton YouTube - lecteur intégré: " + url);
                
                // Ouvrir le lecteur vidéo intégré avec l'URL
                String videoId = YouTubeService.extractVideoId(url);
                if (videoId != null) {
                    VideoPlayerHelper.openVideoPlayerWithVideo(videoId);
                    System.out.println("🎬 Vidéo chargée dans WebView: " + videoId);
                } else {
                    System.err.println("❌ URL YouTube invalide: " + url);
                }
            } catch (Exception ex) {
                System.err.println("❌ Erreur ouverture lecteur vidéo: " + ex.getMessage());
            }
        });
        
        System.out.println("✅ DEBUG: Listener clic configuré");
        
        // Effet hover
        videoLinkLabel.setOnMouseEntered(e -> {
            videoLinkLabel.setStyle(
                "-fx-background-color: #FF3333;" +
                "-fx-text-fill: WHITE;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 16px;" +
                "-fx-padding: 12px 24px;" +
                "-fx-background-radius: 8px;" +
                "-fx-cursor: hand;" +
                "-fx-border-color: #FF0000;" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 8px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 15, 0, 0, 6);"
            );
        });
        
        videoLinkLabel.setOnMouseExited(e -> {
            videoLinkLabel.setStyle(buttonStyle);
        });
        
        System.out.println("✅ DEBUG: Listeners hover configurés");
        
        if (noVideoLabel != null) {
            noVideoLabel.setVisible(false);
            noVideoLabel.setManaged(false);
            System.out.println("✅ DEBUG: noVideoLabel masqué");
        }
        
        System.out.println("🎥 Bouton YouTube créé pour: " + url);
    }

    private String extractVideoId(String url) {
        if (url == null) return null;
        
        if (url.contains("v=")) {
            return url.split("v=")[1].split("&")[0];
        }
        
        if (url.contains("youtu.be/")) {
            return url.split("youtu.be/")[1].split("\\?")[0];
        }
        
        if (url.contains("embed/")) {
            return url.split("embed/")[1].split("\\?")[0];
        }
        
        return null;
    }

    
    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); 
        a.setHeaderText(null); 
        a.setContentText(msg); 
        a.showAndWait();
    }
    
    // ===== SYSTÈME DE NOTATION =====
    
    /**
     * Initialise le système de notation
     */
    private void initializeRatingSystem() {
        System.out.println("=== INITIALISATION SYSTÈME NOTATION ===");
        
        // Vérifier si les champs FXML sont bien injectés
        System.out.println("starsContainer: " + (starsContainer != null ? "OK" : "NULL"));
        System.out.println("ratingInfo: " + (ratingInfo != null ? "OK" : "NULL"));
        System.out.println("commentArea: " + (commentArea != null ? "OK" : "NULL"));
        System.out.println("submitRatingBtn: " + (submitRatingBtn != null ? "OK" : "NULL"));
        
        if (starsContainer == null) {
            System.err.println("ERREUR: starsContainer est NULL - problème d'injection FXML");
            return;
        }
        if (ratingInfo == null || commentArea == null || submitRatingBtn == null || ratingMessage == null) {
            System.err.println("ERREUR: composants de notation manquants dans le FXML - vérifiez les fx:id");
            return;
        }
        
        // Créer les étoiles EN PREMIER - indépendant de la DB
        createStars();
        updateRatingInfo();
        submitRatingBtn.setOnAction(e -> submitRating());
        System.out.println("Étoiles créées: " + stars.size() + " étoiles");
        System.out.println("Étoiles dans container: " + starsContainer.getChildren().size());
        
        // Ensuite tenter la connexion DB
        try (Connection connection = org.example.utils.MyDataBase.getConnection()) {
            ratingDAO = new GymRatingDAO(connection);
            System.out.println("DAO initialisé: OK");
            
            // Vérifier si l'utilisateur a déjà noté
            checkExistingRating();
            System.out.println("=== INITIALISATION TERMINÉE ===");
            
        } catch (SQLException e) {
            e.printStackTrace();
            if (ratingMessage != null) {
                ratingMessage.setText("Impossible de charger les avis existants.");
                ratingMessage.setStyle("-fx-text-fill: orange;");
                ratingMessage.setVisible(true);
                ratingMessage.setManaged(true);
            }
        }
    }
    
    /**
     * Crée les 5 étoiles cliquables
     */
    private void createStars() {
        System.out.println("=== CRÉATION DES ÉTOILES ===");
        System.out.println("Container avant clear: " + starsContainer.getChildren().size() + " enfants");
        
        starsContainer.getChildren().clear();
        stars.clear();
        
        System.out.println("Container après clear: " + starsContainer.getChildren().size() + " enfants");
        
        for (int i = 1; i <= 5; i++) {
            final int starIndex = i;
            Label star = new Label("\u2605"); // étoile pleine
            star.setStyle(
                "-fx-font-size: 36px;" +
                "-fx-cursor: hand;" +
                "-fx-text-fill: #cccccc;"
            );
            star.setOnMouseClicked(e -> setRating(starIndex));
            star.setOnMouseEntered(e -> highlightStars(starIndex));
            star.setOnMouseExited(e -> highlightStars(currentRating));
            
            System.out.println("Création étoile " + i + ": " + star.getText() + " | Style: " + star.getStyle());
            
            stars.add(star);
            starsContainer.getChildren().add(star);
            
            System.out.println("Container après ajout étoile " + i + ": " + starsContainer.getChildren().size() + " enfants");
        }
        
        System.out.println("=== CRÉATION TERMINÉE: " + stars.size() + " étoiles ===");
    }
    
    /**
     * Définit la note sélectionnée
     */
    private void setRating(int rating) {
        currentRating = rating;
        highlightStars(rating);
        updateRatingInfo();
    }
    
    /**
     * Met en surbrillance les étoiles jusqu'à la note spécifiée
     */
    private void highlightStars(int rating) {
        System.out.println("DEBUG: highlightStars appelé avec rating = " + rating);
        for (int i = 0; i < stars.size(); i++) {
            boolean shouldHighlight = i < rating;
            System.out.println("DEBUG: Étoile " + (i+1) + " | i=" + i + " < rating=" + rating + " = " + shouldHighlight);
            
            if (shouldHighlight) {
                stars.get(i).setStyle(
                    "-fx-font-size: 36px;" +
                    "-fx-cursor: hand;" +
                    "-fx-text-fill: #FFD700;" // jaune
                );
            } else {
                stars.get(i).setStyle(
                    "-fx-font-size: 36px;" +
                    "-fx-cursor: hand;" +
                    "-fx-text-fill: #cccccc;" // gris
                );
            }
        }
    }
    
    /**
     * Met à jour le texte d'information de la note
     */
    private void updateRatingInfo() {
        String[] texts = {"", "Mauvais", "Moyen", "Bon", "Très bon", "Excellent"};
        ratingInfo.setText(currentRating > 0 ? texts[currentRating] : "Cliquez sur les étoiles pour noter");
    }
    
    /**
     * Vérifie si l'utilisateur a déjà noté cette salle
     */
    private void checkExistingRating() {
        System.out.println("=== CHECK EXISTING RATING START ===");
        System.out.println("CHECK: userId=" + currentUserId + " salleId=" + salle.getId());
        try {
            existingRating = ratingDAO.getUserRating(currentUserId, salle.getId());
            System.out.println("CHECK: existingRating=" + 
                (existingRating != null ? existingRating.getRating() : "NULL"));
            
            if (existingRating != null) {
                // L'utilisateur a déjà noté : afficher sa note en readonly
                currentRating = existingRating.getRating();
                System.out.println("DEBUG: Note existante trouvée = " + currentRating);
                updateRatingInfo();
                
                commentArea.setText(existingRating.getComment() != null ? existingRating.getComment() : "");
                commentArea.setEditable(false);
                commentArea.setStyle("-fx-opacity: 0.7;");
                
                submitRatingBtn.setText("Vous avez déjà noté");
                submitRatingBtn.setDisable(true);
                
                ratingMessage.setText("Vous avez déjà noté cette salle le " + 
                    existingRating.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                ratingMessage.setVisible(true);
                ratingMessage.setManaged(true);
                
                // Désactiver les étoiles avec le bon style readonly
                System.out.println("DEBUG: Début boucle readonly - rating=" + existingRating.getRating());
                for (int i = 0; i < stars.size(); i++) {
                    boolean shouldHighlight = i < existingRating.getRating();
                    String color = shouldHighlight ? "#FFD700" : "#cccccc";
                    System.out.println("DEBUG: Étoile " + (i+1) + " | i=" + i + " < rating=" + existingRating.getRating() + " = " + shouldHighlight + " | color=" + color);
                    
                    Label star = stars.get(i);
                    star.setStyle(
                        "-fx-font-size: 36px;" +
                        "-fx-cursor: default;" +
                        "-fx-text-fill: " + color + ";"
                    );
                    star.setOnMouseClicked(null);
                    star.setOnMouseEntered(null);
                    star.setOnMouseExited(null);
                }
                System.out.println("DEBUG: Fin boucle readonly");
            } else {
                System.out.println("DEBUG: Aucune note existante trouvée");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("=== CHECK EXISTING RATING END ===");
    }
    
    /**
     * Soumet la note
     */
    @FXML
    private void submitRating() {
        if (currentRating == 0) {
            showInfo("Erreur", "Veuillez sélectionner une note");
            return;
        }
        
        // Vérifier si le DAO est initialisé
        if (ratingDAO == null) {
            showInfo("Erreur", "Base de données non disponible. Impossible d'enregistrer la note.");
            return;
        }
        
        try {
            // Créer la note
            GymRating rating = new GymRating(
                currentRating,
                commentArea.getText().trim().isEmpty() ? null : commentArea.getText().trim(),
                currentUserId,
                salle.getId()
            );
            
            // Sauvegarder
            if (ratingDAO.addRating(rating)) {
                showInfo("Succès", "Votre note a été enregistrée avec succès !");
                
                // Mettre à jour la note moyenne dans la table gymnasia
                try {
                    if (ratingDAO.updateGymnasiumRating(salle.getId())) {
                        System.out.println("Note moyenne mise à jour dans gymnasia");
                    } else {
                        System.err.println("Erreur mise à jour note moyenne dans gymnasia");
                    }
                } catch (SQLException e) {
                    System.err.println("Erreur mise à jour gymnasia: " + e.getMessage());
                }
                
                // Mettre à jour l'affichage local
                existingRating = rating;
                checkExistingRating();
                
                // Mettre à jour la note moyenne affichée
                updateSalleRating();
                
                // Rafraîchir la liste des salles (si possible)
                refreshSalleList();
            } else {
                showInfo("Erreur", "Impossible d'enregistrer votre note");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showInfo("Erreur", "Erreur lors de l'enregistrement: " + e.getMessage());
        }
    }
    
    /**
     * Met à jour la note moyenne affichée pour la salle
     */
    private void updateSalleRating() {
        if (ratingDAO == null) {
            System.out.println("DAO non initialisé - impossible de mettre à jour la note moyenne");
            return;
        }
        
        try {
            double avgRating = ratingDAO.getAverageRating(salle.getId());
            int ratingCount = ratingDAO.getRatingCount(salle.getId());
            
            salleRating.setText(String.format("Note %.1f / 5", avgRating) + " · " + ratingCount + " avis");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Rafraîchit la liste des salles pour afficher les notes mises à jour
     */
    private void refreshSalleList() {
        try {
            // Notifier le controller de la liste des salles de se recharger
            // Cela dépend de votre architecture - voici une approche simple
            
            // Option 1: Recharger depuis la BDD via le service
            List<Salle> refreshedSalles = service.getSallesActives();
            System.out.println("Liste des salles rafraîchie avec " + refreshedSalles.size() + " salles");
            
            // Option 2: Envoyer un événement ou notifier un listener
            // (à implémenter selon votre architecture)
            
        } catch (SQLException e) {
            System.err.println("Erreur rafraîchissement liste salles: " + e.getMessage());
        }
    }
}
