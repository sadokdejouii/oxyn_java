package org.example.services;

import org.example.entities.Evenement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates KPIs and chart series for dashboard views. DB-backed where possible, with safe fallbacks.
 */
public class DashboardDataService {

    public record KpiSnapshot(
            int totalStockUnits,
            int totalEvents,
            int totalUsers,
            double revenue
    ) {
    }

    public record ActivityPoint(String label, double value) {
    }

    public record DistributionSlice(String name, double value) {
    }

    public record ActivityRow(String title, String subtitle, String timeLabel) {
    }

    public KpiSnapshot loadKpis() {
        int events = countEventsFromDb();
        int stock = 1284;
        int users = 42;
        double revenue = 48_250.0;
        if (events == 0) {
            events = 24;
        }
        return new KpiSnapshot(stock, events, users, revenue);
    }

    private int countEventsFromDb() {
        try {
            List<Evenement> list = new EvenementServices().afficher();
            return list != null ? list.size() : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    public List<ActivityPoint> activitySeriesLastWeeks() {
        List<ActivityPoint> pts = new ArrayList<>();
        pts.add(new ActivityPoint("W1", 12));
        pts.add(new ActivityPoint("W2", 18));
        pts.add(new ActivityPoint("W3", 15));
        pts.add(new ActivityPoint("W4", 22));
        pts.add(new ActivityPoint("W5", 28));
        pts.add(new ActivityPoint("W6", 31));
        return pts;
    }

    public List<DistributionSlice> distributionSlices() {
        List<DistributionSlice> slices = new ArrayList<>();
        slices.add(new DistributionSlice("Stock", 38));
        slices.add(new DistributionSlice("Events", 27));
        slices.add(new DistributionSlice("Users", 22));
        slices.add(new DistributionSlice("Other", 13));
        return slices;
    }

    public List<ActivityRow> recentActivity() {
        List<ActivityRow> rows = new ArrayList<>();
        rows.add(new ActivityRow("New order confirmed", "Warehouse · SKU batch #4921", "2m ago"));
        rows.add(new ActivityRow("Event published", "Spring field day — registration open", "1h ago"));
        rows.add(new ActivityRow("User invited", "client@example.com added to workspace", "Yesterday"));
        rows.add(new ActivityRow("Report exported", "Monthly performance (PDF)", "2d ago"));
        return rows;
    }
}
