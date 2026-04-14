package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.example.entities.Salle;
import org.example.entities.Session;
import org.example.entities.SubscriptionOffer;
import org.example.services.ClientSalleService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ClientSalleDetailController implements Initializable {

    @FXML private Button    btnBack;
    @FXML private StackPane photoPane;
    @FXML private ImageView sallePhoto;
    @FXML private Label     photoPlaceholder;
    @FXML private Label     salleName, salleDesc, salleAddr, sallePhone, salleEmail, salleRating;
    @FXML private FlowPane  offresGrid, sessionsGrid;
    @FXML private Label     noOffres, noSessions;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ClientSalleService service = new ClientSalleService();
    private Salle salle;

    public void setSalle(Salle salle) {
        this.salle = salle;
        populateInfo();
        loadOffres();
        loadSessions();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

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
            Label desc = new Label(o.getDescription()); desc.getStyleClass().add("cl-card-desc"); desc.setWrapText(true);
            card.getChildren().addAll(name, dur, prix, desc);
        } else {
            card.getChildren().addAll(name, dur, prix);
        }

        Button btn = new Button("S'abonner");
        btn.getStyleClass().addAll("front-card-button", "client-salle-btn-success");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> showInfo("Abonnement", "Fonctionnalite a connecter au module paiement."));
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
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/FXML/pages/ClientSalleList.fxml"));
            javafx.scene.Node list = loader.load();
            javafx.scene.Node root = btnBack.getScene().getRoot();
            javafx.scene.layout.StackPane contentArea =
                (javafx.scene.layout.StackPane) root.lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(list);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
