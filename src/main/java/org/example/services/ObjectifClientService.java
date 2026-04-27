package org.example.services;

import org.example.entities.produits;
import org.example.model.planning.objectif.ObjectifClientAdminRow;
import org.example.model.planning.objectif.ObjectifClientRow;
import org.example.repository.planning.ObjectifClientRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Orchestration objectifs libres Planning : persistance, IA locale, recommandations boutique.
 */
public final class ObjectifClientService {

    private final ObjectifClientRepository repo = new ObjectifClientRepository();
    private final ObjectifIAService ia = new ObjectifIAService();
    private final ProduitsService produitsService = new ProduitsService();

    public Optional<ObjectifClientRow> findLatestForUser(int userId) throws SQLException {
        Optional<ObjectifClientRow> opt = repo.findLatestByUserId(userId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        ObjectifClientRow r = opt.get();
        if (r.idsProduitsRecommandes() != null && !r.idsProduitsRecommandes().isBlank()) {
            return opt;
        }
        return Optional.of(enrichWithComputedMeta(r));
    }

    /**
     * Analyse et enregistre un objectif libre dans objectifs_hebdomadaires, puis retourne la ligne + produits recommandés.
     */
    public AnalyseResult analyzeAndSave(int userId, String texteObjectif) throws SQLException {
        if (texteObjectif == null || texteObjectif.isBlank()) {
            throw new SQLException("Veuillez décrire votre objectif ou problème.");
        }
        // Pipeline IA local: extraction -> matching produits -> generation reponse.
        List<ObjectifIAService.Keyword> keywords = ia.extractKeywords(texteObjectif);
        List<produits> catalogue = produitsService.listAvailableForRecommendations();
        List<produits> recommended = ia.recommendProducts(keywords, catalogue);
        String keywordsText = ia.keywordsAsText(keywords);
        String reponse = ia.generateResponse(texteObjectif, keywords);
        String ids = recommended.stream()
                .map(p -> String.valueOf(p.getId_produit()))
                .collect(Collectors.joining(","));
        int newId = repo.insert(userId, texteObjectif.trim(), reponse, keywordsText, ids);
        ObjectifClientRow row = repo.findById(newId).orElseThrow(() -> new SQLException("Lecture objectif après insert impossible."));
        ObjectifClientRow enriched = new ObjectifClientRow(
                row.id(), row.userId(), row.texteObjectif(), row.reponseIa(),
                keywordsText, ids, row.dateEnregistrement(), row.interventionEncadrant());
        try {
            // Evenement interne planning pour rafraichir les ecrans relies (dashboard, hub, etc.).
            org.example.realtime.RealtimePlanningSyncService.getInstance()
                    .notifyObjectifUpdated(userId, newId);
        } catch (Exception ignored) {
        }
        return new AnalyseResult(enriched, recommended);
    }

    public void saveEncadrantInterventionForLatest(int userId, String intervention) throws SQLException {
        if (intervention == null) {
            intervention = "";
        }
        repo.updateInterventionOnLatestForUser(userId, intervention.trim());
        try {
            org.example.realtime.RealtimePlanningSyncService.getInstance()
                    .notifyObjectifUpdated(userId, -1);
        } catch (Exception ignored) {
        }
    }

    public List<ObjectifClientAdminRow> listRecentForAdmin(int limit) throws SQLException {
        List<ObjectifClientAdminRow> raw = repo.listRecentWithUsers(limit);
        List<ObjectifClientAdminRow> out = new ArrayList<>();
        for (ObjectifClientAdminRow r : raw) {
            String ids = r.idsProduitsRecommandes();
            String motsCles = r.motsCles();
            if ((ids == null || ids.isBlank()) || (motsCles == null || motsCles.isBlank())) {
                ObjectifClientRow tmp = new ObjectifClientRow(
                        r.id(), r.userId(), r.texteObjectif(), r.reponseIa(),
                        r.motsCles(), r.idsProduitsRecommandes(), r.dateEnregistrement(), r.interventionEncadrant());
                ObjectifClientRow enriched = enrichWithComputedMeta(tmp);
                ids = enriched.idsProduitsRecommandes();
                motsCles = enriched.motsCles();
            }
            String libelles = resolveProductLabels(ids);
            out.add(new ObjectifClientAdminRow(
                    r.id(),
                    r.userId(),
                    r.userDisplayName(),
                    r.userEmail(),
                    r.texteObjectif(),
                    r.reponseIa(),
                    motsCles,
                    ids,
                    libelles,
                    r.dateEnregistrement(),
                    r.interventionEncadrant()));
        }
        return out;
    }

    private ObjectifClientRow enrichWithComputedMeta(ObjectifClientRow row) throws SQLException {
        // Backfill defensif: recalcule metadonnees si anciennes lignes incomplètes.
        List<ObjectifIAService.Keyword> keywords = ia.extractKeywords(row.texteObjectif());
        String keywordsText = ia.keywordsAsText(keywords);
        List<produits> recommended = ia.recommendProducts(keywords, produitsService.listAvailableForRecommendations());
        String ids = recommended.stream()
                .map(p -> String.valueOf(p.getId_produit()))
                .collect(Collectors.joining(","));
        return new ObjectifClientRow(
                row.id(),
                row.userId(),
                row.texteObjectif(),
                row.reponseIa(),
                keywordsText,
                ids,
                row.dateEnregistrement(),
                row.interventionEncadrant());
    }

    private String resolveProductLabels(String idsCsv) {
        if (idsCsv == null || idsCsv.isBlank()) {
            return "—";
        }
        List<String> names = new ArrayList<>();
        for (String part : idsCsv.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                int id = Integer.parseInt(s);
                Optional<produits> p = produitsService.findById(id);
                p.ifPresent(pr -> names.add(pr.getNom_produit()));
            } catch (NumberFormatException | SQLException ignored) {
                // skip
            }
        }
        if (names.isEmpty()) {
            return "—";
        }
        return String.join(" · ", names);
    }

    public record AnalyseResult(ObjectifClientRow row, List<produits> recommendedProducts) {
    }
}
