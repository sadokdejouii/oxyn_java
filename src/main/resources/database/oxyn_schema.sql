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

-- ========== GOOGLE CALENDAR TOKENS TABLE ==========
CREATE TABLE IF NOT EXISTS google_calendar_user_tokens (
    id_google_token INT AUTO_INCREMENT PRIMARY KEY,
    id_user_google_token INT NOT NULL,
    access_token_google_token LONGTEXT NOT NULL,
    refresh_token_google_token LONGTEXT,
    access_token_expires_at_google_token DATETIME,
    scope_google_token VARCHAR(500),
    token_type_google_token VARCHAR(50),
    created_at_google_token TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at_google_token TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uniq_google_token_user (id_user_google_token),
    INDEX idx_google_token_user (id_user_google_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ========== GOOGLE CALENDAR EVENT LINKS TABLE ==========
CREATE TABLE IF NOT EXISTS google_calendar_event_links (
    id_google_event_link INT AUTO_INCREMENT PRIMARY KEY,
    id_inscription_google_link INT NOT NULL,
    id_user_google_link INT NOT NULL,
    id_evenement_google_link INT NOT NULL,
    google_calendar_id VARCHAR(255) NOT NULL DEFAULT 'primary',
    google_event_id VARCHAR(255) NOT NULL,
    sync_status_google_link VARCHAR(50) NOT NULL DEFAULT 'synced',
    created_at_google_link TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at_google_link TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uniq_google_link_inscription (id_inscription_google_link),
    INDEX idx_google_link_user (id_user_google_link),
    INDEX idx_google_link_evenement (id_evenement_google_link)
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
