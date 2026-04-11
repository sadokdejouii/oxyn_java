package org.example.model.planning.encadrant;

import org.example.entities.FicheSanteRow;
import org.example.entities.ObjectifRow;
import org.example.entities.ProgrammeGenereRow;
import org.example.model.planning.task.TacheQuotidienne;
import org.example.model.planning.task.WeeklyTaskSummary;

import java.util.List;
import java.util.Optional;

/**
 * Données Planning d’un client pour la vue encadrant (lecture + champs éditables côté UI).
 */
public record EncadrantClientPlanningSnapshot(
        int clientUserId,
        String clientLabel,
        Optional<FicheSanteRow> fiche,
        Optional<ProgrammeGenereRow> programme,
        List<TacheQuotidienne> tachesSemaine,
        WeeklyTaskSummary taskSummary,
        Optional<ObjectifRow> objectifSemaine,
        String programmeTextPreview,
        String conseilsIaSynthese
) {
}
