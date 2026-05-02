-- Migration pour mettre à jour le schéma de gym_subscription_orders
-- Compatible avec l'intégration Flouci v2

-- Vérifier si la table existe déjà
CREATE TABLE IF NOT EXISTS gym_subscription_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    offre_id INT NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_id VARCHAR(100) NULL,
    montant DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_payment_id (payment_id),
    INDEX idx_statut (statut),
    INDEX idx_created_at (created_at)
);

-- Si la table existe mais avec des colonnes différentes, ajouter les colonnes manquantes
ALTER TABLE gym_subscription_orders 
ADD COLUMN IF NOT EXISTS date_debut DATE NOT NULL DEFAULT (CURDATE()),
ADD COLUMN IF NOT EXISTS date_fin DATE NOT NULL DEFAULT (DATE_ADD(CURDATE(), INTERVAL 1 MONTH)),
ADD COLUMN IF NOT EXISTS statut VARCHAR(20) NOT NULL DEFAULT 'PENDING',
ADD COLUMN IF NOT EXISTS payment_id VARCHAR(100) NULL,
ADD COLUMN IF NOT EXISTS montant DECIMAL(10,2) NOT NULL DEFAULT 0.00,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Mettre à jour les enregistrements existants si nécessaire
UPDATE gym_subscription_orders 
SET statut = 'ACTIF' 
WHERE statut IS NULL OR statut = '';

-- Ajouter les index manquants si la table existait déjà
ALTER TABLE gym_subscription_orders 
ADD INDEX IF NOT EXISTS idx_user_id (user_id),
ADD INDEX IF NOT EXISTS idx_payment_id (payment_id),
ADD INDEX IF NOT EXISTS idx_statut (statut),
ADD INDEX IF NOT EXISTS idx_created_at (created_at);

-- Afficher la structure finale
DESCRIBE gym_subscription_orders;
