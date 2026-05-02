-- Ajouter le champ pour stocker l'ID de session Flouci dans les commandes d'abonnement
ALTER TABLE gym_subscription_orders 
ADD COLUMN flouci_session_id VARCHAR(255) NULL AFTER total_price;

-- Ajouter un index pour optimiser les recherches par session
CREATE INDEX idx_flouci_session ON gym_subscription_orders(flouci_session_id);
