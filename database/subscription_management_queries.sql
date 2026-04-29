-- ========================================
-- GESTION DES ABONNEMENTS - FLUX COMPLET
-- Flux : PENDING → PAYÉ → ACTIVE → EXPIRÉ
-- ========================================

-- 1. Vérifier les abonnements actifs d'un utilisateur
SELECT 
    o.*,
    g.name as offre_name,
    g.duration_months,
    u.name as client_name
FROM gym_subscription_orders o
JOIN gym_subscription_offers g ON o.offre_id = g.id
JOIN users u ON o.user_id = u.id
WHERE o.user_id = 10 
AND o.status = 'active'
AND o.date_fin >= CURDATE()
ORDER BY o.created_at DESC;

-- 2. Vérifier les paiements confirmés non encore activés (statut = 'payé')
SELECT 
    o.*,
    g.name as offre_name,
    u.name as client_name
FROM gym_subscription_orders o
JOIN gym_subscription_offers g ON o.offre_id = g.id
JOIN users u ON o.user_id = u.id
WHERE o.status = 'payé'
ORDER BY o.created_at ASC;

-- 3. Expirer les abonnements automatiquement
UPDATE gym_subscription_orders
SET status = 'expiré',
    updated_at = NOW()
WHERE status = 'active'
AND date_fin < CURDATE();

-- 4. Obtenir les statistiques des abonnements par statut
SELECT 
    status,
    COUNT(*) as nombre,
    SUM(montant) as total_montant
FROM gym_subscription_orders
GROUP BY status
ORDER BY nombre DESC;

-- 5. Obtenir les abonnements qui expireront dans les 7 prochains jours
SELECT 
    o.*,
    g.name as offre_name,
    u.name as client_name,
    DATEDIFF(o.date_fin, CURDATE()) as jours_restants
FROM gym_subscription_orders o
JOIN gym_subscription_offers g ON o.offre_id = g.id
JOIN users u ON o.user_id = u.id
WHERE o.status = 'active'
AND o.date_fin BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY)
ORDER BY o.date_fin ASC;

-- 6. Historique des abonnements d'un utilisateur
SELECT 
    o.*,
    g.name as offre_name,
    g.duration_months,
    CASE 
        WHEN o.status = 'active' AND o.date_fin >= CURDATE() THEN 'Actif'
        WHEN o.status = 'active' AND o.date_fin < CURDATE() THEN 'Expiré'
        ELSE o.status
    END as statut_affiche
FROM gym_subscription_orders o
JOIN gym_subscription_offers g ON o.offre_id = g.id
WHERE o.user_id = 10
ORDER BY o.created_at DESC;

-- 7. Créer une commande en attente (PENDING)
INSERT INTO gym_subscription_orders (
    user_id, 
    offre_id, 
    statut, 
    payment_id, 
    montant, 
    created_at
) VALUES (
    10,  -- user_id
    1,   -- offre_id
    'pending',  -- statut initial
    NULL, -- payment_id (sera rempli après paiement Flouci)
    29.99, -- montant
    NOW()
);

-- 8. Mettre à jour le statut vers PAYÉ (après confirmation Flouci)
UPDATE gym_subscription_orders
SET statut = 'payé',
    payment_id = 'flouci_payment_12345',
    updated_at = NOW()
WHERE id = 1;

-- 9. Activer l'abonnement (passer de PAYÉ → ACTIVE)
UPDATE gym_subscription_orders
SET 
    statut = 'active',
    date_debut = CURDATE(),
    date_fin = DATE_ADD(CURDATE(), INTERVAL 1 MONTH), -- ou selon durée de l'offre
    updated_at = NOW()
WHERE payment_id = 'flouci_payment_12345';

-- 10. Annuler une commande (PENDING → ANNULE)
UPDATE gym_subscription_orders
SET statut = 'annule',
    updated_at = NOW()
WHERE id = 1 AND statut = 'pending';

-- 11. Marquer comme échec (PAYÉ → ECHEC)
UPDATE gym_subscription_orders
SET statut = 'echec',
    updated_at = NOW()
WHERE payment_id = 'flouci_payment_failed_12345';

-- 12. Nettoyer les anciennes commandes (optionnel)
DELETE FROM gym_subscription_orders 
WHERE created_at < DATE_SUB(NOW(), INTERVAL 2 YEAR)
AND status IN ('annule', 'echec', 'expiré');

-- 13. Vue synthétique des abonnements actifs
CREATE OR REPLACE VIEW v_abonnements_actifs AS
SELECT 
    o.id,
    o.user_id,
    u.name as client_name,
    u.email as client_email,
    o.offre_id,
    g.name as offre_name,
    o.montant,
    o.date_debut,
    o.date_fin,
    DATEDIFF(o.date_fin, CURDATE()) as jours_restants,
    o.payment_id,
    o.created_at
FROM gym_subscription_orders o
JOIN users u ON o.user_id = u.id
JOIN gym_subscription_offers g ON o.offre_id = g.id
WHERE o.status = 'active'
AND o.date_fin >= CURDATE();

-- Utilisation de la vue
SELECT * FROM v_abonnements_actifs WHERE jours_restants <= 7;

-- 14. Recherche par payment_id
SELECT 
    o.*,
    g.name as offre_name,
    u.name as client_name
FROM gym_subscription_orders o
JOIN gym_subscription_offers g ON o.offre_id = g.id
JOIN users u ON o.user_id = u.id
WHERE o.payment_id = 'flouci_payment_12345';

-- 15. Dashboard des revenus mensuels
SELECT 
    DATE_FORMAT(created_at, '%Y-%m') as mois,
    COUNT(*) as nombre_abonnements,
    SUM(montant) as revenu_total,
    AVG(montant) as panier_moyen
FROM gym_subscription_orders
WHERE status IN ('active', 'payé')
GROUP BY DATE_FORMAT(created_at, '%Y-%m')
ORDER BY mois DESC
LIMIT 12;
