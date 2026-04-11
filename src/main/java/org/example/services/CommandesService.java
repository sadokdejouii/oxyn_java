package org.example.services;

import org.example.entities.LigneCommandeAffichage;
import org.example.entities.LignePanier;
import org.example.entities.commandes;

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
import java.util.ArrayList;
import java.util.List;

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
        con = MyDataBase.getInstance().getConnection();
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
        String insertCmd = "INSERT INTO commandes (date_commande, total_commande, statut_commande, mode_paiement_commande, id_client_commande, Adresse_commande) VALUES (?,?,?,?,?,?)";
        String insertLigne = "INSERT INTO ligne_commande ("
                + "quantite_ligne_commande, prix_unitaire_ligne_commande, sous_total_ligne_commande, "
                + "id_commande_ligne_commande, id_produit_ligne_commande) VALUES (?,?,?,?,?)";
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
                for (LignePanier ligne : lignes) {
                    if (ligne.getProduit() == null) {
                        continue;
                    }
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
        String sql = "DELETE FROM commandes WHERE id_commande=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
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
        String sql = "UPDATE commandes SET statut_commande = ? WHERE id_commande = ? AND id_client_commande = ? AND statut_commande = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "annulée");
            ps.setInt(2, idCommande);
            ps.setInt(3, idClient);
            ps.setString(4, "validée");
            return ps.executeUpdate() == 1;
        }
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
