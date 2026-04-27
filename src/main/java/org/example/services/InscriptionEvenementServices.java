package org.example.services;

import org.example.entities.InscriptionEvenement;
import org.example.utils.MyDataBase;
import org.example.utils.SqlDateReaders;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InscriptionEvenementServices implements ICrud<InscriptionEvenement> {

    Connection con;

    public InscriptionEvenementServices() {
        con = MyDataBase.getInstance().getConnection();
    }

    /** Noms réels des colonnes tels que renvoyés par le driver (SELECT * … LIMIT 0). */
    private static final class InscriptionCols {
        final String idInscription;
        final String idEvenement;
        final String idUser;
        final String dateInscription;
        final String statut;

        InscriptionCols(String idInscription, String idEvenement, String idUser, String dateInscription, String statut) {
            this.idInscription = idInscription;
            this.idEvenement = idEvenement;
            this.idUser = idUser;
            this.dateInscription = dateInscription;
            this.statut = statut;
        }
    }

    private static String bestColumnName(ResultSetMetaData md, int c) throws SQLException {
        String label = md.getColumnLabel(c);
        if (label != null && !label.isBlank()) {
            return label.trim();
        }
        String name = md.getColumnName(c);
        return name == null ? "" : name.trim();
    }

    private static void assertSqlIdentifier(String s) throws SQLException {
        if (s == null || !s.matches("^[a-zA-Z0-9_]+$")) {
            throw new SQLException("Nom de colonne inscription invalide: " + s);
        }
    }

    private static String quoteIdent(Connection con, String ident) throws SQLException {
        assertSqlIdentifier(ident);
        String q = con.getMetaData().getIdentifierQuoteString();
        if (q == null || q.isBlank()) {
            q = "`";
        }
        String esc = ident.replace(q, "");
        return q + esc + q;
    }

    /**
     * Détecte les 5 colonnes à partir des métadonnées (compatible libellés vides, préfixes de table, variantes de nom).
     */
    private static InscriptionCols detectInscriptionCols(ResultSetMetaData md) throws SQLException {
        String idInscription = null;
        String idEvenement = null;
        String idUser = null;
        String dateInscription = null;
        String statut = null;
        List<String> seen = new ArrayList<>();

        int n = md.getColumnCount();
        for (int c = 1; c <= n; c++) {
            String raw = bestColumnName(md, c);
            if (raw.isEmpty()) {
                continue;
            }
            seen.add(raw);
            String l = raw.toLowerCase(Locale.ROOT);
            if (l.contains("statut_inscription")) {
                statut = raw;
            } else if (l.contains("date_inscription")) {
                dateInscription = raw;
            } else if (l.contains("id_user") && l.contains("inscription")) {
                idUser = raw;
            } else if (l.contains("id_evenement") && l.contains("inscription")) {
                idEvenement = raw;
            } else if (l.contains("id_inscription") && l.contains("inscription")) {
                idInscription = raw;
            }
        }

        if (idInscription == null || idEvenement == null || idUser == null || dateInscription == null || statut == null) {
            throw new SQLException(
                    "Colonnes inscription_incomplètes. Colonnes vues : " + String.join(", ", seen));
        }
        return new InscriptionCols(idInscription, idEvenement, idUser, dateInscription, statut);
    }

    private InscriptionCols loadInscriptionCols() throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM inscription_evenement LIMIT 0")) {
            return detectInscriptionCols(rs.getMetaData());
        }
    }

    @Override
    public void ajouter(InscriptionEvenement i) throws SQLException {
        ajouterEtRetournerId(i);
    }

    public int ajouterEtRetournerId(InscriptionEvenement i) throws SQLException {
        InscriptionCols cols = loadInscriptionCols();
        String qd = quoteIdent(con, cols.dateInscription);
        String qs = quoteIdent(con, cols.statut);
        String qe = quoteIdent(con, cols.idEvenement);
        String qu = quoteIdent(con, cols.idUser);
        String sql = "INSERT INTO inscription_evenement (" + qd + ", " + qs + ", " + qe + ", " + qu + ") VALUES (?,?,?,?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, new Timestamp(i.getDateInscription().getTime()));
            ps.setString(2, i.getStatut());
            ps.setInt(3, i.getIdEvenement());
            ps.setInt(4, i.getIdUser());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    i.setId(keys.getInt(1));
                }
            }
            System.out.println("Inscription ajoutée avec succès !");
            return i.getId();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        InscriptionCols cols = loadInscriptionCols();
        String sql = "DELETE FROM inscription_evenement WHERE " + quoteIdent(con, cols.idInscription) + " = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        System.out.println("Inscription supprimée avec succès !");
    }

    @Override
    public List<InscriptionEvenement> afficher() throws SQLException {
        List<InscriptionEvenement> inscriptions = new ArrayList<>();

        String sql = "SELECT * FROM inscription_evenement";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData md = rs.getMetaData();
            InscriptionCols cols = detectInscriptionCols(md);

            while (rs.next()) {
                InscriptionEvenement ins = new InscriptionEvenement();
                ins.setId(rs.getInt(cols.idInscription));
                ins.setDateInscription(SqlDateReaders.readTimestampOrNull(rs, cols.dateInscription));
                ins.setStatut(rs.getString(cols.statut));
                ins.setIdEvenement(rs.getInt(cols.idEvenement));
                ins.setIdUser(rs.getInt(cols.idUser));
                inscriptions.add(ins);
            }
        }

        return inscriptions;
    }

    @Override
    public void modifier(int id) throws SQLException {
        InscriptionCols cols = loadInscriptionCols();
        String qd = quoteIdent(con, cols.dateInscription);
        String qs = quoteIdent(con, cols.statut);
        String qe = quoteIdent(con, cols.idEvenement);
        String qu = quoteIdent(con, cols.idUser);
        String qid = quoteIdent(con, cols.idInscription);
        String sql = "UPDATE inscription_evenement SET "
                + qd + " = ?, "
                + qs + " = ?, "
                + qe + " = ?, "
                + qu + " = ? "
                + "WHERE " + qid + " = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf("2026-04-05 10:00:00"));
            ps.setString(2, "confirmée");
            ps.setInt(3, 1);
            ps.setInt(4, 1);
            ps.setInt(5, id);
            ps.executeUpdate();
            System.out.println("Inscription modifiée avec succès !");
        }
    }
}
