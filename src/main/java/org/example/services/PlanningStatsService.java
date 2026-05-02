package org.example.services;

import org.example.model.planning.task.TacheQuotidienne;
import org.example.repository.TacheQuotidienneRepository;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Statistiques analytiques Planning (comparaison semaine actuelle/précédente).
 */
public final class PlanningStatsService {

    private final TacheQuotidienneRepository tacheRepo = new TacheQuotidienneRepository();

    public WeekStats getCurrentWeekStats(int userId) throws SQLException {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        return buildWeekStats(userId, monday);
    }

    public WeekStats getPreviousWeekStats(int userId) throws SQLException {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1);
        return buildWeekStats(userId, monday);
    }

    private WeekStats buildWeekStats(int userId, LocalDate monday) throws SQLException {
        LocalDate sunday = monday.plusDays(6);
        List<TacheQuotidienne> tasks = tacheRepo.findForUserBetween(userId, monday, sunday);

        String[] labels = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        List<DayPoint> points = new ArrayList<>(7);
        int weekDone = 0;
        int weekTotal = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            int total = 0;
            int done = 0;
            for (TacheQuotidienne t : tasks) {
                if (!d.equals(t.date())) {
                    continue;
                }
                total++;
                if (t.etat() != null && t.etat().name().equals("FAIT")) {
                    done++;
                }
            }
            weekDone += done;
            weekTotal += total;
            double score = total == 0 ? 0 : Math.round((done * 10000.0 / total)) / 100.0;
            points.add(new DayPoint(labels[i], score, done, total));
        }

        double weekPct = weekTotal == 0 ? 0 : Math.round((weekDone * 10000.0 / weekTotal)) / 100.0;
        return new WeekStats(monday, sunday, points, weekPct, weekDone, weekTotal);
    }

    public record DayPoint(
            String dayLabel,
            double scorePct,
            int doneTasks,
            int totalTasks
    ) {
    }

    public record WeekStats(
            LocalDate monday,
            LocalDate sunday,
            List<DayPoint> points,
            double weekCompletionPct,
            int doneTasks,
            int totalTasks
    ) {
    }
}
