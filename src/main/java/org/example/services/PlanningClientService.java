package org.example.services;



import org.example.dao.FicheSanteDao;

import org.example.entities.FicheSanteRow;

import org.example.model.planning.FicheSanteFormData;

import org.example.model.planning.PlanningClientDashboardData;

import org.example.model.planning.task.TacheQuotidienne;



import java.sql.SQLException;

import java.util.List;



/**

 * Orchestration métier Planning côté client : fiche santé, génération programme, agrégat dashboard.

 */

public final class PlanningClientService {



    private final FicheSanteDao ficheDao = new FicheSanteDao();

    private final ProgrammeGeneratorService programmeGenerator = new ProgrammeGeneratorService();

    private final ProgrammePlanningService planningRead = new ProgrammePlanningService();

    private final WeeklyTaskService weeklyTaskService = new WeeklyTaskService();



    public PlanningClientDashboardData loadDashboard(int userId) throws SQLException {

        weeklyTaskService.ensureTasksForCurrentWeek(userId);

        var fiche = ficheDao.findByUserId(userId);

        var programme = planningRead.findProgrammeByUserId(userId);

        var obj = planningRead.findCurrentWeekObjectif(userId);

        List<TacheQuotidienne> taches = weeklyTaskService.loadCurrentWeekTasks(userId);

        return new PlanningClientDashboardData(fiche, programme, obj, taches);

    }



    public void createFicheAndGenerateProgram(int userId, FicheSanteFormData form) throws SQLException {

        ficheDao.insert(userId, form);

        programmeGenerator.regenerateProgramForUser(userId);

    }



    public void updateFicheAndRegenerateProgram(int userId, FicheSanteFormData form) throws SQLException {

        FicheSanteRow existing = ficheDao.findByUserId(userId).orElseThrow(() -> new SQLException("Aucune fiche à mettre à jour"));

        ficheDao.update(existing.id(), userId, form);

        programmeGenerator.regenerateProgramForUser(userId);

    }



    public void cycleTacheEtat(int tacheId, int userId) throws SQLException {

        weeklyTaskService.cycleEtat(tacheId, userId);

    }

}

