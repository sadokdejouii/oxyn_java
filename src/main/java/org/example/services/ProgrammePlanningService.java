package org.example.services;

import org.example.entities.FicheListRow;
import org.example.entities.FicheSanteRow;
import org.example.entities.ObjectifListRow;
import org.example.entities.ObjectifRow;
import org.example.entities.ProgrammeGenereRow;
import org.example.repository.ProgrammeGenereRepository;
import org.example.repository.planning.FicheSanteRepository;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lecture / écriture des tables programme (aligné Symfony / base {@code oxyn}).
 */
public final class ProgrammePlanningService {

    private final FicheSanteRepository ficheSanteRepository = new FicheSanteRepository();
    private final ProgrammeGenereRepository programmeGenereRepository = new ProgrammeGenereRepository();

    public Optional<FicheSanteRow> findFicheByUserId(int userId) throws SQLException {
        return ficheSanteRepository.findByUserId(userId);
    }

    public Optional<ProgrammeGenereRow> findProgrammeByUserId(int userId) throws SQLException {
        return programmeGenereRepository.findLatestByUserId(userId);
    }

    public Optional<ObjectifRow> findCurrentWeekObjectif(int userId) throws SQLException {
        LocalDate today = LocalDate.now();
        int week = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = today.get(IsoFields.WEEK_BASED_YEAR);
        return findObjectif(userId, week, year);
    }

    public Optional<ObjectifRow> findObjectif(int userId, int weekNumber, int year) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, user_id, week_number, year, objectifs, objectif_principal,
                       taches_prevues, taches_realisees, taux_realisation, statut,
                       message_ia, message_encadrant, efforts_valides
                FROM objectifs_hebdomadaires
                WHERE user_id = ? AND week_number = ? AND year = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, weekNumber);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapObjectif(rs));
            }
        }
    }

    private static ObjectifRow mapObjectif(ResultSet rs) throws SQLException {
        Boolean eff = rs.getObject("efforts_valides") != null ? rs.getBoolean("efforts_valides") : null;
        if (rs.wasNull()) {
            eff = null;
        }
        return new ObjectifRow(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("week_number"),
                rs.getInt("year"),
                rs.getString("objectifs"),
                rs.getString("objectif_principal"),
                rs.getInt("taches_prevues"),
                rs.getInt("taches_realisees"),
                rs.getDouble("taux_realisation"),
                rs.getString("statut"),
                rs.getString("message_ia"),
                rs.getString("message_encadrant"),
                eff
        );
    }

    public List<ObjectifListRow> findAllObjectifsRecent(int limit) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return List.of();
        }
        int lim = Math.min(Math.max(limit, 1), 500);
        String sql = """
                SELECT o.id, o.user_id, o.week_number, o.year, o.statut, o.taux_realisation,
                       o.objectif_principal, o.message_encadrant,
                       u.first_name_user, u.last_name_user, u.email_user
                FROM objectifs_hebdomadaires o
                JOIN users u ON u.id_user = o.user_id
                ORDER BY o.year DESC, o.week_number DESC, o.id DESC
                LIMIT ?
                """;
        List<ObjectifListRow> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, lim);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String fn = rs.getString("first_name_user");
                    String ln = rs.getString("last_name_user");
                    String name = ((fn == null ? "" : fn) + " " + (ln == null ? "" : ln)).trim();
                    list.add(new ObjectifListRow(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            name,
                            rs.getString("email_user"),
                            rs.getInt("week_number"),
                            rs.getInt("year"),
                            rs.getString("statut"),
                            rs.getDouble("taux_realisation"),
                            rs.getString("objectif_principal"),
                            rs.getString("message_encadrant")
                    ));
                }
            }
        }
        return list;
    }

    public void saveEncadrantIntervention(int objectifId, String messageEncadrant, boolean effortsValides,
                                          Integer tachesPrevues, String newStatut) throws SQLException {
        Connection c = conn();
        if (c == null) {
            return;
        }
        StringBuilder sb = new StringBuilder("""
                UPDATE objectifs_hebdomadaires SET
                message_encadrant = ?,
                message_encadrant_date = NOW(),
                efforts_valides = ?,
                updated_at = NOW()
                """);
        if (tachesPrevues != null) {
            sb.append(", taches_prevues = ?");
        }
        if (newStatut != null && !newStatut.isBlank()) {
            sb.append(", statut = ?");
        }
        sb.append(" WHERE id = ?");
        try (PreparedStatement ps = c.prepareStatement(sb.toString())) {
            int i = 1;
            ps.setString(i++, messageEncadrant);
            ps.setBoolean(i++, effortsValides);
            if (tachesPrevues != null) {
                ps.setInt(i++, tachesPrevues);
            }
            if (newStatut != null && !newStatut.isBlank()) {
                ps.setString(i++, newStatut);
            }
            ps.setInt(i, objectifId);
            ps.executeUpdate();
        }
    }

    public List<FicheListRow> listFichesSanteWithClients() throws SQLException {
        Connection c = conn();
        if (c == null) {
            return List.of();
        }
        String sql = """
                SELECT f.id, f.user_id, f.genre, f.age, f.objectif,
                       u.first_name_user, u.last_name_user, u.email_user
                FROM fiche_sante f
                JOIN users u ON u.id_user = f.user_id
                ORDER BY f.updated_at DESC, f.id DESC
                """;
        List<FicheListRow> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String fn = rs.getString("first_name_user");
                String ln = rs.getString("last_name_user");
                String name = ((fn == null ? "" : fn) + " " + (ln == null ? "" : ln)).trim();
                list.add(new FicheListRow(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        name,
                        rs.getString("email_user"),
                        rs.getString("genre"),
                        (Integer) rs.getObject("age"),
                        rs.getString("objectif")
                ));
            }
        }
        return list;
    }

    private static Connection conn() throws SQLException {
        Connection c = MyDataBase.getInstance().getConnection();
        if (c == null || c.isClosed()) {
            return null;
        }
        return c;
    }
}
