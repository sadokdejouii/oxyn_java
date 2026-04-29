package org.example.services;

import org.example.entities.Offre;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour la gestion des offres d'abonnement
 * Pattern Service + DAO avec try-with-resources
 */
public class OffreService {
    
    /**
     * Ajoute une nouvelle offre dans la base de données
     */
    public void ajouter(Offre offre) throws SQLException {
        String sql = "INSERT INTO offres (nom, prix, salle_id, description, active, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, true, NOW(), NOW())";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, offre.getNom());
            ps.setDouble(2, offre.getPrix());
            ps.setInt(3, offre.getSalleId());
            ps.setString(4, offre.getDescription() != null ? offre.getDescription() : "");
            
            int result = ps.executeUpdate();
            
            if (result > 0) {
                // Récupérer l'ID généré
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        offre.setId(rs.getInt(1));
                    }
                }
                System.out.println("✅ Offre ajoutée : " + offre.getNom() + " (ID: " + offre.getId() + ")");
            } else {
                throw new SQLException("Échec de l'ajout de l'offre");
            }
        }
    }
    
    /**
     * Modifie une offre existante
     */
    public void modifier(Offre offre) throws SQLException {
        String sql = "UPDATE offres SET nom = ?, prix = ?, salle_id = ?, description = ?, updated_at = NOW() " +
                    "WHERE id = ?";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, offre.getNom());
            ps.setDouble(2, offre.getPrix());
            ps.setInt(3, offre.getSalleId());
            ps.setString(4, offre.getDescription() != null ? offre.getDescription() : "");
            ps.setInt(5, offre.getId());
            
            int result = ps.executeUpdate();
            
            if (result > 0) {
                System.out.println("✅ Offre modifiée : " + offre.getNom() + " (ID: " + offre.getId() + ")");
            } else {
                throw new SQLException("Aucune offre trouvée avec l'ID : " + offre.getId());
            }
        }
    }
    
    /**
     * Supprime une offre par son ID
     */
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM offres WHERE id = ?";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, id);
            
            int result = ps.executeUpdate();
            
            if (result > 0) {
                System.out.println("✅ Offre supprimée (ID: " + id + ")");
            } else {
                throw new SQLException("Aucune offre trouvée avec l'ID : " + id);
            }
        }
    }
    
    /**
     * Récupère toutes les offres
     */
    public List<Offre> getAllOffres() throws SQLException {
        List<Offre> offres = new ArrayList<>();
        String sql = "SELECT * FROM offres ORDER BY created_at DESC";
        
        try (Connection conn = MyDataBase.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Offre offre = mapResultSetToOffre(rs);
                offres.add(offre);
            }
        }
        
        return offres;
    }
    
    /**
     * Récupère les offres d'une salle spécifique
     */
    public List<Offre> getOffresBySalleId(int salleId) throws SQLException {
        List<Offre> offres = new ArrayList<>();
        String sql = "SELECT * FROM offres WHERE salle_id = ? AND active = true ORDER BY created_at DESC";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, salleId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Offre offre = mapResultSetToOffre(rs);
                    offres.add(offre);
                }
            }
        }
        
        return offres;
    }
    
    /**
     * Récupère une offre par son ID
     */
    public Offre getOffreById(int id) throws SQLException {
        String sql = "SELECT * FROM offres WHERE id = ?";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToOffre(rs);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Active ou désactive une offre
     */
    public void setOffreActive(int id, boolean active) throws SQLException {
        String sql = "UPDATE offres SET active = ?, updated_at = NOW() WHERE id = ?";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            
            int result = ps.executeUpdate();
            
            if (result > 0) {
                System.out.println("✅ Offre " + (active ? "activée" : "désactivée") + " (ID: " + id + ")");
            } else {
                throw new SQLException("Aucune offre trouvée avec l'ID : " + id);
            }
        }
    }
    
    /**
     * Vérifie si une offre existe
     */
    public boolean offreExists(int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM offres WHERE id = ?";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    
    /**
     * Compte le nombre d'offres pour une salle
     */
    public int countOffresBySalle(int salleId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM offres WHERE salle_id = ? AND active = true";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, salleId);
            
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
    
    /**
     * Recherche des offres par nom
     */
    public List<Offre> rechercherOffres(String terme, int salleId) throws SQLException {
        List<Offre> offres = new ArrayList<>();
        String sql = "SELECT * FROM offres WHERE salle_id = ? AND active = true AND " +
                    "(nom LIKE ? OR description LIKE ?) ORDER BY nom";
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, salleId);
            ps.setString(2, "%" + terme + "%");
            ps.setString(3, "%" + terme + "%");
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Offre offre = mapResultSetToOffre(rs);
                    offres.add(offre);
                }
            }
        }
        
        return offres;
    }
    
    /**
     * Mappe un ResultSet vers un objet Offre
     */
    private Offre mapResultSetToOffre(ResultSet rs) throws SQLException {
        Offre offre = new Offre();
        offre.setId(rs.getInt("id"));
        offre.setNom(rs.getString("nom"));
        offre.setPrix(rs.getDouble("prix"));
        offre.setSalleId(rs.getInt("salle_id"));
        offre.setDescription(rs.getString("description"));
        offre.setActive(rs.getBoolean("active"));
        offre.setCreatedAt(rs.getTimestamp("created_at"));
        offre.setUpdatedAt(rs.getTimestamp("updated_at"));
        return offre;
    }
    
    /**
     * Validation des données avant insertion/modification
     */
    public boolean validerOffre(Offre offre) {
        if (offre == null) return false;
        
        // Validation du nom
        if (offre.getNom() == null || offre.getNom().trim().isEmpty()) {
            System.err.println("❌ Le nom de l'offre est obligatoire");
            return false;
        }
        
        if (offre.getNom().trim().length() < 2) {
            System.err.println("❌ Le nom doit contenir au moins 2 caractères");
            return false;
        }
        
        // Validation du prix
        if (offre.getPrix() <= 0) {
            System.err.println("❌ Le prix doit être supérieur à 0");
            return false;
        }
        
        if (offre.getPrix() > 9999) {
            System.err.println("❌ Le prix ne peut pas dépasser 9999 TND");
            return false;
        }
        
        // Validation de la salle
        if (offre.getSalleId() <= 0) {
            System.err.println("❌ L'ID de la salle est invalide");
            return false;
        }
        
        return true;
    }
}
