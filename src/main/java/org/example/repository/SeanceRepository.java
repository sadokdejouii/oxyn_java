package org.example.repository;

import org.example.entities.Seance;
import org.example.utils.MyDataBase;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository pour la gestion des séances en base de données
 */
public class SeanceRepository {

    private final Connection connection;

    public SeanceRepository() {
        this.connection = MyDataBase.getConnection();
    }

    /**
     * Récupère toutes les séances d'une salle
     * @param salleId L'ID de la salle
     * @return Liste des séances
     */
    public List<Seance> findBySalleId(int salleId) {
        List<Seance> seances = new ArrayList<>();
        String query = "SELECT * FROM seances WHERE salle_id = ? ORDER BY date, heure_debut";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, salleId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                seances.add(mapResultSetToSeance(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des séances de la salle: " + e.getMessage());
            e.printStackTrace();
        }

        return seances;
    }

    /**
     * Récupère les séances d'une salle dans un intervalle de dates
     * @param salleId L'ID de la salle
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Liste des séances dans l'intervalle
     */
    public List<Seance> findBySalleIdAndDateRange(int salleId, LocalDateTime dateDebut, LocalDateTime dateFin) {
        List<Seance> seances = new ArrayList<>();
        String query = "SELECT * FROM seances WHERE salle_id = ? AND date >= ? AND date <= ? ORDER BY date, heure_debut";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, salleId);
            stmt.setTimestamp(2, Timestamp.valueOf(dateDebut));
            stmt.setTimestamp(3, Timestamp.valueOf(dateFin));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                seances.add(mapResultSetToSeance(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des séances par intervalle: " + e.getMessage());
            e.printStackTrace();
        }

        return seances;
    }

    /**
     * Récupère les séances d'une salle pour une date spécifique
     * @param salleId L'ID de la salle
     * @param date La date
     * @return Liste des séances pour cette date
     */
    public List<Seance> findBySalleIdAndDate(int salleId, LocalDateTime date) {
        List<Seance> seances = new ArrayList<>();
        String query = "SELECT * FROM seances WHERE salle_id = ? AND DATE(date) = DATE(?) ORDER BY heure_debut";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, salleId);
            stmt.setTimestamp(2, Timestamp.valueOf(date));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                seances.add(mapResultSetToSeance(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des séances par date: " + e.getMessage());
            e.printStackTrace();
        }

        return seances;
    }

    /**
     * Sauvegarde une nouvelle séance
     * @param seance La séance à sauvegarder
     * @return true si succès, false sinon
     */
    public boolean save(Seance seance) {
        String query = "INSERT INTO seances (salle_id, nom, date, heure_debut, heure_fin, coach, max_participants, couleur) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, seance.getSalleId());
            stmt.setString(2, seance.getNom());
            stmt.setTimestamp(3, Timestamp.valueOf(seance.getDate()));
            stmt.setTime(4, Time.valueOf(seance.getHeureDebut().toLocalTime()));
            stmt.setTime(5, Time.valueOf(seance.getHeureFin().toLocalTime()));
            stmt.setString(6, seance.getCoach());
            stmt.setInt(7, seance.getMaxParticipants());
            stmt.setString(8, seance.getCouleur());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    seance.setId(generatedKeys.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la sauvegarde de la séance: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Met à jour une séance existante
     * @param seance La séance à mettre à jour
     * @return true si succès, false sinon
     */
    public boolean update(Seance seance) {
        String query = "UPDATE seances SET salle_id = ?, nom = ?, date = ?, heure_debut = ?, heure_fin = ?, coach = ?, max_participants = ?, couleur = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, seance.getSalleId());
            stmt.setString(2, seance.getNom());
            stmt.setTimestamp(3, Timestamp.valueOf(seance.getDate()));
            stmt.setTime(4, Time.valueOf(seance.getHeureDebut().toLocalTime()));
            stmt.setTime(5, Time.valueOf(seance.getHeureFin().toLocalTime()));
            stmt.setString(6, seance.getCoach());
            stmt.setInt(7, seance.getMaxParticipants());
            stmt.setString(8, seance.getCouleur());
            stmt.setInt(9, seance.getId());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de la séance: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Supprime une séance
     * @param seanceId L'ID de la séance à supprimer
     * @return true si succès, false sinon
     */
    public boolean delete(int seanceId) {
        String query = "DELETE FROM seances WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, seanceId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de la séance: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Récupère une séance par son ID
     * @param seanceId L'ID de la séance
     * @return La séance ou null si non trouvée
     */
    public Seance findById(int seanceId) {
        String query = "SELECT * FROM seances WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, seanceId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToSeance(rs);
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de la séance: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Compte le nombre de séances pour une salle
     * @param salleId L'ID de la salle
     * @return Nombre de séances
     */
    public int countBySalleId(int salleId) {
        String query = "SELECT COUNT(*) FROM seances WHERE salle_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, salleId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors du comptage des séances: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Convertit un ResultSet en objet Seance
     * @param rs Le ResultSet
     * @return L'objet Seance
     * @throws SQLException En cas d'erreur SQL
     */
    private Seance mapResultSetToSeance(ResultSet rs) throws SQLException {
        Seance seance = new Seance();
        seance.setId(rs.getInt("id"));
        seance.setSalleId(rs.getInt("salle_id"));
        seance.setNom(rs.getString("nom"));
        
        Timestamp dateTimestamp = rs.getTimestamp("date");
        if (dateTimestamp != null) {
            seance.setDate(dateTimestamp.toLocalDateTime());
        }
        
        Time heureDebutTime = rs.getTime("heure_debut");
        if (heureDebutTime != null) {
            seance.setHeureDebut(LocalDateTime.of(dateTimestamp.toLocalDateTime().toLocalDate(), 
                                                  heureDebutTime.toLocalTime()));
        }
        
        Time heureFinTime = rs.getTime("heure_fin");
        if (heureFinTime != null) {
            seance.setHeureFin(LocalDateTime.of(dateTimestamp.toLocalDateTime().toLocalDate(), 
                                                heureFinTime.toLocalTime()));
        }
        
        seance.setCoach(rs.getString("coach"));
        seance.setMaxParticipants(rs.getInt("max_participants"));
        seance.setCouleur(rs.getString("couleur"));
        
        return seance;
    }

    /**
     * Crée la table seances si elle n'existe pas
     */
    public void createTableIfNotExists() {
        String query = """
            CREATE TABLE IF NOT EXISTS seances (
                id INT AUTO_INCREMENT PRIMARY KEY,
                salle_id INT NOT NULL,
                nom VARCHAR(100) NOT NULL,
                date DATETIME NOT NULL,
                heure_debut TIME NOT NULL,
                heure_fin TIME NOT NULL,
                coach VARCHAR(100),
                max_participants INT DEFAULT 0,
                couleur VARCHAR(7) DEFAULT '#64748b',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (salle_id) REFERENCES salles(id) ON DELETE CASCADE,
                INDEX idx_salle_date (salle_id, date),
                INDEX idx_date (date)
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(query);
            System.out.println("✅ Table 'seances' vérifiée/créée avec succès");

        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de la table seances: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
