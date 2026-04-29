-- Création de la table gym_subscription_orders selon la structure exacte
-- Compatible avec la méthode saveFlouciOrder() simplifiée

-- Suppression de la table si elle existe (pour développement)
DROP TABLE IF EXISTS gym_subscription_orders;

-- Création de la table gym_subscription_orders
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
    FOREIGN KEY (offer_id) REFERENCES offres(id) ON DELETE CASCADE,
    
    -- Contraintes de validation
    CONSTRAINT chk_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_price_positive CHECK (unit_price > 0 AND total_price > 0),
    CONSTRAINT chk_status_valid CHECK (status IN ('active', 'expired', 'cancelled', 'pending'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Commandes d\'abonnement par utilisateur';

-- Insertion de données de test
INSERT INTO gym_subscription_orders (user_id, offer_id, quantity, unit_price, total_price, status) VALUES
(1, 1, 1, 29.99, 29.99, 'active'),
(1, 2, 1, 49.99, 49.99, 'active'),
(2, 1, 1, 29.99, 29.99, 'active'),
(3, 3, 1, 299.99, 299.99, 'active'),
(4, 1, 1, 29.99, 29.99, 'expired'),
(5, 2, 1, 49.99, 49.99, 'pending');

-- Requêtes de vérification
SELECT 'Table gym_subscription_orders créée avec succès' as status;
SELECT COUNT(*) as nombre_commandes FROM gym_subscription_orders;
SELECT * FROM gym_subscription_orders ORDER BY created_at DESC;

-- Vue pour les commandes actives
CREATE VIEW v_gym_subscription_orders_active AS
SELECT 
    o.id,
    o.user_id,
    u.first_name_user,
    u.last_name_user,
    u.email_user,
    o.offer_id,
    of.nom as offre_nom,
    of.prix as offre_prix,
    o.quantity,
    o.unit_price,
    o.total_price,
    o.status,
    o.created_at
FROM gym_subscription_orders o
JOIN users u ON o.user_id = u.id_user
JOIN offres of ON o.offer_id = of.id
WHERE o.status = 'active'
ORDER BY o.created_at DESC;

-- Requêtes de test pour la vue
SELECT * FROM v_gym_subscription_orders_active;

-- Procédure pour obtenir les commandes d'un utilisateur
DELIMITER //
CREATE PROCEDURE GetOrdersByUser(IN p_user_id INT)
BEGIN
    SELECT 
        o.id,
        o.offer_id,
        of.nom as offre_nom,
        of.prix,
        o.quantity,
        o.unit_price,
        o.total_price,
        o.status,
        o.created_at
    FROM gym_subscription_orders o
    JOIN offres of ON o.offer_id = of.id
    WHERE o.user_id = p_user_id
    ORDER BY o.created_at DESC;
END //
DELIMITER ;

-- Procédure pour obtenir les commandes par offre
DELIMITER //
CREATE PROCEDURE GetOrdersByOffer(IN p_offer_id INT)
BEGIN
    SELECT 
        o.id,
        o.user_id,
        u.first_name_user,
        u.last_name_user,
        u.email_user,
        o.quantity,
        o.unit_price,
        o.total_price,
        o.status,
        o.created_at
    FROM gym_subscription_orders o
    JOIN users u ON o.user_id = u.id_user
    WHERE o.offer_id = p_offer_id
    ORDER BY o.created_at DESC;
END //
DELIMITER ;

-- Trigger pour journaliser les changements de statut
DELIMITER //
CREATE TRIGGER gym_subscription_orders_before_update 
BEFORE UPDATE ON gym_subscription_orders
FOR EACH ROW
BEGIN
    -- Journaliser les changements de statut
    IF NEW.status <> OLD.status THEN
        INSERT INTO log_status_changes (table_name, record_id, old_status, new_status, changed_at)
        VALUES ('gym_subscription_orders', NEW.id, OLD.status, NEW.status, NOW());
    END IF;
END //
DELIMITER ;

-- Table de journalisation (optionnelle)
CREATE TABLE IF NOT EXISTS log_status_changes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    table_name VARCHAR(50) NOT NULL,
    record_id INT NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_table_record (table_name, record_id),
    INDEX idx_log_changed_at (changed_at)
) COMMENT='Journal des changements de statut';

-- Fonction pour calculer le revenu total par offre
DELIMITER //
CREATE FUNCTION GetRevenueByOffer(p_offer_id INT) 
RETURNS DECIMAL(10,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE total_revenue DECIMAL(10,2);
    
    SELECT COALESCE(SUM(total_price), 0) INTO total_revenue
    FROM gym_subscription_orders 
    WHERE offer_id = p_offer_id AND status = 'active';
    
    RETURN total_revenue;
END //
DELIMITER ;

-- Fonction pour compter les abonnements actifs par utilisateur
DELIMITER //
CREATE FUNCTION CountActiveSubscriptions(p_user_id INT) 
RETURNS INT
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE active_count INT;
    
    SELECT COUNT(*) INTO active_count
    FROM gym_subscription_orders 
    WHERE user_id = p_user_id AND status = 'active';
    
    RETURN active_count;
END //
DELIMITER ;

-- Tests des fonctions
SELECT GetRevenueByOffer(1) as revenu_offre_1;
SELECT CountActiveSubscriptions(1) as abonnements_actifs_user_1;

-- Requêtes statistiques utiles
-- Nombre total d'abonnements par statut
SELECT status, COUNT(*) as nombre, SUM(total_price) as revenu_total
FROM gym_subscription_orders
GROUP BY status;

-- Top 5 des offres les plus populaires
SELECT 
    of.nom,
    COUNT(o.id) as nombre_ventes,
    SUM(o.total_price) as revenu_total
FROM gym_subscription_orders o
JOIN offres of ON o.offer_id = of.id
WHERE o.status = 'active'
GROUP BY of.id, of.nom
ORDER BY nombre_ventes DESC
LIMIT 5;

-- Revenus mensuels
SELECT 
    DATE_FORMAT(created_at, '%Y-%m') as mois,
    COUNT(*) as nombre_commandes,
    SUM(total_price) as revenu_mensuel
FROM gym_subscription_orders
WHERE status = 'active'
GROUP BY DATE_FORMAT(created_at, '%Y-%m')
ORDER BY mois DESC;
