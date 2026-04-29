package org.example.services;

import org.example.entities.Seance;
import org.example.repository.SeanceRepository;

import java.util.List;

/**
 * Service pour la gestion des séances
 * Fait le pont entre les contrôleurs et le repository
 */
public class SeanceService {

    private final SeanceRepository seanceRepository;

    public SeanceService() {
        this.seanceRepository = new SeanceRepository();
    }

    /**
     * Récupère toutes les séances d'une salle
     * @param salleId L'ID de la salle
     * @return Liste des séances
     */
    public List<Seance> getSeancesBySalleId(int salleId) {
        try {
            return seanceRepository.findBySalleId(salleId);
        } catch (Exception e) {
            System.err.println("Erreur dans SeanceService.getSeancesBySalleId: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Ajoute une nouvelle séance
     * @param seance La séance à ajouter
     * @return true si succès
     */
    public boolean ajouterSeance(Seance seance) {
        try {
            return seanceRepository.save(seance);
        } catch (Exception e) {
            System.err.println("Erreur dans SeanceService.ajouterSeance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Modifie une séance existante
     * @param seance La séance modifiée
     * @return true si succès
     */
    public boolean modifierSeance(Seance seance) {
        try {
            return seanceRepository.update(seance);
        } catch (Exception e) {
            System.err.println("Erreur dans SeanceService.modifierSeance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Supprime une séance
     * @param seanceId L'ID de la séance
     * @return true si succès
     */
    public boolean supprimerSeance(int seanceId) {
        try {
            return seanceRepository.delete(seanceId);
        } catch (Exception e) {
            System.err.println("Erreur dans SeanceService.supprimerSeance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Récupère une séance par son ID
     * @param seanceId L'ID de la séance
     * @return La séance ou null
     */
    public Seance getSeanceById(int seanceId) {
        try {
            return seanceRepository.findById(seanceId);
        } catch (Exception e) {
            System.err.println("Erreur dans SeanceService.getSeanceById: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Initialise la table seances si nécessaire
     */
    public void initializeDatabase() {
        try {
            seanceRepository.createTableIfNotExists();
        } catch (Exception e) {
            System.err.println("Erreur dans SeanceService.initializeDatabase: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
