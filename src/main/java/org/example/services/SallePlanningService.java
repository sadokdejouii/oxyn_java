package org.example.services;

import org.example.entities.Seance;
import org.example.entities.Salle;
import org.example.repository.SeanceRepository;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour la gestion du planning des salles de sport
 * Fournit les méthodes pour récupérer et manipuler les séances d'une salle
 */
public class SallePlanningService {

    private final SeanceRepository seanceRepository;

    public SallePlanningService() {
        this.seanceRepository = new SeanceRepository();
    }

    /**
     * Récupère toutes les séances d'une salle pour la semaine en cours
     * @param salleId L'ID de la salle
     * @return Liste des séances de la semaine
     */
    public List<Seance> getSeancesSemaineSalle(int salleId) {
        try {
            // Calculer les dates de début et fin de semaine
            LocalDateTime debutSemaine = LocalDateTime.now()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            LocalDateTime finSemaine = debutSemaine.plusDays(6)
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(59)
                    .withNano(999999999);

            // Récupérer les séances dans cet intervalle pour cette salle
            return seanceRepository.findBySalleIdAndDateRange(salleId, debutSemaine, finSemaine);

        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des séances de la semaine: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Récupère toutes les séances d'une salle (sans filtrage de date)
     * @param salleId L'ID de la salle
     * @return Liste de toutes les séances de la salle
     */
    public List<Seance> getAllSeancesSalle(int salleId) {
        try {
            return seanceRepository.findBySalleId(salleId);
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des séances: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Ajoute une nouvelle séance à une salle
     * @param seance La séance à ajouter
     * @return true si l'ajout a réussi, false sinon
     */
    public boolean ajouterSeance(Seance seance) {
        try {
            if (seance == null || seance.getSalleId() <= 0) {
                System.err.println("Séance invalide ou ID salle non spécifié");
                return false;
            }

            // Validation des données
            if (!validerSeance(seance)) {
                return false;
            }

            return seanceRepository.save(seance);

        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout de la séance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Modifie une séance existante
     * @param seance La séance modifiée
     * @return true si la modification a réussi, false sinon
     */
    public boolean modifierSeance(Seance seance) {
        try {
            if (seance == null || seance.getId() <= 0) {
                System.err.println("Séance invalide ou ID non spécifié");
                return false;
            }

            // Validation des données
            if (!validerSeance(seance)) {
                return false;
            }

            return seanceRepository.update(seance);

        } catch (Exception e) {
            System.err.println("Erreur lors de la modification de la séance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Supprime une séance
     * @param seanceId L'ID de la séance à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public boolean supprimerSeance(int seanceId) {
        try {
            if (seanceId <= 0) {
                System.err.println("ID de séance invalide");
                return false;
            }

            return seanceRepository.delete(seanceId);

        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression de la séance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Vérifie si une séance est disponible (pas de conflit d'horaire)
     * @param seance La séance à vérifier
     * @param salleId L'ID de la salle
     * @return true si disponible, false si conflit
     */
    public boolean verifierDisponibilite(Seance seance, int salleId) {
        try {
            // Récupérer toutes les séances de la salle le même jour
            List<Seance> seancesJour = seanceRepository.findBySalleIdAndDate(salleId, seance.getDate());

            for (Seance autreSeance : seancesJour) {
                // Ignorer la séance elle-même en cas de modification
                if (seance.getId() > 0 && autreSeance.getId() == seance.getId()) {
                    continue;
                }

                // Vérifier le conflit d'horaire
                if (conflitHoraire(seance, autreSeance)) {
                    return false; // Conflit détecté
                }
            }

            return true; // Pas de conflit

        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification de disponibilité: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Exporte le planning d'une salle au format JSON
     * @param salleId L'ID de la salle
     * @return Chaîne JSON du planning
     */
    public String exporterPlanningJSON(int salleId) {
        try {
            List<Seance> seances = getSeancesSemaineSalle(salleId);
            
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"salleId\":").append(salleId).append(",");
            json.append("\"seances\":[");
            
            for (int i = 0; i < seances.size(); i++) {
                if (i > 0) json.append(",");
                Seance s = seances.get(i);
                json.append("{");
                json.append("\"id\":").append(s.getId()).append(",");
                json.append("\"nom\":\"").append(s.getNom()).append("\",");
                json.append("\"date\":\"").append(s.getDate()).append("\",");
                json.append("\"heureDebut\":\"").append(s.getHeureDebut()).append("\",");
                json.append("\"heureFin\":\"").append(s.getHeureFin()).append("\",");
                json.append("\"coach\":\"").append(s.getCoach() != null ? s.getCoach() : "").append("\",");
                json.append("\"maxParticipants\":").append(s.getMaxParticipants()).append(",");
                json.append("\"couleur\":\"").append(s.getCouleur() != null ? s.getCouleur() : "#64748b").append("\"");
                json.append("}");
            }
            
            json.append("]}");
            return json.toString();

        } catch (Exception e) {
            System.err.println("Erreur lors de l'export JSON: " + e.getMessage());
            e.printStackTrace();
            return "{\"error\":\"Erreur lors de l'export\"}";
        }
    }

    /**
     * Valide les données d'une séance
     * @param seance La séance à valider
     * @return true si valide, false sinon
     */
    private boolean validerSeance(Seance seance) {
        if (seance.getNom() == null || seance.getNom().trim().isEmpty()) {
            System.err.println("Le nom de la séance est obligatoire");
            return false;
        }

        if (seance.getDate() == null) {
            System.err.println("La date de la séance est obligatoire");
            return false;
        }

        if (seance.getHeureDebut() == null) {
            System.err.println("L'heure de début est obligatoire");
            return false;
        }

        if (seance.getHeureFin() == null) {
            System.err.println("L'heure de fin est obligatoire");
            return false;
        }

        if (seance.getHeureDebut().isAfter(seance.getHeureFin())) {
            System.err.println("L'heure de début doit être avant l'heure de fin");
            return false;
        }

        if (seance.getMaxParticipants() < 0) {
            System.err.println("Le nombre de participants ne peut pas être négatif");
            return false;
        }

        return true;
    }

    /**
     * Vérifie s'il y a un conflit d'horaire entre deux séances
     * @param seance1 Première séance
     * @param seance2 Deuxième séance
     * @return true si conflit, false sinon
     */
    private boolean conflitHoraire(Seance seance1, Seance seance2) {
        // Vérifier si les séances sont le même jour
        if (!seance1.getDate().toLocalDate().equals(seance2.getDate().toLocalDate())) {
            return false;
        }

        // Vérifier le chevauchement des horaires
        LocalDateTime debut1 = seance1.getDate().withHour(seance1.getHeureDebut().getHour())
                                               .withMinute(seance1.getHeureDebut().getMinute());
        LocalDateTime fin1 = seance1.getDate().withHour(seance1.getHeureFin().getHour())
                                             .withMinute(seance1.getHeureFin().getMinute());

        LocalDateTime debut2 = seance2.getDate().withHour(seance2.getHeureDebut().getHour())
                                               .withMinute(seance2.getHeureDebut().getMinute());
        LocalDateTime fin2 = seance2.getDate().withHour(seance2.getHeureFin().getHour())
                                             .withMinute(seance2.getHeureFin().getMinute());

        // Conflit si les intervalles se chevauchent
        return debut1.isBefore(fin2) && debut2.isBefore(fin1);
    }

    /**
     * Génère des couleurs par défaut pour les types de séances
     * @param nomSeance Nom de la séance
     * @return Couleur hexadécimale
     */
    public String getCouleurParDefaut(String nomSeance) {
        if (nomSeance == null) return "#64748b";
        
        String lower = nomSeance.toLowerCase();
        if (lower.contains("yoga") || lower.contains("pilates")) return "#10b981";
        if (lower.contains("box") || lower.contains("combat")) return "#ef4444";
        if (lower.contains("cardio") || lower.contains("hiit")) return "#f59e0b";
        if (lower.contains("muscu") || lower.contains("musculation")) return "#8b5cf6";
        if (lower.contains("danse") || lower.contains("zumba")) return "#ec4899";
        if (lower.contains("fitness") || lower.contains("aérobic")) return "#06b6d4";
        
        return "#64748b"; // Gris par défaut
    }
}
