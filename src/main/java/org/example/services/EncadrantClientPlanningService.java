package org.example.services;

import org.example.dao.FicheSanteDao;
import org.example.entities.FicheSanteRow;
import org.example.entities.ObjectifRow;
import org.example.entities.ProgrammeGenereRow;
import org.example.model.planning.ai.FicheSante;
import org.example.model.planning.ai.ProgrammeGenere;
import org.example.model.planning.ai.WeeklyProgress;
import org.example.model.planning.encadrant.EncadrantClientCardRow;
import org.example.model.planning.encadrant.EncadrantClientPlanningSnapshot;
import org.example.model.planning.task.TacheQuotidienne;
import org.example.model.planning.task.WeeklyTaskSummary;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lecture des données Planning d’un client pour l’encadrant (pas de génération de tâches ici).
 */
public final class EncadrantClientPlanningService {

    private final FicheSanteDao ficheDao = new FicheSanteDao();
    private final ProgrammePlanningService planning = new ProgrammePlanningService();
    private final WeeklyTaskService weeklyTaskService = new WeeklyTaskService();
    private final PlanningAiAdviceService aiAdvice = new PlanningAiAdviceService();

    /**
     * Clients actifs avec fiche santé — pour la grille encadrant (lecture seule).
     */
    public List<EncadrantClientCardRow> listActiveClientsWithFicheCards() throws SQLException {
        Connection c = MyDataBase.getInstance().getConnection();
        if (c == null || c.isClosed()) {
            return List.of();
        }
        var mon = weeklyTaskService.currentWeekMonday();
        var sun = weeklyTaskService.currentWeekSunday();
        Date d1 = Date.valueOf(mon);
        Date d2 = Date.valueOf(sun);
        String sql = """
                SELECT u.id_user,
                       u.first_name_user,
                       u.last_name_user,
                       u.email_user,
                       f.objectif,
                       f.niveau_activite,
                       (SELECT COUNT(*) FROM taches_quotidiennes t
                        WHERE t.user_id = u.id_user AND t.`date` BETWEEN ? AND ?) AS ntot,
                       (SELECT COUNT(*) FROM taches_quotidiennes t
                        WHERE t.user_id = u.id_user AND t.`date` BETWEEN ? AND ? AND t.etat = 'FAIT') AS ndone
                FROM users u
                INNER JOIN fiche_sante f ON f.user_id = u.id_user
                WHERE u.is_active_user = 1
                  AND u.roles_user LIKE '%ROLE_CLIENT%'
                ORDER BY u.last_name_user ASC, u.first_name_user ASC, u.id_user ASC
                """;
        List<EncadrantClientCardRow> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, d1);
            ps.setDate(2, d2);
            ps.setDate(3, d1);
            ps.setDate(4, d2);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String fn = rs.getString("first_name_user");
                    String ln = rs.getString("last_name_user");
                    String name = ((fn != null ? fn : "") + " " + (ln != null ? ln : "")).trim();
                    if (name.isEmpty()) {
                        name = "Client";
                    }
                    int ntot = rs.getInt("ntot");
                    int ndone = rs.getInt("ndone");
                    out.add(new EncadrantClientCardRow(
                            rs.getInt("id_user"),
                            name,
                            rs.getString("email_user") != null ? rs.getString("email_user") : "",
                            rs.getString("objectif"),
                            rs.getString("niveau_activite"),
                            ntot,
                            ndone));
                }
            }
        }
        return out;
    }

    public EncadrantClientPlanningSnapshot loadSnapshot(int clientUserId) throws SQLException {
        weeklyTaskService.ensureCurrentWeekObjectifForUser(clientUserId);
        String label = resolveClientLabel(clientUserId);
        Optional<FicheSanteRow> fiche = ficheDao.findByUserId(clientUserId);
        Optional<ProgrammeGenereRow> programme = planning.findProgrammeByUserId(clientUserId);
        List<TacheQuotidienne> taches = weeklyTaskService.loadCurrentWeekTasks(clientUserId);
        WeeklyTaskSummary summary = WeeklyTaskSummary.from(taches);
        Optional<ObjectifRow> obj = planning.findCurrentWeekObjectif(clientUserId);

        String progPreview = programme.map(this::buildProgrammePreview).orElse("Aucun programme généré en base pour ce client.");
        String ia = buildAiSynthese(fiche, programme, obj);

        return new EncadrantClientPlanningSnapshot(
                clientUserId,
                label,
                fiche,
                programme,
                taches,
                summary,
                obj,
                progPreview,
                ia);
    }

    public void saveObservation(int clientUserId, String messageEncadrant, boolean effortsValides) throws SQLException {
        weeklyTaskService.ensureCurrentWeekObjectifForUser(clientUserId);
        Optional<ObjectifRow> o = planning.findCurrentWeekObjectif(clientUserId);
        if (o.isEmpty()) {
            throw new SQLException("Aucune ligne objectif pour la semaine courante — "
                    + "les observations seront persistées lorsque l’objectif hebdomadaire existera (ex. après suivi client).");
        }
        planning.saveEncadrantIntervention(o.get().id(), messageEncadrant, effortsValides, null, null);
    }

    private String buildAiSynthese(
            Optional<FicheSanteRow> fiche,
            Optional<ProgrammeGenereRow> programme,
            Optional<ObjectifRow> obj) {
        if (fiche.isEmpty()) {
            return "Données insuffisantes : fiche santé absente pour ce client.";
        }
        FicheSante f = PlanningAiAdviceMapper.fromFiche(fiche.get());
        ProgrammeGenere p = PlanningAiAdviceMapper.fromProgramme(programme);
        WeeklyProgress w = PlanningAiAdviceMapper.fromObjectif(obj);
        return aiAdvice.generateAdvice(f, p, w, 0);
    }

    private String buildProgrammePreview(ProgrammeGenereRow p) {
        StringBuilder sb = new StringBuilder();
        sb.append("Calories / jour : ").append(p.caloriesParJour() != null ? p.caloriesParJour() + " kcal" : "—").append("\n");
        sb.append("Objectif principal : ").append(nullToDash(p.objectifPrincipal())).append("\n\n");
        sb.append("— Conseils généraux —\n");
        sb.append(nullToDash(p.conseilsGeneraux())).append("\n\n");
        sb.append("— Entraînement (JSON) —\n");
        sb.append(truncate(p.exercicesHebdomadairesJson(), 3500)).append("\n\n");
        sb.append("— Nutrition (JSON) —\n");
        sb.append(truncate(p.plansRepasJson(), 3500));
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null || s.isBlank()) {
            return "—";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "\n… (tronqué)";
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String resolveClientLabel(int userId) throws SQLException {
        Connection c = MyDataBase.getInstance().getConnection();
        if (c == null || c.isClosed()) {
            return "Client";
        }
        String sql = "SELECT first_name_user, last_name_user, email_user FROM users WHERE id_user = ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return "Client";
                }
                String fn = rs.getString("first_name_user");
                String ln = rs.getString("last_name_user");
                String em = rs.getString("email_user");
                String name = ((fn != null ? fn : "") + " " + (ln != null ? ln : "")).trim();
                if (!name.isEmpty()) {
                    return name + (em != null && !em.isBlank() ? " · " + em : "");
                }
                return em != null && !em.isBlank() ? em : "Client";
            }
        }
    }
}
