-- Création de la table 'offres' pour les abonnements
-- Compatible avec le pattern Service + DAO + FXML

-- Suppression de la table si elle existe (pour développement)
DROP TABLE IF EXISTS offres;

-- Création de la table offres
CREATE TABLE offres (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(255) NOT NULL COMMENT 'Nom de l''offre d''abonnement',
    prix DECIMAL(10,2) NOT NULL COMMENT 'Prix de l''offre en TND',
    salle_id INT NOT NULL COMMENT 'ID de la salle associée',
    description TEXT COMMENT 'Description détaillée de l''offre',
    active BOOLEAN DEFAULT TRUE COMMENT 'Statut actif/inactif de l''offre',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Date de dernière modification',
    
    -- Index pour optimisation
    INDEX idx_offres_salle_id (salle_id),
    INDEX idx_offres_active (active),
    INDEX idx_offres_created_at (created_at),
    
    -- Contrainte de clé étrangère vers la table gymnasia
    FOREIGN KEY (salle_id) REFERENCES gymnasia(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Table des offres d''abonnement par salle';

-- Insertion de données de test
INSERT INTO offres (nom, prix, salle_id, description) VALUES
('Abonnement Mensuel Standard', 29.99, 1, 'Accès illimité à la salle pendant 30 jours avec équipements de base'),
('Abonnement Mensuel Premium', 49.99, 1, 'Accès illimité + cours collectifs + coaching personnel 1x/mois'),
('Abonnement Annuel Standard', 299.99, 1, 'Accès illimité pendant 365 jours - économisez 2 mois !'),
('Abonnement Étudiant', 19.99, 1, 'Tarif spécial pour étudiants avec carte d''étudiant valide'),
('Pack Familiale 4 Pers', 99.99, 1, 'Abonnement pour 4 personnes de la même famille'),

('Abonnement Mensuel Standard', 34.99, 2, 'Accès illimité à la salle pendant 30 jours avec équipements de base'),
('Abonnement Mensuel Premium', 59.99, 2, 'Accès illimité + cours collectifs + coaching personnel 2x/mois'),
('Abonnement Annuel Standard', 349.99, 2, 'Accès illimité pendant 365 jours - économisez 2 mois !'),

('Abonnement Mensuel Standard', 39.99, 3, 'Accès illimité à la salle pendant 30 jours avec équipements de base'),
('Abonnement Mensuel Premium', 69.99, 3, 'Accès illimité + cours collectifs + coaching personnalisé illimité'),
('Abonnement Annuel Standard', 399.99, 3, 'Accès illimité pendant 365 jours - économisez 2 mois !');

-- Requêtes de vérification
SELECT 'Table offres créée avec succès' as status;
SELECT COUNT(*) as nombre_offres FROM offres;
SELECT * FROM offres ORDER BY salle_id, prix;

-- Procédure pour vérifier les offres par salle
DELIMITER //
CREATE PROCEDURE GetOffresBySalle(IN p_salle_id INT)
BEGIN
    SELECT 
        id,
        nom,
        prix,
        description,
        active,
        created_at,
        updated_at
    FROM offres 
    WHERE salle_id = p_salle_id AND active = TRUE
    ORDER BY prix ASC;
END //
DELIMITER ;

-- Vue pour les offres actives
CREATE VIEW v_offres_actives AS
SELECT 
    o.id,
    o.nom,
    o.prix,
    o.description,
    o.salle_id,
    g.name as salle_nom,
    o.created_at,
    o.updated_at
FROM offres o
JOIN gymnasia g ON o.salle_id = g.id
WHERE o.active = TRUE
ORDER BY g.name, o.prix;

-- Requêtes de test pour la vue
SELECT * FROM v_offres_actives;

-- Trigger pour journaliser les modifications (optionnel)
DELIMITER //
CREATE TRIGGER offres_before_update 
BEFORE UPDATE ON offres
FOR EACH ROW
BEGIN
    -- Journalisation des modifications de prix importantes
    IF NEW.prix <> OLD.prix AND ABS(NEW.prix - OLD.prix) > 10 THEN
        INSERT INTO log_prix_offres (offre_id, ancien_prix, nouveau_prix, date_modification)
        VALUES (OLD.id, OLD.prix, NEW.prix, NOW());
    END IF;
END //
DELIMITER ;

-- Table de journalisation (optionnelle)
CREATE TABLE IF NOT EXISTS log_prix_offres (
    id INT PRIMARY KEY AUTO_INCREMENT,
    offre_id INT NOT NULL,
    ancien_prix DECIMAL(10,2) NOT NULL,
    nouveau_prix DECIMAL(10,2) NOT NULL,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_offre_id (offre_id),
    FOREIGN KEY (offre_id) REFERENCES offres(id) ON DELETE CASCADE
) COMMENT='Journal des modifications de prix des offres';
