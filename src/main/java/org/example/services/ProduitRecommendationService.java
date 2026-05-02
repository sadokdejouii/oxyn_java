package org.example.services;

import org.example.entities.produits;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Recommandation locale de produits.
 * - Priorise les achats passés du client.
 * - Ajoute un boost de similarité textuelle (nom/description).
 * - Peut intégrer la recherche courante pour mieux adapter la proposition.
 */
public final class ProduitRecommendationService {

    private final ProduitsService produitsService = new ProduitsService();
    private final CommandesService commandesService = new CommandesService();

    public List<produits> recommanderPourClient(int idClient, String rechercheCourante, int limit) throws SQLException {
        List<produits> catalogue = produitsService.afficher();
        if (catalogue.isEmpty() || limit <= 0) {
            return List.of();
        }

        Map<Integer, Integer> achatsParProduit = commandesService.compterAchatsProduitParClient(idClient);
        if (achatsParProduit.isEmpty()) {
            return recommanderParDefaut(catalogue, rechercheCourante, limit);
        }

        Set<String> profilTokens = new HashSet<>();
        for (produits p : catalogue) {
            Integer n = achatsParProduit.get(p.getId_produit());
            if (n != null && n > 0) {
                profilTokens.addAll(tokens(p.getNom_produit()));
                profilTokens.addAll(tokens(p.getDescription_produit()));
            }
        }
        Set<String> queryTokens = tokens(rechercheCourante);

        List<ProduitScore> scores = new ArrayList<>();
        for (produits p : catalogue) {
            if (p.getQuantite_stock_produit() <= 0) {
                continue;
            }
            double score = 0;
            int dejaAchete = achatsParProduit.getOrDefault(p.getId_produit(), 0);
            score += dejaAchete * 50.0;

            Set<String> tokensProduit = new HashSet<>();
            tokensProduit.addAll(tokens(p.getNom_produit()));
            tokensProduit.addAll(tokens(p.getDescription_produit()));
            score += overlap(tokensProduit, profilTokens) * 6.0;
            score += overlap(tokensProduit, queryTokens) * 8.0;

            // Petit bonus pour les produits bien stockés (évite recommander un stock très bas).
            score += Math.min(10, p.getQuantite_stock_produit()) * 0.15;
            scores.add(new ProduitScore(p, score));
        }

        scores.sort(Comparator.comparingDouble(ProduitScore::score).reversed());
        return scores.stream().limit(limit).map(ProduitScore::produit).toList();
    }

    private static List<produits> recommanderParDefaut(List<produits> catalogue, String rechercheCourante, int limit) {
        Set<String> queryTokens = tokens(rechercheCourante);
        List<ProduitScore> scores = new ArrayList<>();
        for (produits p : catalogue) {
            if (p.getQuantite_stock_produit() <= 0) {
                continue;
            }
            Set<String> tokensProduit = new HashSet<>();
            tokensProduit.addAll(tokens(p.getNom_produit()));
            tokensProduit.addAll(tokens(p.getDescription_produit()));
            double score = overlap(tokensProduit, queryTokens) * 8.0;
            score += Math.min(20, p.getQuantite_stock_produit()) * 0.2;
            // Favorise aussi les produits accessibles quand pas d'historique.
            score += p.getPrix_produit() < 80 ? 2 : 0;
            scores.add(new ProduitScore(p, score));
        }
        scores.sort(Comparator.comparingDouble(ProduitScore::score).reversed());
        return scores.stream().limit(limit).map(ProduitScore::produit).toList();
    }

    private static int overlap(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (String token : a) {
            if (b.contains(token)) {
                n++;
            }
        }
        return n;
    }

    private static Set<String> tokens(String texte) {
        if (texte == null || texte.isBlank()) {
            return Set.of();
        }
        String[] brut = texte.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd} ]", " ")
                .trim()
                .split("\\s+");
        Set<String> out = new HashSet<>();
        for (String t : brut) {
            if (t.length() >= 3) {
                out.add(t);
            }
        }
        return out;
    }

    private record ProduitScore(produits produit, double score) {
    }
}
