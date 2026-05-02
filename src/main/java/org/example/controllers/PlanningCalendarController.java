package org.example.controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.services.CalendarService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sous-page Planning : calendrier des tâches hebdomadaires.
 */
public final class PlanningCalendarController {

    private final int userId;
    private final Runnable onBack;
    private final CalendarService calendarService = new CalendarService();

    private YearMonth displayedMonth = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();
    private CalendarService.CalendarMonthResponse monthResponse;

    private DatePicker miniCalendar;
    private CheckBox filterDone;
    private CheckBox filterNotDone;
    private Label monthTitle;
    private Label lblDayProgress;
    private Label lblWeekProgress;
    private VBox dayTasksBox;
    private GridPane monthGrid;
    private Label detailDateLabel;
    private VBox detailTasksBox;

    public PlanningCalendarController(int userId, Runnable onBack) {
        this.userId = userId;
        this.onBack = onBack;
    }

    public VBox buildRoot() {
        Button btnBack = new Button("← Retour au planning");
        btnBack.getStyleClass().add("planning-cal-back");
        btnBack.setOnAction(e -> onBack.run());

        Label title = new Label("Calendrier des tâches hebdomadaires");
        title.getStyleClass().add("planning-cal-title");
        Label sub = new Label("Vue mensuelle interactive, pointage fait/non fait, détails du jour et progression.");
        sub.getStyleClass().add("planning-cal-sub");
        sub.setWrapText(true);

        HBox split = new HBox(18);
        split.getStyleClass().add("planning-cal-main-shell");
        VBox.setVgrow(split, Priority.ALWAYS);

        VBox left = buildLeftPane();
        VBox right = buildRightPane();
        HBox.setHgrow(right, Priority.ALWAYS);

        split.getChildren().addAll(left, right);

        VBox root = new VBox(14, btnBack, title, sub, split);
        root.setPadding(new Insets(4, 4, 24, 4));
        root.getStyleClass().add("planning-cal-root");

        refreshMonth();
        return root;
    }

    private VBox buildLeftPane() {
        Label leftTitle = new Label("Calendrier & filtres");
        leftTitle.getStyleClass().add("planning-cal-card-title");

        miniCalendar = new DatePicker(selectedDate);
        miniCalendar.getStyleClass().add("planning-cal-mini-picker");
        miniCalendar.setOnAction(e -> {
            LocalDate picked = miniCalendar.getValue();
            if (picked == null) {
                return;
            }
            selectedDate = picked;
            displayedMonth = YearMonth.from(picked);
            refreshMonth();
        });
        HBox miniCalendarWrap = new HBox(miniCalendar);
        miniCalendarWrap.setAlignment(Pos.CENTER);
        miniCalendarWrap.getStyleClass().add("planning-cal-mini-wrap");

        filterDone = new CheckBox("Fait");
        filterDone.setSelected(true);
        filterDone.getStyleClass().add("planning-cal-filter");
        filterNotDone = new CheckBox("Non fait");
        filterNotDone.setSelected(true);
        filterNotDone.getStyleClass().add("planning-cal-filter");
        filterDone.selectedProperty().addListener((obs, oldV, newV) -> refreshDateDetails());
        filterNotDone.selectedProperty().addListener((obs, oldV, newV) -> refreshDateDetails());

        Label dayTaskTitle = new Label("Tâches du jour");
        dayTaskTitle.getStyleClass().add("planning-cal-card-title");
        dayTasksBox = new VBox(8);
        dayTasksBox.getStyleClass().add("planning-cal-day-list");

        VBox left = new VBox(12, leftTitle, miniCalendarWrap, filterDone, filterNotDone, dayTaskTitle, dayTasksBox);
        left.getStyleClass().addAll("planning-cal-card", "planning-cal-sidebar");
        left.setMinWidth(280);
        left.setPrefWidth(280);
        left.setMaxWidth(280);
        return left;
    }

    private VBox buildRightPane() {
        Button prev = new Button("◀");
        prev.getStyleClass().add("planning-cal-nav-btn");
        prev.setOnAction(e -> {
            displayedMonth = displayedMonth.minusMonths(1);
            refreshMonth();
        });

        Button next = new Button("▶");
        next.getStyleClass().add("planning-cal-nav-btn");
        next.setOnAction(e -> {
            displayedMonth = displayedMonth.plusMonths(1);
            refreshMonth();
        });

        monthTitle = new Label();
        monthTitle.getStyleClass().add("planning-cal-month-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lblDayProgress = new Label("Jour: 0%");
        lblDayProgress.getStyleClass().add("planning-cal-progress-pill");
        lblWeekProgress = new Label("Semaine: 0%");
        lblWeekProgress.getStyleClass().add("planning-cal-progress-pill");

        HBox top = new HBox(10, prev, monthTitle, next, spacer, lblDayProgress, lblWeekProgress);
        top.setAlignment(Pos.CENTER_LEFT);

        monthGrid = new GridPane();
        monthGrid.getStyleClass().add("planning-cal-grid");
        monthGrid.setHgap(10);
        monthGrid.setVgap(10);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0);
            cc.setHgrow(Priority.ALWAYS);
            monthGrid.getColumnConstraints().add(cc);
        }

        Label detailTitle = new Label("Détail du jour");
        detailTitle.getStyleClass().add("planning-cal-card-title");
        detailDateLabel = new Label();
        detailDateLabel.getStyleClass().add("planning-cal-detail-date");
        detailTasksBox = new VBox(10);
        detailTasksBox.getStyleClass().add("planning-cal-detail-list");

        VBox detailCard = new VBox(10, detailTitle, detailDateLabel, detailTasksBox);
        detailCard.getStyleClass().addAll("planning-cal-card", "planning-cal-detail-card");

        VBox calendarCard = new VBox(12, top, monthGrid);
        calendarCard.getStyleClass().addAll("planning-cal-card", "planning-cal-calendar-card");
        VBox.setVgrow(monthGrid, Priority.ALWAYS);

        VBox right = new VBox(20, calendarCard, detailCard);
        right.getStyleClass().add("planning-cal-right-col");
        VBox.setVgrow(calendarCard, Priority.ALWAYS);
        VBox.setVgrow(detailCard, Priority.NEVER);
        return right;
    }

    private void refreshMonth() {
        try {
            monthResponse = calendarService.getTasksByMonth(userId, displayedMonth);
            monthTitle.setText(capitalize(displayedMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH))
                    + " " + displayedMonth.getYear());
            renderMonthGrid();
            refreshDateDetails();
        } catch (SQLException e) {
            monthGrid.getChildren().clear();
            monthGrid.add(errorLabel("Chargement du calendrier impossible: " + safeMsg(e)), 0, 0, 7, 1);
            detailTasksBox.getChildren().setAll(errorLabel("Erreur: " + safeMsg(e)));
        }
    }

    private void renderMonthGrid() {
        monthGrid.getChildren().clear();
        String[] days = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int c = 0; c < days.length; c++) {
            Label h = new Label(days[c]);
            h.getStyleClass().add("planning-cal-grid-head");
            monthGrid.add(h, c, 0);
        }

        LocalDate first = displayedMonth.atDay(1);
        int startCol = first.getDayOfWeek().getValue() - 1;
        int total = displayedMonth.lengthOfMonth();
        Map<LocalDate, CalendarService.CalendarDayItem> byDate = monthResponse.days().stream()
                .collect(Collectors.toMap(CalendarService.CalendarDayItem::date, d -> d));

        int cell = 0;
        for (int i = 0; i < 42; i++) {
            int row = (i / 7) + 1;
            int col = i % 7;
            VBox dayCell = new VBox(4);
            dayCell.getStyleClass().add("planning-cal-day-cell");
            dayCell.setPadding(new Insets(8));
            dayCell.setMinHeight(96);
            dayCell.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(dayCell, Priority.ALWAYS);

            if (i >= startCol && cell < total) {
                LocalDate date = displayedMonth.atDay(++cell);
                if (date.equals(selectedDate)) {
                    dayCell.getStyleClass().add("planning-cal-day-cell--selected");
                }
                Label num = new Label(String.valueOf(date.getDayOfMonth()));
                num.getStyleClass().add("planning-cal-day-num");
                dayCell.getChildren().add(num);

                CalendarService.CalendarDayItem item = byDate.get(date);
                if (item != null) {
                    List<CalendarService.CalendarTaskItem> filtered = applyFilters(item.taches());
                    int done = (int) filtered.stream().filter(CalendarService.CalendarTaskItem::fait).count();
                    int pending = Math.max(0, filtered.size() - done);
                    Label count = new Label(filtered.size() + " tâche(s)");
                    count.getStyleClass().add("planning-cal-day-count");
                    dayCell.getChildren().add(count);

                    HBox dots = new HBox(6);
                    dots.setAlignment(Pos.CENTER_LEFT);
                    if (done > 0) {
                        Label dotDone = new Label("● Fait");
                        dotDone.getStyleClass().add("planning-cal-dot-done");
                        dots.getChildren().add(dotDone);
                    }
                    if (pending > 0) {
                        Label dotPending = new Label("● Non fait");
                        dotPending.getStyleClass().add("planning-cal-dot-pending");
                        dots.getChildren().add(dotPending);
                    }
                    if (!dots.getChildren().isEmpty()) {
                        dayCell.getChildren().add(dots);
                    }
                    filtered.stream().limit(2).forEach(t -> {
                        Label pv = new Label("• " + t.nom());
                        pv.getStyleClass().add("planning-cal-preview");
                        pv.setWrapText(true);
                        dayCell.getChildren().add(pv);
                    });
                }

                dayCell.setOnMouseClicked(e -> {
                    selectedDate = date;
                    miniCalendar.setValue(date);
                    renderMonthGrid();
                    refreshDateDetails();
                });
            } else {
                dayCell.getStyleClass().add("planning-cal-day-cell--empty");
            }
            monthGrid.add(dayCell, col, row);
        }
    }

    private void refreshDateDetails() {
        if (selectedDate == null) {
            return;
        }
        try {
            CalendarService.CalendarDateResponse day = calendarService.getTasksByDate(userId, selectedDate);
            List<CalendarService.CalendarTaskItem> filtered = applyFilters(day.taches());

            detailDateLabel.setText(selectedDate + " · " + capitalize(selectedDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH)));
            lblDayProgress.setText(String.format(Locale.FRANCE, "Jour: %.0f%%", progress(filtered)));
            lblWeekProgress.setText(String.format(Locale.FRANCE, "Semaine: %.0f%%", day.progressionSemainePct()));

            dayTasksBox.getChildren().clear();
            if (filtered.isEmpty()) {
                dayTasksBox.getChildren().add(emptyLabel("Aucune tâche pour ce jour."));
            } else {
                filtered.forEach(t -> dayTasksBox.getChildren().add(dayTaskMiniRow(t)));
            }

            detailTasksBox.getChildren().clear();
            if (filtered.isEmpty()) {
                detailTasksBox.getChildren().add(emptyLabel("Aucune tâche à afficher."));
                return;
            }
            filtered.forEach(t -> detailTasksBox.getChildren().add(detailTaskRow(t)));
        } catch (SQLException e) {
            detailTasksBox.getChildren().setAll(errorLabel("Erreur détail: " + safeMsg(e)));
        }
    }

    private HBox dayTaskMiniRow(CalendarService.CalendarTaskItem t) {
        Label name = new Label("• " + t.nom());
        name.setWrapText(true);
        name.getStyleClass().add("planning-cal-mini-task");
        Label status = new Label(t.fait() ? "✔" : "⏳");
        status.getStyleClass().add(t.fait() ? "planning-cal-mini-status-done" : "planning-cal-mini-status-pending");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, name, spacer, status);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox detailTaskRow(CalendarService.CalendarTaskItem t) {
        Label name = new Label(t.nom());
        name.getStyleClass().add("planning-cal-task-name");
        name.setWrapText(true);

        Label duration = new Label("Durée: " + t.duree());
        duration.getStyleClass().add("planning-cal-task-duration");

        Label status = new Label(t.fait() ? "✔ FAIT" : "⏳ NON FAIT");
        status.getStyleClass().add(t.fait() ? "planning-cal-task-status-done" : "planning-cal-task-status-pending");

        Button toggle = new Button(t.fait() ? "Marquer non fait" : "Marquer comme fait");
        toggle.getStyleClass().add("planning-cal-toggle-btn");
        toggle.setOnAction(e -> {
            try {
                calendarService.updateTaskStatus(userId, t.taskId());
                refreshMonth();
            } catch (SQLException ex) {
                detailTasksBox.getChildren().add(errorLabel("Mise à jour impossible: " + safeMsg(ex)));
            }
        });

        HBox meta = new HBox(10, duration, status);
        meta.setAlignment(Pos.CENTER_LEFT);

        VBox row = new VBox(8, name, meta, toggle);
        row.getStyleClass().add("planning-cal-task-row");
        return row;
    }

    private List<CalendarService.CalendarTaskItem> applyFilters(List<CalendarService.CalendarTaskItem> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        boolean includeDone = filterDone == null || filterDone.isSelected();
        boolean includePending = filterNotDone == null || filterNotDone.isSelected();
        if (!includeDone && !includePending) {
            return List.of();
        }
        return tasks.stream()
                .filter(t -> (t.fait() && includeDone) || (!t.fait() && includePending))
                .toList();
    }

    private static double progress(List<CalendarService.CalendarTaskItem> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        long done = tasks.stream().filter(CalendarService.CalendarTaskItem::fait).count();
        return Math.round((done * 10000.0 / tasks.size())) / 100.0;
    }

    private static Label emptyLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("planning-cal-empty");
        l.setWrapText(true);
        return l;
    }

    private static Label errorLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("planning-cal-error");
        l.setWrapText(true);
        return l;
    }

    private static String safeMsg(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
