-- OXYN Database Schema
-- This script creates all necessary tables for the OXYN application

-- Ensure database is selected (you may need to create it first)
-- CREATE DATABASE IF NOT EXISTS oxyn CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- ========== EVENTS TABLE ==========
CREATE TABLE IF NOT EXISTS evenements (
    id_evenement INT AUTO_INCREMENT PRIMARY KEY,
    titre_evenement VARCHAR(255) NOT NULL,
    description_evenement LONGTEXT,
    lieu_evenement VARCHAR(255),
    ville_evenement VARCHAR(255),
    places_max_evenement INT DEFAULT 1,
    statut_evenement VARCHAR(50),
    debut_evenement DATETIME,
    fin_evenement DATETIME,
    created_at_evenement TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_titre (titre_evenement),
    INDEX idx_statut (statut_evenement),
    INDEX idx_created_at (created_at_evenement DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ========== INSCRIPTIONS TABLE ==========
CREATE TABLE IF NOT EXISTS inscriptions_evenement (
    id_inscription INT AUTO_INCREMENT PRIMARY KEY,
    id_user_inscription INT NOT NULL,
    id_evenement_inscription INT NOT NULL,
    date_inscription_inscription TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut_inscription VARCHAR(50),
    FOREIGN KEY (id_evenement_inscription) REFERENCES evenements(id_evenement) ON DELETE CASCADE,
    INDEX idx_evenement (id_evenement_inscription),
    INDEX idx_user (id_user_inscription),
    INDEX idx_statut (statut_inscription)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ========== EVENT NOTIFICATIONS TABLE ==========
CREATE TABLE IF NOT EXISTS event_notifications (
    id_notification INT AUTO_INCREMENT PRIMARY KEY,
    id_user_notification INT NOT NULL,
    id_evenement_notification INT NOT NULL,
    titre_notification VARCHAR(255) NOT NULL,
    message_notification VARCHAR(1000) NOT NULL,
    lu_notification TINYINT(1) NOT NULL DEFAULT 0,
    created_at_notification TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notification_user (id_user_notification),
    INDEX idx_notification_event (id_evenement_notification),
    INDEX idx_notification_unread (id_user_notification, lu_notification),
    FOREIGN KEY (id_evenement_notification) REFERENCES evenements(id_evenement) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ========== REVIEWS/AVIS TABLE ==========
CREATE TABLE IF NOT EXISTS avis_evenement (
    id_note_avis_evenement INT AUTO_INCREMENT PRIMARY KEY,
    note_avis_evenement SMALLINT NOT NULL DEFAULT 0,
    commentaire_avis_evenement VARCHAR(500),
    created_at_avis_evenement DATETIME NOT NULL,
    id_evenement_avis_evenement_id INT NOT NULL,
    id_user_avis_evenement_id INT NOT NULL,
    INDEX idx_evenement (id_evenement_avis_evenement_id),
    INDEX idx_user (id_user_avis_evenement_id),
    INDEX idx_note (note_avis_evenement),
    INDEX idx_created_at (created_at_avis_evenement DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ========== SALLES TABLE (if needed) ==========
CREATE TABLE IF NOT EXISTS salles (
    id_salle INT AUTO_INCREMENT PRIMARY KEY,
    nom_salle VARCHAR(255) NOT NULL,
    capacite_salle INT DEFAULT 1,
    created_at_salle TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_nom (nom_salle)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ========== SESSIONS TABLE (if needed) ==========
CREATE TABLE IF NOT EXISTS sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    activite VARCHAR(255),
    coach_nom VARCHAR(255),
    date_session DATE,
    heure_debut TIME,
    capacite INT DEFAULT 1,
    places_restantes INT DEFAULT 1,
    statut VARCHAR(50),
    salle_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_date (date_session),
    INDEX idx_statut (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
