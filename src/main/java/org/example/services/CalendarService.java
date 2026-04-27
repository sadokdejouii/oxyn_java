package org.example.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.model.planning.task.TacheEtat;
import org.example.model.planning.task.TacheQuotidienne;
import org.example.repository.TacheQuotidienneRepository;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * API métier calendrier des tâches hebdomadaires.
 */
public final class CalendarService {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)\\s*(min|mn|minutes|h|heure|heures)", Pattern.CASE_INSENSITIVE);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final TacheQuotidienneRepository tacheRepo = new TacheQuotidienneRepository();
    private final WeeklyTaskService weeklyTaskService = new WeeklyTaskService();

    public CalendarMonthResponse getTasksByMonth(int userId) throws SQLException {
        return getTasksByMonth(userId, YearMonth.now());
    }

    public CalendarMonthResponse getTasksByMonth(int userId, YearMonth month) throws SQLException {
        YearMonth ym = month != null ? month : YearMonth.now();
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        // API interne planning: source unique des taches du mois pour la vue calendrier.
        List<TacheQuotidienne> monthTasks = tacheRepo.findForUserBetween(userId, from, to);

        Map<LocalDate, List<CalendarTaskItem>> grouped = monthTasks.stream()
                .sorted(Comparator.comparing(TacheQuotidienne::date).thenComparing(TacheQuotidienne::id))
                .collect(Collectors.groupingBy(
                        TacheQuotidienne::date,
                        LinkedHashMap::new,
                        Collectors.mapping(CalendarService::toApiTask, Collectors.toList())
                ));

        List<CalendarDayItem> days = grouped.entrySet().stream()
                .map(e -> {
                    // Pré-calcule les KPI affiches sur les cartes jours (fait/non fait/apercu).
                    long done = e.getValue().stream().filter(CalendarTaskItem::fait).count();
                    long pending = e.getValue().size() - done;
                    List<String> preview = e.getValue().stream().map(CalendarTaskItem::nom).limit(2).toList();
                    return new CalendarDayItem(e.getKey(), e.getValue().size(), (int) done, (int) pending, preview, e.getValue());
                })
                .toList();

        double weekProgress = weekProgress(userId, LocalDate.now());
        return new CalendarMonthResponse(userId, ym, days, weekProgress);
    }

    public CalendarDateResponse getTasksByDate(int userId, LocalDate date) throws SQLException {
        LocalDate selected = date != null ? date : LocalDate.now();
        List<CalendarTaskItem> tasks = tacheRepo.findByUserId(userId).stream()
                .filter(t -> selected.equals(t.date()))
                .sorted(Comparator.comparing(TacheQuotidienne::id))
                .map(CalendarService::toApiTask)
                .toList();
        double dayProgress = progress(tasks);
        double weekProgress = weekProgress(userId, selected);
        return new CalendarDateResponse(userId, selected, dayProgress, weekProgress, tasks);
    }

    public CalendarTaskItem updateTaskStatus(int userId, int taskId) throws SQLException {
        TacheQuotidienne existing = tacheRepo.findById(taskId, userId)
                .orElseThrow(() -> new SQLException("Tâche introuvable"));
        TacheEtat next = existing.etat() == TacheEtat.FAIT ? TacheEtat.NON_FAIT : TacheEtat.FAIT;
        // Point d'ecriture principal du calendrier: toggle etat puis resynchronisation hebdo.
        tacheRepo.updateEtat(taskId, userId, next);
        weeklyTaskService.ensureCurrentWeekObjectifForUser(userId);
        TacheQuotidienne updated = tacheRepo.findById(taskId, userId)
                .orElseThrow(() -> new SQLException("Tâche mise à jour introuvable"));
        return toApiTask(updated);
    }

    public String getTasksByMonthJson(int userId, YearMonth month) throws SQLException {
        return GSON.toJson(getTasksByMonth(userId, month));
    }

    public String getTasksByDateJson(int userId, LocalDate date) throws SQLException {
        return GSON.toJson(getTasksByDate(userId, date));
    }

    private static CalendarTaskItem toApiTask(TacheQuotidienne t) {
        // Mapping stable DB -> DTO UI/API interne (nom, duree lisible, statut logique).
        String desc = t.description() != null ? t.description().trim() : "";
        String nom = extractName(desc);
        String duree = extractDuration(desc);
        boolean fait = t.etat() == TacheEtat.FAIT;
        String status = fait ? "FAIT" : "NON_FAIT";
        return new CalendarTaskItem(t.id(), t.date(), nom, duree, status, fait);
    }

    private static String extractName(String desc) {
        if (desc == null || desc.isBlank()) {
            return "Tâche";
        }
        int sep = desc.indexOf("—");
        if (sep <= 0) {
            sep = desc.indexOf("-");
        }
        String base = sep > 0 ? desc.substring(0, sep).trim() : desc;
        return base.isBlank() ? "Tâche" : base;
    }

    private static String extractDuration(String desc) {
        if (desc == null || desc.isBlank()) {
            return "—";
        }
        Matcher m = DURATION_PATTERN.matcher(desc);
        if (!m.find()) {
            return "—";
        }
        return m.group(1) + " " + m.group(2).toLowerCase(Locale.ROOT);
    }

    private double weekProgress(int userId, LocalDate anchorDate) throws SQLException {
        LocalDate day = anchorDate != null ? anchorDate : LocalDate.now();
        LocalDate mon = day.with(DayOfWeek.MONDAY);
        LocalDate sun = mon.plusDays(6);
        List<CalendarTaskItem> weekTasks = tacheRepo.findForUserBetween(userId, mon, sun).stream()
                .map(CalendarService::toApiTask)
                .toList();
        return progress(weekTasks);
    }

    private static double progress(List<CalendarTaskItem> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        long done = tasks.stream().filter(CalendarTaskItem::fait).count();
        return Math.round((done * 10000.0 / tasks.size())) / 100.0;
    }

    public record CalendarTaskItem(
            int taskId,
            LocalDate date,
            String nom,
            String duree,
            String statut,
            boolean fait
    ) {
    }

    public record CalendarDayItem(
            LocalDate date,
            int totalTaches,
            int tachesFaites,
            int tachesNonFaites,
            List<String> apercu,
            List<CalendarTaskItem> taches
    ) {
    }

    public record CalendarMonthResponse(
            int userId,
            YearMonth month,
            List<CalendarDayItem> days,
            double progressionSemainePct
    ) {
    }

    public record CalendarDateResponse(
            int userId,
            LocalDate date,
            double progressionJourPct,
            double progressionSemainePct,
            List<CalendarTaskItem> taches
    ) {
    }
}
