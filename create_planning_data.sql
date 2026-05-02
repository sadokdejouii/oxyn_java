-- ═══════════════════════════════════════════════════════════════
--   OXYN - Script de création de données de planning pour salles
--   ═══════════════════════════════════════════════════════════════

-- Supprimer les sessions existantes (optionnel)
-- DELETE FROM training_sessions WHERE gymnasium_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

-- Insérer des sessions de planning pour différentes salles
INSERT INTO training_sessions (title, description, start_at, end_at, capacity, price, is_active, created_at, gymnasium_id, coach_user_id) VALUES
-- California Gymm (ID: 1) - Sessions Cardio et Fitness
('Cardio HIIT Intense', 'Session de cardio haute intensité avec intervalles', '2026-04-28 07:00:00', '2026-04-28 08:00:00', 25, 15.0, 1, NOW(), 1, 1),
('Yoga Relaxation', 'Session de yoga pour tous niveaux avec postures et respiration', '2026-04-28 09:00:00', '2026-04-28 10:00:00', 20, 18.0, 1, NOW(), 1, 2),
('Musculation Force', 'Entraînement de musculation avec poids libres et machines', '2026-04-28 10:30:00', '2026-04-28 12:00:00', 15, 20.0, 1, NOW(), 1, 3),
('Zumba Party', 'Session de danse latine avec musique entraînante', '2026-04-28 18:00:00', '2026-04-28 19:00:00', 30, 12.0, 1, NOW(), 1, 4),
('Boxing Technique', 'Apprentissage des techniques de boxe et combat', '2026-04-29 07:00:00', '2026-04-29 08:30:00', 12, 22.0, 1, NOW(), 1, 5),
('Circuit Training', 'Entraînement circuit avec plusieurs stations', '2026-04-29 09:00:00', '2026-04-29 10:30:00', 18, 25.0, 1, NOW(), 1, 6),
('Pilates Core', 'Renforcement des muscles profonds avec Pilates', '2026-04-29 11:00:00', '2026-04-29 12:00:00', 16, 20.0, 1, NOW(), 1, 7),
('Body Combat', 'Entraînement complet corps à corps', '2026-04-29 17:00:00', '2026-04-29 18:30:00', 10, 25.0, 1, NOW(), 1, 8),
('Fitness Senior', 'Fitness adapté pour seniors', '2026-04-30 08:00:00', '2026-04-30 09:00:00', 12, 15.0, 1, NOW(), 1, 9),

-- Fitness Plus (ID: 2) - Sessions variées
('Running Club', 'Course à pied en groupe tous niveaux', '2026-04-27 06:30:00', '2026-04-27 07:30:00', 20, 10.0, 1, NOW(), 2, 1),
('Spinning RPM', 'Cours de spinning avec musique dynamique', '2026-04-27 08:00:00', '2026-04-27 09:00:00', 25, 14.0, 1, NOW(), 2, 2),
('CrossFit WOD', 'Workout of the day CrossFit', '2026-04-27 09:30:00', '2026-04-27 11:00:00', 8, 28.0, 1, NOW(), 2, 3),
('Step Aerobic', 'Choregraphies step sur plateformes', '2026-04-27 11:30:00', '2026-04-27 12:30:00', 22, 12.0, 1, NOW(), 2, 4),
('Body Pump', 'Entraînement avec barres et poids légers', '2026-04-27 17:00:00', '2026-04-27 18:00:00', 18, 18.0, 1, NOW(), 2, 5),
('Core Training', 'Renforcement gainage et stabilité', '2026-04-28 07:00:00', '2026-04-28 08:00:00', 15, 16.0, 1, NOW(), 2, 6),
('Danse Moderne', 'Initiation à la danse contemporaine', '2026-04-28 19:00:00', '2026-04-28 20:00:00', 25, 15.0, 1, NOW(), 2, 7),
('Musculation Power', 'Entraînement force avec surcharge progressive', '2026-04-29 08:00:00', '2026-04-29 09:30:00', 12, 22.0, 1, NOW(), 2, 8),

-- Elite Gym (ID: 3) - Sessions spécialisées
('Boxe Muay Thai', 'Techniques de Muay Thai traditionnel', '2026-04-27 18:00:00', '2026-04-27 19:30:00', 10, 30.0, 1, NOW(), 3, 1),
('Karate Kyokushin', 'Art martial japonais avec katas', '2026-04-28 06:00:00', '2026-04-28 07:30:00', 8, 35.0, 1, NOW(), 3, 2),
('Self Defense', 'Techniques d'auto-défense pratique', '2026-04-28 08:00:00', '2026-04-27 09:00:00', 6, 40.0, 1, NOW(), 3, 3),
('HIIT Extreme', 'HIIT très intense avec obstacles', '2026-04-28 10:00:00', '2026-04-28 11:00:00', 5, 32.0, 1, NOW(), 3, 4),
('Salsa Sensuelle', 'Initiation à la salsa en couple', '2026-04-28 19:00:00', '2026-04-28 20:30:00', 16, 20.0, 1, NOW(), 3, 5),
('Bachata Romantique', 'Danse bachata pour débutants', '2026-04-29 20:00:00', '2026-04-29 21:00:00', 14, 18.0, 1, NOW(), 3, 6),
('Circuit Elite', 'Circuit avancé pour athlètes', '2026-04-30 07:00:00', '2026-04-30 08:30:00', 6, 35.0, 1, NOW(), 3, 7),
('Olympic Lifting', 'Haltérophilie olympique', '2026-04-30 09:00:00', '2026-04-30 10:30:00', 4, 40.0, 1, NOW(), 3, 8),

-- Sport Center (ID: 4) - Sessions familiales
('Yoga Enfant', 'Yoga adapté pour enfants 6-12 ans', '2026-04-27 16:00:00', '2026-04-27 16:45:00', 15, 12.0, 1, NOW(), 4, 1),
('Fitness Famille', 'Activités physiques pour toute la famille', '2026-04-27 17:00:00', '2026-04-27 18:00:00', 25, 10.0, 1, NOW(), 4, 2),
('Danse Créative', 'Danse libre pour enfants', '2026-04-28 15:00:00', '2026-04-28 16:00:00', 20, 8.0, 1, NOW(), 4, 3),
('Aquagym Dynamique', 'Aquagym avec musique et jeux', '2026-04-29 10:00:00', '2026-04-29 11:00:00', 30, 15.0, 1, NOW(), 4, 4),
('Boxe Junior', 'Boxe pour adolescents 13-17 ans', '2026-04-29 16:00:00', '2026-04-29 17:00:00', 12, 18.0, 1, NOW(), 4, 5),
('Stretching Doux', 'Étirements doux et relaxation', '2026-04-30 11:00:00', '2026-04-30 11:30:00', 20, 10.0, 1, NOW(), 4, 6),
('Tai Chi Chinois', 'Tai Chi pour équilibre et souplesse', '2026-04-30 15:00:00', '2026-04-30 16:00:00', 18, 12.0, 1, NOW(), 4, 7),
('Pilates Rééducation', 'Pilates pour rééducation posturale', '2026-04-30 16:30:00', '2026-04-30 17:30:00', 10, 25.0, 1, NOW(), 4, 8),

-- Gym Pro (ID: 5) - Sessions performance
('Power Lifting', 'Haltérophilie olympique avancée', '2026-04-27 05:00:00', '2026-04-27 07:00:00', 4, 45.0, 1, NOW(), 5, 1),
('CrossFit Competition', 'Préparation compétition CrossFit', '2026-04-27 07:30:00', '2026-04-27 09:00:00', 6, 38.0, 1, NOW(), 5, 2),
('Sprint Interval', 'Training de vitesse par intervalles', '2026-04-27 09:30:00', '2026-04-27 10:00:00', 8, 25.0, 1, NOW(), 5, 3),
('Endurance Ultra', 'Course d'endurance longue distance', '2026-04-27 10:30:00', '2026-04-27 12:00:00', 5, 30.0, 1, NOW(), 5, 4),
('Circuit Athlétique', 'Circuit spécifique athlètes', '2026-04-27 12:30:00', '2026-04-27 14:00:00', 7, 35.0, 1, NOW(), 5, 5),
('Nutrition Coaching', 'Consultation nutrition sportive', '2026-04-27 14:30:00', '2026-04-27 15:30:00', 1, 50.0, 1, NOW(), 5, 6),
('Recovery Stretching', 'Étirements récupération post-effort', '2026-04-27 15:00:00', '2026-04-27 15:30:00', 12, 15.0, 1, NOW(), 5, 7),
('Boxe Sparring', 'Combat d'entraînement boxe', '2026-04-27 16:00:00', '2026-04-27 17:00:00', 3, 28.0, 1, NOW(), 5, 8),
('Functional Training', 'Entraînement fonctionnel global', '2026-04-28 06:00:00', '2026-04-28 07:30:00', 10, 22.0, 1, NOW(), 5, 9),

-- Wellness Center (ID: 6) - Sessions bien-être
('Méditation Guidée', 'Méditation de pleine conscience', '2026-04-27 08:00:00', '2026-04-27 08:30:00', 20, 15.0, 1, NOW(), 6, 1),
('Pilates Thérapie', 'Pilates thérapeutique individuel', '2026-04-27 09:00:00', '2026-04-27 10:00:00', 1, 30.0, 1, NOW(), 6, 2),
('Yoga Restauratif', 'Yoga pour récupération et souplesse', '2026-04-27 10:30:00', '2026-04-27 11:30:00', 8, 18.0, 1, NOW(), 6, 3),
('Massage Bien-être', 'Massage relaxant et thérapeutique', '2026-04-27 11:00:00', '2026-04-27 12:00:00', 2, 60.0, 1, NOW(), 6, 4),
('Qi Gong', 'Exercices énergétiques traditionnels', '2026-04-27 12:30:00', '2026-04-27 13:00:00', 15, 20.0, 1, NOW(), 6, 5),
('Stretching Actif', 'Étirements dynamiques et souplesse', '2026-04-27 13:00:00', '2026-04-27 13:45:00', 12, 12.0, 1, NOW(), 6, 6),
('Aquabike Intense', 'Cyclisme aquatique haute intensité', '2026-04-27 14:00:00', '2026-04-27 14:30:00', 8, 18.0, 1, NOW(), 6, 7),
('Yoga Nidra', 'Yoga nidra relaxation profonde', '2026-04-27 18:00:00', '2026-04-27 19:00:00', 25, 10.0, 1, NOW(), 6, 8),

-- Dance Studio (ID: 7) - Sessions de danse
('Hip Hop Beginner', 'Initiation hip hop pour débutants', '2026-04-27 17:00:00', '2026-04-27 18:00:00', 20, 15.0, 1, NOW(), 7, 1),
('Salsa Latino', 'Salsa latine passionnée', '2026-04-27 18:30:00', '2026-04-27 20:00:00', 16, 18.0, 1, NOW(), 7, 2),
('Tango Argentin', 'Tango argentin pour débutants', '2026-04-28 19:00:00', '2026-04-28 20:30:00', 12, 20.0, 1, NOW(), 7, 3),
('Danse Moderne', 'Danse contemporaine expressive', '2026-04-29 18:00:00', '2026-04-29 19:30:00', 18, 16.0, 1, NOW(), 7, 4),
('Bachata Romantique', 'Bachata romantique pour couples', '2026-04-29 20:30:00', '2026-04-29 21:30:00', 10, 18.0, 1, NOW(), 7, 5),
('Street Dance', 'Danse urbaine et freestyle', '2026-04-30 16:00:00', '2026-04-30 17:30:00', 25, 12.0, 1, NOW(), 7, 6),
('Danse Jazz', 'Initiation au jazz et improvisation', '2026-04-30 18:00:00', '2026-04-30 19:00:00', 15, 17.0, 1, NOW(), 7, 7),
('Danse Contemporaine', 'Danse moderne et créative', '2026-04-30 19:30:00', '2026-04-30 21:00:00', 12, 16.0, 1, NOW(), 7, 8),

-- Fight Club (ID: 8) - Sessions combat
('Boxe Technique', 'Techniques de boxe anglaise', '2026-04-27 19:00:00', '2026-04-27 20:30:00', 8, 25.0, 1, NOW(), 8, 1),
('Muay Thai Tradition', 'Muay Thai traditionnel et authentique', '2026-04-27 21:00:00', '2026-04-27 22:30:00', 6, 30.0, 1, NOW(), 8, 2),
('Karate Shotokan', 'Karaté style Shotokan', '2026-04-28 18:00:00', '2026-04-28 19:30:00', 10, 35.0, 1, NOW(), 8, 3),
('Jiu Jitsu Brésilien', 'Self-défense Jiu Jitsu', '2026-04-28 20:00:00', '2026-04-28 21:30:00', 7, 32.0, 1, NOW(), 8, 4),
('Krav Maga Civil', 'Auto-défense situationnelle', '2026-04-29 17:00:00', '2026-04-29 18:00:00', 5, 40.0, 1, NOW(), 8, 5),
('Mixed Martial Arts', 'Combinaison arts martiaux', '2026-04-29 18:30:00', '2026-04-29 20:00:00', 4, 38.0, 1, NOW(), 8, 6),
('Combat Conditioning', 'Conditionnement physique pour combat', '2026-04-30 06:00:00', '2026-04-30 07:30:00', 6, 28.0, 1, NOW(), 8, 7),
('Sparring Session', 'Session d'entraînement combat', '2026-04-30 19:00:00', '2026-04-30 20:00:00', 3, 25.0, 1, NOW(), 8, 8),

-- Cardio Blast (ID: 9) - Sessions cardio
('HIIT Supreme', 'HIIT très haute intensité', '2026-04-27 06:00:00', '2026-04-27 07:00:00', 5, 28.0, 1, NOW(), 9, 1),
('Circuit Cardio', 'Circuit cardio avec plusieurs stations', '2026-04-27 07:30:00', '2026-04-27 08:30:00', 12, 20.0, 1, NOW(), 9, 2),
('Running Group', 'Course à pied en groupe', '2026-04-27 09:00:00', '2026-04-27 10:00:00', 15, 12.0, 1, NOW(), 9, 3),
('Step Aerobic', 'Chorégraphies step avec musique', '2026-04-27 10:30:00', '2026-04-27 11:00:00', 20, 10.0, 1, NOW(), 9, 4),
('Kickboxing Cardio', 'Kickboxing avec cardio', '2026-04-27 11:30:00', '2026-04-27 12:30:00', 8, 22.0, 1, NOW(), 9, 5),
('Rowing Machine', 'Rameur intérieur et ergonomique', '2026-04-27 13:00:00', '2026-04-27 14:00:00', 10, 18.0, 1, NOW(), 9, 6),
('Climbing Cardio', 'Escalade cardio et endurance', '2026-04-27 14:30:00', '2026-04-27 16:00:00', 6, 25.0, 1, NOW(), 9, 7),
('Swim Fitness', 'Natation sportive et endurance', '2026-04-27 16:30:00', '2026-04-27 17:30:00', 8, 20.0, 1, NOW(), 9, 8),
('Aerobic Mix', 'Combinaison aérobic varié', '2026-04-27 18:00:00', '2026-04-27 19:00:00', 25, 14.0, 1, NOW(), 9, 9),

-- Muscle Factory (ID: 10) - Sessions musculation
('Power Building', 'Prise de masse et force', '2026-04-27 05:30:00', '2026-04-27 07:00:00', 6, 35.0, 1, NOW(), 10, 1),
('Body Sculpt', 'Modelage du corps et définition', '2026-04-27 07:00:00', '2026-04-27 08:30:00', 8, 30.0, 1, NOW(), 10, 2),
('CrossFit Strength', 'CrossFit spécial force', '2026-04-27 09:00:00', '2026-04-27 10:00:00', 5, 32.0, 1, NOW(), 10, 3),
('Olympic Lifting', 'Haltérophilie olympique', '2026-04-27 10:30:00', '2026-04-27 12:00:00', 4, 40.0, 1, NOW(), 10, 4),
('Deadlift Technique', 'Technique du deadlift', '2026-04-27 12:00:00', '2026-04-27 13:30:00', 3, 45.0, 1, NOW(), 10, 5),
('Bench Press Power', 'Développement pectoral intense', '2026-04-27 14:00:00', '2026-04-27 15:15:00', 4, 38.0, 1, NOW(), 10, 6),
('Squat Challenge', 'Squat avec surcharge progressive', '2026-04-27 15:30:00', '2026-04-27 16:45:00', 3, 42.0, 1, NOW(), 10, 7),
('Arm Day Focus', 'Entraînement bras spécialisé', '2026-04-27 17:00:00', '2026-04-27 18:00:00', 6, 28.0, 1, NOW(), 10, 8),
('Core Crusher', 'Gainage abdominal intensif', '2026-04-27 18:30:00', '2026-04-27 19:00:00', 5, 32.0, 1, NOW(), 10, 9),
('Leg Day Friday', 'Entraînement jambes intense', '2026-04-28 05:00:00', '2026-04-27 06:30:00', 4, 35.0, 1, NOW(), 10, 10);

-- Afficher le résumé des sessions créées
SELECT 
    g.name as salle_nom,
    COUNT(*) as nombre_sessions,
    MIN(ts.price) as prix_min,
    MAX(ts.price) as prix_max
FROM training_sessions ts
JOIN gymnasia g ON ts.gymnasium_id = g.id
WHERE ts.gymnasium_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
GROUP BY g.name
ORDER BY g.name;
