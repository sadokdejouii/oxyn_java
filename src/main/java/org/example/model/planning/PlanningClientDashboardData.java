package org.example.model.planning;

import org.example.entities.FicheSanteRow;
import org.example.entities.ObjectifRow;
import org.example.entities.ProgrammeGenereRow;
import org.example.model.planning.task.TacheQuotidienne;

import java.util.List;
import java.util.Optional;

/**
 * Agrégat lecture pour l’écran Planning client.
 */
public record PlanningClientDashboardData(
        Optional<FicheSanteRow> fiche,
        Optional<ProgrammeGenereRow> programme,
        Optional<ObjectifRow> objectifSemaine,
        List<TacheQuotidienne> tachesSemaine
) {
}
