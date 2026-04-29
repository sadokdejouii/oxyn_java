package org.example.services;

import org.example.entities.CommandeAdminRow;
import org.example.entities.LigneCommandeAffichage;
import org.example.entities.LignePanier;
import org.example.entities.commandes;

import org.example.utils.CommandeRegles;
import org.example.utils.MyDataBase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CRUD commandes + lignes. Table {@code ligne_commande} attendue (noms phpMyAdmin) :
 * {@code id_ligne_ligne_commande}, {@code quantite_ligne_commande},
 * {@code prix_unitaire_ligne_commande}, {@code sous_total_ligne_commande},
 * {@code id_commande_ligne_commande}, {@code id_produit_ligne_commande}.
 */
public class CommandesService implements ICrud<commandes> {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Connection con;

    public CommandesService() {
        // ✅ getConnection() ne lance plus d'exception
        con = MyDataBase.getConnection();
        if (con == null) {
            System.err.println("❌ Impossible d'initialiser CommandesService - connexion BD null");
        }
    }

    @Override
    public void ajouter(commandes c) throws SQLException {
        String sql = "INSERT INTO commandes (date_commande, total_commande, statut_commande, mode_paiement_commande, id_client_commande, Adresse_commande) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getDate_commande());
            ps.setDouble(2, c.getTotal_commande());
            ps.setString(3, c.getStatut_commande());
            ps.setString(4, c.getMode_paiement_commande());
            ps.setInt(5, c.getId_client_commande());
            ps.setString(6, c.getAdresse_commande());
            ps.executeUpdate();
        }
    }

    /**
     * Insère la commande et ses lignes dans une transaction. Retourne l’id généré dans {@code c}.
     */
    public boolean ajouterCommande(commandes c, List<LignePanier> lignes) {
        if (c == null || lignes == null || lignes.isEmpty()) {
            return false;
        }
        List<LignePanier> lignesValides = new ArrayList<>();
        for (LignePanier ligne : lignes) {
            if (ligne.getProduit() == null || ligne.getQuantite() <= 0) {
                continue;
            }
            lignesValides.add(ligne);
        }
        if (lignesValides.isEmpty()) {
            return false;
        }
        String insertCmd = "INSERT INTO commandes (date_commande, total_commande, statut_commande, mode_paiement_commande, id_client_commande, Adresse_commande) VALUES (?,?,?,?,?,?)";
        String insertLigne = "INSERT INTO ligne_commande ("
                + "quantite_ligne_commande, prix_unitaire_ligne_commande, sous_total_ligne_commande, "
                + "id_commande_ligne_commande, id_produit_ligne_commande) VALUES (?,?,?,?,?)";
        String decrementStock = "UPDATE produits SET quantite_stock_produit = quantite_stock_produit - ? "
                + "WHERE id_produit = ? AND quantite_stock_produit >= ?";
        boolean prevAuto = true;
        try {
            prevAuto = con.getAutoCommit();
            con.setAutoCommit(false);
            int idCommande;
            try (PreparedStatement ps = con.prepareStatement(insertCmd, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, c.getDate_commande());
                ps.setDouble(2, c.getTotal_commande());
                ps.setString(3, c.getStatut_commande());
                ps.setString(4, c.getMode_paiement_commande());
                ps.setInt(5, c.getId_client_commande());
                ps.setString(6, c.getAdresse_commande());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        con.rollback();
                        return false;
                    }
                    idCommande = keys.getInt(1);
                }
            }
            c.setId_commande(idCommande);
            try (PreparedStatement ps = con.prepareStatement(insertLigne)) {
                for (LignePanier ligne : lignesValides) {
                    int qte = ligne.getQuantite();
                    BigDecimal pu = BigDecimal.valueOf(ligne.getPrixUnitaire()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal sous = pu.multiply(BigDecimal.valueOf(qte)).setScale(2, RoundingMode.HALF_UP);
                    ps.setInt(1, qte);
                    ps.setBigDecimal(2, pu);
                    ps.setBigDecimal(3, sous);
                    ps.setInt(4, idCommande);
                    ps.setInt(5, ligne.getProduit().getId_produit());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            try (PreparedStatement ps = con.prepareStatement(decrementStock)) {
                for (LignePanier ligne : lignesValides) {
                    int qte = ligne.getQuantite();
                    int idProd = ligne.getProduit().getId_produit();
                    ps.setInt(1, qte);
                    ps.setInt(2, idProd);
                    ps.setInt(3, qte);
                    if (ps.executeUpdate() != 1) {
                        con.rollback();
                        return false;
                    }
                }
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                e.addSuppressed(ex);
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                con.setAutoCommit(prevAuto);
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        supprimerCommandeEtLignes(id);
    }

    /**
     * Supprime d’abord les lignes ({@code ligne_commande}) puis la commande (contraintes FK).
     */
    public void supprimerCommandeEtLignes(int idCommande) throws SQLException {
        boolean prev = con.getAutoCommit();
        try {
            con.setAutoCommit(false);
            String delLignes = "DELETE FROM ligne_commande WHERE id_commande_ligne_commande = ?";
            try (PreparedStatement ps = con.prepareStatement(delLignes)) {
                ps.setInt(1, idCommande);
                ps.executeUpdate();
            }
            String delCmd = "DELETE FROM commandes WHERE id_commande = ?";
            try (PreparedStatement ps = con.prepareStatement(delCmd)) {
                ps.setInt(1, idCommande);
                ps.executeUpdate();
            }
            con.commit();
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                e.addSuppressed(ex);
            }
            throw e;
        } finally {
            try {
                con.setAutoCommit(prev);
            } catch (SQLException ignored) {
            }
        }
    }


    /**
     * Back-office : suppression manuelle réservée aux commandes {@code annulée}.
     */
    public boolean supprimerCommandeAnnuleeParAdmin(int idCommande) throws SQLException {
        commandes c = findById(idCommande);
        if (c == null || !CommandeRegles.estStatutAnnule(c.getStatut_commande())) {
            return false;
        }
        supprimerCommandeEtLignes(idCommande);
        return true;
    }

    /**
     * Supprime automatiquement les commandes au statut <strong>annulé</strong> dont la date de création remonte à au moins {@code jours} jours.
     *
     * @return nombre de commandes supprimées
     */
    public int purgerCommandesAnnuleesPlusAnciennesQue(int jours) throws SQLException {
        List<commandes> all = afficher();
        int n = 0;
        LocalDate today = LocalDate.now();
        for (commandes c : all) {
            if (!CommandeRegles.estStatutAnnule(c.getStatut_commande())) {
                continue;
            }
            LocalDateTime dt = parseDateCommande(c.getDate_commande());
            if (dt == null) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(dt.toLocalDate(), today);
            if (days >= jours) {
                supprimerCommandeEtLignes(c.getId_commande());
                n++;
            }
        }
        return n;
    }

    /**
     * Liste toutes les commandes pour le back-office avec un libellé client (sans afficher d’identifiant).
     */
    public List<CommandeAdminRow> afficherPourAdmin() throws SQLException {
        List<commandes> liste = afficher();
        Set<Integer> ids = new LinkedHashSet<>();
        for (commandes c : liste) {
            if (c.getId_client_commande() > 0) {
                ids.add(c.getId_client_commande());
            }
        }
        Map<Integer, String> libelles = chargerLibellesClients(ids);
        List<CommandeAdminRow> rows = new ArrayList<>(liste.size());
        for (commandes c : liste) {
            int idCl = c.getId_client_commande();
            String lib = idCl > 0 ? libelles.getOrDefault(idCl, "Client") : "—";
            rows.add(new CommandeAdminRow(c, lib));
        }
        return rows;
    }

    /**
     * Tente de résoudre un libellé lisible (ex. email) pour chaque compte client — jamais un identifiant numérique affiché.
     */
    private Map<Integer, String> chargerLibellesClients(Set<Integer> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<Integer> idList = new ArrayList<>(ids);
        String placeholders = idList.stream().map(i -> "?").collect(Collectors.joining(","));
        Map<Integer, String> primaire = chargerEmailsParRequete(
                "SELECT id_utilisateur AS cid, email AS lbl FROM utilisateur WHERE id_utilisateur IN (" + placeholders + ")",
                idList);
        if (!primaire.isEmpty()) {
            return primaire;
        }
        return chargerEmailsParRequete(
                "SELECT id AS cid, email AS lbl FROM user WHERE id IN (" + placeholders + ")",
                idList);
    }

    private Map<Integer, String> chargerEmailsParRequete(String sql, List<Integer> idList) {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < idList.size(); i++) {
                ps.setInt(i + 1, idList.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                Map<Integer, String> map = new HashMap<>();
                while (rs.next()) {
                    int id = rs.getInt("cid");
                    String lbl = rs.getString("lbl");
                    if (lbl != null && !lbl.isBlank()) {
                        map.put(id, lbl.trim());
                    }
                }
                return map;
            }
        } catch (SQLException ignored) {
            return Map.of();
        }
    }

    @Override
    public List<commandes> afficher() throws SQLException {
        List<commandes> liste = new ArrayList<>();
        String sql = "SELECT * FROM commandes ORDER BY id_commande DESC";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                liste.add(mapCommande(rs));
            }
        }
        return liste;
    }

    public List<commandes> afficherParClient(int idClient) throws SQLException {
        List<commandes> liste = new ArrayList<>();
        String sql = "SELECT * FROM commandes WHERE id_client_commande = ? ORDER BY id_commande DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapCommande(rs));
                }
            }
        }
        return liste;
    }

    public List<LigneCommandeAffichage> getLignesPourCommande(int idCommande) throws SQLException {
        List<LigneCommandeAffichage> lignes = new ArrayList<>();
        String sql = "SELECT p.nom_produit, l.quantite_ligne_commande, l.prix_unitaire_ligne_commande "
                + "FROM ligne_commande l "
                + "INNER JOIN produits p ON p.id_produit = l.id_produit_ligne_commande "
                + "WHERE l.id_commande_ligne_commande = ? ORDER BY l.id_ligne_ligne_commande";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCommande);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lignes.add(new LigneCommandeAffichage(
                            rs.getString("nom_produit"),
                            rs.getInt("quantite_ligne_commande"),
                            rs.getDouble("prix_unitaire_ligne_commande")));
                }
            }
        }
        return lignes;
    }

    /**
     * Passe le statut à {@code annulée} si la commande appartient au client, est validée, et date &lt; 24 h.
     */
    public boolean annulerCommande(int idCommande, int idClient) throws SQLException {
        commandes c = findById(idCommande);
        if (c == null || c.getId_client_commande() != idClient) {
            return false;
        }
        if (!peutAnnuler(c)) {
            return false;
        }
        List<Map.Entry<Integer, Integer>> lignesStock = chargerQuantitesParProduit(idCommande);
        boolean prev = con.getAutoCommit();
        try {
            con.setAutoCommit(false);
            String sqlCmd = "UPDATE commandes SET statut_commande = ? WHERE id_commande = ? AND id_client_commande = ? AND statut_commande = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlCmd)) {
                ps.setString(1, "annulée");
                ps.setInt(2, idCommande);
                ps.setInt(3, idClient);
                ps.setString(4, "validée");
                if (ps.executeUpdate() != 1) {
                    con.rollback();
                    return false;
                }
            }
            String sqlStock = "UPDATE produits SET quantite_stock_produit = quantite_stock_produit + ? WHERE id_produit = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlStock)) {
                for (Map.Entry<Integer, Integer> e : lignesStock) {
                    ps.setInt(1, e.getValue());
                    ps.setInt(2, e.getKey());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                e.addSuppressed(ex);
            }
            throw e;
        } finally {
            try {
                con.setAutoCommit(prev);
            } catch (SQLException ignored) {
            }
        }
    }

    private List<Map.Entry<Integer, Integer>> chargerQuantitesParProduit(int idCommande) throws SQLException {
        List<Map.Entry<Integer, Integer>> list = new ArrayList<>();
        String sql = "SELECT id_produit_ligne_commande, quantite_ligne_commande FROM ligne_commande "
                + "WHERE id_commande_ligne_commande = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCommande);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AbstractMap.SimpleEntry<>(
                            rs.getInt("id_produit_ligne_commande"),
                            rs.getInt("quantite_ligne_commande")));
                }
            }
        }
        return list;
    }

    public boolean peutAnnuler(commandes c) {
        if (c == null) {
            return false;
        }
        String statut = c.getStatut_commande();
        if (statut == null || !statut.equalsIgnoreCase("validée")) {
            return false;
        }
        LocalDateTime dt = parseDateCommande(c.getDate_commande());
        if (dt == null) {
            return false;
        }
        return ChronoUnit.HOURS.between(dt, LocalDateTime.now()) < 24;
    }

    /**
     * Même fenêtre que l’annulation : commande {@code validée}, moins de 24 h après la date enregistrée.
     */
    public boolean peutModifierAdresse(commandes c) {
        return peutAnnuler(c);
    }

    /**
     * Met à jour {@code Adresse_commande} si la commande appartient au client, est validée et dans les 24 h.
     */
    public boolean modifierAdresseCommande(int idCommande, int idClient, String nouvelleAdresse) throws SQLException {
        commandes c = findById(idCommande);
        if (c == null || c.getId_client_commande() != idClient || !peutModifierAdresse(c)) {
            return false;
        }
        String sql = "UPDATE commandes SET Adresse_commande = ? WHERE id_commande = ? AND id_client_commande = ? AND statut_commande = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nouvelleAdresse);
            ps.setInt(2, idCommande);
            ps.setInt(3, idClient);
            ps.setString(4, "validée");
            return ps.executeUpdate() == 1;
        }
    }

    private commandes findById(int id) throws SQLException {
        String sql = "SELECT * FROM commandes WHERE id_commande = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCommande(rs);
                }
            }
        }
        return null;
    }

    private static commandes mapCommande(ResultSet rs) throws SQLException {
        commandes c = new commandes();
        c.setId_commande(rs.getInt("id_commande"));
        c.setDate_commande(rs.getString("date_commande"));
        c.setTotal_commande(rs.getDouble("total_commande"));
        c.setStatut_commande(rs.getString("statut_commande"));
        c.setMode_paiement_commande(rs.getString("mode_paiement_commande"));
        c.setId_client_commande(rs.getInt("id_client_commande"));
        c.setAdresse_commande(rs.getString("Adresse_commande"));
        return c;
    }

    private static LocalDateTime parseDateCommande(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        try {
            return LocalDateTime.parse(s, DT);
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
        return null;
    }

    @Override
    public void modifier(int id) throws SQLException {
        // réservé — utiliser des méthodes dédiées si besoin
    }
}
