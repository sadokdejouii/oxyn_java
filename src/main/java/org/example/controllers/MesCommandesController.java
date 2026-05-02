package org.example.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.entities.LigneCommandeAffichage;
import org.example.entities.commandes;
import org.example.services.CommandesService;
import org.example.services.CurrencyExchangeService;
import org.example.services.FacturePdfService;
import org.example.services.SessionContext;
import org.example.services.UserRole;
import org.example.utils.AdresseCommandeValidator;
import org.example.utils.CommandeClientResolver;
import org.example.utils.TexteRecherche;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.io.File;
import java.io.FileOutputStream;

public class MesCommandesController {

    private static final DateTimeFormatter ISO_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter AFFICHAGE_DATE_CLIENT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);

    @FXML
    private VBox ordersContainer;

    @FXML
    private VBox editAdressePanel;

    @FXML
    private TextArea editAdresseArea;

    @FXML
    private TextField rechercheMesCommandes;

    @FXML
    private ComboBox<String> triMesCommandes;
    @FXML
    private ComboBox<String> deviseMesCommandesCombo;

    private MainLayoutController mainLayoutController;

    private final CommandesService commandesService = new CommandesService();
    private final StripePaymentService stripePaymentService = new StripePaymentService();
    private final FacturePdfService facturePdfService = new FacturePdfService();
    private final CurrencyExchangeService currencyExchangeService = new CurrencyExchangeService();

    private final List<commandes> toutesMesCommandes = new ArrayList<>();

    /** Commande en cours d’édition d’adresse (null si panneau fermé). */
    private commandes commandeEditionAdresse;

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    @FXML
    public void initialize() {
        if (editAdresseArea != null) {
            AdresseCommandeValidator.appliquerLimiteLongueur(editAdresseArea);
        }
        if (triMesCommandes != null) {
            triMesCommandes.getItems().setAll(
                    "Date (récent d’abord)",
                    "Date (ancien d’abord)",
                    "Total (croissant)",
                    "Total (décroissant)",
                    "Statut (A → Z)");
            triMesCommandes.getSelectionModel().selectFirst();
        }
        if (deviseMesCommandesCombo != null) {
            deviseMesCommandesCombo.getItems().setAll("TND", "EUR", "USD");
            deviseMesCommandesCombo.getSelectionModel().select("TND");
        }
        if (rechercheMesCommandes != null) {
            rechercheMesCommandes.textProperty().addListener((o, a, b) -> reafficherListe());
        }
        refresh();
    }

    @FXML
    private void handleRetourBoutique() {
        if (mainLayoutController != null) {
            mainLayoutController.navigate("/FXML/pages/ClientBoutique.fxml", "Boutique", null);
        }
    }

    @FXML
    private void handleRefresh() {
        fermerPanneauEdition();
        if (rechercheMesCommandes != null) {
            rechercheMesCommandes.clear();
        }
        refresh();
    }

    @FXML
    private void handleRechercheOuTriMesCommandes() {
        reafficherListe();
    }

    @FXML
    private void handleSaveAdresse() {
        if (commandeEditionAdresse == null || editAdresseArea == null) {
            return;
        }
        final int idCommande = commandeEditionAdresse.getId_commande();
        final int clientId = CommandeClientResolver.idClientConnecte();

        String err = AdresseCommandeValidator.valider(editAdresseArea.getText());
        if (err != null) {
            warn(err);
            editAdresseArea.requestFocus();
            return;
        }
        String formatted = AdresseCommandeValidator.formaterPourEnregistrement(editAdresseArea.getText());
        try {
            if (commandesService.modifierAdresseCommande(idCommande, clientId, formatted)) {
                info("Adresse de livraison mise à jour.");
                fermerPanneauEdition();
                refresh();
            } else {
                warn("Modification impossible (délai de 24 h dépassé, ou statut de la commande modifié).");
            }
        } catch (SQLException ex) {
            error("Erreur : " + ex.getMessage());
        }
    }

    @FXML
    private void handleCancelEditAdresse() {
        fermerPanneauEdition();
    }

    private void fermerPanneauEdition() {
        commandeEditionAdresse = null;
        if (editAdressePanel != null) {
            editAdressePanel.setVisible(false);
            editAdressePanel.setManaged(false);
        }
        if (editAdresseArea != null) {
            editAdresseArea.clear();
        }
    }

    private void ouvrirEditionAdresse(commandes c) {
        commandeEditionAdresse = c;
        if (editAdresseArea != null) {
            editAdresseArea.setText(c.getAdresse_commande() != null ? c.getAdresse_commande() : "");
        }
        if (editAdressePanel != null) {
            editAdressePanel.setVisible(true);
            editAdressePanel.setManaged(true);
        }
        if (editAdresseArea != null) {
            editAdresseArea.requestFocus();
        }
    }

    private void refresh() {
        SessionContext session = SessionContext.getInstance();
        if (session.getRole() != UserRole.CLIENT) {
            ordersContainer.getChildren().clear();
            Label msg = new Label("Cette page est réservée aux comptes client.");
            msg.getStyleClass().addAll("front-banner-text", "client-shop-muted");
            msg.setWrapText(true);
            ordersContainer.getChildren().add(msg);
            return;
        }
        int clientId = CommandeClientResolver.idClientConnecte();
        if (clientId <= 0) {
            ordersContainer.getChildren().clear();
            Label msg = new Label("Session client invalide. Déconnectez-vous puis reconnectez-vous.");
            msg.getStyleClass().add("client-shop-error");
            msg.setWrapText(true);
            ordersContainer.getChildren().add(msg);
            return;
        }
        try {
            toutesMesCommandes.clear();
            toutesMesCommandes.addAll(commandesService.afficherParClient(clientId));
            if (triMesCommandes != null && triMesCommandes.getSelectionModel().getSelectedIndex() < 0) {
                triMesCommandes.getSelectionModel().selectFirst();
            }
            reafficherListe();
        } catch (SQLException e) {
            ordersContainer.getChildren().clear();
            Label err = new Label("Impossible de charger les commandes : " + e.getMessage());
            err.setWrapText(true);
            err.getStyleClass().add("client-shop-error");
            ordersContainer.getChildren().add(err);
        }
    }

    private void reafficherListe() {
        ordersContainer.getChildren().clear();
        if (toutesMesCommandes.isEmpty()) {
            Label empty = new Label("Vous n’avez pas encore passé de commande.");
            empty.getStyleClass().addAll("front-banner-text", "client-shop-muted");
            empty.setWrapText(true);
            ordersContainer.getChildren().add(empty);
            return;
        }

        String q = rechercheMesCommandes != null ? rechercheMesCommandes.getText() : "";
        String tri = triMesCommandes != null && triMesCommandes.getValue() != null
                ? triMesCommandes.getValue()
                : "Date (récent d’abord)";

        List<commandes> vue = new ArrayList<>();
        for (commandes c : toutesMesCommandes) {
            if (commandeCorrespondRecherche(c, q)) {
                vue.add(c);
            }
        }
        vue.sort(comparateurTriMesCommandes(tri));

        if (vue.isEmpty()) {
            Label rien = new Label("Aucune commande ne correspond à votre recherche.");
            rien.getStyleClass().addAll("front-banner-text", "client-shop-muted");
            rien.setWrapText(true);
            ordersContainer.getChildren().add(rien);
            return;
        }

        for (commandes c : vue) {
            try {
                ordersContainer.getChildren().add(buildOrderCard(c));
            } catch (SQLException ex) {
                error("Erreur lors du chargement d’une commande : " + ex.getMessage());
                break;
            }
        }
    }

    private static boolean commandeCorrespondRecherche(commandes c, String q) {
        String bloc = String.join(" ",
                c.getStatut_commande() != null ? c.getStatut_commande() : "",
                c.getMode_paiement_commande() != null ? c.getMode_paiement_commande() : "",
                c.getAdresse_commande() != null ? c.getAdresse_commande() : "",
                c.getDate_commande() != null ? c.getDate_commande() : "",
                formaterDatePourTitre(c),
                String.format(Locale.FRENCH, "%.2f", c.getTotal_commande()));
        return TexteRecherche.correspond(bloc, q);
    }

    /** Titre sans identifiant technique (recherche inclut cette forme de date). */
    private static String formaterDatePourTitre(commandes c) {
        LocalDateTime dt = parseDateCommande(c);
        if (LocalDateTime.MIN.equals(dt)) {
            return c.getDate_commande() != null ? c.getDate_commande() : "";
        }
        return dt.format(AFFICHAGE_DATE_CLIENT);
    }

    private static Comparator<commandes> comparateurTriMesCommandes(String libelle) {
        Comparator<commandes> parDateDesc = Comparator.comparing(MesCommandesController::parseDateCommande).reversed();
        Comparator<commandes> parDateAsc = Comparator.comparing(MesCommandesController::parseDateCommande);
        if (libelle == null) {
            return parDateDesc;
        }
        return switch (libelle) {
            case "Date (ancien d’abord)" -> parDateAsc;
            case "Total (croissant)" -> Comparator.comparingDouble(commandes::getTotal_commande);
            case "Total (décroissant)" -> Comparator.comparingDouble(commandes::getTotal_commande).reversed();
            case "Statut (A → Z)" -> Comparator.comparing(
                    c -> c.getStatut_commande() != null ? c.getStatut_commande().toLowerCase(Locale.ROOT) : "",
                    String.CASE_INSENSITIVE_ORDER);
            default -> parDateDesc;
        };
    }

    private static LocalDateTime parseDateCommande(commandes c) {
        String raw = c.getDate_commande();
        if (raw == null || raw.isBlank()) {
            return LocalDateTime.MIN;
        }
        String s = raw.trim();
        try {
            return LocalDateTime.parse(s, ISO_DT);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(s).atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }
        return LocalDateTime.MIN;
    }

    private VBox buildOrderCard(commandes c) throws SQLException {
        VBox card = new VBox(12);
        card.getStyleClass().add("client-order-card");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label idLbl = new Label("Commande du " + formaterDatePourTitre(c));
        idLbl.getStyleClass().add("client-order-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statutLbl = new Label(c.getStatut_commande() != null ? c.getStatut_commande() : "—");
        statutLbl.getStyleClass().add("client-order-status");
        String st = c.getStatut_commande() != null ? c.getStatut_commande().toLowerCase(Locale.ROOT) : "";
        if (st.contains("valid")) {
            statutLbl.getStyleClass().add("client-order-status--ok");
        } else if (st.contains("annul")) {
            statutLbl.getStyleClass().add("client-order-status--bad");
        } else {
            statutLbl.getStyleClass().add("client-order-status--neutral");
        }

        header.getChildren().addAll(idLbl, spacer, statutLbl);

        Label meta = new Label(String.format("Date : %s  ·  Paiement : %s",
                c.getDate_commande() != null ? c.getDate_commande() : "—",
                c.getMode_paiement_commande() != null ? c.getMode_paiement_commande() : "—"));
        meta.getStyleClass().add("client-order-meta");
        meta.setWrapText(true);

        Label total = new Label(String.format("Total : %.2f TND", c.getTotal_commande()));
        total.getStyleClass().add("client-order-total");

        Label addrTitle = new Label("Adresse de livraison");
        addrTitle.getStyleClass().add("client-order-addr-title");
        Label addr = new Label(c.getAdresse_commande() != null ? c.getAdresse_commande() : "—");
        addr.getStyleClass().add("client-order-addr-text");
        addr.setWrapText(true);

        Separator sep = new Separator();

        Label lignesTitle = new Label("Détail des articles");
        lignesTitle.getStyleClass().add("client-order-lignes-title");

        VBox lignesBox = new VBox(6);
        List<LigneCommandeAffichage> lignes = commandesService.getLignesPourCommande(c.getId_commande());
        if (lignes.isEmpty()) {
            Label vide = new Label("(Aucune ligne enregistrée)");
            vide.getStyleClass().add("client-order-line");
            lignesBox.getChildren().add(vide);
        } else {
            for (LigneCommandeAffichage l : lignes) {
                Label line = new Label(String.format("· %s  × %d  →  %.2f TND",
                        l.getNomProduit(), l.getQuantite(), l.getSousTotal()));
                line.getStyleClass().add("client-order-line");
                line.setWrapText(true);
                lignesBox.getChildren().add(line);
            }
        }

        card.getChildren().addAll(header, meta, total, addrTitle, addr, sep, lignesTitle, lignesBox);

        HBox actionsSupplementaires = new HBox(10);
        actionsSupplementaires.setAlignment(Pos.CENTER_RIGHT);
        String statut = c.getStatut_commande() == null ? "" : c.getStatut_commande().trim().toLowerCase(Locale.ROOT);
        if (statut.contains("valid")) {
            Button imprimerFacture = new Button("Imprimer facture PDF");
            imprimerFacture.getStyleClass().add("client-shop-outline-btn");
            imprimerFacture.setOnAction(e -> imprimerFacturePdf(c));
            actionsSupplementaires.getChildren().add(imprimerFacture);
        } else if (statut.contains("attente")) {
            Button terminerPaiement = new Button("Terminer le paiement");
            terminerPaiement.getStyleClass().add("front-refresh-btn");
            terminerPaiement.setOnAction(e -> relancerPaiement(c));
            actionsSupplementaires.getChildren().add(terminerPaiement);
        }
        if (!actionsSupplementaires.getChildren().isEmpty()) {
            card.getChildren().add(actionsSupplementaires);
        }

        if (commandesService.peutModifierAdresse(c)) {
            Button modifierAdresse = new Button("Modifier l’adresse");
            modifierAdresse.getStyleClass().add("client-shop-outline-btn");
            modifierAdresse.setOnAction(e -> ouvrirEditionAdresse(c));

            Button annuler = new Button("Annuler la commande");
            annuler.getStyleClass().add("danger-toolbar-btn");
            annuler.setOnAction(ev -> confirmAnnuler(c.getId_commande()));

            HBox actions = new HBox(10, modifierAdresse, annuler);
            actions.setAlignment(Pos.CENTER_RIGHT);
            card.getChildren().add(actions);
        }

        return card;
    }

    private void relancerPaiement(commandes c) {
        int clientId = CommandeClientResolver.idClientConnecte();
        if (clientId <= 0) {
            warn("Session client invalide.");
            return;
        }
        try {
            commandes commande = commandesService.findCommandeClientById(c.getId_commande(), clientId);
            if (commande == null) {
                warn("Commande introuvable.");
                return;
            }
            String st = commande.getStatut_commande() == null ? "" : commande.getStatut_commande().toLowerCase(Locale.ROOT);
            if (!st.contains("attente")) {
                warn("Cette commande n'est plus en attente de paiement.");
                refresh();
                return;
            }
            StripePaymentService.PaymentIntentData intent = stripePaymentService.createPaymentIntentForCommande(commande);
            StripePaymentSession.getInstance().start(
                    commande.getId_commande(),
                    clientId,
                    commande.getTotal_commande(),
                    intent.clientSecret(),
                    intent.publishableKey()
            );
            if (mainLayoutController != null) {
                mainLayoutController.navigate("/FXML/pages/PaiementEnLigne.fxml", "Paiement en ligne", null);
            }
        } catch (Exception ex) {
            error("Impossible de relancer le paiement: " + ex.getMessage());
        }
    }

    private void imprimerFacturePdf(commandes c) {
        try {
            List<LigneCommandeAffichage> lignes = commandesService.getLignesPourCommande(c.getId_commande());
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Enregistrer la facture PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            chooser.setInitialFileName("facture-commande-" + c.getId_commande() + ".pdf");
            File file = chooser.showSaveDialog(getStage());
            if (file == null) {
                return;
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                facturePdfService.genererFacture(fos, c, lignes);
            }
            info("Facture générée: " + file.getAbsolutePath());
        } catch (Exception ex) {
            error("Erreur génération facture: " + ex.getMessage());
        }
    }

    private Stage getStage() {
        if (ordersContainer != null && ordersContainer.getScene() != null) {
            return (Stage) ordersContainer.getScene().getWindow();
        }
        return null;
    }

    private static void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void confirmAnnuler(int idCommande) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annulation");
        confirm.setHeaderText(null);
        confirm.setContentText("Confirmer l’annulation de cette commande ? Cette action est autorisée uniquement dans les 24 heures suivant la validation.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }
        int clientId = CommandeClientResolver.idClientConnecte();
        try {
            boolean ok = commandesService.annulerCommande(idCommande, clientId);
            if (ok) {
                info("La commande a été annulée.");
                fermerPanneauEdition();
                refresh();
            } else {
                warn("Annulation impossible (délai dépassé, statut déjà modifié, ou commande introuvable).");
            }
        } catch (SQLException ex) {
            error("Erreur : " + ex.getMessage());
        }
    }

    private String selectedCurrency() {
        if (deviseMesCommandesCombo == null || deviseMesCommandesCombo.getValue() == null) {
            return "TND";
        }
        return deviseMesCommandesCombo.getValue();
    }

    private String formatFromTnd(double amountTnd) {
        return currencyExchangeService.formatFromTnd(amountTnd, selectedCurrency());
    }
}
