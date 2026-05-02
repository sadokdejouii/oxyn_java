-- ═══════════════════════════════════════════════════════════════
--   OXYN - Quick Setup Planning pour California Gymm (ID: 1)
--   ═══════════════════════════════════════════════════════════════

-- Sessions pour California Gymm cette semaine (27 avril - 3 mai 2026)
INSERT INTO training_sessions (title, description, start_at, end_at, capacity, price, is_active, created_at, gymnasium_id, coach_user_id) VALUES
-- Lundi 28 Avril
('Cardio HIIT Intense', 'Session de cardio haute intensité avec intervalles', '2026-04-28 07:00:00', '2026-04-28 08:00:00', 25, 15.0, 1, NOW(), 1, 1),
('Yoga Relaxation', 'Session de yoga pour tous niveaux avec postures et respiration', '2026-04-28 09:00:00', '2026-04-28 10:00:00', 20, 18.0, 1, NOW(), 1, 2),
('Musculation Force', 'Entraînement de musculation avec poids libres et machines', '2026-04-28 10:30:00', '2026-04-28 12:00:00', 15, 20.0, 1, NOW(), 1, 3),
('Zumba Party', 'Session de danse latine avec musique entraînante', '2026-04-28 18:00:00', '2026-04-28 19:00:00', 30, 12.0, 1, NOW(), 1, 4),

-- Mardi 29 Avril
('Boxing Technique', 'Apprentissage des techniques de boxe et combat', '2026-04-29 07:00:00', '2026-04-29 08:30:00', 12, 22.0, 1, NOW(), 1, 5),
('Circuit Training', 'Entraînement circuit avec plusieurs stations', '2026-04-29 09:00:00', '2026-04-29 10:30:00', 18, 25.0, 1, NOW(), 1, 6),
('Pilates Core', 'Renforcement des muscles profonds avec Pilates', '2026-04-29 11:00:00', '2026-04-29 12:00:00', 16, 20.0, 1, NOW(), 1, 7),
('Body Combat', 'Entraînement complet corps à corps', '2026-04-29 17:00:00', '2026-04-29 18:30:00', 10, 25.0, 1, NOW(), 1, 8),

-- Mercredi 30 Avril
('Fitness Senior', 'Fitness adapté pour seniors', '2026-04-30 08:00:00', '2026-04-30 09:00:00', 12, 15.0, 1, NOW(), 1, 9),
('Core Training', 'Renforcement gainage et stabilité', '2026-04-30 09:30:00', '2026-04-30 10:30:00', 15, 16.0, 1, NOW(), 1, 10),
('Step Aerobic', 'Choregraphies step sur plateformes', '2026-04-30 11:00:00', '2026-04-30 12:00:00', 22, 12.0, 1, NOW(), 1, 11),
('Danse Moderne', 'Initiation à la danse contemporaine', '2026-04-30 19:00:00', '2026-04-30 20:00:00', 25, 15.0, 1, NOW(), 1, 12),

-- Jeudi 1 Mai
('CrossFit WOD', 'Workout of the day CrossFit', '2026-05-01 06:00:00', '2026-05-01 07:30:00', 8, 28.0, 1, NOW(), 1, 13),
('Yoga Power', 'Yoga dynamique avec enchaînements fluides', '2026-05-01 08:00:00', '2026-05-01 09:00:00', 18, 18.0, 1, NOW(), 1, 14),
('Body Pump', 'Entraînement avec barres et poids légers', '2026-05-01 09:30:00', '2026-05-01 10:30:00', 18, 18.0, 1, NOW(), 1, 15),
('Spinning RPM', 'Cours de spinning avec musique dynamique', '2026-05-01 17:00:00', '2026-05-01 18:00:00', 25, 14.0, 1, NOW(), 1, 16),

-- Vendredi 2 Mai
('Running Club', 'Course à pied en groupe tous niveaux', '2026-05-02 06:30:00', '2026-05-02 07:30:00', 20, 10.0, 1, NOW(), 1, 17),
('HIIT Supreme', 'HIIT très haute intensité', '2026-05-02 08:00:00', '2026-05-02 09:00:00', 5, 28.0, 1, NOW(), 1, 18),
('Musculation Power', 'Entraînement force avec surcharge progressive', '2026-05-02 09:30:00', '2026-05-02 11:00:00', 12, 22.0, 1, NOW(), 1, 19),
('Salsa Latino', 'Salsa latine passionnée', '2026-05-02 18:30:00', '2026-05-02 20:00:00', 16, 18.0, 1, NOW(), 1, 20),

-- Samedi 3 Mai
('Bachata Romantique', 'Bachata romantique pour couples', '2026-05-03 10:00:00', '2026-05-03 11:30:00', 14, 18.0, 1, NOW(), 1, 21),
('Circuit Elite', 'Circuit avancé pour athlètes', '2026-05-03 11:00:00', '2026-05-03 12:30:00', 6, 35.0, 1, NOW(), 1, 22),
('Stretching Doux', 'Étirements doux et relaxation', '2026-05-03 14:00:00', '2026-05-03 14:45:00', 20, 10.0, 1, NOW(), 1, 23),
('Boxe Sparring', 'Combat d'entraînement boxe', '2026-05-03 15:00:00', '2026-05-03 16:00:00', 3, 25.0, 1, NOW(), 1, 24);

-- Afficher les sessions créées
SELECT 
    title,
    DATE_FORMAT(start_at, '%W %d/%m %H:%i') as jour_heure,
    TIME_FORMAT(start_at, '%H:%i') as debut,
    TIME_FORMAT(end_at, '%H:%i') as fin,
    capacity,
    price,
    CASE 
        WHEN title LIKE '%yoga%' OR title LIKE '%pilates%' OR title LIKE '%stretch%' THEN 'Yoga'
        WHEN title LIKE '%box%' OR title LIKE '%combat%' OR title LIKE '%karate%' OR title LIKE '%muay%' OR title LIKE '%sparring%' THEN 'Combat'
        WHEN title LIKE '%cardio%' OR title LIKE '%hiit%' OR title LIKE '%aérobic%' OR title LIKE '%running%' OR title LIKE '%step%' OR title LIKE '%spinning%' THEN 'Cardio'
        WHEN title LIKE '%musculation%' OR title LIKE '%cross%' OR title LIKE '%pump%' OR title LIKE '%circuit%' OR title LIKE '%force%' OR title LIKE '%weight%' THEN 'Musculation'
        WHEN title LIKE '%zumba%' OR title LIKE '%danse%' OR title LIKE '%salsa%' OR title LIKE '%bachata%' THEN 'Danse'
        WHEN title LIKE '%fitness%' OR title LIKE '%core%' OR title LIKE '%bike%' OR title LIKE '%body%' OR title LIKE '%tonic%' THEN 'Fitness'
        ELSE 'Autre'
    END as categorie
FROM training_sessions 
WHERE gymnasium_id = 1 AND is_active = 1 
ORDER BY start_at;
