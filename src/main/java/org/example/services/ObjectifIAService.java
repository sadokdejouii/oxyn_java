package org.example.services;

import org.example.entities.produits;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** IA locale pour la vue Objectif : extraction de mots-clés, réponse ciblée et recommandations boutique. */
public final class ObjectifIAService {

    /** Mots-clés métier demandés par la feature. */
    public enum Keyword {
        KNEE,
        BREATH,
        FATIGUE,
        ENERGY,
        MUSCLE
    }

    /** Extrait les mots-clés stricts (KNEE, BREATH, ENERGY). */
    public List<Keyword> extractKeywords(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String n = normalize(raw);
        LinkedHashSet<Keyword> out = new LinkedHashSet<>();
        if (matchesAny(n, "genou", "genoux", "rotule", "knee", "ligament", "menisque", "douleur genou")) {
            out.add(Keyword.KNEE);
        }
        if (matchesAny(n, "respiration", "respir", "souffle", "essouffl", "cardio", "manque de souffle")) {
            out.add(Keyword.BREATH);
        }
        if (matchesAny(n, "fatigue", "fatigu", "energie", "énergie", "epuis", "manque d energie")) {
            out.add(Keyword.ENERGY);
            out.add(Keyword.FATIGUE);
        }
        if (matchesAny(n, "muscle", "musculation", "prise de masse", "devenir muscle", "devenir musclee",
                "devenir muscle", "mass", "gainer", "creatine", "creatine monohydrate")) {
            out.add(Keyword.MUSCLE);
        }
        return new ArrayList<>(out);
    }

    /** Réponse IA professionnelle, claire et personnalisée selon les mots-clés détectés. */
    public String generateResponse(String userText, List<Keyword> keywords) {
        if (userText == null || userText.isBlank()) {
            return "Veuillez saisir un objectif avant analyse.";
        }
        if (keywords == null || keywords.isEmpty()) {
            return "Votre description est encore trop générale pour proposer une analyse précise. "
                    + "Ajoutez une zone ciblée (genou, respiration, fatigue), le contexte et l’intensité.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Analyse de votre situation\n\n");
        sb.append("Problème détecté : ");
        sb.append(keywords.stream().map(this::label).distinct().reduce((a, b) -> a + ", " + b).orElse("—"));
        sb.append(".\n\n");
        sb.append("Explication professionnelle :\n");
        for (Keyword k : keywords) {
            sb.append("• ").append(explain(k)).append("\n");
        }
        sb.append("\nConseils recommandés :\n");
        for (Keyword k : keywords) {
            sb.append("• ").append(advise(k)).append("\n");
        }
        sb.append("\nCe conseil est fourni par l’assistant local et ne remplace pas un avis médical.");
        return sb.toString().trim();
    }

    /** Recommandation produits strictement sur mots-clés détectés (aucun fallback global). */
    public List<produits> recommendProducts(List<Keyword> keywords, List<produits> catalogue) {
        if (keywords == null || keywords.isEmpty() || catalogue == null || catalogue.isEmpty()) {
            return List.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (Keyword k : keywords) {
            tokens.addAll(tokensFor(k));
        }
        record Scored(produits p, int score) {}
        List<Scored> scored = new ArrayList<>();
        for (produits p : catalogue) {
            int s = scoreProduct(p, tokens);
            if (s > 0) {
                scored.add(new Scored(p, s));
            }
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed());
        List<produits> out = new ArrayList<>();
        for (Scored sc : scored) {
            out.add(sc.p());
            if (out.size() >= 8) {
                break;
            }
        }
        return out;
    }

    /** Compat ancien flux. */
    public String analyzeObjective(String userText, List<Keyword> keywords, List<produits> recommended) {
        return generateResponse(userText, keywords);
    }

    public String keywordsAsText(List<Keyword> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return "";
        }
        return keywords.stream().map(Keyword::name).distinct().reduce((a, b) -> a + "; " + b).orElse("");
    }

    private String label(Keyword k) {
        return switch (k) {
            case KNEE -> "gêne au genou";
            case BREATH -> "difficulté respiratoire à l’effort";
            case FATIGUE, ENERGY -> "fatigue / manque d’énergie";
            case MUSCLE -> "objectif prise de masse musculaire";
        };
    }

    private String explain(Keyword k) {
        return switch (k) {
            case KNEE -> "Vous semblez souffrir d’une gêne au niveau du genou. "
                    + "Ce type de symptôme est souvent lié à une surcharge, une technique inadaptée "
                    + "ou une récupération insuffisante.";
            case BREATH -> "Votre description indique une difficulté respiratoire à l’effort. "
                    + "Cela peut venir d’une intensité trop élevée par rapport à votre base cardio.";
            case FATIGUE, ENERGY -> "Votre description indique un manque d’énergie. "
                    + "Les causes principales sont souvent le sommeil, la récupération et l’équilibre nutritionnel.";
            case MUSCLE -> "Votre objectif correspond à une prise de masse musculaire. "
                    + "Ce résultat dépend d’un entraînement progressif, d’un apport protéino-calorique suffisant "
                    + "et d’une récupération bien structurée.";
        };
    }

    private String advise(Keyword k) {
        return switch (k) {
            case KNEE -> "Réduire temporairement les impacts, renforcer quadriceps/fessiers, "
                    + "et reprendre progressivement la charge.";
            case BREATH -> "Baisser l’intensité pendant 1 à 2 semaines, privilégier un travail d’endurance progressive "
                    + "et de respiration contrôlée.";
            case FATIGUE, ENERGY -> "Prioriser sommeil, hydratation, et distribution des repas. "
                    + "Limiter les séances intenses tant que l’énergie ne remonte pas.";
            case MUSCLE -> "Organiser 3 à 5 séances de résistance par semaine, suivre une surcharge progressive, "
                    + "et maintenir un apport protéique régulier sur la journée.";
        };
    }

    private static List<String> tokensFor(Keyword k) {
        return switch (k) {
            case KNEE -> List.of("genou", "genoux", "knee", "rotule", "protection", "protege", "protège", "articulation", "vitamine");
            case BREATH -> List.of("respiration", "respiratoire", "souffle", "bandelette", "bandlette", "nasale", "oxygene", "oxygène");
            case FATIGUE, ENERGY -> List.of("energie", "énergie", "fatigue", "complement", "complément", "vitamine", "multivitamine", "boost");
            case MUSCLE -> List.of("mass", "gainer", "creatine", "créatine", "muscle", "musculation", "protein", "protéine");
        };
    }

    private static boolean matchesAny(String normalized, String... needles) {
        for (String n : needles) {
            if (normalized.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.toLowerCase(Locale.FRANCE).replace('\n', ' ');
    }

    private static int scoreProduct(produits p, Set<String> tokens) {
        String blob = normalize(safe(p.getNom_produit()) + " " + safe(p.getDescription_produit()));
        int score = 0;
        for (String tok : tokens) {
            if (tok != null && !tok.isBlank() && blob.contains(normalize(tok))) {
                score += 2;
            }
        }
        return score;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
