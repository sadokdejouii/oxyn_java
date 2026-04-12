package org.example.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.entities.FicheSanteRow;
import org.example.entities.ObjectifRow;
import org.example.entities.ProgrammeGenereRow;
import org.example.model.planning.ai.FicheSante;
import org.example.model.planning.ai.ProgrammeGenere;
import org.example.model.planning.ai.WeeklyProgress;

import java.util.Locale;
import java.util.Optional;

/**
 * Construit les entrées {@link org.example.services.PlanningAiAdviceService} à partir des entités JDBC.
 */
public final class PlanningAiAdviceMapper {

    private PlanningAiAdviceMapper() {
    }

    public static FicheSante fromFiche(FicheSanteRow f) {
        return new FicheSante(
                f.genre(),
                f.age(),
                f.tailleCm(),
                f.poidsKg(),
                f.objectif(),
                f.niveauActivite());
    }

    public static ProgrammeGenere fromProgramme(Optional<ProgrammeGenereRow> row) {
        if (row.isEmpty()) {
            return ProgrammeGenere.absent();
        }
        ProgrammeGenereRow p = row.get();
        int sessions = countTrainingSessions(p.exercicesHebdomadairesJson());
        return new ProgrammeGenere(
                p.caloriesParJour(),
                p.objectifPrincipal(),
                p.conseilsGeneraux(),
                sessions);
    }

    public static WeeklyProgress fromObjectif(Optional<ObjectifRow> obj) {
        if (obj.isEmpty()) {
            return WeeklyProgress.absent();
        }
        ObjectifRow o = obj.get();
        return new WeeklyProgress(
                true,
                o.weekNumber(),
                o.year(),
                o.tauxRealisation(),
                o.tachesPrevues(),
                o.tachesRealisees(),
                o.statut(),
                o.messageEncadrant(),
                o.messageIa());
    }

    static int countTrainingSessions(String exercicesJson) {
        if (exercicesJson == null || exercicesJson.isBlank()) {
            return 0;
        }
        try {
            JsonObject o = JsonParser.parseString(exercicesJson).getAsJsonObject();
            String[] jours = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
            int total = 0;
            for (String jour : jours) {
                if (!o.has(jour) || !o.get(jour).isJsonArray()) {
                    continue;
                }
                JsonArray arr = o.getAsJsonArray(jour);
                for (JsonElement el : arr) {
                    if (el.isJsonObject()) {
                        total++;
                    }
                }
            }
            return total;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Normalise le code objectif fiche (même logique que la génération de programme).
     */
    public static String objectifCode(FicheSante fiche) {
        String o = fiche.objectif();
        if (o == null || o.isBlank()) {
            return "maintien";
        }
        return o.toLowerCase(Locale.ROOT).trim();
    }

    public static boolean isLowActivity(String niveauActivite) {
        if (niveauActivite == null || niveauActivite.isBlank()) {
            return true;
        }
        String n = niveauActivite.toLowerCase(Locale.ROOT);
        return n.contains("sedent") || n.equals("sedentaire") || n.equals("sédentaire") || n.contains("peu_actif");
    }
}
