-- Création des tables pour le système d'abonnement
-- gym_subscription_offers et gym_subscription_orders

-- Suppression des tables si elles existent (pour développement)
DROP TABLE IF EXISTS gym_subscription_orders;
DROP TABLE IF EXISTS gym_subscription_offers;

-- 1️⃣ Table gym_subscription_offers (les offres disponibles)
CREATE TABLE gym_subscription_offers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    gymnasium_id INT NOT NULL COMMENT 'ID de la salle/gymnase',
    name VARCHAR(255) NOT NULL COMMENT 'Nom de l\'offre d\'abonnement',
    duration_months INT NOT NULL COMMENT 'Durée en mois',
    price DECIMAL(10,2) NOT NULL COMMENT 'Prix en TND',
    description TEXT COMMENT 'Description détaillée de l\'offre',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Statut actif/inactif',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Date de dernière modification',
    
    -- Index pour optimisation
    INDEX idx_gym_offers_gymnasium_id (gymnasium_id),
    INDEX idx_gym_offers_active (is_active),
    INDEX idx_gym_offers_created_at (created_at),
    INDEX idx_gym_offers_price (price),
    
    -- Contrainte de clé étrangère
    FOREIGN KEY (gymnasium_id) REFERENCES gymnasia(id) ON DELETE CASCADE,
    
    -- Contraintes de validation
    CONSTRAINT chk_gym_offers_duration_positive CHECK (duration_months > 0),
    CONSTRAINT chk_gym_offers_price_positive CHECK (price > 0),
    CONSTRAINT chk_gym_offers_duration_max CHECK (duration_months <= 120),
    CONSTRAINT chk_gym_offers_price_max CHECK (price <= 9999.99)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offres d\'abonnement par salle';

-- 2️⃣ Table gym_subscription_orders (les commandes clients)
CREATE TABLE gym_subscription_orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL COMMENT 'ID de l\'utilisateur qui souscrit',
    offer_id INT NOT NULL COMMENT 'ID de l\'offre d\'abonnement',
    quantity INT DEFAULT 1 COMMENT 'Quantité (toujours 1 pour les abonnements)',
    unit_price DECIMAL(10,2) NOT NULL COMMENT 'Prix unitaire en TND',
    total_price DECIMAL(10,2) NOT NULL COMMENT 'Prix total (quantité × prix unitaire)',
    status VARCHAR(20) DEFAULT 'active' COMMENT 'Statut : active, expired, cancelled, pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création de la commande',
    
    -- Index pour optimisation
    INDEX idx_gym_orders_user_id (user_id),
    INDEX idx_gym_orders_offer_id (offer_id),
    INDEX idx_gym_orders_status (status),
    INDEX idx_gym_orders_created_at (created_at),
    
    -- Contraintes de clé étrangère
    FOREIGN KEY (user_id) REFERENCES users(id_user) ON DELETE CASCADE,
    FOREIGN KEY (offer_id) REFERENCES gym_subscription_offers(id) ON DELETE CASCADE,
    
    -- Contraintes de validation
    CONSTRAINT chk_gym_orders_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_gym_orders_price_positive CHECK (unit_price > 0 AND total_price > 0),
    CONSTRAINT chk_gym_orders_status_valid CHECK (status IN ('active', 'expired', 'cancelled', 'pending'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Commandes d\'abonnement par utilisateur';

-- Insertion de données de test pour gym_subscription_offers
INSERT INTO gym_subscription_offers (gymnasium_id, name, duration_months, price, description) VALUES
-- Offres pour la salle ID 1 (Olympia Gym)
(1, 'Abonnement Mensuel Standard', 1, 29.99, 'Accès illimité à la salle pendant 30 jours avec équipements de base'),
(1, 'Abonnement Mensuel Premium', 1, 49.99, 'Accès illimité + cours collectifs + coaching personnel 1x/mois'),
(1, 'Abonnement Trimestriel Standard', 3, 79.99, 'Accès illimité pendant 90 jours - économisez 10%'),
(1, 'Abonnement Semestriel Premium', 6, 259.99, 'Accès illimité + cours collectifs + coaching personnel 2x/mois'),
(1, 'Abonnement Annuel Standard', 12, 299.99, 'Accès illimité pendant 365 jours - économisez 2 mois !'),
(1, 'Pack Étudiant', 6, 149.99, 'Tarif spécial pour étudiants avec carte d\'étudiant valide'),

-- Offres pour la salle ID 2 (FitCenter)
(2, 'Abonnement Mensuel Standard', 1, 34.99, 'Accès illimité à la salle pendant 30 jours avec équipements de base'),
(2, 'Abonnement Mensuel Premium', 1, 59.99, 'Accès illimité + cours collectifs + coaching personnel 2x/mois'),
(2, 'Abonnement Trimestriel Standard', 3, 89.99, 'Accès illimité pendant 90 jours - économisez 15%'),
(2, 'Abonnement Annuel Premium', 12, 599.99, 'Accès illimité + cours collectifs illimités + coaching personnalisé'),

-- Offres pour la salle ID 3 (Power Gym)
(3, 'Abonnement Mensuel Standard', 1, 39.99, 'Accès illimité à la salle pendant 30 jours avec équipements de base'),
(3, 'Abonnement Mensuel Premium', 1, 69.99, 'Accès illimité + cours collectifs + coaching personnalisé illimité'),
(3, 'Abonnement Trimestriel Premium', 3, 179.99, 'Accès illimité + cours collectifs + coaching personnalisé'),
(3, 'Abonnement Annuel VIP', 12, 699.99, 'Accès VIP avec toutes les prestations incluses');

-- Insertion de données de test pour gym_subscription_orders
INSERT INTO gym_subscription_orders (user_id, offer_id, quantity, unit_price, total_price, status) VALUES
-- Commandes pour l'utilisateur ID 1
(1, 1, 1, 29.99, 29.99, 'active'),
(1, 2, 1, 49.99, 49.99, 'active'),

-- Commandes pour l'utilisateur ID 2
(2, 1, 1, 29.99, 29.99, 'active'),
(2, 4, 1, 259.99, 259.99, 'active'),

-- Commandes pour l'utilisateur ID 3
(3, 5, 1, 299.99, 299.99, 'active'),
(3, 7, 1, 34.99, 34.99, 'expired'),

-- Commandes pour l'utilisateur ID 4
(4, 2, 1, 49.99, 49.99, 'active'),
(4, 8, 1, 59.99, 59.99, 'pending'),

-- Commandes pour l'utilisateur ID 5
(5, 3, 1, 79.99, 79.99, 'cancelled'),
(5, 9, 1, 39.99, 39.99, 'active');

-- Requêtes de vérification
SELECT 'Tables gym_subscription_offers et gym_subscription_orders créées avec succès' as status;
SELECT COUNT(*) as nombre_offres FROM gym_subscription_offers;
SELECT COUNT(*) as nombre_commandes FROM gym_subscription_orders;

-- Vue pour les offres actives avec détails de la salle
CREATE VIEW v_gym_subscription_offers_active AS
SELECT 
    o.id,
    o.gymnasium_id,
    g.name as gymnasium_name,
    o.name as offer_name,
    o.duration_months,
    o.price,
    o.description,
    o.is_active,
    o.created_at,
    o.updated_at
FROM gym_subscription_offers o
JOIN gymnasia g ON o.gymnasium_id = g.id
WHERE o.is_active = TRUE
ORDER BY g.name, o.price;

-- Vue pour les commandes actives avec détails
CREATE VIEW v_gym_subscription_orders_active AS
SELECT 
    ord.id,
    ord.user_id,
    u.first_name_user,
    u.last_name_user,
    u.email_user,
    ord.offer_id,
    o.name as offer_name,
    o.gymnasium_id,
    g.name as gymnasium_name,
    ord.quantity,
    ord.unit_price,
    ord.total_price,
    ord.status,
    ord.created_at
FROM gym_subscription_orders ord
JOIN users u ON ord.user_id = u.id_user
JOIN gym_subscription_offers o ON ord.offer_id = o.id
JOIN gymnasia g ON o.gymnasium_id = g.id
WHERE ord.status = 'active'
ORDER BY ord.created_at DESC;

-- Procédure pour obtenir les offres par gymnase
DELIMITER //
CREATE PROCEDURE GetOffersByGymnasium(IN p_gymnasium_id INT)
BEGIN
    SELECT 
        o.id,
        o.name,
        o.duration_months,
        o.price,
        o.description,
        o.is_active,
        o.created_at,
        o.updated_at
    FROM gym_subscription_offers o
    WHERE o.gymnasium_id = p_gymnasium_id AND o.is_active = TRUE
    ORDER BY o.price ASC;
END //
DELIMITER ;

-- Procédure pour obtenir les commandes par utilisateur
DELIMITER //
CREATE PROCEDURE GetOrdersByUser(IN p_user_id INT)
BEGIN
    SELECT 
        ord.id,
        ord.offer_id,
        o.name as offer_name,
        o.gymnasium_id,
        g.name as gymnasium_name,
        ord.quantity,
        ord.unit_price,
        ord.total_price,
        ord.status,
        ord.created_at
    FROM gym_subscription_orders ord
    JOIN gym_subscription_offers o ON ord.offer_id = o.id
    JOIN gymnasia g ON o.gymnasium_id = g.id
    WHERE ord.user_id = p_user_id
    ORDER BY ord.created_at DESC;
END //
DELIMITER ;

-- Fonction pour calculer le revenu total par gymnase
DELIMITER //
CREATE FUNCTION GetRevenueByGymnasium(p_gymnasium_id INT) 
RETURNS DECIMAL(10,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE total_revenue DECIMAL(10,2);
    
    SELECT COALESCE(SUM(ord.total_price), 0) INTO total_revenue
    FROM gym_subscription_orders ord
    JOIN gym_subscription_offers o ON ord.offer_id = o.id
    WHERE o.gymnasium_id = p_gymnasium_id AND ord.status = 'active';
    
    RETURN total_revenue;
END //
DELIMITER ;

-- Fonction pour compter les abonnements actifs par offre
DELIMITER //
CREATE FUNCTION CountActiveSubscriptions(p_offer_id INT) 
RETURNS INT
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE active_count INT;
    
    SELECT COUNT(*) INTO active_count
    FROM gym_subscription_orders 
    WHERE offer_id = p_offer_id AND status = 'active';
    
    RETURN active_count;
END //
DELIMITER ;

-- Trigger pour journaliser les changements de prix des offres
DELIMITER //
CREATE TRIGGER gym_subscription_offers_before_update 
BEFORE UPDATE ON gym_subscription_offers
FOR EACH ROW
BEGIN
    -- Journaliser les modifications de prix importantes
    IF NEW.price <> OLD.price AND ABS(NEW.price - OLD.price) > 5 THEN
        INSERT INTO log_price_changes (table_name, record_id, old_price, new_price, changed_at)
        VALUES ('gym_subscription_offers', NEW.id, OLD.price, NEW.price, NOW());
    END IF;
END //
DELIMITER ;

-- Table de journalisation (optionnelle)
CREATE TABLE IF NOT EXISTS log_price_changes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    table_name VARCHAR(50) NOT NULL,
    record_id INT NOT NULL,
    old_price DECIMAL(10,2) NOT NULL,
    new_price DECIMAL(10,2) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_table_record (table_name, record_id),
    INDEX idx_log_changed_at (changed_at)
) COMMENT='Journal des modifications de prix';

-- Requêtes de test pour les vues
SELECT * FROM v_gym_subscription_offers_active LIMIT 5;
SELECT * FROM v_gym_subscription_orders_active LIMIT 5;

-- Tests des procédures
CALL GetOffersByGymnasium(1);
CALL GetOrdersByUser(1);

-- Tests des fonctions
SELECT GetRevenueByGymnasium(1) as revenu_gym_1;
SELECT CountActiveSubscriptions(1) as abonnements_actifs_offre_1;

-- Requêtes statistiques utiles
-- Nombre d'offres par gymnase
SELECT 
    g.name as gymnase,
    COUNT(o.id) as nombre_offres,
    AVG(o.price) as prix_moyen
FROM gymnasia g
LEFT JOIN gym_subscription_offers o ON g.id = o.gymnasium_id AND o.is_active = TRUE
GROUP BY g.id, g.name
ORDER BY nombre_offres DESC;

-- Revenus par gymnase
SELECT 
    g.name as gymnase,
    COUNT(ord.id) as nombre_commandes,
    SUM(ord.total_price) as revenu_total,
    AVG(ord.total_price) as panier_moyen
FROM gymnasia g
LEFT JOIN gym_subscription_offers o ON g.id = o.gymnasium_id
LEFT JOIN gym_subscription_orders ord ON o.id = ord.offer_id AND ord.status = 'active'
GROUP BY g.id, g.name
ORDER BY revenu_total DESC;

-- Top 5 des offres les plus populaires
SELECT 
    o.name,
    g.name as gymnase,
    COUNT(ord.id) as nombre_ventes,
    SUM(ord.total_price) as revenu_total
FROM gym_subscription_offers o
JOIN gymnasia g ON o.gymnasium_id = g.id
LEFT JOIN gym_subscription_orders ord ON o.id = ord.offer_id AND ord.status = 'active'
WHERE o.is_active = TRUE
GROUP BY o.id, o.name, g.name
ORDER BY nombre_ventes DESC
LIMIT 5;
