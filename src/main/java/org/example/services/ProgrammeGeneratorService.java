package org.example.services;

import org.example.dao.FicheSanteDao;
import org.example.entities.FicheSanteRow;
import org.example.model.programme.ExerciseItem;
import org.example.model.programme.MealBlock;
import org.example.model.programme.MealExample;
import org.example.model.programme.PlansRepas;
import org.example.model.programme.ProgrammeGenere;
import org.example.model.programme.WeeklyExercisePlan;
import org.example.repository.ProgrammeGenereRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Génération de programme personnalisé (logique métier proche Symfony) : calories Mifflin–St Jeor,
 * séances hebdo, plans repas, conseils — persistance via {@link #regenerateProgramForUser(int)}.
 */
public final class ProgrammeGeneratorService {

    private final FicheSanteDao ficheDao = new FicheSanteDao();
    private final ProgrammeGenereRepository programmeRepository = new ProgrammeGenereRepository();

    /**
     * Reconstruit le domaine {@link ProgrammeGenere} sans écrire en base (tests / prévisualisation).
     */
    public ProgrammeGenere generateFromFiche(FicheSanteRow fiche) {
        int userId = fiche.userId();
        int age = fiche.age() != null ? fiche.age() : 30;
        int tailleCm = fiche.tailleCm() != null ? fiche.tailleCm() : 170;
        double poidsKg = fiche.poidsKg() != null ? fiche.poidsKg() : 70.0;
        String genre = fiche.genre() != null ? fiche.genre() : "M";
        String objectifCode = fiche.objectif() != null ? fiche.objectif().toLowerCase(Locale.ROOT) : "maintien";
        String niveau = fiche.niveauActivite();

        int calories = MifflinStJeor.dailyCaloriesTarget(genre, poidsKg, tailleCm, age, niveau, objectifCode);
        String objectifPrincipal = MifflinStJeor.objectifPrincipalLabel(objectifCode);
        WeeklyExercisePlan exercices = WeeklyPlanBuilder.build(objectifCode);
        PlansRepas repas = MealPlanBuilder.build(calories, objectifCode);
        String conseils = AdviceBuilder.build(objectifCode, poidsKg, tailleCm, age);

        return new ProgrammeGenere(0, userId, calories, objectifPrincipal, exercices, repas, conseils);
    }

    /**
     * Charge la fiche santé, génère le programme, supprime les anciens programmes de l’utilisateur et insère la nouvelle ligne.
     */
    public ProgrammeGenere regenerateProgramForUser(int userId) throws SQLException {
        FicheSanteRow fiche = ficheDao.findByUserId(userId)
                .orElseThrow(() -> new SQLException("Aucune fiche santé pour ce compte."));
        if (fiche.userId() != userId) {
            throw new SQLException("Incohérence fiche / utilisateur");
        }
        ProgrammeGenere draft = generateFromFiche(fiche);
        return programmeRepository.replaceForUser(draft);
    }

    public FicheSanteDao getFicheDao() {
        return ficheDao;
    }

    public ProgrammeGenereRepository getProgrammeRepository() {
        return programmeRepository;
    }

    /** Formule Mifflin–St Jeor + facteurs d’activité et d’objectif (déficit / surplus / maintien). */
    static final class MifflinStJeor {

        private MifflinStJeor() {
        }

        static int dailyCaloriesTarget(String genre, double poidsKg, int tailleCm, int age,
                                       String niveauActivite, String objectifCode) {
            boolean homme = genre != null && genre.trim().equalsIgnoreCase("M");
            double bmr = homme
                    ? 10 * poidsKg + 6.25 * tailleCm - 5 * age + 5
                    : 10 * poidsKg + 6.25 * tailleCm - 5 * age - 161;
            double tdee = bmr * activityFactor(niveauActivite);
            double adjusted = tdee * objectiveMultiplier(objectifCode);
            int kcal = (int) Math.round(adjusted);
            return Math.max(1200, Math.min(4500, kcal));
        }

        static double activityFactor(String niveau) {
            if (niveau == null || niveau.isBlank()) {
                return 1.375;
            }
            return switch (niveau.toLowerCase(Locale.ROOT)) {
                case "sedentaire", "sédentaire" -> 1.2;
                case "peu_actif" -> 1.375;
                case "moderement_actif", "modere", "modéré", "modérément_actif", "actif" -> 1.55;
                case "tres_actif", "très_actif" -> 1.725;
                default -> 1.375;
            };
        }

        /**
         * Ajustement type Symfony : déficit modéré en perte, surplus maîtrisé en prise de masse.
         */
        static double objectiveMultiplier(String objectif) {
            if (objectif == null) {
                return 1.0;
            }
            return switch (objectif.toLowerCase(Locale.ROOT)) {
                case "perte_poids" -> 0.80;
                case "gain_poids" -> 1.15;
                case "devenir_muscle", "prise_muscle" -> 1.12;
                case "maintien" -> 1.0;
                default -> 1.0;
            };
        }

        static String objectifPrincipalLabel(String code) {
            if (code == null) {
                return "Programme personnalisé OXYN";
            }
            return switch (code.toLowerCase(Locale.ROOT)) {
                case "perte_poids" -> "Perte de poids progressive et durable";
                case "gain_poids" -> "Prise de masse maigre / rééquilibrage énergétique";
                case "devenir_muscle", "prise_muscle" -> "Développement musculaire et force";
                case "maintien" -> "Maintien de la composition et de l’énergie au quotidien";
                default -> "Programme personnalisé OXYN";
            };
        }
    }

    static final class WeeklyPlanBuilder {

        private WeeklyPlanBuilder() {
        }

        static WeeklyExercisePlan build(String objectifCode) {
            LinkedHashMap<String, List<ExerciseItem>> map = new LinkedHashMap<>();
            String[] jours = WeeklyExercisePlan.dayKeys();
            List<List<ExerciseItem>> modeles = List.of(
                    jourCardioEtire(objectifCode),
                    jourCardioRenfo(objectifCode),
                    jourReposActif(),
                    jourHautCorps(objectifCode),
                    jourYoga(),
                    jourLibre(objectifCode),
                    jourRepos()
            );
            for (int i = 0; i < jours.length; i++) {
                map.put(jours[i], modeles.get(i));
            }
            return WeeklyExercisePlan.fromOrderedMap(map);
        }

        private static List<ExerciseItem> jourCardioEtire(String obj) {
            List<ExerciseItem> list = new ArrayList<>();
            int bonus = "perte_poids".equals(obj) ? 10 : 0;
            list.add(ExerciseItem.withDuree("Marche rapide ou vélo doux", "35 minutes", "Modérée", 150 + bonus));
            list.add(ExerciseItem.withDuree("Étirements dynamiques", "12 minutes", "Légère", 25));
            return list;
        }

        private static List<ExerciseItem> jourCardioRenfo(String obj) {
            List<ExerciseItem> list = new ArrayList<>();
            list.add(ExerciseItem.withDuree("Cardio (course légère, rameur ou vélo)", "28 minutes", "Modérée à soutenue", 220));
            if ("devenir_muscle".equals(obj) || "prise_muscle".equals(obj) || "gain_poids".equals(obj)) {
                list.add(ExerciseItem.withRepetitions("Squats / fentes", "4 séries × 10", "Modérée à lourde", 95));
            } else {
                list.add(ExerciseItem.withRepetitions("Squats", "3 séries × 12", "Modérée", 80));
            }
            return list;
        }

        private static List<ExerciseItem> jourReposActif() {
            return List.of(ExerciseItem.withDuree("Marche légère ou natation tranquille", "25 minutes", "Légère", 85));
        }

        private static List<ExerciseItem> jourHautCorps(String obj) {
            List<ExerciseItem> list = new ArrayList<>();
            list.add(ExerciseItem.withRepetitions("Pompes (ou inclinées)", "3 séries × 10–12", "Modérée", 65));
            list.add(ExerciseItem.withDuree("Planche", "3 × 40 secondes", "Modérée", 45));
            if ("devenir_muscle".equals(obj) || "prise_muscle".equals(obj)) {
                list.add(ExerciseItem.withRepetitions("Tractions assistées ou rowing haltère", "3 séries × 8", "Élevée", 90));
            } else {
                list.add(ExerciseItem.withRepetitions("Burpees modérés", "3 séries × 8", "Élevée", 100));
            }
            return list;
        }

        private static List<ExerciseItem> jourYoga() {
            return List.of(ExerciseItem.withDuree("Yoga, Pilates ou mobilité", "32 minutes", "Modérée", 125));
        }

        private static List<ExerciseItem> jourLibre(String obj) {
            String duree = "perte_poids".equals(obj) ? "50 minutes" : "45 minutes";
            int cal = "perte_poids".equals(obj) ? 320 : 300;
            return List.of(ExerciseItem.withDuree(
                    "Activité libre (natation, vélo, randonnée, sport collectif)", duree, "Modérée à élevée", cal));
        }

        private static List<ExerciseItem> jourRepos() {
            return List.of(ExerciseItem.withDuree("Repos actif — marche digestive optionnelle", "20 minutes", "Très légère", 55));
        }
    }

    static final class MealPlanBuilder {

        private MealPlanBuilder() {
        }

        static PlansRepas build(int totalJour, String objectifCode) {
            double p = 0.25, d = 0.35, c = 0.10, di = 0.30;
            if ("gain_poids".equals(objectifCode) || "devenir_muscle".equals(objectifCode) || "prise_muscle".equals(objectifCode)) {
                p = 0.24;
                d = 0.33;
                c = 0.12;
                di = 0.31;
            }
            int petit = Math.round((float) (totalJour * p));
            int dej = Math.round((float) (totalJour * d));
            int coll = Math.round((float) (totalJour * c));
            int din = Math.max(0, totalJour - petit - dej - coll);

            MealBlock petitDej = new MealBlock(petit, List.of(
                    new MealExample("Bol flocons d’avoine complet", petit, List.of(
                            "50 g flocons d’avoine", "200 ml lait ou boisson végétale", "1 fruit", "1 c.à.s. graines")),
                    new MealExample("Yaourt grec & fruits rouges", petit, List.of(
                            "150 g yaourt grec", "Fruits rouges", "Quelques amandes", "1 c.à.c. miel"))
            ));
            MealBlock dejeuner = new MealBlock(dej, List.of(
                    new MealExample("Assiette protéinée équilibrée", dej, List.of(
                            "120–150 g protéine maigre", "150 g féculent complet", "200 g légumes", "1 c.à.s. huile d’olive")),
                    new MealExample("Bowl végétarien protéiné", dej, List.of(
                            "Pois chiches / lentilles", "Quinoa ou riz complet", "Légumes grillés", "Sauce yaourt aux herbes"))
            ));
            MealBlock collation = new MealBlock(coll, List.of(
                    new MealExample("Collation protéinée", coll, List.of("Fruit + poignée d’oléagineux")),
                    new MealExample("Smoothie protéiné", coll, List.of("Banane", "Lait / boisson végétale", "Protéine en poudre"))
            ));
            MealBlock diner = new MealBlock(din, List.of(
                    new MealExample("Repas du soir léger et complet", din, List.of(
                            "100–130 g protéine", "Légumes vapeur ou salade", "Lipides de qualité (huile, avocat)")),
                    new MealExample("Soupe-repas maison", din, List.of(
                            "Légumes variés", "Protéine (poulet, tofu)", "1 petite portion de féculent"))
            ));
            return new PlansRepas(petitDej, dejeuner, collation, diner);
        }
    }

    static final class AdviceBuilder {

        private AdviceBuilder() {
        }

        static String build(String objectif, double poidsKg, int tailleCm, int age) {
            double imc = tailleCm > 0 ? poidsKg / Math.pow(tailleCm / 100.0, 2) : 0;
            StringBuilder sb = new StringBuilder();
            sb.append("💧 Hydratation : visez au moins 2 L par jour, davantage les jours d’entraînement.\n\n");
            sb.append("🏃 Régularité : mieux vaut plusieurs séances modérées qu’une seule séance extrême.\n\n");

            if ("perte_poids".equalsIgnoreCase(objectif == null ? "" : objectif)) {
                sb.append("📉 Perte de poids : privilégiez un déficit progressif, des protéines à chaque repas et beaucoup de légumes.\n\n");
            } else if ("gain_poids".equalsIgnoreCase(objectif == null ? "" : objectif)) {
                sb.append("📈 Prise de poids : ajoutez des collations saines et des féculents complets pour soutenir l’effort.\n\n");
            } else if ("devenir_muscle".equalsIgnoreCase(objectif == null ? "" : objectif)
                    || "prise_muscle".equalsIgnoreCase(objectif == null ? "" : objectif)) {
                sb.append("💪 Musculation : progressez sur les charges / le nombre de répétitions et dormez suffisamment pour récupérer.\n\n");
            } else {
                sb.append("⚖️ Maintien : alternez des journées plus actives et des séances de récupération pour rester stable.\n\n");
            }

            if (imc > 0 && imc < 18.5) {
                sb.append("⚠️ IMC bas : un suivi médical peut aider à fixer des objectifs de poids sûrs.\n\n");
            } else if (imc >= 30) {
                sb.append("⚠️ IMC élevé : associer ce programme à un accompagnement professionnel est particulièrement pertinent.\n\n");
            }

            sb.append("😴 Sommeil : 7 à 9 h pour la récupération hormonale et la gestion de l’appétit.\n\n");
            sb.append("📝 Suivi : notez poids, tour de taille et sensation d’énergie une fois par semaine.\n");
            return sb.toString();
        }
    }
}
