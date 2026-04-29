-- Création d'une salle de test pour rating
-- Compatible avec la structure existante

-- Suppression de la salle de test si elle existe
DELETE FROM salles WHERE id = 999;

-- Insertion d'une salle de test avec YouTube
INSERT INTO salles (
    id, nom, adresse, description, capacite, 
    surface, prix_mensuel, image_url, youtube_url,
    statut, created_at, updated_at
) VALUES (
    999,
    'Salle Test Rating',
    'Salle de test spécialement conçue pour tester le système de notation/rating',
    50,
    200,
    99.99,
    'https://example.com/images/salle-test.jpg',
    'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
    'active',
    NOW(),
    NOW()
);

-- Insertion des horaires pour la salle de test
INSERT INTO horaires_salle (
    salle_id, jour, heure_ouverture, heure_fermeture
) VALUES 
    (999, 'lundi', '06:00', '23:00'),
    (999, 'mardi', '06:00', '23:00'),
    (999, 'mercredi', '06:00', '23:00'),
    (999, 'jeudi', '06:00', '23:00'),
    (999, 'vendredi', '06:00', '23:00'),
    (999, 'samedi', '08:00', '20:00'),
    (999, 'dimanche', '08:00', '18:00');

-- Insertion des equipements pour la salle de test
INSERT INTO equipements_salle (
    salle_id, nom, description, quantite
) VALUES 
    (999, 'Tapis de course', 'Tapis professionnel de haute qualite', 20),
    (999, 'Machines cardio', 'Tapis roulant, velos et elliptiques', 15),
    (999, 'Halteres', 'Halteres de 2kg a 30kg', 10),
    (999, 'Barres', 'Barres de musculation et poids', 8);

-- Insertion des offres d'abonnement pour la salle de test
INSERT INTO gym_subscription_offers (
    gymnasium_id, name, prix, description, 
    duree_mois, features, statut, created_at, updated_at
) VALUES 
    (999, 'Essai Mensuel', 29.99, 'Accès complet à la salle test', 1, '{"wifi": true, "parking": true, "douche": true}', 'active', NOW(), NOW()),
    (999, 'Essai Annuel', 299.99, 'Accès complet + réductions exclusives', 12, '{"wifi": true, "parking": true, "douche": true, "locker": true}', 'active', NOW(), NOW()),
    (999, 'Essai Premium', 499.99, 'Accès VIP + coaching perso', 6, '{"wifi": true, "parking": true, "douche": true, "locker": true, "coaching": true}', 'active', NOW(), NOW());

-- Insertion de quelques sessions pour la salle de test
INSERT INTO sessions (
    salle_id, titre, description, date_session, 
    heure_debut, heure_fin, capacite_max, prix, 
    statut, created_at, updated_at
) VALUES 
    (999, 'Session Test Matin', 'Session de test pour rating', DATE(NOW() + INTERVAL 1 DAY), '08:00', '10:00', 20, 10.00, 'active', NOW(), NOW()),
    (999, 'Session Test Soir', 'Session de test pour rating', DATE(NOW() + INTERVAL 1 DAY), '18:00', '20:00', 20, 15.00, 'active', NOW(), NOW()),
    (999, 'Session Test Week-end', 'Session de test pour rating', DATE(NOW() + INTERVAL 2 DAY), '14:00', '16:00', 25, 20.00, 'active', NOW(), NOW());

-- Insertion de quelques ratings de test pour la salle 999
INSERT INTO ratings_salle (
    salle_id, user_id, note, commentaire, 
    date_rating, statut, created_at, updated_at
) VALUES 
    (999, 1, 5, 'Salle excellente ! Très propre et bien équipée.', DATE(NOW() - INTERVAL 7 DAY), 'approuve', NOW(), NOW()),
    (999, 2, 4, 'Bonne salle, mais pourrait être améliorée.', DATE(NOW() - INTERVAL 5 DAY), 'approuve', NOW(), NOW()),
    (999, 3, 3, 'Correct mais un peu cher pour ce qu''elle offre.', DATE(NOW() - INTERVAL 3 DAY), 'approuve', NOW(), NOW()),
    (999, 4, 4, 'Très satisfait de l''équipement et du service.', DATE(NOW() - INTERVAL 1 DAY), 'approuve', NOW(), NOW()),
    (999, 5, 2, 'Salle propre, bien localisée.', DATE(NOW() - INTERVAL 2 DAY), 'approuve', NOW(), NOW());

-- Vérification de la création
SELECT 
    'Salle de test créée avec succès' as status,
    COUNT(*) as nombre_salles,
    COUNT(*) as nombre_ratings
FROM salles s
LEFT JOIN ratings_salle r ON s.id = r.salle_id
WHERE s.id = 999;

-- Requêtes utiles pour tester
-- 1. Voir la salle de test
SELECT * FROM salles WHERE id = 999;

-- 2. Voir les ratings de la salle de test
SELECT 
    r.*,
    u.first_name_user,
    u.last_name_user,
    u.email_user
FROM ratings_salle r
JOIN users u ON r.user_id = u.id_user
WHERE r.salle_id = 999
ORDER BY r.date_rating DESC;

-- 3. Statistiques de ratings pour la salle de test
SELECT 
    COUNT(*) as nombre_total_ratings,
    AVG(note) as moyenne_ratings,
    MIN(note) as note_minimale,
    MAX(note) as note_maximale
FROM ratings_salle
WHERE salle_id = 999
GROUP BY salle_id;
