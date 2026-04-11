package org.example.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.dao.FicheSanteDao;
import org.example.entities.FicheSanteRow;
import org.example.entities.ProgrammeGenereRow;
import org.example.model.planning.task.TacheEtat;
import org.example.model.planning.task.TacheQuotidienne;
import org.example.model.planning.task.WeeklyTaskSummary;
import org.example.repository.TacheQuotidienneRepository;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Tâches hebdomadaires : lecture, génération si semaine vide, cycle d’état, synchronisation objectif.
 */
public final class WeeklyTaskService {

    private final TacheQuotidienneRepository tacheRepo = new TacheQuotidienneRepository();
    private final FicheSanteDao ficheDao = new FicheSanteDao();
    private final ProgrammePlanningService planning = new ProgrammePlanningService();

    public LocalDate currentWeekMonday() {
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }

    public LocalDate currentWeekSunday() {
        return currentWeekMonday().plusDays(6);
    }

    /**
     * Si aucune tâche pour la semaine ISO courante : génère des lignes à partir de la fiche / programme.
     */
    public void ensureTasksForCurrentWeek(int userId) throws SQLException {
        LocalDate mon = currentWeekMonday();
        LocalDate sun = currentWeekSunday();
        var existing = tacheRepo.findForUserBetween(userId, mon, sun);
        if (!existing.isEmpty()) {
            syncObjectifMetricsFromTasks(userId);
            return;
        }
        Optional<FicheSanteRow> fiche = ficheDao.findByUserId(userId);
        if (fiche.isEmpty()) {
            return;
        }
        Optional<ProgrammeGenereRow> prog = planning.findProgrammeByUserId(userId);
        generateAndInsertWeek(userId, mon, fiche.get(), prog.orElse(null));
        ensureObjectifRowExists(userId);
        syncObjectifMetricsFromTasks(userId);
    }

    public List<TacheQuotidienne> loadCurrentWeekTasks(int userId) throws SQLException {
        return tacheRepo.findForUserBetween(userId, currentWeekMonday(), currentWeekSunday());
    }

    public WeeklyTaskSummary summarize(List<TacheQuotidienne> taches) {
        return WeeklyTaskSummary.from(taches);
    }

    /**
     * Cycle d’état : NON_FAIT → EN_COURS → FAIT → NON_FAIT.
     */
    public void cycleEtat(int tacheId, int userId) throws SQLException {
        TacheQuotidienne t = tacheRepo.findById(tacheId, userId)
                .orElseThrow(() -> new SQLException("Tâche introuvable"));
        TacheEtat next = switch (t.etat()) {
            case NON_FAIT -> TacheEtat.EN_COURS;
            case EN_COURS -> TacheEtat.FAIT;
            case FAIT -> TacheEtat.NON_FAIT;
        };
        tacheRepo.updateEtat(tacheId, userId, next);
        ensureObjectifRowExists(userId);
        syncObjectifMetricsFromTasks(userId);
    }

    private void generateAndInsertWeek(int userId, LocalDate monday, FicheSanteRow fiche, ProgrammeGenereRow programmeOrNull) throws SQLException {
        JsonObject exRoot = null;
        if (programmeOrNull != null && programmeOrNull.exercicesHebdomadairesJson() != null
                && !programmeOrNull.exercicesHebdomadairesJson().isBlank()) {
            try {
                exRoot = JsonParser.parseString(programmeOrNull.exercicesHebdomadairesJson()).getAsJsonObject();
            } catch (Exception ignored) {
                exRoot = null;
            }
        }
        String objectifCode = fiche.objectif() != null ? fiche.objectif().toLowerCase(Locale.ROOT) : "maintien";

        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            String jourLabel = capitalize(d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH));
            String dayKey = dayKeyFr(d.getDayOfWeek());
            String description = buildDescriptionForDay(dayKey, exRoot, objectifCode, i);
            tacheRepo.insert(userId, d, jourLabel, description, TacheEtat.NON_FAIT);
        }
    }

    private static String buildDescriptionForDay(String dayKey, JsonObject exRoot, String objectifCode, int dayIndex) {
        String fromProg = firstExerciseName(exRoot, dayKey);
        if (fromProg != null && !fromProg.isBlank()) {
            return fromProg + " — séance prévue au programme";
        }
        String base = switch (objectifCode) {
            case "perte_poids" -> "Cardio léger ou marche active 25–40 min";
            case "gain_poids" -> "Renforcement modéré + apports réguliers";
            case "devenir_muscle", "prise_muscle" -> "Séance force / mobilité (programme à venir)";
            default -> "Mouvement quotidien : marche + étirements";
        };
        if (dayIndex % 2 == 0) {
            return base + " · hydratation suivie";
        }
        return base + " · repas équilibrés";
    }

    private static String firstExerciseName(JsonObject root, String dayKey) {
        if (root == null || !root.has(dayKey) || !root.get(dayKey).isJsonArray()) {
            return null;
        }
        JsonArray arr = root.getAsJsonArray(dayKey);
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            if (o.has("nom") && !o.get("nom").isJsonNull()) {
                String n = o.get("nom").getAsString().trim();
                if (!n.isEmpty()) {
                    return n;
                }
            }
        }
        return null;
    }

    private static String dayKeyFr(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "lundi";
            case TUESDAY -> "mardi";
            case WEDNESDAY -> "mercredi";
            case THURSDAY -> "jeudi";
            case FRIDAY -> "vendredi";
            case SATURDAY -> "samedi";
            case SUNDAY -> "dimanche";
        };
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void ensureObjectifRowExists(int userId) throws SQLException {
        LocalDate today = LocalDate.now();
        int week = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = today.get(IsoFields.WEEK_BASED_YEAR);
        if (planning.findObjectif(userId, week, year).isPresent()) {
            return;
        }
        Connection c = conn();
        if (c == null) {
            return;
        }
        String sql = """
                INSERT INTO objectifs_hebdomadaires
                (user_id, week_number, year, objectifs, objectif_principal, taches_prevues, taches_realisees,
                 taux_realisation, statut, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 0, 0, 0, 'EN_COURS', NOW(), NOW())
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, week);
            ps.setInt(3, year);
            ps.setString(4, "Suivi généré automatiquement");
            ps.setString(5, "Objectifs de la semaine");
            ps.executeUpdate();
        }
    }

    private void syncObjectifMetricsFromTasks(int userId) throws SQLException {
        LocalDate today = LocalDate.now();
        int week = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = today.get(IsoFields.WEEK_BASED_YEAR);
        LocalDate mon = currentWeekMonday();
        LocalDate sun = currentWeekSunday();
        var tasks = tacheRepo.findForUserBetween(userId, mon, sun);
        WeeklyTaskSummary s = WeeklyTaskSummary.from(tasks);
        int prevues = Math.max(0, s.total());
        int realisees = Math.max(0, s.fait());
        double taux = prevues == 0 ? 0 : Math.round((realisees * 1000.0 / prevues)) / 10.0;

        Connection c = conn();
        if (c == null) {
            return;
        }
        String upd = """
                UPDATE objectifs_hebdomadaires
                SET taches_prevues = ?, taches_realisees = ?, taux_realisation = ?, updated_at = NOW()
                WHERE user_id = ? AND week_number = ? AND year = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(upd)) {
            ps.setInt(1, prevues);
            ps.setInt(2, realisees);
            ps.setDouble(3, taux);
            ps.setInt(4, userId);
            ps.setInt(5, week);
            ps.setInt(6, year);
            int n = ps.executeUpdate();
            if (n == 0 && prevues > 0) {
                ensureObjectifRowExists(userId);
                try (PreparedStatement ps2 = c.prepareStatement(upd)) {
                    ps2.setInt(1, prevues);
                    ps2.setInt(2, realisees);
                    ps2.setDouble(3, taux);
                    ps2.setInt(4, userId);
                    ps2.setInt(5, week);
                    ps2.setInt(6, year);
                    ps2.executeUpdate();
                }
            }
        }
    }

    private static Connection conn() throws SQLException {
        Connection c = MyDataBase.getInstance().getConnection();
        if (c == null || c.isClosed()) {
            return null;
        }
        return c;
    }
}
