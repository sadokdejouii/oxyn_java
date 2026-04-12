package org.example.services;

import org.example.model.planning.ai.FicheSante;
import org.example.model.planning.ai.ProgrammeGenere;
import org.example.model.planning.ai.WeeklyProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Génération de conseils personnalisés en local (règles métier), sans API externe.
 * Prévu pour être remplacé ou complété plus tard par un appel LLM en conservant la même façade.
 */
public final class PlanningAiAdviceService {

    public String generateAdvice(FicheSante fiche, ProgrammeGenere programme, WeeklyProgress progress) {
        return generateAdvice(fiche, programme, progress, 0);
    }

    /**
     * @param variantIndex permet de faire varier légèrement les formulations (ex. bouton « Régénérer »).
     */
    public String generateAdvice(FicheSante fiche, ProgrammeGenere programme, WeeklyProgress progress, int variantIndex) {
        int v = Math.floorMod(variantIndex, 4);
        List<String> blocks = new ArrayList<>();

        blocks.add(headerLine(v));
        blocks.add(syntheseProfil(fiche, programme, v));
        blocks.add(encouragementEtProgression(progress, v));

        if (programme != null && programme.isPresent()) {
            blocks.add(voletProgramme(programme, fiche, v));
        } else {
            blocks.add(section("Programme",
                    "Votre programme n’est pas encore disponible ou doit être régénéré. "
                            + "Après mise à jour de la fiche santé, vous obtiendrez des cibles caloriques et des séances cohérentes avec votre profil."));
        }

        blocks.add(voletNutrition(fiche, programme, v));
        blocks.add(voletRecuperation(fiche, programme, v));
        blocks.add(vigilanceImcEtActivite(fiche, v));

        if (progress != null && progress.defined()) {
            String enc = progress.messageEncadrant();
            if (enc != null && !enc.isBlank()) {
                blocks.add(section("Message de votre encadrant", enc.trim()));
            }
            String ia = progress.messageIa();
            if (ia != null && !ia.isBlank() && (enc == null || !ia.trim().equals(enc.trim()))) {
                blocks.add(section("Note de suivi (IA / équipe)", ia.trim()));
            }
        }

        blocks.add(footerDisclaimer());

        return String.join("\n\n", blocks.stream().filter(s -> s != null && !s.isBlank()).toList());
    }

    private static String headerLine(int v) {
        String[] salut = {
                "Bonjour — voici votre synthèse personnalisée (assistant local, sans cloud).",
                "Voici une lecture structurée de votre situation, basée sur votre fiche et votre programme.",
                "Résumé express : points forts, axes d’attention et pistes concrètes pour la semaine.",
                "Analyse locale : conseils générés à partir de vos données enregistrées."
        };
        return salut[v];
    }

    private static String syntheseProfil(FicheSante fiche, ProgrammeGenere programme, int v) {
        String objectifCode = PlanningAiAdviceMapper.objectifCode(fiche);
        String libObjectif = labelObjectif(objectifCode);
        Double imc = bmi(fiche);
        String ligneImc = imc == null
                ? "IMC : non calculable (taille ou poids manquant) — complétez la fiche pour un suivi plus fin."
                : String.format(Locale.FRANCE, "IMC estimé : %.1f (%s).", imc, imcCategory(imc));

        String act = describeActivity(fiche.niveauActivite());
        String cal = (programme != null && programme.caloriesParJour() != null)
                ? String.format("Cible énergétique actuelle du programme : environ %d kcal/j.", programme.caloriesParJour())
                : "Cible calorique : à définir lorsque le programme sera généré.";

        String[] transitions = {
                "Votre objectif déclaré oriente l’ensemble du plan.",
                "L’objectif choisi pilote l’équilibre entraînement / nutrition.",
                "Nous calons les priorités sur votre objectif et votre niveau d’activité.",
                "La cohérence entre objectif, activité et calories est le fil directeur."
        };

        return section("Profil & orientation",
                transitions[v] + "\n\n"
                        + "• Objectif : " + libObjectif + ".\n"
                        + "• Activité : " + act + ".\n"
                        + "• " + ligneImc + "\n"
                        + "• " + cal);
    }

    private static String encouragementEtProgression(WeeklyProgress progress, int v) {
        if (progress == null || !progress.defined()) {
            String[] soft = {
                    "Fixez des repères simples cette semaine : 2 séances courtes valent mieux qu’une grosse intention différée.",
                    "Sans objectif hebdo en base, concentrez-vous sur la régularité (sommeil, pas, hydratation).",
                    "Avancez par petits jalons : une habitude consolidée change la trajectoire.",
                    "Priorité à la constance : même des séances légères comptent si elles sont répétées."
            };
            return section("Motivation & suivi", soft[v]);
        }
        double t = progress.tauxRealisationPct();
        String ligne = String.format(Locale.FRANCE,
                "Semaine %d / %d — environ %.0f %% des tâches prévues sont cochées (%d / %d).",
                progress.weekNumber(), progress.year(), t,
                progress.tachesRealisees(), progress.tachesPrevues());

        String ton;
        if (t >= 85) {
            ton = "Rythme excellent : gardez cette dynamique en protégeant la récupération.";
        } else if (t >= 55) {
            ton = "Bon équilibre : quelques tâches supplémentaires peuvent verrouiller une semaine très solide.";
        } else if (t >= 25) {
            ton = "Marge de progression : visez une tâche de plus avant la fin de semaine, sans surcharger d’un coup.";
        } else {
            ton = "Repérez un créneau réaliste (20–30 min) : le déclic est souvent la planification, pas l’intensité.";
        }

        String[] intros = {
                ligne + "\n\n" + ton,
                ton + "\n\n" + ligne,
                ligne + "\n" + ton,
                ligne + "\n\n" + ton + " Statut indiqué : " + nullToDash(progress.statut()) + "."
        };
        return section("Motivation & suivi", intros[v % intros.length]);
    }

    private static String voletProgramme(ProgrammeGenere programme, FicheSante fiche, int v) {
        String obj = nullToDash(programme.objectifPrincipal());
        int n = Math.max(0, programme.entrainementsParSemaine());
        String charge = n >= 5
                ? "Volume d’entraînement soutenu : veillez au sommeil et à l’hydratation."
                : n <= 2
                ? "Peu de séances listées : si c’est volontaire (début), augmentez progressivement la fréquence."
                : "Volume modéré : bon compromis pour progresser tout en limitant la fatigue cumulée.";

        String[] alt = {
                "Objectif principal du programme : " + obj + "\n" + charge,
                charge + "\n\nObjectif principal : " + obj,
                "L’intention du programme : " + obj + "\n" + charge,
                obj + " — " + charge
        };
        return section("Entraînement & cohérence", alt[v]);
    }

    private static String voletNutrition(FicheSante fiche, ProgrammeGenere programme, int v) {
        String code = PlanningAiAdviceMapper.objectifCode(fiche);
        String base = switch (code) {
            case "perte_poids" -> "Priorité aux protéines à chaque repas, légumes à volonté, et repas réguliers pour limiter les écarts le soir.";
            case "gain_poids" -> "Ajoutez des collations denses mais saines (oléagineux, produits laitiers) sans sauter les repas principaux.";
            case "devenir_muscle", "prise_muscle" -> "Synchronisez apports protéiques avec vos séances ; hydratez-vous autour de l’effort.";
            default -> "Viser des assiettes équilibrées, peu transformées, avec des quantités stables d’un jour à l’autre.";
        };
        if (programme != null && programme.caloriesParJour() != null) {
            base += String.format(Locale.FRANCE,
                    "\nVotre programme vise environ %d kcal/j : ajustez par paliers si la faim ou la fatigue persistent plusieurs jours.",
                    programme.caloriesParJour());
        }
        String[] extra = {
                "\nAstuce : notez vos repas 3 jours d’affilée pour repérer le seul réglage utile (portion, timing ou qualité).",
                "\nHydratation : 1,5–2 L/j en visant une eau régulière, plus autour des séances.",
                "\nÉvitez les excès le week-end : l’écart ponctuel se gère, la régularité fait la différence.",
                "\nPréparez une option « plan B » (repas simple 15 min) pour les soirs chargés."
        };
        return section("Nutrition", base + extra[v]);
    }

    private static String voletRecuperation(FicheSante fiche, ProgrammeGenere programme, int v) {
        boolean low = PlanningAiAdviceMapper.isLowActivity(fiche.niveauActivite());
        String s = low
                ? "Marche quotidienne légère + étirements courts : utile si l’activité structurée est encore limitée."
                : "Alternez séances plus intenses et journées plus légères ; la progression tient aussi au sommeil (7–8 h).";
        if (programme != null && programme.entrainementsParSemaine() >= 5) {
            s += "\nCharge élevée : une séance de mobilité ou cardio léger peut aider entre deux jours plus durs.";
        }
        String[] tips = {
                s + "\nÉcoutez la douleur articulaire : distinction nette avec la simple courbature.",
                s + "\nHydratation et sodium modéré : surtout si sudation importante.",
                s + "\nRespiration ventrale 2–3 min avant le coucher : simple levier de récupération nerveuse.",
                s + "\nÉvitez d’enchaîner deux jours « à bloc » sans sommeil correct la veille."
        };
        return section("Récupération", tips[v]);
    }

    private static String vigilanceImcEtActivite(FicheSante fiche, int v) {
        Double imc = bmi(fiche);
        List<String> points = new ArrayList<>();
        if (imc != null && imc < 18.5) {
            points.add("IMC bas : proscrivez les déficits agressifs ; privilégiez un suivi médical si la maigreur est involontaire.");
        }
        if (imc != null && imc >= 30) {
            points.add("IMC élevé : progressez par l’activité modérée régulière et des ajustements alimentaires durables, pas par la restriction extrême.");
        }
        if (PlanningAiAdviceMapper.isLowActivity(fiche.niveauActivite())) {
            points.add("Activité faible au quotidien : ajoutez des micro-mouvements (escaliers, pauses marche) avant d’augmenter brutalement le volume d’entraînement.");
        }
        if (points.isEmpty()) {
            points.add("Pas de signal d’alerte particulier côté IMC / activité sur les seuils utilisés ici — restez attentif à la fatigue et à la faim.");
        }
        String[] titles = {
                "Points de vigilance",
                "Prudence & signaux à surveiller",
                "Lecture prudente du profil",
                "Axes d’attention"
        };
        return section(titles[v], String.join("\n", points));
    }

    private static String footerDisclaimer() {
        return "—\nAssistant local — les formulations peuvent être régénérées. Les conseils sont indicatifs et ne remplacent pas un avis médical.";
    }

    private static String section(String title, String body) {
        return "▸ " + title + "\n" + body.trim();
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s.trim();
    }

    private static Double bmi(FicheSante f) {
        if (f.tailleCm() == null || f.tailleCm() <= 0 || f.poidsKg() == null || f.poidsKg() <= 0) {
            return null;
        }
        double m = f.tailleCm() / 100.0;
        return f.poidsKg() / (m * m);
    }

    private static String imcCategory(double imc) {
        if (imc < 18.5) {
            return "en dessous de la fourchette habituelle";
        }
        if (imc < 25) {
            return "dans une fourchette couramment associée à un poids « normal » (indicateur statistique)";
        }
        if (imc < 30) {
            return "surpoids possible selon indicateur IMC — à interpréter avec un professionnel";
        }
        return "obésité possible selon indicateur IMC — à interpréter avec un professionnel";
    }

    private static String labelObjectif(String code) {
        return switch (code) {
            case "perte_poids" -> "perte de poids progressive";
            case "gain_poids" -> "prise de poids / rééquilibrage";
            case "devenir_muscle", "prise_muscle" -> "développement musculaire / performance";
            case "maintien" -> "maintien";
            default -> nullToDash(code);
        };
    }

    private static String describeActivity(String niveau) {
        if (niveau == null || niveau.isBlank()) {
            return "non renseigné (considéré comme modéré par défaut pour les conseils)";
        }
        String n = niveau.toLowerCase(Locale.ROOT);
        if (n.contains("sedent") || n.contains("sédent")) {
            return "plutôt sédentaire — intégrer du mouvement quotidien en priorité";
        }
        if (n.contains("peu")) {
            return "peu actif — marge pour augmenter la NEAT (marche, déplacements)";
        }
        if (n.contains("modér") || n.contains("modere") || n.contains("actif")) {
            return "modérément actif à actif — attention à la récupération si volume déjà élevé";
        }
        if (n.contains("tres") || n.contains("très")) {
            return "très actif — veiller au sommeil et aux apports pour soutenir la charge";
        }
        return niveau.replace('_', ' ');
    }
}
