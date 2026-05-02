-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1:3307
-- Généré le : sam. 02 mai 2026 à 01:30
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.1.25

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `oxyn`
--

-- --------------------------------------------------------

--
-- Structure de la table `avis_evenement`
--

CREATE TABLE `avis_evenement` (
  `id_note_avis_evenement` int(11) NOT NULL,
  `id_evenement_avis_evenement` int(11) NOT NULL,
  `id_user_avis_evenement` int(11) NOT NULL,
  `note_avis_evenement` smallint(6) NOT NULL,
  `commentaire_avis_evenement` varchar(500) DEFAULT NULL,
  `created_at_avis_evenement` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `avis_evenement`
--

INSERT INTO `avis_evenement` (`id_note_avis_evenement`, `id_evenement_avis_evenement`, `id_user_avis_evenement`, `note_avis_evenement`, `commentaire_avis_evenement`, `created_at_avis_evenement`) VALUES
(7, 4, 1, 5, 'TOO Great ', '2026-02-10 10:40:13'),
(9, 6, 1, 5, 'very good', '2026-02-10 12:14:16'),
(10, 6, 9, 4, 'veryyyyy goood', '2026-02-10 12:15:43');

-- --------------------------------------------------------

--
-- Structure de la table `commandes`
--

CREATE TABLE `commandes` (
  `id_commande` int(11) NOT NULL,
  `date_commande` date NOT NULL,
  `total_commande` decimal(10,2) NOT NULL,
  `statut_commande` varchar(30) NOT NULL,
  `mode_paiement_commande` varchar(50) DEFAULT NULL,
  `id_client_commande` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `commandes`
--

INSERT INTO `commandes` (`id_commande`, `date_commande`, `total_commande`, `statut_commande`, `mode_paiement_commande`, `id_client_commande`) VALUES
(1, '2026-02-09', 120.00, 'AnnulÃ©e', NULL, 4),
(3, '2026-02-09', 40.00, 'AnnulÃ©e', NULL, 4),
(6, '2026-02-09', 80.00, 'AnnulÃ©e', NULL, 7),
(7, '2026-02-10', 40.00, 'En attente', NULL, 4),
(8, '2026-02-10', 40.00, 'AnnulÃ©e', NULL, 7),
(9, '2026-02-10', 40.00, 'AnnulÃ©e', NULL, 7),
(10, '2026-02-10', 200.00, 'AnnulÃ©e', NULL, 7),
(11, '2026-02-10', 280.00, 'En attente', NULL, 4),
(12, '2026-02-10', 120.00, 'AnnulÃ©e', NULL, 9),
(13, '2026-02-12', 40.00, 'En attente', NULL, 1);

-- --------------------------------------------------------

--
-- Structure de la table `comment`
--

CREATE TABLE `comment` (
  `id_comment` int(11) NOT NULL,
  `content_comment` longtext NOT NULL,
  `created_at_comment` datetime NOT NULL,
  `updated_at_comment` datetime DEFAULT NULL,
  `like_count` int(11) DEFAULT NULL,
  `is_edited` tinyint(1) DEFAULT NULL,
  `id_author_comment` int(11) NOT NULL,
  `post_id` int(11) DEFAULT NULL,
  `parent_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `comment`
--

INSERT INTO `comment` (`id_comment`, `content_comment`, `created_at_comment`, `updated_at_comment`, `like_count`, `is_edited`, `id_author_comment`, `post_id`, `parent_id`) VALUES
(3, 'Super sÃšance! Je vais essayer Ã¾a demain matin ', '2026-02-09 13:13:39', '2026-02-09 13:13:39', 12, 0, 3, 5, NULL),
(4, 'Bravo! Continue comme Ã¾a, tu vas voir des rÃšsultats incroyables! ', '2026-02-09 13:03:39', '2026-02-09 13:03:39', 3, 0, 3, 5, NULL),
(17, 'testest', '2026-04-12 16:12:55', '2026-04-13 14:20:53', 0, 1, 1, 5, NULL),
(19, 'creeunpost', '2026-04-13 14:28:15', '2026-04-13 14:28:15', 0, 0, 1, 5, 4),
(23, 'dqzdqzdqz', '2026-04-26 19:41:13', '2026-04-26 19:41:13', 0, 0, 11, 40, NULL),
(26, 'dazqdq', '2026-04-26 20:20:30', '2026-04-26 20:20:30', 0, 0, 11, 38, NULL),
(28, 'hey', '2026-04-26 20:20:45', '2026-04-26 20:20:45', 0, 0, 11, 14, NULL),
(29, 'hey', '2026-04-26 20:20:52', '2026-04-26 20:20:52', 0, 0, 11, 30, NULL),
(30, 'hey', '2026-04-26 21:32:06', '2026-04-26 21:32:06', 5, 0, 11, 37, NULL),
(31, 'test', '2026-04-26 21:32:12', '2026-04-26 21:32:12', 1, 0, 11, 38, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `comment_likes`
--

CREATE TABLE `comment_likes` (
  `id` int(11) NOT NULL,
  `comment_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `created_at` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `conversations`
--

CREATE TABLE `conversations` (
  `id` int(11) NOT NULL,
  `is_active` tinyint(1) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `client_id` int(11) NOT NULL,
  `encadrant_id` int(11) DEFAULT NULL,
  `clientTypingAt` datetime DEFAULT NULL,
  `encadrantTypingAt` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `conversations`
--

INSERT INTO `conversations` (`id`, `is_active`, `created_at`, `updated_at`, `client_id`, `encadrant_id`, `clientTypingAt`, `encadrantTypingAt`) VALUES
(1, 1, '2026-02-08 13:44:17', '2026-02-08 13:44:22', 1, NULL, NULL, NULL),
(2, 1, '2026-02-08 14:55:52', '2026-02-09 00:45:41', 4, 2, NULL, NULL),
(3, 1, '2026-02-09 22:41:35', '2026-02-10 02:36:50', 7, 2, NULL, NULL),
(4, 1, '2026-02-10 13:12:42', '2026-02-10 13:14:23', 9, 4, NULL, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `doctrine_migration_versions`
--

CREATE TABLE `doctrine_migration_versions` (
  `version` varchar(191) NOT NULL,
  `executed_at` datetime DEFAULT NULL,
  `execution_time` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `doctrine_migration_versions`
--

INSERT INTO `doctrine_migration_versions` (`version`, `executed_at`, `execution_time`) VALUES
('DoctrineMigrations\\Version20260205171624', '2026-02-08 05:15:39', 327),
('DoctrineMigrations\\Version20260205181711', '2026-02-08 05:15:39', 51),
('DoctrineMigrations\\Version20260205200832', '2026-02-08 05:15:39', 34),
('DoctrineMigrations\\Version20260205203245', '2026-02-08 05:15:39', 48),
('DoctrineMigrations\\Version20260205212937', '2026-02-08 05:15:40', 50),
('DoctrineMigrations\\Version20260205232246', '2026-02-08 05:15:40', 169),
('DoctrineMigrations\\Version20260206001648', '2026-02-08 05:15:40', 362),
('DoctrineMigrations\\Version20260206002749', '2026-02-08 05:15:40', 9),
('DoctrineMigrations\\Version20260206004351', '2026-02-08 05:15:40', 117),
('DoctrineMigrations\\Version20260206011348', '2026-02-08 20:26:03', 350),
('DoctrineMigrations\\Version20260206131038', '2026-02-08 20:26:03', 172),
('DoctrineMigrations\\Version20260206160000', '2026-02-08 20:26:04', 5),
('DoctrineMigrations\\Version20260206175704', '2026-02-08 20:26:04', 108),
('DoctrineMigrations\\Version20260206180057', '2026-02-08 20:26:04', 159),
('DoctrineMigrations\\Version20260206193000', '2026-02-08 20:26:04', 103),
('DoctrineMigrations\\Version20260206233817', '2026-02-08 05:15:40', 89),
('DoctrineMigrations\\Version20260207120217', '2026-02-08 05:15:40', 4),
('DoctrineMigrations\\Version20260207221834', '2026-02-08 05:15:40', 3),
('DoctrineMigrations\\Version20260208014000', NULL, NULL),
('DoctrineMigrations\\Version20260208040019', NULL, NULL),
('DoctrineMigrations\\Version20260208043000', NULL, NULL),
('DoctrineMigrations\\Version20260208044500', '2026-02-08 05:17:51', 130),
('DoctrineMigrations\\Version20260208100000', '2026-02-08 23:21:46', 176),
('DoctrineMigrations\\Version20260209010312', '2026-02-09 02:03:55', 745),
('DoctrineMigrations\\Version20260209114509', '2026-02-09 12:45:17', 216),
('DoctrineMigrations\\Version20260209120000', '2026-02-09 02:03:56', 5),
('DoctrineMigrations\\Version20260209152341', '2026-02-09 16:23:50', 381),
('DoctrineMigrations\\Version20260209180000', '2026-02-09 21:17:25', 65),
('DoctrineMigrations\\Version20260209192000', '2026-02-10 00:43:49', 149),
('DoctrineMigrations\\Version20260210100000', '2026-02-10 00:43:49', 87),
('DoctrineMigrations\\Version20260213190000', '2026-03-02 01:45:33', 179),
('DoctrineMigrations\\Version20260214121000', '2026-03-02 01:45:33', 89),
('DoctrineMigrations\\Version20260214150000', '2026-03-02 01:45:34', 67),
('DoctrineMigrations\\Version20260214162000', '2026-03-02 01:45:34', 123);

-- --------------------------------------------------------

--
-- Structure de la table `equipments`
--

CREATE TABLE `equipments` (
  `id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` longtext DEFAULT NULL,
  `quantity` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `gymnasium_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `evenements`
--

CREATE TABLE `evenements` (
  `id_evenement` int(11) NOT NULL,
  `created_by_evenement` int(11) NOT NULL,
  `titre_evenement` varchar(150) NOT NULL,
  `description_evenement` longtext DEFAULT NULL,
  `date_debut_evenement` datetime NOT NULL,
  `date_fin_evenement` datetime NOT NULL,
  `lieu_evenement` varchar(150) NOT NULL,
  `ville_evenement` varchar(100) DEFAULT NULL,
  `places_max_evenement` int(11) NOT NULL,
  `statut_evenement` varchar(30) NOT NULL,
  `created_at_evenement` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `evenements`
--

INSERT INTO `evenements` (`id_evenement`, `created_by_evenement`, `titre_evenement`, `description_evenement`, `date_debut_evenement`, `date_fin_evenement`, `lieu_evenement`, `ville_evenement`, `places_max_evenement`, `statut_evenement`, `created_at_evenement`) VALUES
(4, 1, 'Circuit', 'emoji', '2026-02-10 22:24:00', '2026-05-15 22:24:00', 'tunis', 'tunsiia', 7, 'En cours', '2026-02-09 22:25:29'),
(5, 1, 'marathon', NULL, '2026-02-12 08:01:00', '2026-02-12 12:39:00', 'sidi bou said', 'tunis', 200, 'Terminée', '2026-02-10 04:40:23'),
(6, 1, 'VTT', NULL, '2026-02-20 04:40:00', '2026-02-20 09:41:00', 'Hammamet', 'nabeul', 120, 'TerminÃ©', '2026-02-10 04:41:34'),
(8, 1, 'sadek', NULL, '2026-02-05 12:10:00', '2026-02-19 12:10:00', 'tunissia', 'tunis', 20, 'Terminée', '2026-02-10 12:11:06');

-- --------------------------------------------------------

--
-- Structure de la table `fiche_sante`
--

CREATE TABLE `fiche_sante` (
  `id` int(11) NOT NULL,
  `genre` varchar(10) NOT NULL,
  `age` int(11) DEFAULT NULL,
  `taille` int(11) DEFAULT NULL,
  `poids` double DEFAULT NULL,
  `objectif` varchar(50) DEFAULT NULL,
  `niveau_activite` varchar(50) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `fiche_sante`
--

INSERT INTO `fiche_sante` (`id`, `genre`, `age`, `taille`, `poids`, `objectif`, `niveau_activite`, `created_at`, `updated_at`, `user_id`) VALUES
(1, 'M', 30, 180, 50, 'gain_poids', 'peu_actif', '2026-02-08 21:05:31', '2026-02-08 23:29:52', 4),
(2, 'M', 40, 187, 50, 'gain_poids', 'tres_actif', '2026-02-10 01:29:34', '2026-02-10 01:57:56', 7),
(3, 'M', 16, 170, 90, 'perte_poids', 'peu_actif', '2026-02-10 13:10:29', '2026-02-10 13:11:37', 9);

-- --------------------------------------------------------

--
-- Structure de la table `gymnasia`
--

CREATE TABLE `gymnasia` (
  `id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` longtext DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `google_maps_url` varchar(500) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `rating` double DEFAULT NULL,
  `rating_count` int(11) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `gymnasia`
--

INSERT INTO `gymnasia` (`id`, `name`, `description`, `address`, `google_maps_url`, `phone`, `email`, `image_url`, `rating`, `rating_count`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 'california gymm', 'lheLKRirhqghqir', 'centre urbain nord', 'https://maps.app.goo.gl/8zMzyb7Zo2joXoQm7', '53275207', 'merhbene.mohamed@esprit.tn', 'https://images.unsplash.com/photo-1545205597-3d9d02c29597?w=800', 3, 3, 1, '2026-02-09 16:46:14', '2026-02-09 19:10:04'),
(3, 'BLUE', NULL, 'hammamet, av habib bourguiba', 'https://maps.app.goo.gl/xepQBYTzZs5doTyQ6', '53275207', 'SYRR@esprit.tn', 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48', NULL, 0, 1, '2026-02-09 22:36:31', '2026-02-10 09:45:01'),
(4, 'factory', NULL, 'sfax', 'https://maps.app.goo.gl/xepQBYTzZs5doTyQ6', '28000099', 'merhbene.mohamed@esprit.tn', 'https://lh3.googleusercontent.com/gps-cs-s/AHVAweois3qNfFK3R-feDJdV8y7aGHRQi59rZfUccWQ4m0fdMHu5V9ouwW1cox2-OaWHLS0iJISYfIZawKi9J9hRKxtkth0tDw-EkC5svtcfQMlEVXuWN35EXuM21qr9C0WJxlKPsDQaOA=w408-h408-k-no', 2, 1, 1, '2026-02-10 03:31:57', '2026-02-10 03:31:57'),
(5, 'factro', NULL, 'hammamet, av habib bourguiba', 'https://maps.app.goo.gl/xepQBYTzZs5doTyQ6', '28000099', 'merhbenmerhben@gmail.com', 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48', NULL, 0, 1, '2026-02-10 12:56:10', '2026-02-10 12:56:26');

-- --------------------------------------------------------

--
-- Structure de la table `gym_ratings`
--

CREATE TABLE `gym_ratings` (
  `id` int(11) NOT NULL,
  `rating` double NOT NULL,
  `comment` longtext DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `gymnasium_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `gym_ratings`
--

INSERT INTO `gym_ratings` (`id`, `rating`, `comment`, `created_at`, `user_id`, `gymnasium_id`) VALUES
(1, 1, 'tayara', '2026-02-09 19:00:30', 4, 1),
(2, 4, 'aaaaa', '2026-02-09 22:31:00', 1, 1),
(3, 2, NULL, '2026-02-10 10:45:07', 1, 4),
(4, 4, NULL, '2026-02-10 13:01:55', 7, 1);

-- --------------------------------------------------------

--
-- Structure de la table `gym_subscription_offers`
--

CREATE TABLE `gym_subscription_offers` (
  `id` int(11) NOT NULL,
  `gymnasium_id` int(11) DEFAULT NULL,
  `name` varchar(150) DEFAULT NULL,
  `duration_months` int(11) DEFAULT NULL,
  `price` decimal(10,2) DEFAULT NULL,
  `description` longtext DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `gym_subscription_offers`
--

INSERT INTO `gym_subscription_offers` (`id`, `gymnasium_id`, `name`, `duration_months`, `price`, `description`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 1, 'OFFRE ETUDIANT', 3, 200.00, NULL, 1, '2026-02-09 21:22:01', '2026-02-09 21:22:01'),
(2, 1, 'offr', 1, 50.00, 'bienvenu ...', 1, '2026-02-09 22:35:07', '2026-02-10 12:57:46');

-- --------------------------------------------------------

--
-- Structure de la table `gym_subscription_orders`
--

CREATE TABLE `gym_subscription_orders` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `offer_id` int(11) NOT NULL,
  `quantity` int(11) NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `total_price` decimal(10,2) NOT NULL,
  `status` varchar(50) NOT NULL,
  `created_at` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `gym_subscription_orders`
--

INSERT INTO `gym_subscription_orders` (`id`, `user_id`, `offer_id`, `quantity`, `unit_price`, `total_price`, `status`, `created_at`) VALUES
(1, 4, 2, 1, 50.00, 50.00, 'En attente', '2026-02-10 03:07:13');

-- --------------------------------------------------------

--
-- Structure de la table `inscription_evenement`
--

CREATE TABLE `inscription_evenement` (
  `id_inscription_inscription_evenement` int(11) NOT NULL,
  `id_evenement_inscription_evenemen` int(11) NOT NULL,
  `id_user_inscription_evenemen` int(11) NOT NULL,
  `date_inscription_evenemen` datetime NOT NULL,
  `statut_inscription_evenemen` varchar(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `inscription_evenement`
--

INSERT INTO `inscription_evenement` (`id_inscription_inscription_evenement`, `id_evenement_inscription_evenemen`, `id_user_inscription_evenemen`, `date_inscription_evenemen`, `statut_inscription_evenemen`) VALUES
(4, 5, 4, '2026-02-10 10:35:40', 'Inscrit'),
(5, 4, 4, '2026-02-10 10:35:53', 'Inscrit'),
(6, 6, 4, '2026-02-10 10:35:57', 'AnnulÃ©'),
(7, 5, 1, '2026-02-10 10:39:20', 'Inscrit'),
(8, 8, 1, '2026-02-10 12:11:35', 'Inscrit'),
(9, 8, 9, '2026-02-10 12:15:29', 'Inscrit');

-- --------------------------------------------------------

--
-- Structure de la table `ligne_commande`
--

CREATE TABLE `ligne_commande` (
  `id_ligne_ligne_commande` int(11) NOT NULL,
  `quantite_ligne_commande` int(11) NOT NULL,
  `prix_unitaire_ligne_commande` decimal(10,2) NOT NULL,
  `sous_total_ligne_commande` decimal(10,2) NOT NULL,
  `id_commande_ligne_commande` int(11) NOT NULL,
  `id_produit_ligne_commande` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `ligne_commande`
--

INSERT INTO `ligne_commande` (`id_ligne_ligne_commande`, `quantite_ligne_commande`, `prix_unitaire_ligne_commande`, `sous_total_ligne_commande`, `id_commande_ligne_commande`, `id_produit_ligne_commande`) VALUES
(1, 3, 40.00, 120.00, 1, 1),
(3, 1, 40.00, 40.00, 3, 1),
(6, 1, 40.00, 40.00, 6, 2),
(7, 1, 40.00, 40.00, 6, 1),
(8, 1, 40.00, 40.00, 7, 1),
(9, 1, 40.00, 40.00, 8, 1),
(10, 1, 40.00, 40.00, 9, 1),
(11, 5, 40.00, 200.00, 10, 1),
(12, 7, 40.00, 280.00, 11, 1),
(13, 3, 40.00, 120.00, 12, 1),
(14, 1, 40.00, 40.00, 13, 1);

-- --------------------------------------------------------

--
-- Structure de la table `messages`
--

CREATE TABLE `messages` (
  `id` int(11) NOT NULL,
  `contenu` longtext NOT NULL,
  `type` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `ai_suggested_content` longtext DEFAULT NULL,
  `ai_suggestion_status` varchar(20) DEFAULT NULL,
  `is_ai_generated` tinyint(1) NOT NULL,
  `conversation_id` int(11) NOT NULL,
  `sender_id` int(11) NOT NULL,
  `parent_message_id` int(11) DEFAULT NULL,
  `original_message_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `messages`
--

INSERT INTO `messages` (`id`, `contenu`, `type`, `created_at`, `ai_suggested_content`, `ai_suggestion_status`, `is_ai_generated`, `conversation_id`, `sender_id`, `parent_message_id`, `original_message_id`) VALUES
(1, 'hello', 'MESSAGE', '2026-02-08 13:44:22', NULL, NULL, 0, 1, 1, NULL, NULL),
(2, 'okey', 'MESSAGE', '2026-02-08 14:55:58', NULL, NULL, 0, 2, 4, NULL, NULL),
(3, '', 'IA_SUGGESTION', '2026-02-08 14:55:58', 'Bonjour fafa,\n\nMerci pour votre message. Pour vous apporter l\'aide la plus pertinente :\n\n**ðŸŽ¯ Aidez-moi Ã  vous aider :**\n\n**1. PrÃ©cisez votre demande**\n   - Sujet principal : Programme, Nutrition, Technique, RÃ©cupÃ©ration ?\n   - Y a-t-il un problÃ¨me spÃ©cifique ?\n\n**2. Donnez du contexte**\n   - Depuis combien de temps vous entraÃ®nez-vous ?\n   - Quel est votre objectif actuel ?\n\n**ðŸ’¡ Je peux vous aider sur :**\n   âœ… Conception programmes d\'entraÃ®nement\n   âœ… Conseils nutritionnels et timing\n   âœ… Techniques d\'exÃ©cution exercices\n   âœ… Gestion fatigue et rÃ©cupÃ©ration\n   âœ… StratÃ©gies progression et motivation\n   âœ… PrÃ©vention et gestion douleurs\n\nVotre encadrant analysera Ã©galement votre situation pour un suivi personnalisÃ©.', 'ACCEPTED', 1, 2, 2, NULL, 2),
(4, 'Bonjour fafa,\n\nMerci pour votre message. Pour vous apporter l\'aide la plus pertinente :\n\n**ðŸŽ¯ Aidez-moi Ã  vous aider :**\n\n**1. PrÃ©cisez votre demande**\n   - Sujet principal : Programme, Nutrition, Technique, RÃ©cupÃ©ration ?\n   - Y a-t-il un problÃ¨me spÃ©cifique ?\n\n**2. Donnez du contexte**\n   - Depuis combien de temps vous entraÃ®nez-vous ?\n   - Quel est votre objectif actuel ?\n\n**ðŸ’¡ Je peux vous aider sur :**\n   âœ… Conception programmes d\'entraÃ®nement\n   âœ… Conseils nutritionnels et timing\n   âœ… Techniques d\'exÃ©cution exercices\n   âœ… Gestion fatigue et rÃ©cupÃ©ration\n   âœ… StratÃ©gies progression et motivation\n   âœ… PrÃ©vention et gestion douleurs\n\nVotre encadrant analysera Ã©galement votre situation pour un suivi personnalisÃ©.', 'CONSEIL', '2026-02-08 15:05:48', NULL, NULL, 1, 2, 2, NULL, 2),
(5, '^hey j\'ai un blessure', 'MESSAGE', '2026-02-08 18:42:44', NULL, NULL, 0, 2, 4, NULL, NULL),
(7, 'okey', 'CONSEIL', '2026-02-08 18:45:04', NULL, NULL, 0, 2, 2, NULL, NULL),
(8, 'je veux plus de detaille pour le programme', 'MESSAGE', '2026-02-08 20:03:21', NULL, NULL, 0, 2, 4, NULL, NULL),
(10, 'jekjevkjBVMKJBQV', 'CONSEIL', '2026-02-08 20:04:09', NULL, NULL, 0, 2, 2, NULL, NULL),
(11, 'HEY', 'MESSAGE', '2026-02-08 21:05:46', NULL, NULL, 0, 2, 4, NULL, NULL),
(12, '', 'IA_SUGGESTION', '2026-02-08 21:05:46', 'Bonjour feriel,\n\nâš ï¸ Je remarque que ce problÃ¨me revient rÃ©guliÃ¨rement dans notre conversation.\n\n**Douleur rÃ©currente : Action nÃ©cessaire**\n\nUne douleur qui revient n\'est PAS normale.\n\n**Actions prioritaires :**\n1. ðŸ¥ **Consultation mÃ©dicale/kinÃ©** : ImpÃ©ratif\n2. ðŸ“Š **Analyse biomÃ©canique** : DÃ©sÃ©quilibres musculaires ?\n3. ðŸ”„ **Modification technique** : ExÃ©cution Ã  revoir\n4. ðŸ’ª **Renforcement prÃ©ventif** : Muscles stabilisateurs\n\nðŸŽ¯ Votre encadrant va prioriser ce sujet pour trouver une solution durable.', 'ACCEPTED', 1, 2, 2, NULL, 11),
(13, 'Bonjour feriel,\n\nâš ï¸ Je remarque que ce problÃ¨me revient rÃ©guliÃ¨rement dans notre conversation.\n\n**Douleur rÃ©currente : Action nÃ©cessaire**\n\nUne douleur qui revient n\'est PAS normale.\n\n**Actions prioritaires :**\n1. ðŸ¥ **Consultation mÃ©dicale/kinÃ©** : ImpÃ©ratif\n2. ðŸ“Š **Analyse biomÃ©canique** : DÃ©sÃ©quilibres musculaires ?\n3. ðŸ”„ **Modification technique** : ExÃ©cution Ã  revoir\n4. ðŸ’ª **Renforcement prÃ©ventif** : Muscles stabilisateurs\n\nðŸŽ¯ Votre encadrant va prioriser ce sujet pour trouver une solution durable.', 'CONSEIL', '2026-02-08 21:07:49', NULL, NULL, 1, 2, 2, NULL, 11),
(14, 'hey', 'MESSAGE', '2026-02-08 21:56:05', NULL, NULL, 0, 2, 4, NULL, NULL),
(16, 'oww', 'MESSAGE', '2026-02-08 23:30:09', NULL, NULL, 0, 2, 4, NULL, NULL),
(17, '', 'IA_SUGGESTION', '2026-02-08 23:30:09', 'Bonjour feriel,\n\nâš ï¸ Je remarque que ce problÃ¨me revient rÃ©guliÃ¨rement dans notre conversation.\n\n**Douleur rÃ©currente : Action nÃ©cessaire**\n\nUne douleur qui revient n\'est PAS normale.\n\n**Actions prioritaires :**\n1. ðŸ¥ **Consultation mÃ©dicale/kinÃ©** : ImpÃ©ratif\n2. ðŸ“Š **Analyse biomÃ©canique** : DÃ©sÃ©quilibres musculaires ?\n3. ðŸ”„ **Modification technique** : ExÃ©cution Ã  revoir\n4. ðŸ’ª **Renforcement prÃ©ventif** : Muscles stabilisateurs\n\nðŸŽ¯ Votre encadrant va prioriser ce sujet pour trouver une solution durable.', 'ACCEPTED', 1, 2, 2, NULL, 16),
(18, 'Bonjour feriel,\n\nâš ï¸ Je remarque que ce problÃ¨me revient rÃ©guliÃ¨rement dans notre conversation.\n\n**Douleur rÃ©currente : Action nÃ©cessaire**\n\nUne douleur qui revient n\'est PAS normale.\n\n**Actions prioritaires :**\n1. ðŸ¥ **Consultation mÃ©dicale/kinÃ©** : ImpÃ©ratif\n2. ðŸ“Š **Analyse biomÃ©canique** : DÃ©sÃ©quilibres musculaires ?\n3. ðŸ”„ **Modification technique** : ExÃ©cution Ã  revoir\n4. ðŸ’ª **Renforcement prÃ©ventif** : Muscles stabilisateurs\n\nðŸŽ¯ Votre encadrant va prioriser ce sujet pour trouver une solution durable.', 'CONSEIL', '2026-02-09 00:06:45', NULL, NULL, 1, 2, 2, NULL, 16),
(19, 'hola', 'MESSAGE', '2026-02-09 00:45:41', NULL, NULL, 0, 2, 4, NULL, NULL),
(20, '', 'IA_SUGGESTION', '2026-02-09 00:45:41', 'Bonjour feriel,\n\nâš ï¸ Je remarque que ce problÃ¨me revient rÃ©guliÃ¨rement dans notre conversation.\n\n**Douleur rÃ©currente : Action nÃ©cessaire**\n\nUne douleur qui revient n\'est PAS normale.\n\n**Actions prioritaires :**\n1. ðŸ¥ **Consultation mÃ©dicale/kinÃ©** : ImpÃ©ratif\n2. ðŸ“Š **Analyse biomÃ©canique** : DÃ©sÃ©quilibres musculaires ?\n3. ðŸ”„ **Modification technique** : ExÃ©cution Ã  revoir\n4. ðŸ’ª **Renforcement prÃ©ventif** : Muscles stabilisateurs\n\nðŸŽ¯ Votre encadrant va prioriser ce sujet pour trouver une solution durable.', 'PENDING', 1, 2, 2, NULL, 19),
(21, 'hello', 'MESSAGE', '2026-02-10 01:44:48', NULL, NULL, 0, 3, 7, NULL, NULL),
(23, 'hi', 'MESSAGE', '2026-02-10 01:44:54', NULL, NULL, 0, 3, 7, NULL, NULL),
(25, 'heyyy', 'MESSAGE', '2026-02-10 02:28:51', NULL, NULL, 0, 3, 7, NULL, NULL),
(27, 'Comment je peut t\'aider', 'CONSEIL', '2026-02-10 02:36:50', NULL, NULL, 0, 3, 4, NULL, NULL),
(28, 'HELLO , j\'ai un blessure et douleur', 'MESSAGE', '2026-02-10 13:12:59', NULL, NULL, 0, 4, 9, NULL, NULL),
(30, 'okey on va voir', 'CONSEIL', '2026-02-10 13:14:23', NULL, NULL, 0, 4, 4, NULL, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `messenger_messages`
--

CREATE TABLE `messenger_messages` (
  `id` bigint(20) NOT NULL,
  `body` longtext NOT NULL,
  `headers` longtext NOT NULL,
  `queue_name` varchar(190) NOT NULL,
  `created_at` datetime NOT NULL,
  `available_at` datetime NOT NULL,
  `delivered_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `messenger_messages`
--

INSERT INTO `messenger_messages` (`id`, `body`, `headers`, `queue_name`, `created_at`, `available_at`, `delivered_at`) VALUES
(1, 'O:36:\\\"Symfony\\\\Component\\\\Messenger\\\\Envelope\\\":2:{s:44:\\\"\\0Symfony\\\\Component\\\\Messenger\\\\Envelope\\0stamps\\\";a:1:{s:46:\\\"Symfony\\\\Component\\\\Messenger\\\\Stamp\\\\BusNameStamp\\\";a:1:{i:0;O:46:\\\"Symfony\\\\Component\\\\Messenger\\\\Stamp\\\\BusNameStamp\\\":1:{s:55:\\\"\\0Symfony\\\\Component\\\\Messenger\\\\Stamp\\\\BusNameStamp\\0busName\\\";s:21:\\\"messenger.bus.default\\\";}}}s:45:\\\"\\0Symfony\\\\Component\\\\Messenger\\\\Envelope\\0message\\\";O:51:\\\"Symfony\\\\Component\\\\Mailer\\\\Messenger\\\\SendEmailMessage\\\":2:{s:60:\\\"\\0Symfony\\\\Component\\\\Mailer\\\\Messenger\\\\SendEmailMessage\\0message\\\";O:28:\\\"Symfony\\\\Component\\\\Mime\\\\Email\\\":6:{i:0;N;i:1;N;i:2;s:2254:\\\"<!DOCTYPE html>\n<html>\n<head>\n    <meta charset=\\\"UTF-8\\\">\n    <style>\n        body {\n            font-family: Arial, sans-serif;\n            line-height: 1.6;\n            color: #333;\n        }\n        .container {\n            max-width: 600px;\n            margin: 0 auto;\n            padding: 20px;\n        }\n        .header {\n            background: #007bff;\n            color: white;\n            padding: 20px;\n            text-align: center;\n            border-radius: 8px 8px 0 0;\n        }\n        .content {\n            background: #f9f9f9;\n            padding: 30px;\n            border-radius: 0 0 8px 8px;\n        }\n        .button {\n            display: inline-block;\n            padding: 12px 24px;\n            background: #007bff;\n            color: white;\n            text-decoration: none;\n            border-radius: 6px;\n            margin: 20px 0;\n        }\n        .footer {\n            margin-top: 20px;\n            font-size: 12px;\n            color: #666;\n            text-align: center;\n        }\n    </style>\n</head>\n<body>\n    <div class=\\\"container\\\">\n        <div class=\\\"header\\\">\n            <h1>RÃ©initialisation de mot de passe</h1>\n        </div>\n        <div class=\\\"content\\\">\n            <p>Bonjour yacddd,</p>\n            \n            <p>Vous avez demandÃ© Ã  rÃ©initialiser votre mot de passe. Cliquez sur le bouton ci-dessous pour crÃ©er un nouveau mot de passe :</p>\n            \n            <p style=\\\"text-align: center;\\\">\n                <a href=\\\"http://127.0.0.1:8000/mot-de-passe/reinitialiser/1daee2b5a3903049f13c10aa1f58aebe7109d0747f24aa46773b8e7cac185a14\\\" class=\\\"button\\\">RÃ©initialiser mon mot de passe</a>\n            </p>\n            \n            <p>Ou copiez ce lien dans votre navigateur :</p>\n            <p style=\\\"word-break: break-all; color: #007bff;\\\">http://127.0.0.1:8000/mot-de-passe/reinitialiser/1daee2b5a3903049f13c10aa1f58aebe7109d0747f24aa46773b8e7cac185a14</p>\n            \n            <p><strong>Ce lien expirera dans 1 heure.</strong></p>\n            \n            <p>Si vous n\\\'avez pas demandÃ© cette rÃ©initialisation, ignorez simplement cet email.</p>\n        </div>\n        <div class=\\\"footer\\\">\n            <p>Â© 2026 OXYN. Tous droits rÃ©servÃ©s.</p>\n        </div>\n    </div>\n</body>\n</html>\n\\\";i:3;s:5:\\\"utf-8\\\";i:4;a:0:{}i:5;a:2:{i:0;O:37:\\\"Symfony\\\\Component\\\\Mime\\\\Header\\\\Headers\\\":2:{s:46:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\Headers\\0headers\\\";a:3:{s:4:\\\"from\\\";a:1:{i:0;O:47:\\\"Symfony\\\\Component\\\\Mime\\\\Header\\\\MailboxListHeader\\\":5:{s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0name\\\";s:4:\\\"From\\\";s:56:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lineLength\\\";i:76;s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lang\\\";N;s:53:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0charset\\\";s:5:\\\"utf-8\\\";s:58:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\MailboxListHeader\\0addresses\\\";a:1:{i:0;O:30:\\\"Symfony\\\\Component\\\\Mime\\\\Address\\\":2:{s:39:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Address\\0address\\\";s:16:\\\"noreply@oxyn.com\\\";s:36:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Address\\0name\\\";s:0:\\\"\\\";}}}}s:2:\\\"to\\\";a:1:{i:0;O:47:\\\"Symfony\\\\Component\\\\Mime\\\\Header\\\\MailboxListHeader\\\":5:{s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0name\\\";s:2:\\\"To\\\";s:56:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lineLength\\\";i:76;s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lang\\\";N;s:53:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0charset\\\";s:5:\\\"utf-8\\\";s:58:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\MailboxListHeader\\0addresses\\\";a:1:{i:0;O:30:\\\"Symfony\\\\Component\\\\Mime\\\\Address\\\":2:{s:39:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Address\\0address\\\";s:16:\\\"daikhi@gmail.com\\\";s:36:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Address\\0name\\\";s:0:\\\"\\\";}}}}s:7:\\\"subject\\\";a:1:{i:0;O:48:\\\"Symfony\\\\Component\\\\Mime\\\\Header\\\\UnstructuredHeader\\\":5:{s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0name\\\";s:7:\\\"Subject\\\";s:56:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lineLength\\\";i:76;s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lang\\\";N;s:53:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0charset\\\";s:5:\\\"utf-8\\\";s:55:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\UnstructuredHeader\\0value\\\";s:46:\\\"RÃ©initialisation de votre mot de passe - OXYN\\\";}}}s:49:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\Headers\\0lineLength\\\";i:76;}i:1;N;}}s:61:\\\"\\0Symfony\\\\Component\\\\Mailer\\\\Messenger\\\\SendEmailMessage\\0envelope\\\";N;}}', '[]', 'default', '2026-02-08 18:32:31', '2026-02-08 18:32:31', NULL),
(2, 'O:36:\\\"Symfony\\\\Component\\\\Messenger\\\\Envelope\\\":2:{s:44:\\\"\\0Symfony\\\\Component\\\\Messenger\\\\Envelope\\0stamps\\\";a:1:{s:46:\\\"Symfony\\\\Component\\\\Messenger\\\\Stamp\\\\BusNameStamp\\\";a:1:{i:0;O:46:\\\"Symfony\\\\Component\\\\Messenger\\\\Stamp\\\\BusNameStamp\\\":1:{s:55:\\\"\\0Symfony\\\\Component\\\\Messenger\\\\Stamp\\\\BusNameStamp\\0busName\\\";s:21:\\\"messenger.bus.default\\\";}}}s:45:\\\"\\0Symfony\\\\Component\\\\Messenger\\\\Envelope\\0message\\\";O:51:\\\"Symfony\\\\Component\\\\Mailer\\\\Messenger\\\\SendEmailMessage\\\":2:{s:60:\\\"\\0Symfony\\\\Component\\\\Mailer\\\\Messenger\\\\SendEmailMessage\\0message\\\";O:28:\\\"Symfony\\\\Component\\\\Mime\\\\Email\\\":6:{i:0;N;i:1;N;i:2;s:2254:\\\"<!DOCTYPE html>\n<html>\n<head>\n    <meta charset=\\\"UTF-8\\\">\n    <style>\n        body {\n            font-family: Arial, sans-serif;\n            line-height: 1.6;\n            color: #333;\n        }\n        .container {\n            max-width: 600px;\n            margin: 0 auto;\n            padding: 20px;\n        }\n        .header {\n            background: #007bff;\n            color: white;\n            padding: 20px;\n            text-align: center;\n            border-radius: 8px 8px 0 0;\n        }\n        .content {\n            background: #f9f9f9;\n            padding: 30px;\n            border-radius: 0 0 8px 8px;\n        }\n        .button {\n            display: inline-block;\n            padding: 12px 24px;\n            background: #007bff;\n            color: white;\n            text-decoration: none;\n            border-radius: 6px;\n            margin: 20px 0;\n        }\n        .footer {\n            margin-top: 20px;\n            font-size: 12px;\n            color: #666;\n            text-align: center;\n        }\n    </style>\n</head>\n<body>\n    <div class=\\\"container\\\">\n        <div class=\\\"header\\\">\n            <h1>RÃ©initialisation de mot de passe</h1>\n        </div>\n        <div class=\\\"content\\\">\n            <p>Bonjour tttttt,</p>\n            \n            <p>Vous avez demandÃ© Ã  rÃ©initialiser votre mot de passe. Cliquez sur le bouton ci-dessous pour crÃ©er un nouveau mot de passe :</p>\n            \n            <p style=\\\"text-align: center;\\\">\n                <a href=\\\"http://127.0.0.1:8000/mot-de-passe/reinitialiser/dfac98f43176c7ab28eb22d514969b46028fab1778071b3a964e474fc094e659\\\" class=\\\"button\\\">RÃ©initialiser mon mot de passe</a>\n            </p>\n            \n            <p>Ou copiez ce lien dans votre navigateur :</p>\n            <p style=\\\"word-break: break-all; color: #007bff;\\\">http://127.0.0.1:8000/mot-de-passe/reinitialiser/dfac98f43176c7ab28eb22d514969b46028fab1778071b3a964e474fc094e659</p>\n            \n            <p><strong>Ce lien expirera dans 1 heure.</strong></p>\n            \n            <p>Si vous n\\\'avez pas demandÃ© cette rÃ©initialisation, ignorez simplement cet email.</p>\n        </div>\n        <div class=\\\"footer\\\">\n            <p>Â© 2026 OXYN. Tous droits rÃ©servÃ©s.</p>\n        </div>\n    </div>\n</body>\n</html>\n\\\";i:3;s:5:\\\"utf-8\\\";i:4;a:0:{}i:5;a:2:{i:0;O:37:\\\"Symfony\\\\Component\\\\Mime\\\\Header\\\\Headers\\\":2:{s:46:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\Headers\\0headers\\\";a:3:{s:4:\\\"from\\\";a:1:{i:0;O:47:\\\"Symfony\\\\Component\\\\Mime\\\\Header\\\\MailboxListHeader\\\":5:{s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0name\\\";s:4:\\\"From\\\";s:56:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lineLength\\\";i:76;s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lang\\\";N;s:53:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0charset\\\";s:5:\\\"utf-8\\\";s:58:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\MailboxListHeader\\0addresses\\\";a:1:{i:0;O:30:\\\"Symfony\\\\Component\\\\Mime\\\\Address\\\":2:{s:39:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Address\\0address\\\";s:16:\\\"noreply@oxyn.com\\\";s:36:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Address\\0name\\\";s:0:\\\"\\\";}}}}s:2:\\\"to\\\";a:1:{i:0;O:47:\\\"Symfony\\\\Component\\\\Mime\\\\Header\\\\MailboxListHeader\\\":5:{s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0name\\\";s:2:\\\"To\\\";s:56:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lineLength\\\";i:76;s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lang\\\";N;s:53:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0charset\\\";s:5:\\\"utf-8\\\";s:58:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\MailboxListHeader\\0addresses\\\";a:1:{i:0;O:30:\\\"Symfony\\\\Component\\\\Mime\\\\Address\\\":2:{s:39:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Address\\0address\\\";s:18:\\\"daikhi11@gmail.com\\\";s:36:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Address\\0name\\\";s:0:\\\"\\\";}}}}s:7:\\\"subject\\\";a:1:{i:0;O:48:\\\"Symfony\\\\Component\\\\Mime\\\\Header\\\\UnstructuredHeader\\\":5:{s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0name\\\";s:7:\\\"Subject\\\";s:56:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lineLength\\\";i:76;s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lang\\\";N;s:53:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0charset\\\";s:5:\\\"utf-8\\\";s:55:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\UnstructuredHeader\\0value\\\";s:46:\\\"RÃ©initialisation de votre mot de passe - OXYN\\\";}}}s:49:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\Headers\\0lineLength\\\";i:76;}i:1;N;}}s:61:\\\"\\0Symfony\\\\Component\\\\Mailer\\\\Messenger\\\\SendEmailMessage\\0envelope\\\";N;}}', '[]', 'default', '2026-02-09 20:50:22', '2026-02-09 20:50:22', NULL),
(3, 'O:36:\\\"Symfony\\\\Component\\\\Messenger\\\\Envelope\\\":2:{s:44:\\\"\\0Symfony\\\\Component\\\\Messenger\\\\Envelope\\0stamps\\\";a:1:{s:46:\\\"Symfony\\\\Component\\\\Messenger\\\\Stamp\\\\BusNameStamp\\\";a:1:{i:0;O:46:\\\"Symfony\\\\Component\\\\Messenger\\\\Stamp\\\\BusNameStamp\\\":1:{s:55:\\\"\\0Symfony\\\\Component\\\\Messenger\\\\Stamp\\\\BusNameStamp\\0busName\\\";s:21:\\\"messenger.bus.default\\\";}}}s:45:\\\"\\0Symfony\\\\Component\\\\Messenger\\\\Envelope\\0message\\\";O:51:\\\"Symfony\\\\Component\\\\Mailer\\\\Messenger\\\\SendEmailMessage\\\":2:{s:60:\\\"\\0Symfony\\\\Component\\\\Mailer\\\\Messenger\\\\SendEmailMessage\\0message\\\";O:28:\\\"Symfony\\\\Component\\\\Mime\\\\Email\\\":6:{i:0;N;i:1;N;i:2;s:2254:\\\"<!DOCTYPE html>\n<html>\n<head>\n    <meta charset=\\\"UTF-8\\\">\n    <style>\n        body {\n            font-family: Arial, sans-serif;\n            line-height: 1.6;\n            color: #333;\n        }\n        .container {\n            max-width: 600px;\n            margin: 0 auto;\n            padding: 20px;\n        }\n        .header {\n            background: #007bff;\n            color: white;\n            padding: 20px;\n            text-align: center;\n            border-radius: 8px 8px 0 0;\n        }\n        .content {\n            background: #f9f9f9;\n            padding: 30px;\n            border-radius: 0 0 8px 8px;\n        }\n        .button {\n            display: inline-block;\n            padding: 12px 24px;\n            background: #007bff;\n            color: white;\n            text-decoration: none;\n            border-radius: 6px;\n            margin: 20px 0;\n        }\n        .footer {\n            margin-top: 20px;\n            font-size: 12px;\n            color: #666;\n            text-align: center;\n        }\n    </style>\n</head>\n<body>\n    <div class=\\\"container\\\">\n        <div class=\\\"header\\\">\n            <h1>RÃ©initialisation de mot de passe</h1>\n        </div>\n        <div class=\\\"content\\\">\n            <p>Bonjour yacibn,</p>\n            \n            <p>Vous avez demandÃ© Ã  rÃ©initialiser votre mot de passe. Cliquez sur le bouton ci-dessous pour crÃ©er un nouveau mot de passe :</p>\n            \n            <p style=\\\"text-align: center;\\\">\n                <a href=\\\"http://127.0.0.1:8000/mot-de-passe/reinitialiser/03a20b65b4118ac5b44d08860f337486a947391e22b7e6e6fc5916f5fab11aef\\\" class=\\\"button\\\">RÃ©initialiser mon mot de passe</a>\n            </p>\n            \n            <p>Ou copiez ce lien dans votre navigateur :</p>\n            <p style=\\\"word-break: break-all; color: #007bff;\\\">http://127.0.0.1:8000/mot-de-passe/reinitialiser/03a20b65b4118ac5b44d08860f337486a947391e22b7e6e6fc5916f5fab11aef</p>\n            \n            <p><strong>Ce lien expirera dans 1 heure.</strong></p>\n            \n            <p>Si vous n\\\'avez pas demandÃ© cette rÃ©initialisation, ignorez simplement cet email.</p>\n        </div>\n        <div class=\\\"footer\\\">\n            <p>Â© 2026 OXYN. Tous droits rÃ©servÃ©s.</p>\n        </div>\n    </div>\n</body>\n</html>\n\\\";i:3;s:5:\\\"utf-8\\\";i:4;a:0:{}i:5;a:2:{i:0;O:37:\\\"Symfony\\\\Component\\\\Mime\\\\Header\\\\Headers\\\":2:{s:46:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\Headers\\0headers\\\";a:3:{s:4:\\\"from\\\";a:1:{i:0;O:47:\\\"Symfony\\\\Component\\\\Mime\\\\Header\\\\MailboxListHeader\\\":5:{s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0name\\\";s:4:\\\"From\\\";s:56:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lineLength\\\";i:76;s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lang\\\";N;s:53:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0charset\\\";s:5:\\\"utf-8\\\";s:58:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\MailboxListHeader\\0addresses\\\";a:1:{i:0;O:30:\\\"Symfony\\\\Component\\\\Mime\\\\Address\\\":2:{s:39:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Address\\0address\\\";s:16:\\\"noreply@oxyn.com\\\";s:36:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Address\\0name\\\";s:0:\\\"\\\";}}}}s:2:\\\"to\\\";a:1:{i:0;O:47:\\\"Symfony\\\\Component\\\\Mime\\\\Header\\\\MailboxListHeader\\\":5:{s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0name\\\";s:2:\\\"To\\\";s:56:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lineLength\\\";i:76;s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lang\\\";N;s:53:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0charset\\\";s:5:\\\"utf-8\\\";s:58:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\MailboxListHeader\\0addresses\\\";a:1:{i:0;O:30:\\\"Symfony\\\\Component\\\\Mime\\\\Address\\\":2:{s:39:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Address\\0address\\\";s:15:\\\"merhben@oxyn.tn\\\";s:36:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Address\\0name\\\";s:0:\\\"\\\";}}}}s:7:\\\"subject\\\";a:1:{i:0;O:48:\\\"Symfony\\\\Component\\\\Mime\\\\Header\\\\UnstructuredHeader\\\":5:{s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0name\\\";s:7:\\\"Subject\\\";s:56:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lineLength\\\";i:76;s:50:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0lang\\\";N;s:53:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\AbstractHeader\\0charset\\\";s:5:\\\"utf-8\\\";s:55:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\UnstructuredHeader\\0value\\\";s:46:\\\"RÃ©initialisation de votre mot de passe - OXYN\\\";}}}s:49:\\\"\\0Symfony\\\\Component\\\\Mime\\\\Header\\\\Headers\\0lineLength\\\";i:76;}i:1;N;}}s:61:\\\"\\0Symfony\\\\Component\\\\Mailer\\\\Messenger\\\\SendEmailMessage\\0envelope\\\";N;}}', '[]', 'default', '2026-02-10 03:36:24', '2026-02-10 03:36:24', NULL);

-- --------------------------------------------------------

--
-- Structure de la table `notifications`
--

CREATE TABLE `notifications` (
  `id` int(11) NOT NULL,
  `type` varchar(50) NOT NULL,
  `titre` varchar(255) NOT NULL,
  `message` longtext NOT NULL,
  `is_read` tinyint(1) NOT NULL,
  `created_at` datetime NOT NULL,
  `user_id` int(11) NOT NULL,
  `related_message_id` int(11) DEFAULT NULL,
  `related_conversation_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `notifications`
--

INSERT INTO `notifications` (`id`, `type`, `titre`, `message`, `is_read`, `created_at`, `user_id`, `related_message_id`, `related_conversation_id`) VALUES
(1, 'rendement_faible', 'Rendement faible cette semaine', 'Votre rendement de complÃ©tion des tÃ¢ches est Ã  0%. Essayez de terminer vos tÃ¢ches pour atteindre 60% ou plus. Besoin d\'aide? Contactez votre encadrant!', 0, '2026-02-08 21:05:32', 4, NULL, NULL),
(2, 'message', 'Nouveau message de feriel yahia', 'Vous avez reÃ§u un message de feriel yahia', 1, '2026-02-08 21:05:46', 2, 11, 2),
(3, 'message', 'Nouvelle rÃ©ponse de votre encadrant', 'Votre encadrant a rÃ©pondu Ã  votre message', 1, '2026-02-08 21:07:49', 4, 13, 2),
(4, 'intervention_encadrant', 'ðŸ‘¨â€ðŸ« Message de votre encadrant', 'Votre encadrant vous a envoyÃ© un message concernant votre objectif hebdomadaire de la semaine 6, 2026 :\n\nðŸ’¬ besbes\n\nðŸ‘ Vos efforts ont Ã©tÃ© validÃ©s. Continuez sur cette voie !', 0, '2026-02-08 21:08:45', 4, NULL, NULL),
(5, 'message', 'Nouveau message de feriel yahia', 'Vous avez reÃ§u un message de feriel yahia', 1, '2026-02-08 21:56:05', 2, 14, 2),
(6, 'message', 'Nouveau message de feriel yahia', 'Vous avez reÃ§u un message de feriel yahia', 1, '2026-02-08 23:30:09', 2, 16, 2),
(7, 'message', 'Nouvelle rÃ©ponse de votre encadrant', 'Votre encadrant a rÃ©pondu Ã  votre message', 1, '2026-02-09 00:06:45', 4, 18, 2),
(8, 'rendement_faible', 'Rendement faible cette semaine', 'Votre rendement de complÃ©tion des tÃ¢ches est Ã  0%. Essayez de terminer vos tÃ¢ches pour atteindre 60% ou plus. Besoin d\'aide? Contactez votre encadrant!', 0, '2026-02-09 00:08:21', 4, NULL, NULL),
(9, 'evenement_supprime', 'âŒ Ã‰vÃ©nement supprimÃ©', 'L\'Ã©vÃ©nement auquel vous Ã©tiez inscrit a Ã©tÃ© supprimÃ© :\n\nðŸ“ OLUIUGUIPGUIOL\n\nðŸ“ Motif : Cet Ã©vÃ©nement a Ã©tÃ© supprimÃ© par un administrateur.', 0, '2026-02-09 00:44:51', 1, NULL, NULL),
(10, 'message', 'ðŸ’¬ Nouveau message', 'Vous avez reÃ§u un message de feriel yahia\n\n\"hola\"', 1, '2026-02-09 00:45:41', 2, NULL, NULL),
(11, 'evenement_supprime', 'âŒ Ã‰vÃ©nement supprimÃ©', 'L\'Ã©vÃ©nement auquel vous Ã©tiez inscrit a Ã©tÃ© supprimÃ© :\n\nðŸ“ hdfgfsdg\n\nðŸ“ Motif : Vous avez Ã©tÃ© retirÃ© de cet Ã©vÃ©nement par un administrateur.', 0, '2026-02-09 00:52:35', 1, NULL, NULL),
(12, 'evenement_supprime', 'âŒ Ã‰vÃ©nement supprimÃ©', 'L\'Ã©vÃ©nement auquel vous Ã©tiez inscrit a Ã©tÃ© supprimÃ© :\n\nðŸ“ hdfgfsdg\n\nðŸ“ Motif : Vous avez Ã©tÃ© retirÃ© de cet Ã©vÃ©nement par un administrateur.', 0, '2026-02-09 00:57:04', 1, NULL, NULL),
(13, 'evenement_inscrit', 'ðŸŽ‰ Inscription rÃ©ussie', 'Vous Ãªtes maintenant inscrit Ã  l\'Ã©vÃ©nement :\n\nðŸ“ hdfgfsdg\nðŸ“… 09/02/2026 Ã  00:50', 0, '2026-02-09 22:20:02', 7, NULL, NULL),
(14, 'evenement_inscrit', 'âŒ Inscription annulÃ©e', 'Votre inscription Ã  l\'Ã©vÃ©nement a Ã©tÃ© annulÃ©e :\n\nðŸ“ hdfgfsdg', 0, '2026-02-09 22:20:08', 7, NULL, NULL),
(15, 'evenement_inscrit', 'âŒ Inscription annulÃ©e', 'Votre inscription Ã  l\'Ã©vÃ©nement a Ã©tÃ© annulÃ©e :\n\nðŸ“ hdfgfsdg', 0, '2026-02-09 22:20:54', 1, NULL, NULL),
(16, 'evenement_supprime', 'âŒ Ã‰vÃ©nement supprimÃ©', 'L\'Ã©vÃ©nement auquel vous Ã©tiez inscrit a Ã©tÃ© supprimÃ© :\n\nðŸ“ hdfgfsdg\n\nðŸ“ Motif : Vous avez Ã©tÃ© retirÃ© de cet Ã©vÃ©nement par un administrateur.', 0, '2026-02-09 22:21:10', 1, NULL, NULL),
(17, 'evenement_supprime', 'âŒ Ã‰vÃ©nement supprimÃ©', 'L\'Ã©vÃ©nement auquel vous Ã©tiez inscrit a Ã©tÃ© supprimÃ© :\n\nðŸ“ qdssq\n\nðŸ“ Motif : Cet Ã©vÃ©nement a Ã©tÃ© supprimÃ© par un administrateur.', 0, '2026-02-09 22:25:36', 1, NULL, NULL),
(18, 'evenement_supprime', 'âŒ Ã‰vÃ©nement supprimÃ©', 'L\'Ã©vÃ©nement auquel vous Ã©tiez inscrit a Ã©tÃ© supprimÃ© :\n\nðŸ“ qdssq\n\nðŸ“ Motif : Cet Ã©vÃ©nement a Ã©tÃ© supprimÃ© par un administrateur.', 0, '2026-02-09 22:25:36', 7, NULL, NULL),
(19, 'rendement_faible', 'Rendement faible cette semaine', 'Votre rendement de complÃ©tion des tÃ¢ches est Ã  0%. Essayez de terminer vos tÃ¢ches pour atteindre 60% ou plus. Besoin d\'aide? Contactez votre encadrant!', 0, '2026-02-10 01:29:35', 7, NULL, NULL),
(20, 'message', 'ðŸ’¬ Nouveau message', 'Vous avez reÃ§u un message de tttttt vavavvav\n\n\"hello\"', 0, '2026-02-10 01:44:48', 2, NULL, NULL),
(21, 'message', 'ðŸ’¬ Nouveau message', 'Vous avez reÃ§u un message de tttttt vavavvav\n\n\"hi\"', 0, '2026-02-10 01:44:54', 2, NULL, NULL),
(22, 'programme_jour', 'ðŸŽ¯ Programme du jour atteint !', 'FÃ©licitations ! Vous avez atteint votre objectif du jour :\n\nâœ… Lundi 09/02\n\nðŸ“ Vous avez complÃ©tÃ© une tÃ¢che de votre programme du jour!', 0, '2026-02-10 02:09:29', 7, NULL, NULL),
(23, 'programme_jour', 'ðŸŽ¯ Programme du jour atteint !', 'FÃ©licitations ! Vous avez atteint votre objectif du jour :\n\nâœ… Mardi 10/02\n\nðŸ“ Vous avez complÃ©tÃ© une tÃ¢che de votre programme du jour!', 0, '2026-02-10 02:09:31', 7, NULL, NULL),
(24, 'programme_jour', 'ðŸŽ¯ Programme du jour atteint !', 'FÃ©licitations ! Vous avez atteint votre objectif du jour :\n\nâœ… Mercredi 11/02\n\nðŸ“ Vous avez complÃ©tÃ© une tÃ¢che de votre programme du jour!', 0, '2026-02-10 02:12:29', 7, NULL, NULL),
(25, 'intervention_encadrant', 'ðŸ‘¨â€ðŸ« Message de votre encadrant', 'Votre encadrant vous a envoyÃ© un message concernant votre objectif hebdomadaire de la semaine 7, 2026 :\n\nðŸ’¬ trÃ©s bien rendement\n\nðŸ‘ Vos efforts ont Ã©tÃ© validÃ©s. Continuez sur cette voie !', 0, '2026-02-10 02:23:19', 7, NULL, NULL),
(26, 'intervention_encadrant', 'ðŸ‘¨â€ðŸ« Message de votre encadrant', 'Votre encadrant vous a envoyÃ© un message concernant votre objectif hebdomadaire de la semaine 7, 2026 :\n\nðŸ’¬ trÃ©s bien rendemen\n\nðŸ‘ Vos efforts ont Ã©tÃ© validÃ©s. Continuez sur cette voie !', 0, '2026-02-10 02:23:39', 7, NULL, NULL),
(27, 'message', 'ðŸ’¬ Nouveau message', 'Vous avez reÃ§u un message de tttttt vavavvav\n\n\"heyyy\"', 0, '2026-02-10 02:28:51', 2, NULL, NULL),
(28, 'message', 'ðŸ’¬ Nouveau message', 'Vous avez reÃ§u un message de feriel yahia\n\n\"Comment je peut t\'aider\"', 0, '2026-02-10 02:36:50', 7, NULL, NULL),
(29, 'evenement_inscrit', 'ðŸŽ‰ Inscription rÃ©ussie', 'Vous Ãªtes maintenant inscrit Ã  l\'Ã©vÃ©nement :\n\nðŸ“ marathon\nðŸ“… 12/02/2026 Ã  08:01', 0, '2026-02-10 10:35:40', 4, NULL, NULL),
(30, 'evenement_inscrit', 'âŒ Inscription annulÃ©e', 'Votre inscription Ã  l\'Ã©vÃ©nement a Ã©tÃ© annulÃ©e :\n\nðŸ“ marathon', 0, '2026-02-10 10:35:45', 4, NULL, NULL),
(31, 'evenement_inscrit', 'ðŸŽ‰ Inscription rÃ©ussie', 'Vous Ãªtes maintenant inscrit Ã  l\'Ã©vÃ©nement :\n\nðŸ“ Circuit\nðŸ“… 10/02/2026 Ã  22:24', 0, '2026-02-10 10:35:53', 4, NULL, NULL),
(32, 'evenement_inscrit', 'ðŸŽ‰ Inscription rÃ©ussie', 'Vous Ãªtes maintenant inscrit Ã  l\'Ã©vÃ©nement :\n\nðŸ“ VTT\nðŸ“… 20/02/2026 Ã  04:40', 0, '2026-02-10 10:35:57', 4, NULL, NULL),
(33, 'evenement_supprime', 'âŒ Ã‰vÃ©nement supprimÃ©', 'L\'Ã©vÃ©nement auquel vous Ã©tiez inscrit a Ã©tÃ© supprimÃ© :\n\nðŸ“ VTT\n\nðŸ“ Motif : Vous avez Ã©tÃ© retirÃ© de cet Ã©vÃ©nement par un administrateur.', 0, '2026-02-10 10:39:14', 4, NULL, NULL),
(34, 'evenement_inscrit', 'ðŸŽ‰ Inscription rÃ©ussie', 'Vous Ãªtes maintenant inscrit Ã  l\'Ã©vÃ©nement :\n\nðŸ“ marathon\nðŸ“… 12/02/2026 Ã  08:01', 0, '2026-02-10 10:39:20', 1, NULL, NULL),
(35, 'evenement_inscrit', 'âŒ Inscription annulÃ©e', 'Votre inscription Ã  l\'Ã©vÃ©nement a Ã©tÃ© annulÃ©e :\n\nðŸ“ marathon', 0, '2026-02-10 10:39:27', 1, NULL, NULL),
(36, 'evenement_inscrit', 'ðŸŽ‰ Inscription rÃ©ussie', 'Vous Ãªtes maintenant inscrit Ã  l\'Ã©vÃ©nement :\n\nðŸ“ sadek\nðŸ“… 05/02/2026 Ã  12:10', 0, '2026-02-10 12:11:35', 1, NULL, NULL),
(37, 'evenement_supprime', 'âŒ Ã‰vÃ©nement supprimÃ©', 'L\'Ã©vÃ©nement auquel vous Ã©tiez inscrit a Ã©tÃ© supprimÃ© :\n\nðŸ“ sadek\n\nðŸ“ Motif : Vous avez Ã©tÃ© retirÃ© de cet Ã©vÃ©nement par un administrateur.', 0, '2026-02-10 12:12:14', 1, NULL, NULL),
(38, 'evenement_supprime', 'âŒ Ã‰vÃ©nement supprimÃ©', 'L\'Ã©vÃ©nement auquel vous Ã©tiez inscrit a Ã©tÃ© supprimÃ© :\n\nðŸ“ sadek\n\nðŸ“ Motif : Vous avez Ã©tÃ© retirÃ© de cet Ã©vÃ©nement par un administrateur.', 0, '2026-02-10 12:12:32', 1, NULL, NULL),
(39, 'evenement_inscrit', 'ðŸŽ‰ Inscription rÃ©ussie', 'Vous Ãªtes maintenant inscrit Ã  l\'Ã©vÃ©nement :\n\nðŸ“ sadek\nðŸ“… 05/02/2026 Ã  12:10', 0, '2026-02-10 12:15:29', 9, NULL, NULL),
(40, 'rendement_faible', 'Rendement faible cette semaine', 'Votre rendement de complÃ©tion des tÃ¢ches est Ã  0%. Essayez de terminer vos tÃ¢ches pour atteindre 60% ou plus. Besoin d\'aide? Contactez votre encadrant!', 0, '2026-02-10 13:10:29', 9, NULL, NULL),
(41, 'programme_jour', 'ðŸŽ¯ Programme du jour atteint !', 'FÃ©licitations ! Vous avez atteint votre objectif du jour :\n\nâœ… Lundi 09/02\n\nðŸ“ Vous avez complÃ©tÃ© une tÃ¢che de votre programme du jour!', 0, '2026-02-10 13:12:05', 9, NULL, NULL),
(42, 'programme_jour', 'ðŸŽ¯ Programme du jour atteint !', 'FÃ©licitations ! Vous avez atteint votre objectif du jour :\n\nâœ… Mardi 10/02\n\nðŸ“ Vous avez complÃ©tÃ© une tÃ¢che de votre programme du jour!', 0, '2026-02-10 13:12:09', 9, NULL, NULL),
(43, 'message', 'ðŸ’¬ Nouveau message', 'Vous avez reÃ§u un message de AB merhbene\n\n\"HELLO , j\'ai un blessure et douleur\"', 0, '2026-02-10 13:12:59', 4, NULL, NULL),
(44, 'message', 'ðŸ’¬ Nouveau message', 'Vous avez reÃ§u un message de feriel yahia\n\n\"okey on va voir\"', 0, '2026-02-10 13:14:23', 9, NULL, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `notification_evenement`
--

CREATE TABLE `notification_evenement` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `evenement_id` int(11) DEFAULT NULL,
  `type` varchar(50) NOT NULL,
  `title` varchar(120) NOT NULL,
  `message` longtext NOT NULL,
  `created_at` datetime NOT NULL,
  `read_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `objectifs_hebdomadaires`
--

CREATE TABLE `objectifs_hebdomadaires` (
  `id` int(11) NOT NULL,
  `week_number` int(11) NOT NULL,
  `year` int(11) NOT NULL,
  `objectifs` longtext NOT NULL,
  `objectif_principal` varchar(255) DEFAULT NULL,
  `taches_prevues` int(11) NOT NULL DEFAULT 0,
  `taches_realisees` int(11) NOT NULL DEFAULT 0,
  `taux_realisation` double NOT NULL DEFAULT 0,
  `statut` varchar(50) NOT NULL DEFAULT 'EN_COURS',
  `message_ia` longtext DEFAULT NULL,
  `message_encadrant` longtext DEFAULT NULL,
  `efforts_valides` tinyint(1) DEFAULT NULL,
  `message_encadrant_date` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `objectifs_hebdomadaires`
--

INSERT INTO `objectifs_hebdomadaires` (`id`, `week_number`, `year`, `objectifs`, `objectif_principal`, `taches_prevues`, `taches_realisees`, `taux_realisation`, `statut`, `message_ia`, `message_encadrant`, `efforts_valides`, `message_encadrant_date`, `created_at`, `updated_at`, `user_id`) VALUES
(1, 6, 2026, 'hey je veux un kgkjg:lj', 'Prise de masse', 7, 2, 28.57, 'EN_COURS', 'ðŸ“ˆ Pour gagner du poids sainement, augmentez vos calories de 300-500 kcal/jour avec des aliments nutritifs et l\'entrainement en musculation.\n\nðŸ’§ N\'oubliez pas l\'hydratation ! Buvez au minimum 2 litres d\'eau par jour, davantage pendant l\'entrainement.\n\nðŸ“… Restez regulier ! Les resultats viennent de la constance. Fixez-vous 3-4 seances par semaine et augmentez progressivement l\'intensite.', 'besbes', 1, '2026-02-08 21:08:45', '2026-02-08 21:05:32', '2026-02-08 21:56:22', 4),
(2, 7, 2026, 'ðŸ’ª Effectuer 4-5 sÃ©ances de musculation avec charges progressives\nðŸ½ï¸ Atteindre un surplus calorique de 300-500 kcal/jour\nðŸ˜´ Assurer 8h de sommeil par nuit pour la rÃ©cupÃ©ration', 'Prise de masse', 7, 0, 0, 'EN_COURS', 'ðŸ’ª Hey feriel ! Cap sur Prise de masse cette semaine. 4 entraÃ®nements planifiÃ©s. Reste motivÃ©! ðŸŽ¯', NULL, NULL, NULL, '2026-02-09 00:08:21', '2026-02-09 00:08:21', 4),
(3, 7, 2026, 'j\'ai douleur', 'Prise de masse', 7, 3, 42.86, 'EN_COURS', 'ðŸ’§ N\'oubliez pas l\'hydratation ! Buvez au minimum 2 litres d\'eau par jour, davantage pendant l\'entrainement.\n\nðŸ“ˆ Pour gagner du poids sainement, augmentez vos calories de 300-500 kcal/jour avec des aliments nutritifs et l\'entrainement en musculation.\n\nðŸ“… Restez regulier ! Les resultats viennent de la constance. Fixez-vous 3-4 seances par semaine et augmentez progressivement l\'intensite.', 'trÃ©s bien rendemen', 1, '2026-02-10 02:23:39', '2026-02-10 01:29:35', '2026-02-10 02:23:39', 7),
(4, 7, 2026, 'ðŸ’ª Effectuer 4-5 sÃ©ances de musculation avec charges progressives\nðŸ½ï¸ Atteindre un surplus calorique de 300-500 kcal/jour\nðŸ˜´ Assurer 8h de sommeil par nuit pour la rÃ©cupÃ©ration', 'Prise de masse', 7, 2, 28.57, 'EN_COURS', 'ðŸŽ¯ Salut AB ! Voici ton objectif de la semaine : Prise de masse. Bon courage ðŸ’ª', NULL, NULL, NULL, '2026-02-10 13:10:29', '2026-02-10 13:12:09', 9);

-- --------------------------------------------------------

--
-- Structure de la table `post`
--

CREATE TABLE `post` (
  `id_post` int(11) NOT NULL,
  `content_post` longtext NOT NULL,
  `media_url_post` varchar(200) DEFAULT NULL,
  `media_type_post` varchar(20) DEFAULT NULL,
  `visibility_post` varchar(50) NOT NULL,
  `created_at_post` datetime NOT NULL,
  `updated_at_post` datetime NOT NULL,
  `like_count_post` int(11) NOT NULL,
  `category_post` varchar(100) DEFAULT NULL,
  `id_author_post` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `post`
--

INSERT INTO `post` (`id_post`, `content_post`, `media_url_post`, `media_type_post`, `visibility_post`, `created_at_post`, `updated_at_post`, `like_count_post`, `category_post`, `id_author_post`) VALUES
(5, 'SÃšance de mÃšditation et yoga ce matin  20 minutes pour se reconnecter avec soi-mÃ›me. Le bien-Ã›tre mental est tout aussi important que le physique!', NULL, NULL, 'public', '2026-02-08 13:13:28', '2026-02-08 13:13:28', 1, 'Wellness', 3),
(14, 'gdgdgd', '', '', 'public', '2026-04-11 18:41:31', '2026-04-12 14:09:00', 0, 'general', 1),
(23, 'hhhhhhhhhhhhhhhh', '', '', 'public', '2026-04-13 15:29:45', '2026-04-13 15:29:45', 0, 'Général', 1),
(25, 'heyyyy', '', '', 'public', '2026-04-24 21:22:01', '2026-04-24 21:22:01', 0, 'Général', 1),
(30, 'testcloudianryapi', 'https://res.cloudinary.com/dlhz8gtag/image/upload/v1777142282/lriwz8w6q1yjhswwkynd.png', 'image', 'public', '2026-04-25 20:37:59', '2026-04-25 20:38:57', 0, 'Général', 11),
(31, 'testvideo', 'https://res.cloudinary.com/dlhz8gtag/video/upload/v1777143802/bistlx0k4uttkoqcsvan.mp4', 'video', 'public', '2026-04-25 21:03:10', '2026-04-25 21:03:10', 0, 'Général', 11),
(32, 'testmp3', 'https://res.cloudinary.com/dlhz8gtag/video/upload/v1777143869/drv3nueqtoo2p6vuve7o.mp3', 'audio', 'public', '2026-04-25 21:04:26', '2026-04-25 21:04:26', 0, 'Général', 11),
(34, 'testvideo2👍👍', 'https://res.cloudinary.com/dlhz8gtag/video/upload/v1777144981/hu7qiabrdxt21kmqhukb.mp4', 'video', 'public', '2026-04-25 21:22:43', '2026-04-25 21:22:43', 1, 'Général', 11),
(35, 'bonjour', '', '', 'public', '2026-04-25 23:07:54', '2026-04-25 23:07:54', 0, 'Général', 11),
(36, 'L\'activité physique régulière et une alimentation saine sont essentielles à un mode de vie équilibré. La pratique sportive améliore non seulement la santé cardiovasculaire, la force musculaire et la souplesse, mais aussi le bien-être mental en réduisant le stress et l\'anxiété. Cependant, les bienfaits de l\'exercice ne sont pleinement exploités que s\'il est associé à une alimentation équilibrée. Les athlètes et les personnes actives ont besoin d\'un apport suffisant en macronutriments, tels que les glucides pour l\'énergie, les protéines pour la réparation et la croissance musculaire, et les lipides sains pour le bon fonctionnement de l\'organisme.', '', '', 'public', '2026-04-26 01:13:28', '2026-04-26 13:50:10', 0, 'Général', 11),
(37, 'يُعد النشاط البدني المنتظم والتغذية السليمة عنصرين أساسيين لنمط حياة صحي. فممارسة الرياضة لا تُحسّن صحة القلب والأوعية الدموية وقوة العضلات ومرونتها فحسب، بل تُعزز أيضًا الصحة النفسية من خلال تقليل التوتر والقلق. ومع ذلك، لا يُمكن تحقيق فوائد التمارين الرياضية بالكامل إلا عند دمجها مع تغذية متوازنة. يحتاج الرياضيون والأفراد النشطون إلى تناول كميات كافية من المغذيات الكبرى، مثل الكربوهيدرات لتوفير الطاقة، والبروتينات لإصلاح العضلات ونموها، والدهون الصحية لوظائف الجسم العامة.', '', '', 'public', '2026-04-26 13:51:28', '2026-04-26 13:51:28', 1, 'Nutrition', 11),
(38, 'Regular physical activity and proper nutrition are essential components of a healthy lifestyle. Exercise not only improves cardiovascular health, muscle strength, and flexibility, but also promotes mental well-being by reducing stress and anxiety. However, the full benefits of exercise are only achieved when combined with a balanced diet. Athletes and active individuals must consume adequate amounts of macronutrients, such as carbohydrates for energy, protein, and fiber.', '', '', 'public', '2026-04-26 13:52:09', '2026-04-26 14:16:10', 1, 'Nutrition', 11),
(40, 'Ça a commencé avec rien d\'autre qu\'une forme marquée sur l\'herbe.\nPuis les fouilles ont commencé - plus profondes... et plus profond', '', '', 'public', '2026-04-26 13:59:56', '2026-04-26 13:59:56', 1, 'Général', 11),
(44, 'L\'activité physique régulière et une alimentation saine sont essentielles à un mode de vie équilibré. La pratique sportive améliore non seulement la santé cardiovasculaire, la force musculaire et la souplesse, mais aussi le bien-être mental en réduisant le stress et l\'anxiété. Cependant, les bienfaits de l\'exercice ne sont pleinement exploités que s\'il est associé à une alimentation équilibrée. Les athlètes et les personnes actives ont besoin d\'un apport suffisant en macronutriments, tels que les glucides pour l\'énergie, les protéines pour la réparation et la croissance musculaire, et les lipides sains pour le bon fonctionnement de l\'organisme.', 'https://res.cloudinary.com/dlhz8gtag/image/upload/v1777374274/iw8ttzkbbs0br9hbuc0x.png', 'image', 'public', '2026-04-28 13:04:32', '2026-04-28 13:04:32', 1, 'Général', 11);

-- --------------------------------------------------------

--
-- Structure de la table `post_likes`
--

CREATE TABLE `post_likes` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `post_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `post_likes`
--

INSERT INTO `post_likes` (`id`, `user_id`, `post_id`) VALUES
(5, 11, 5),
(6, 11, 34),
(9, 11, 38),
(8, 11, 40),
(11, 11, 44);

-- --------------------------------------------------------

--
-- Structure de la table `produits`
--

CREATE TABLE `produits` (
  `id_produit` int(11) NOT NULL,
  `nom_produit` varchar(150) NOT NULL,
  `description_produit` longtext DEFAULT NULL,
  `prix_produit` decimal(10,2) NOT NULL,
  `quantite_stock_produit` int(11) NOT NULL,
  `image_produit` varchar(255) DEFAULT NULL,
  `date_creation_produit` date DEFAULT NULL,
  `statut_produit` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `produits`
--

INSERT INTO `produits` (`id_produit`, `nom_produit`, `description_produit`, `prix_produit`, `quantite_stock_produit`, `image_produit`, `date_creation_produit`, `statut_produit`) VALUES
(1, 'validation', 'tsjhqstrhj', 40.00, 36, 'https://imageproxy.wolt.com/menu/menu-images/5d385060d4770cb2e9a24314/8e3f2402-b311-11f0-b1b2-b6f76ef60bc2_creatin_phx_210caps_750ml.png_v_1761122516', '2026-02-09', 'Actif'),
(2, 'bandlette', 'azeddsd', 40.00, 50, 'https://imageproxy.wolt.com/menu/menu-images/5d385060d4770cb2e9a24314/8e3f2402-b311-11f0-b1b2-b6f76ef60bc2_creatin_phx_210caps_750ml.png_v_1761122516', '2026-02-09', 'Actif'),
(4, 'aaazz', 'aaaaaaaaaaa', 40.00, 33, 'https://imageproxy.wolt.com/menu/menu-images/5d385060d4770cb2e9a24314/8e3f2402-b311-11f0-b1b2-b6f76ef60bc2_creatin_phx_210caps_750ml.png_v_1761122516', '2026-02-09', 'Inactif'),
(5, 'aaaaa', 'aaaaaa', 40.00, 12, 'https://imageproxy.wolt.com/menu/menu-images/5d385060d4770cb2e9a24314/8e3f2402-b311-11f0-b1b2-b6f76ef60bc2_creatin_phx_210caps_750ml.png_v_1761122516', '2026-02-10', 'Actif');

-- --------------------------------------------------------

--
-- Structure de la table `programmes_generes`
--

CREATE TABLE `programmes_generes` (
  `id` int(11) NOT NULL,
  `calories_par_jour` int(11) DEFAULT NULL,
  `objectif_principal` longtext DEFAULT NULL,
  `exercices_hebdomadaires` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`exercices_hebdomadaires`)),
  `plans_repas` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`plans_repas`)),
  `conseils_generaux` longtext DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `programmes_generes`
--

INSERT INTO `programmes_generes` (`id`, `calories_par_jour`, `objectif_principal`, `exercices_hebdomadaires`, `plans_repas`, `conseils_generaux`, `created_at`, `updated_at`, `user_id`) VALUES
(3, 2340, 'gain_poids', '{\"lundi\":[{\"nom\":\"Marche rapide\",\"duree\":\"30 minutes\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":150},{\"nom\":\"\\u00c9tirements\",\"duree\":\"10 minutes\",\"intensite\":\"L\\u00e9g\\u00e8re\",\"calories\":20}],\"mardi\":[{\"nom\":\"Cardio (course l\\u00e9g\\u00e8re ou v\\u00e9lo)\",\"duree\":\"25 minutes\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":200},{\"nom\":\"Squats\",\"repetitions\":\"3 s\\u00e9ries de 12\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":80}],\"mercredi\":[{\"nom\":\"Repos actif - Marche l\\u00e9g\\u00e8re\",\"duree\":\"20 minutes\",\"intensite\":\"L\\u00e9g\\u00e8re\",\"calories\":80}],\"jeudi\":[{\"nom\":\"Pompes (ou sur les genoux)\",\"repetitions\":\"3 s\\u00e9ries de 10\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":60},{\"nom\":\"Planche\",\"duree\":\"3 s\\u00e9ries de 30 secondes\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":40},{\"nom\":\"Burpees\",\"repetitions\":\"3 s\\u00e9ries de 8\",\"intensite\":\"\\u00c9lev\\u00e9e\",\"calories\":100}],\"vendredi\":[{\"nom\":\"Yoga ou Pilates\",\"duree\":\"30 minutes\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":120}],\"samedi\":[{\"nom\":\"Activit\\u00e9 au choix (natation, v\\u00e9lo, randonn\\u00e9e)\",\"duree\":\"45 minutes\",\"intensite\":\"Mod\\u00e9r\\u00e9e \\u00e0 \\u00e9lev\\u00e9e\",\"calories\":300}],\"dimanche\":[{\"nom\":\"Repos complet ou marche l\\u00e9g\\u00e8re\",\"duree\":\"20 minutes (optionnel)\",\"intensite\":\"Tr\\u00e8s l\\u00e9g\\u00e8re\",\"calories\":60}]}', '{\"petit_dejeuner\":{\"calories\":585,\"exemples\":[{\"nom\":\"Bol de flocons d\'avoine\",\"ingredients\":[\"50g flocons d\'avoine\",\"200ml lait d\'amande\",\"1 banane\",\"1 c.s. miel\",\"Poign\\u00e9e d\'amandes\"],\"calories\":585},{\"nom\":\"Omelette prot\\u00e9in\\u00e9e\",\"ingredients\":[\"3 \\u0153ufs\",\"L\\u00e9gumes (tomates, \\u00e9pinards)\",\"1 tranche pain complet\",\"Avocat\"],\"calories\":585},{\"nom\":\"Smoothie bowl\",\"ingredients\":[\"1 banane congel\\u00e9e\",\"Fruits rouges\",\"1 yaourt grec\",\"Granola\",\"Graines de chia\"],\"calories\":585}]},\"dejeuner\":{\"calories\":819,\"exemples\":[{\"nom\":\"Poulet grill\\u00e9 et quinoa\",\"ingredients\":[\"150g poulet\",\"100g quinoa\",\"L\\u00e9gumes verts vapeur\",\"Vinaigrette l\\u00e9g\\u00e8re\"],\"calories\":819},{\"nom\":\"Saumon et patate douce\",\"ingredients\":[\"120g saumon\",\"150g patate douce\",\"Brocoli\",\"Huile d\'olive\"],\"calories\":819},{\"nom\":\"Bowl v\\u00e9g\\u00e9tarien\",\"ingredients\":[\"Pois chiches r\\u00f4tis\",\"Riz complet\",\"Avocat\",\"L\\u00e9gumes grill\\u00e9s\",\"Tahini\"],\"calories\":819}]},\"collation\":{\"calories\":234,\"exemples\":[{\"nom\":\"Fruits frais + amandes\",\"calories\":234},{\"nom\":\"Yaourt grec + miel\",\"calories\":234},{\"nom\":\"Smoothie prot\\u00e9in\\u00e9\",\"calories\":234},{\"nom\":\"Barre de c\\u00e9r\\u00e9ales maison\",\"calories\":234}]},\"diner\":{\"calories\":702,\"exemples\":[{\"nom\":\"Soupe aux l\\u00e9gumes et prot\\u00e9ines\",\"ingredients\":[\"L\\u00e9gumes vari\\u00e9s\",\"100g poulet ou tofu\",\"1 tranche pain complet\"],\"calories\":702},{\"nom\":\"Poisson blanc et l\\u00e9gumes\",\"ingredients\":[\"150g cabillaud\",\"Haricots verts\",\"Carottes\",\"Citron\"],\"calories\":702},{\"nom\":\"Salade compl\\u00e8te\",\"ingredients\":[\"Laitue, tomates, concombre\",\"Thon\",\"\\u0152uf dur\",\"Vinaigrette maison\"],\"calories\":702}]}}', 'ðŸ’§ Hydratation : Buvez au moins 2 litres d\'eau par jour, davantage lors des entraÃ®nements.\n\nðŸƒ ActivitÃ© : Maintenez une routine rÃ©guliÃ¨re et variez les exercices pour Ã©viter la monotonie.\n\nðŸ“ˆ Gain de poids : Augmentez progressivement vos calories (+15%) avec des aliments nutritifs et denses en Ã©nergie.\n\nðŸ½ï¸ Alimentation : Mangez 5-6 petits repas par jour, privilÃ©giez les protÃ©ines, glucides complexes et bonnes graisses.\n\nâš ï¸ IMC : Votre IMC indique un poids insuffisant. Consultez un professionnel de santÃ© pour un suivi adaptÃ©.\n\nðŸ˜´ Sommeil : Dormez 7-9 heures par nuit pour optimiser votre rÃ©cupÃ©ration et vos rÃ©sultats.\n\nðŸ“… RÃ©gularitÃ© : La constance est la clÃ© du succÃ¨s. Suivez votre programme pendant au moins 8 semaines avant d\'Ã©valuer les rÃ©sultats.\n\nðŸ“ Suivi : Notez vos progrÃ¨s chaque semaine (poids, mensurations, performances) pour rester motivÃ©.', '2026-02-08 23:29:52', '2026-02-08 23:29:52', 4),
(6, 2924, 'gain_poids', '{\"lundi\":[{\"nom\":\"Marche rapide\",\"duree\":\"30 minutes\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":150},{\"nom\":\"\\u00c9tirements\",\"duree\":\"10 minutes\",\"intensite\":\"L\\u00e9g\\u00e8re\",\"calories\":20}],\"mardi\":[{\"nom\":\"Cardio (course l\\u00e9g\\u00e8re ou v\\u00e9lo)\",\"duree\":\"25 minutes\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":200},{\"nom\":\"Squats\",\"repetitions\":\"3 s\\u00e9ries de 12\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":80}],\"mercredi\":[{\"nom\":\"Repos actif - Marche l\\u00e9g\\u00e8re\",\"duree\":\"20 minutes\",\"intensite\":\"L\\u00e9g\\u00e8re\",\"calories\":80}],\"jeudi\":[{\"nom\":\"Pompes (ou sur les genoux)\",\"repetitions\":\"3 s\\u00e9ries de 10\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":60},{\"nom\":\"Planche\",\"duree\":\"3 s\\u00e9ries de 30 secondes\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":40},{\"nom\":\"Burpees\",\"repetitions\":\"3 s\\u00e9ries de 8\",\"intensite\":\"\\u00c9lev\\u00e9e\",\"calories\":100}],\"vendredi\":[{\"nom\":\"Yoga ou Pilates\",\"duree\":\"30 minutes\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":120}],\"samedi\":[{\"nom\":\"Activit\\u00e9 au choix (natation, v\\u00e9lo, randonn\\u00e9e)\",\"duree\":\"45 minutes\",\"intensite\":\"Mod\\u00e9r\\u00e9e \\u00e0 \\u00e9lev\\u00e9e\",\"calories\":300}],\"dimanche\":[{\"nom\":\"Repos complet ou marche l\\u00e9g\\u00e8re\",\"duree\":\"20 minutes (optionnel)\",\"intensite\":\"Tr\\u00e8s l\\u00e9g\\u00e8re\",\"calories\":60}]}', '{\"petit_dejeuner\":{\"calories\":731,\"exemples\":[{\"nom\":\"Bol de flocons d\'avoine\",\"ingredients\":[\"50g flocons d\'avoine\",\"200ml lait d\'amande\",\"1 banane\",\"1 c.s. miel\",\"Poign\\u00e9e d\'amandes\"],\"calories\":731},{\"nom\":\"Omelette prot\\u00e9in\\u00e9e\",\"ingredients\":[\"3 \\u0153ufs\",\"L\\u00e9gumes (tomates, \\u00e9pinards)\",\"1 tranche pain complet\",\"Avocat\"],\"calories\":731},{\"nom\":\"Smoothie bowl\",\"ingredients\":[\"1 banane congel\\u00e9e\",\"Fruits rouges\",\"1 yaourt grec\",\"Granola\",\"Graines de chia\"],\"calories\":731}]},\"dejeuner\":{\"calories\":1023,\"exemples\":[{\"nom\":\"Poulet grill\\u00e9 et quinoa\",\"ingredients\":[\"150g poulet\",\"100g quinoa\",\"L\\u00e9gumes verts vapeur\",\"Vinaigrette l\\u00e9g\\u00e8re\"],\"calories\":1023},{\"nom\":\"Saumon et patate douce\",\"ingredients\":[\"120g saumon\",\"150g patate douce\",\"Brocoli\",\"Huile d\'olive\"],\"calories\":1023},{\"nom\":\"Bowl v\\u00e9g\\u00e9tarien\",\"ingredients\":[\"Pois chiches r\\u00f4tis\",\"Riz complet\",\"Avocat\",\"L\\u00e9gumes grill\\u00e9s\",\"Tahini\"],\"calories\":1023}]},\"collation\":{\"calories\":292,\"exemples\":[{\"nom\":\"Fruits frais + amandes\",\"calories\":292},{\"nom\":\"Yaourt grec + miel\",\"calories\":292},{\"nom\":\"Smoothie prot\\u00e9in\\u00e9\",\"calories\":292},{\"nom\":\"Barre de c\\u00e9r\\u00e9ales maison\",\"calories\":292}]},\"diner\":{\"calories\":877,\"exemples\":[{\"nom\":\"Soupe aux l\\u00e9gumes et prot\\u00e9ines\",\"ingredients\":[\"L\\u00e9gumes vari\\u00e9s\",\"100g poulet ou tofu\",\"1 tranche pain complet\"],\"calories\":877},{\"nom\":\"Poisson blanc et l\\u00e9gumes\",\"ingredients\":[\"150g cabillaud\",\"Haricots verts\",\"Carottes\",\"Citron\"],\"calories\":877},{\"nom\":\"Salade compl\\u00e8te\",\"ingredients\":[\"Laitue, tomates, concombre\",\"Thon\",\"\\u0152uf dur\",\"Vinaigrette maison\"],\"calories\":877}]}}', 'ðŸ’§ Hydratation : Buvez au moins 2 litres d\'eau par jour, davantage lors des entraÃ®nements.\n\nðŸƒ ActivitÃ© : Maintenez une routine rÃ©guliÃ¨re et variez les exercices pour Ã©viter la monotonie.\n\nðŸ“ˆ Gain de poids : Augmentez progressivement vos calories (+15%) avec des aliments nutritifs et denses en Ã©nergie.\n\nðŸ½ï¸ Alimentation : Mangez 5-6 petits repas par jour, privilÃ©giez les protÃ©ines, glucides complexes et bonnes graisses.\n\nâš ï¸ IMC : Votre IMC indique un poids insuffisant. Consultez un professionnel de santÃ© pour un suivi adaptÃ©.\n\nðŸ˜´ Sommeil : Dormez 7-9 heures par nuit pour optimiser votre rÃ©cupÃ©ration et vos rÃ©sultats.\n\nðŸ“… RÃ©gularitÃ© : La constance est la clÃ© du succÃ¨s. Suivez votre programme pendant au moins 8 semaines avant d\'Ã©valuer les rÃ©sultats.\n\nðŸ“ Suivi : Notez vos progrÃ¨s chaque semaine (poids, mensurations, performances) pour rester motivÃ©.', '2026-02-10 01:57:56', '2026-02-10 01:57:56', 7),
(8, 2076, 'perte_poids', '{\"lundi\":[{\"nom\":\"Marche rapide\",\"duree\":\"30 minutes\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":150},{\"nom\":\"\\u00c9tirements\",\"duree\":\"10 minutes\",\"intensite\":\"L\\u00e9g\\u00e8re\",\"calories\":20}],\"mardi\":[{\"nom\":\"Cardio (course l\\u00e9g\\u00e8re ou v\\u00e9lo)\",\"duree\":\"25 minutes\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":200},{\"nom\":\"Squats\",\"repetitions\":\"3 s\\u00e9ries de 12\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":80}],\"mercredi\":[{\"nom\":\"Repos actif - Marche l\\u00e9g\\u00e8re\",\"duree\":\"20 minutes\",\"intensite\":\"L\\u00e9g\\u00e8re\",\"calories\":80}],\"jeudi\":[{\"nom\":\"Pompes (ou sur les genoux)\",\"repetitions\":\"3 s\\u00e9ries de 10\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":60},{\"nom\":\"Planche\",\"duree\":\"3 s\\u00e9ries de 30 secondes\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":40},{\"nom\":\"Burpees\",\"repetitions\":\"3 s\\u00e9ries de 8\",\"intensite\":\"\\u00c9lev\\u00e9e\",\"calories\":100}],\"vendredi\":[{\"nom\":\"Yoga ou Pilates\",\"duree\":\"30 minutes\",\"intensite\":\"Mod\\u00e9r\\u00e9e\",\"calories\":120}],\"samedi\":[{\"nom\":\"Activit\\u00e9 au choix (natation, v\\u00e9lo, randonn\\u00e9e)\",\"duree\":\"45 minutes\",\"intensite\":\"Mod\\u00e9r\\u00e9e \\u00e0 \\u00e9lev\\u00e9e\",\"calories\":300}],\"dimanche\":[{\"nom\":\"Repos complet ou marche l\\u00e9g\\u00e8re\",\"duree\":\"20 minutes (optionnel)\",\"intensite\":\"Tr\\u00e8s l\\u00e9g\\u00e8re\",\"calories\":60}]}', '{\"petit_dejeuner\":{\"calories\":519,\"exemples\":[{\"nom\":\"Bol de flocons d\'avoine\",\"ingredients\":[\"50g flocons d\'avoine\",\"200ml lait d\'amande\",\"1 banane\",\"1 c.s. miel\",\"Poign\\u00e9e d\'amandes\"],\"calories\":519},{\"nom\":\"Omelette prot\\u00e9in\\u00e9e\",\"ingredients\":[\"3 \\u0153ufs\",\"L\\u00e9gumes (tomates, \\u00e9pinards)\",\"1 tranche pain complet\",\"Avocat\"],\"calories\":519},{\"nom\":\"Smoothie bowl\",\"ingredients\":[\"1 banane congel\\u00e9e\",\"Fruits rouges\",\"1 yaourt grec\",\"Granola\",\"Graines de chia\"],\"calories\":519}]},\"dejeuner\":{\"calories\":727,\"exemples\":[{\"nom\":\"Poulet grill\\u00e9 et quinoa\",\"ingredients\":[\"150g poulet\",\"100g quinoa\",\"L\\u00e9gumes verts vapeur\",\"Vinaigrette l\\u00e9g\\u00e8re\"],\"calories\":727},{\"nom\":\"Saumon et patate douce\",\"ingredients\":[\"120g saumon\",\"150g patate douce\",\"Brocoli\",\"Huile d\'olive\"],\"calories\":727},{\"nom\":\"Bowl v\\u00e9g\\u00e9tarien\",\"ingredients\":[\"Pois chiches r\\u00f4tis\",\"Riz complet\",\"Avocat\",\"L\\u00e9gumes grill\\u00e9s\",\"Tahini\"],\"calories\":727}]},\"collation\":{\"calories\":208,\"exemples\":[{\"nom\":\"Fruits frais + amandes\",\"calories\":208},{\"nom\":\"Yaourt grec + miel\",\"calories\":208},{\"nom\":\"Smoothie prot\\u00e9in\\u00e9\",\"calories\":208},{\"nom\":\"Barre de c\\u00e9r\\u00e9ales maison\",\"calories\":208}]},\"diner\":{\"calories\":623,\"exemples\":[{\"nom\":\"Soupe aux l\\u00e9gumes et prot\\u00e9ines\",\"ingredients\":[\"L\\u00e9gumes vari\\u00e9s\",\"100g poulet ou tofu\",\"1 tranche pain complet\"],\"calories\":623},{\"nom\":\"Poisson blanc et l\\u00e9gumes\",\"ingredients\":[\"150g cabillaud\",\"Haricots verts\",\"Carottes\",\"Citron\"],\"calories\":623},{\"nom\":\"Salade compl\\u00e8te\",\"ingredients\":[\"Laitue, tomates, concombre\",\"Thon\",\"\\u0152uf dur\",\"Vinaigrette maison\"],\"calories\":623}]}}', 'ðŸ’§ Hydratation : Buvez au moins 2 litres d\'eau par jour, davantage lors des entraÃ®nements.\n\nðŸƒ ActivitÃ© : Maintenez une routine rÃ©guliÃ¨re et variez les exercices pour Ã©viter la monotonie.\n\nðŸ“‰ Perte de poids : PrivilÃ©giez un dÃ©ficit calorique modÃ©rÃ© (-20%) et soyez patient. Visez 0.5-1kg par semaine maximum.\n\nðŸ½ï¸ Alimentation : Favorisez les protÃ©ines maigres, les lÃ©gumes et les glucides complexes. Ã‰vitez les aliments ultra-transformÃ©s.\n\nâš ï¸ IMC : Votre IMC indique une obÃ©sitÃ©. Un suivi mÃ©dical est recommandÃ© en complÃ©ment de ce programme.\n\nðŸ˜´ Sommeil : Dormez 7-9 heures par nuit pour optimiser votre rÃ©cupÃ©ration et vos rÃ©sultats.\n\nðŸ“… RÃ©gularitÃ© : La constance est la clÃ© du succÃ¨s. Suivez votre programme pendant au moins 8 semaines avant d\'Ã©valuer les rÃ©sultats.\n\nðŸ“ Suivi : Notez vos progrÃ¨s chaque semaine (poids, mensurations, performances) pour rester motivÃ©.', '2026-02-10 13:11:37', '2026-02-10 13:11:37', 9);

-- --------------------------------------------------------

--
-- Structure de la table `resultat_evenement`
--

CREATE TABLE `resultat_evenement` (
  `id_resultat_resultat_evenement` int(11) NOT NULL,
  `id_evenement_resultat_evenement` int(11) NOT NULL,
  `published_by_resultat_evenement` int(11) NOT NULL,
  `contenu_resultat_evenement` longtext NOT NULL,
  `winners_text_resultat_evenement` varchar(255) DEFAULT NULL,
  `published_at_resultat_evenement` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `session_participants`
--

CREATE TABLE `session_participants` (
  `id` int(11) NOT NULL,
  `joined_at` datetime NOT NULL,
  `attended` tinyint(1) NOT NULL,
  `user_id` int(11) NOT NULL,
  `training_session_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `taches_quotidiennes`
--

CREATE TABLE `taches_quotidiennes` (
  `id` int(11) NOT NULL,
  `date` date NOT NULL,
  `jour_semaine` varchar(20) NOT NULL,
  `description` longtext NOT NULL,
  `etat` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `taches_quotidiennes`
--

INSERT INTO `taches_quotidiennes` (`id`, `date`, `jour_semaine`, `description`, `etat`, `created_at`, `updated_at`, `user_id`) VALUES
(8, '2026-02-02', 'lundi', 'Marche lÃ©gÃ¨re 20 min + 5 repas riches en protÃ©ines', 'FAIT', '2026-02-08 21:06:23', '2026-02-08 21:56:21', 4),
(9, '2026-02-03', 'mardi', 'Musculation: dÃ©veloppÃ© couchÃ©, tractions', 'FAIT', '2026-02-08 21:06:23', '2026-02-08 21:56:22', 4),
(10, '2026-02-04', 'mercredi', 'Repos actif: marche lÃ©gÃ¨re 20 min', 'NON_FAIT', '2026-02-08 21:06:23', '2026-02-08 21:06:23', 4),
(11, '2026-02-05', 'jeudi', 'Musculation jambes: squats lourds, fentes', 'NON_FAIT', '2026-02-08 21:06:23', '2026-02-08 21:06:23', 4),
(12, '2026-02-06', 'vendredi', 'Yoga ou Pilates 30 min', 'NON_FAIT', '2026-02-08 21:06:23', '2026-02-08 21:06:23', 4),
(13, '2026-02-07', 'samedi', 'ActivitÃ© libre: natation, vÃ©lo ou randonnÃ©e 45 min', 'NON_FAIT', '2026-02-08 21:06:23', '2026-02-08 21:06:23', 4),
(14, '2026-02-08', 'dimanche', 'Repos complet + prÃ©paration de la semaine', 'NON_FAIT', '2026-02-08 21:06:23', '2026-02-08 21:06:23', 4),
(15, '2026-02-09', 'lundi', 'Marche lÃ©gÃ¨re 20 min + 5 repas riches en protÃ©ines', 'NON_FAIT', '2026-02-09 00:08:21', '2026-02-09 00:08:21', 4),
(16, '2026-02-10', 'mardi', 'Musculation: dÃ©veloppÃ© couchÃ©, tractions', 'NON_FAIT', '2026-02-09 00:08:21', '2026-02-09 00:08:21', 4),
(17, '2026-02-11', 'mercredi', 'Repos actif: marche lÃ©gÃ¨re 20 min', 'NON_FAIT', '2026-02-09 00:08:21', '2026-02-09 00:08:21', 4),
(18, '2026-02-12', 'jeudi', 'Musculation jambes: squats lourds, fentes', 'NON_FAIT', '2026-02-09 00:08:21', '2026-02-09 00:08:21', 4),
(19, '2026-02-13', 'vendredi', 'Yoga ou Pilates 30 min', 'NON_FAIT', '2026-02-09 00:08:21', '2026-02-09 00:08:21', 4),
(20, '2026-02-14', 'samedi', 'ActivitÃ© libre: natation, vÃ©lo ou randonnÃ©e 45 min', 'NON_FAIT', '2026-02-09 00:08:21', '2026-02-09 00:08:21', 4),
(21, '2026-02-15', 'dimanche', 'Repos complet + prÃ©paration de la semaine', 'NON_FAIT', '2026-02-09 00:08:21', '2026-02-09 00:08:21', 4),
(29, '2026-02-09', 'lundi', 'Marche lÃ©gÃ¨re 20 min + 5 repas riches en protÃ©ines', 'FAIT', '2026-02-10 01:43:19', '2026-02-10 02:09:29', 7),
(30, '2026-02-10', 'mardi', 'Musculation: dÃ©veloppÃ© couchÃ©, tractions', 'FAIT', '2026-02-10 01:43:19', '2026-02-10 02:09:31', 7),
(31, '2026-02-11', 'mercredi', 'Repos actif: marche lÃ©gÃ¨re 20 min', 'FAIT', '2026-02-10 01:43:19', '2026-02-10 02:12:29', 7),
(32, '2026-02-12', 'jeudi', 'Musculation jambes: squats lourds, fentes', 'NON_FAIT', '2026-02-10 01:43:19', '2026-02-10 01:43:19', 7),
(33, '2026-02-13', 'vendredi', 'Yoga ou Pilates 30 min', 'NON_FAIT', '2026-02-10 01:43:19', '2026-02-10 01:43:19', 7),
(34, '2026-02-14', 'samedi', 'ActivitÃ© libre: natation, vÃ©lo ou randonnÃ©e 45 min', 'NON_FAIT', '2026-02-10 01:43:19', '2026-02-10 01:43:19', 7),
(35, '2026-02-15', 'dimanche', 'Repos complet + prÃ©paration de la semaine', 'NON_FAIT', '2026-02-10 01:43:19', '2026-02-10 01:43:19', 7),
(36, '2026-02-09', 'lundi', 'Marche lÃ©gÃ¨re 20 min + 5 repas riches en protÃ©ines', 'FAIT', '2026-02-10 13:10:29', '2026-02-10 13:12:05', 9),
(37, '2026-02-10', 'mardi', 'Musculation: dÃ©veloppÃ© couchÃ©, tractions', 'FAIT', '2026-02-10 13:10:29', '2026-02-10 13:12:09', 9),
(38, '2026-02-11', 'mercredi', 'Repos actif: marche lÃ©gÃ¨re 20 min', 'NON_FAIT', '2026-02-10 13:10:29', '2026-02-10 13:10:29', 9),
(39, '2026-02-12', 'jeudi', 'Musculation jambes: squats lourds, fentes', 'NON_FAIT', '2026-02-10 13:10:29', '2026-02-10 13:10:29', 9),
(40, '2026-02-13', 'vendredi', 'Yoga ou Pilates 30 min', 'NON_FAIT', '2026-02-10 13:10:29', '2026-02-10 13:10:29', 9),
(41, '2026-02-14', 'samedi', 'ActivitÃ© libre: natation, vÃ©lo ou randonnÃ©e 45 min', 'NON_FAIT', '2026-02-10 13:10:29', '2026-02-10 13:10:29', 9),
(42, '2026-02-15', 'dimanche', 'Repos complet + prÃ©paration de la semaine', 'NON_FAIT', '2026-02-10 13:10:29', '2026-02-10 13:10:29', 9);

-- --------------------------------------------------------

--
-- Structure de la table `training_sessions`
--

CREATE TABLE `training_sessions` (
  `id` int(11) NOT NULL,
  `title` varchar(150) DEFAULT NULL,
  `description` longtext DEFAULT NULL,
  `start_at` datetime DEFAULT NULL,
  `end_at` datetime DEFAULT NULL,
  `capacity` int(11) DEFAULT NULL,
  `price` decimal(10,2) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL,
  `created_at` datetime NOT NULL,
  `gymnasium_id` int(11) DEFAULT NULL,
  `coach_user_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `training_sessions`
--

INSERT INTO `training_sessions` (`id`, `title`, `description`, `start_at`, `end_at`, `capacity`, `price`, `is_active`, `created_at`, `gymnasium_id`, `coach_user_id`) VALUES
(1, 'Box Anglaise', 'la rapiditÃ© et la dance de boxeur', '2026-02-27 19:14:00', '2026-02-27 22:14:00', 7, 10.00, 1, '2026-02-09 19:15:19', 1, 2),
(2, 'yoga', 'YOGA anti stress', '2026-02-11 21:06:00', '2026-02-11 23:06:00', 30, 10.00, 1, '2026-02-09 21:06:38', 1, 2),
(3, 'yoga', NULL, '2026-02-11 22:42:00', NULL, 30, 10.00, 1, '2026-02-09 22:42:31', 3, 4),
(4, 'AÃ©robique', NULL, '2026-02-12 12:00:00', '2026-02-13 14:00:00', 88, 27.00, 1, '2026-02-10 10:29:57', 3, 4),
(5, 'PILATE', NULL, '2026-02-11 12:00:00', '2026-02-25 12:00:00', 14, 27.00, 1, '2026-02-10 12:59:00', 1, 4);

-- --------------------------------------------------------

--
-- Structure de la table `users`
--

CREATE TABLE `users` (
  `id_user` int(11) NOT NULL,
  `email_user` varchar(180) NOT NULL,
  `password_user` varchar(255) NOT NULL,
  `roles_user` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`roles_user`)),
  `first_name_user` varchar(100) NOT NULL,
  `last_name_user` varchar(100) NOT NULL,
  `phone_user` varchar(20) DEFAULT NULL,
  `is_active_user` tinyint(1) NOT NULL DEFAULT 1,
  `created_at_user` datetime DEFAULT current_timestamp(),
  `reset_token_user` varchar(255) DEFAULT NULL,
  `reset_token_expires_at_user` datetime DEFAULT NULL,
  `last_seen_at_user` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `users`
--

INSERT INTO `users` (`id_user`, `email_user`, `password_user`, `roles_user`, `first_name_user`, `last_name_user`, `phone_user`, `is_active_user`, `created_at_user`, `reset_token_user`, `reset_token_expires_at_user`, `last_seen_at_user`) VALUES
(1, 'merhben@oxyn.tn', '$2y$13$ZKkYRW/FsxkF0TOfF5GhZ.3xIVdYsA8fhq515tM41adPw5ODMpE5C', '[\"ROLE_ADMIN\"]', 'yacibn', 'merhben', '+21766666666', 1, '2026-02-08 05:20:39', '03a20b65b4118ac5b44d08860f337486a947391e22b7e6e6fc5916f5fab11aef', '2026-02-10 05:36:23', NULL),
(2, 'sadok@oxyn.com', '$2y$13$yVrJK2i/56OFERZWaVp4t.DoGTYia/DCb43jhCu19NZVugksXkJ/a', '[\"ROLE_CLIENT\"]', 'sadakkkkkkk', 'merhbene', '+217234567897', 1, '2026-02-08 13:46:47', NULL, NULL, NULL),
(3, 'client@oxyn.com', '$2y$13$apTNjjnx1Wt9S5gkhI7/9OA5TNUCf.91H1/Of0eATlq7ihTRNIvj.', '[\"ROLE_CLIENT\"]', 'syrine', 'hich', '+21753275207', 1, '2026-02-08 14:50:58', NULL, NULL, NULL),
(4, 'fafa@gmail.com', '$2y$13$IbonvM9OKlRJ/lITcMO6fuPBP3Vi8Od7r4DK1Bb9tF6MDd8hjBFpW', '[\"ROLE_ENCADRANT\"]', 'feriel', 'yahia', '23456789', 1, '2026-02-08 14:53:20', NULL, NULL, NULL),
(5, 'merhben@gmail.com', '$2y$13$ODHDqu5pnSjQpRoXSU5Xg.DMiPmTxzlZKOoyzgXyoPwm/sqW8e7iO', '[\"ROLE_ENCADRANT\"]', 'olaAAAbbb', 'merh', '+21753275207', 1, '2026-02-08 19:20:25', NULL, NULL, NULL),
(7, 'daikhi11@gmail.com', '$2y$13$rJ2Gw4AzLX8aAqQeSy7VmeRMEiw2A4ijNe/aLrYrpiEfVzO3kRs.m', '[\"ROLE_CLIENT\"]', 'tttttt', 'vavavvav', '+21753275207', 1, '2026-02-09 21:49:52', NULL, NULL, NULL),
(8, 'test@oxyn.com', '$2y$13$6AV4rLXCAZX6yu7DqQwaguE.LihDXv1tpSd3Dz0BhOG0IRXIuleyG', '[\"ROLE_CLIENT\"]', 'mohamed', 'merhbene', '+21753275207', 1, '2026-02-10 11:11:10', NULL, NULL, NULL),
(9, 'gaddd@gmail.com', '$2y$13$4nVRN4x8HibauhHmEGMgG.S/9.DwNvRNQxX1P7bz8/KoAFAqJ.oo.', '[\"ROLE_CLIENT\"]', 'AB', 'merhbene', '2744444GGG', 0, '2026-02-10 11:51:36', NULL, NULL, NULL),
(10, 'melekbenrejeb1919@gmail.COM', '$2y$13$FUu50wVdIhu54a6J3SXY9OtfOBAiysn19gY.j12IBWdnP/kd4vdkG', '[\"ROLE_CLIENT\"]', 'malek', 'melekbenrejeb1919@gmail.COM', '58414391', 1, '2026-04-11 16:50:12', NULL, NULL, NULL),
(11, 'melekbenrejeb@gmail.com', '$2a$12$U.s419CtQ5bDpFdYAurJU.1c4Td2b060a2NZSYO44DAHlk/FoIDIi', '[\"ROLE_CLIENT\"]', 'malek', 'benrejeb', '58414391', 1, '2026-04-13 15:23:52', NULL, NULL, NULL),
(12, 'melekbenrejeb1919@gmail.om', '$2a$12$2e192NayAbNeDJL1o.MK2Oie7QB4iO9I.ygLuT2gA7BTpo7timXOe', '[\"ROLE_CLIENT\"]', 'malek', 'benrejeb', '58414391', 1, '2026-04-24 21:03:56', NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `user_gamification`
--

CREATE TABLE `user_gamification` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `points` int(11) DEFAULT 0,
  `level` varchar(50) DEFAULT 'Beginner',
  `badges_json` text DEFAULT '[]',
  `posts_count` int(11) DEFAULT 0,
  `comments_count` int(11) DEFAULT 0,
  `likes_received` int(11) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `user_gamification`
--

INSERT INTO `user_gamification` (`id`, `user_id`, `points`, `level`, `badges_json`, `posts_count`, `comments_count`, `likes_received`, `created_at`, `updated_at`) VALUES
(1, 11, 111, 'Advanced', '[\"Popular\",\"Chatty\",\"Liked\",\"Beloved\",\"Superstar\"]', 4, 10, 64, '2026-04-26 19:38:00', '2026-04-28 11:05:47'),
(2, 3, 3, 'Beginner', '[]', 0, 0, 3, '2026-04-27 19:29:10', '2026-04-27 19:29:20');

-- --------------------------------------------------------

--
-- Structure de la table `user_recommendation`
--

CREATE TABLE `user_recommendation` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `category_scores_json` text NOT NULL,
  `total_interactions` int(11) NOT NULL DEFAULT 0,
  `last_updated` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `user_recommendation`
--

INSERT INTO `user_recommendation` (`id`, `user_id`, `category_scores_json`, `total_interactions`, `last_updated`) VALUES
(1, 11, '{\"Nutrition\":20,\"Wellness\":6,\"Général\":15}', 41, '2026-04-28 11:05:47');

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `avis_evenement`
--
ALTER TABLE `avis_evenement`
  ADD PRIMARY KEY (`id_note_avis_evenement`),
  ADD UNIQUE KEY `uq_note` (`id_evenement_avis_evenement`,`id_user_avis_evenement`),
  ADD KEY `IDX_AD7377DF6E933670` (`id_evenement_avis_evenement`),
  ADD KEY `IDX_AD7377DF801C344D` (`id_user_avis_evenement`);

--
-- Index pour la table `commandes`
--
ALTER TABLE `commandes`
  ADD PRIMARY KEY (`id_commande`),
  ADD KEY `IDX_35D4282CA9525530` (`id_client_commande`);

--
-- Index pour la table `comment`
--
ALTER TABLE `comment`
  ADD PRIMARY KEY (`id_comment`),
  ADD KEY `IDX_9474526CEC287029` (`id_author_comment`),
  ADD KEY `IDX_9474526C4B89032C` (`post_id`),
  ADD KEY `IDX_9474526C727ACA70` (`parent_id`);

--
-- Index pour la table `comment_likes`
--
ALTER TABLE `comment_likes`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_comment_user_like` (`comment_id`,`user_id`),
  ADD KEY `IDX_E050D68CF8697D13` (`comment_id`),
  ADD KEY `IDX_E050D68CA76ED395` (`user_id`);

--
-- Index pour la table `conversations`
--
ALTER TABLE `conversations`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_C2521BF119EB6921` (`client_id`),
  ADD KEY `IDX_C2521BF1FEF1BA4` (`encadrant_id`);

--
-- Index pour la table `doctrine_migration_versions`
--
ALTER TABLE `doctrine_migration_versions`
  ADD PRIMARY KEY (`version`);

--
-- Index pour la table `equipments`
--
ALTER TABLE `equipments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_6F6C25449D8C922A` (`gymnasium_id`);

--
-- Index pour la table `evenements`
--
ALTER TABLE `evenements`
  ADD PRIMARY KEY (`id_evenement`),
  ADD KEY `IDX_E10AD40051236D73` (`created_by_evenement`);

--
-- Index pour la table `fiche_sante`
--
ALTER TABLE `fiche_sante`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_B588D854A76ED395` (`user_id`);

--
-- Index pour la table `gymnasia`
--
ALTER TABLE `gymnasia`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `gym_ratings`
--
ALTER TABLE `gym_ratings`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_user_gym_rating` (`user_id`,`gymnasium_id`),
  ADD KEY `IDX_6AC4C32BA76ED395` (`user_id`),
  ADD KEY `IDX_6AC4C32B9D8C922A` (`gymnasium_id`);

--
-- Index pour la table `gym_subscription_offers`
--
ALTER TABLE `gym_subscription_offers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_ED9A58609D8C922A` (`gymnasium_id`);

--
-- Index pour la table `gym_subscription_orders`
--
ALTER TABLE `gym_subscription_orders`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_D2F3A1A9A76ED395` (`user_id`),
  ADD KEY `IDX_D2F3A1A953C674EE` (`offer_id`);

--
-- Index pour la table `inscription_evenement`
--
ALTER TABLE `inscription_evenement`
  ADD PRIMARY KEY (`id_inscription_inscription_evenement`),
  ADD UNIQUE KEY `uq_evenement_user` (`id_evenement_inscription_evenemen`,`id_user_inscription_evenemen`),
  ADD KEY `IDX_AD33AA0693078BDB` (`id_evenement_inscription_evenemen`),
  ADD KEY `IDX_AD33AA064765970F` (`id_user_inscription_evenemen`);

--
-- Index pour la table `ligne_commande`
--
ALTER TABLE `ligne_commande`
  ADD PRIMARY KEY (`id_ligne_ligne_commande`),
  ADD KEY `IDX_3170B74B9A406FC1` (`id_commande_ligne_commande`),
  ADD KEY `IDX_3170B74B50E4A1A9` (`id_produit_ligne_commande`);

--
-- Index pour la table `messages`
--
ALTER TABLE `messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_DB021E969AC0396` (`conversation_id`),
  ADD KEY `IDX_DB021E96F624B39D` (`sender_id`),
  ADD KEY `IDX_DB021E9614399779` (`parent_message_id`),
  ADD KEY `IDX_DB021E963ECD64BD` (`original_message_id`);

--
-- Index pour la table `messenger_messages`
--
ALTER TABLE `messenger_messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_75EA56E0FB7336F0E3BD61CE16BA31DBBF396750` (`queue_name`,`available_at`,`delivered_at`,`id`);

--
-- Index pour la table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_6000B0D3A76ED395` (`user_id`),
  ADD KEY `IDX_6000B0D3D9B71B99` (`related_message_id`),
  ADD KEY `IDX_6000B0D384C456BF` (`related_conversation_id`);

--
-- Index pour la table `notification_evenement`
--
ALTER TABLE `notification_evenement`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uniq_ne_user_event_type` (`user_id`,`evenement_id`,`type`),
  ADD KEY `idx_ne_user_read` (`user_id`,`read_at`),
  ADD KEY `IDX_ADCF2F41A76ED395` (`user_id`),
  ADD KEY `IDX_ADCF2F41FD02F13` (`evenement_id`);

--
-- Index pour la table `objectifs_hebdomadaires`
--
ALTER TABLE `objectifs_hebdomadaires`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_user_week_year` (`user_id`,`week_number`,`year`),
  ADD KEY `IDX_FB12F8E1A76ED395` (`user_id`);

--
-- Index pour la table `post`
--
ALTER TABLE `post`
  ADD PRIMARY KEY (`id_post`),
  ADD KEY `IDX_5A8A6C8DA67729A5` (`id_author_post`);

--
-- Index pour la table `post_likes`
--
ALTER TABLE `post_likes`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_user_post_like` (`user_id`,`post_id`),
  ADD KEY `post_id` (`post_id`);

--
-- Index pour la table `produits`
--
ALTER TABLE `produits`
  ADD PRIMARY KEY (`id_produit`);

--
-- Index pour la table `programmes_generes`
--
ALTER TABLE `programmes_generes`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_4D719794A76ED395` (`user_id`);

--
-- Index pour la table `resultat_evenement`
--
ALTER TABLE `resultat_evenement`
  ADD PRIMARY KEY (`id_resultat_resultat_evenement`),
  ADD UNIQUE KEY `uq_resultat_event` (`id_evenement_resultat_evenement`),
  ADD KEY `IDX_6BC040BD52B18CF6` (`published_by_resultat_evenement`);

--
-- Index pour la table `session_participants`
--
ALTER TABLE `session_participants`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_user_session` (`user_id`,`training_session_id`),
  ADD KEY `IDX_BFF4CD13A76ED395` (`user_id`),
  ADD KEY `IDX_BFF4CD13DB8156B9` (`training_session_id`);

--
-- Index pour la table `taches_quotidiennes`
--
ALTER TABLE `taches_quotidiennes`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_DB2CD843A76ED395` (`user_id`),
  ADD KEY `idx_user_date` (`user_id`,`date`);

--
-- Index pour la table `training_sessions`
--
ALTER TABLE `training_sessions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_7D058E849D8C922A` (`gymnasium_id`),
  ADD KEY `IDX_7D058E841CBE3BDD` (`coach_user_id`);

--
-- Index pour la table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id_user`),
  ADD UNIQUE KEY `UNIQ_1483A5E912A5F6CC` (`email_user`);

--
-- Index pour la table `user_gamification`
--
ALTER TABLE `user_gamification`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `user_id` (`user_id`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_points` (`points`);

--
-- Index pour la table `user_recommendation`
--
ALTER TABLE `user_recommendation`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `user_id` (`user_id`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_total_interactions` (`total_interactions`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `avis_evenement`
--
ALTER TABLE `avis_evenement`
  MODIFY `id_note_avis_evenement` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT pour la table `commandes`
--
ALTER TABLE `commandes`
  MODIFY `id_commande` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT pour la table `comment`
--
ALTER TABLE `comment`
  MODIFY `id_comment` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=32;

--
-- AUTO_INCREMENT pour la table `comment_likes`
--
ALTER TABLE `comment_likes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `conversations`
--
ALTER TABLE `conversations`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `equipments`
--
ALTER TABLE `equipments`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `evenements`
--
ALTER TABLE `evenements`
  MODIFY `id_evenement` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT pour la table `fiche_sante`
--
ALTER TABLE `fiche_sante`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `gymnasia`
--
ALTER TABLE `gymnasia`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT pour la table `gym_ratings`
--
ALTER TABLE `gym_ratings`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `gym_subscription_offers`
--
ALTER TABLE `gym_subscription_offers`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `gym_subscription_orders`
--
ALTER TABLE `gym_subscription_orders`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `inscription_evenement`
--
ALTER TABLE `inscription_evenement`
  MODIFY `id_inscription_inscription_evenement` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT pour la table `ligne_commande`
--
ALTER TABLE `ligne_commande`
  MODIFY `id_ligne_ligne_commande` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15;

--
-- AUTO_INCREMENT pour la table `messages`
--
ALTER TABLE `messages`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=31;

--
-- AUTO_INCREMENT pour la table `messenger_messages`
--
ALTER TABLE `messenger_messages`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=45;

--
-- AUTO_INCREMENT pour la table `notification_evenement`
--
ALTER TABLE `notification_evenement`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `objectifs_hebdomadaires`
--
ALTER TABLE `objectifs_hebdomadaires`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `post`
--
ALTER TABLE `post`
  MODIFY `id_post` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=45;

--
-- AUTO_INCREMENT pour la table `post_likes`
--
ALTER TABLE `post_likes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT pour la table `produits`
--
ALTER TABLE `produits`
  MODIFY `id_produit` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT pour la table `programmes_generes`
--
ALTER TABLE `programmes_generes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT pour la table `resultat_evenement`
--
ALTER TABLE `resultat_evenement`
  MODIFY `id_resultat_resultat_evenement` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `session_participants`
--
ALTER TABLE `session_participants`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `taches_quotidiennes`
--
ALTER TABLE `taches_quotidiennes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=43;

--
-- AUTO_INCREMENT pour la table `training_sessions`
--
ALTER TABLE `training_sessions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT pour la table `users`
--
ALTER TABLE `users`
  MODIFY `id_user` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT pour la table `user_gamification`
--
ALTER TABLE `user_gamification`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `user_recommendation`
--
ALTER TABLE `user_recommendation`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `avis_evenement`
--
ALTER TABLE `avis_evenement`
  ADD CONSTRAINT `FK_CF0AB43559F14502C` FOREIGN KEY (`id_evenement_avis_evenement`) REFERENCES `evenements` (`id_evenement`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_CF0AB435FDE36CE` FOREIGN KEY (`id_user_avis_evenement`) REFERENCES `users` (`id_user`) ON DELETE CASCADE;

--
-- Contraintes pour la table `commandes`
--
ALTER TABLE `commandes`
  ADD CONSTRAINT `FK_35D4282CA9525530` FOREIGN KEY (`id_client_commande`) REFERENCES `users` (`id_user`) ON DELETE CASCADE;

--
-- Contraintes pour la table `comment`
--
ALTER TABLE `comment`
  ADD CONSTRAINT `FK_9474526C4B89032C` FOREIGN KEY (`post_id`) REFERENCES `post` (`id_post`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_9474526C727ACA70` FOREIGN KEY (`parent_id`) REFERENCES `comment` (`id_comment`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_9474526CEC287029` FOREIGN KEY (`id_author_comment`) REFERENCES `users` (`id_user`);

--
-- Contraintes pour la table `conversations`
--
ALTER TABLE `conversations`
  ADD CONSTRAINT `FK_C2521BF119EB6921` FOREIGN KEY (`client_id`) REFERENCES `users` (`id_user`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_C2521BF1FEF1BA4` FOREIGN KEY (`encadrant_id`) REFERENCES `users` (`id_user`) ON DELETE SET NULL;

--
-- Contraintes pour la table `equipments`
--
ALTER TABLE `equipments`
  ADD CONSTRAINT `FK_6F6C25449D8C922A` FOREIGN KEY (`gymnasium_id`) REFERENCES `gymnasia` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `evenements`
--
ALTER TABLE `evenements`
  ADD CONSTRAINT `FK_E87F95FB84F1D73E` FOREIGN KEY (`created_by_evenement`) REFERENCES `users` (`id_user`);

--
-- Contraintes pour la table `fiche_sante`
--
ALTER TABLE `fiche_sante`
  ADD CONSTRAINT `FK_B588D854A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`);

--
-- Contraintes pour la table `gym_ratings`
--
ALTER TABLE `gym_ratings`
  ADD CONSTRAINT `FK_6AC4C32B9D8C922A` FOREIGN KEY (`gymnasium_id`) REFERENCES `gymnasia` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_6AC4C32BA76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`) ON DELETE CASCADE;

--
-- Contraintes pour la table `gym_subscription_offers`
--
ALTER TABLE `gym_subscription_offers`
  ADD CONSTRAINT `FK_7C70A9D19D8C922A` FOREIGN KEY (`gymnasium_id`) REFERENCES `gymnasia` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `gym_subscription_orders`
--
ALTER TABLE `gym_subscription_orders`
  ADD CONSTRAINT `FK_1F4F5F2605A0B0D7` FOREIGN KEY (`offer_id`) REFERENCES `gym_subscription_offers` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_1F4F5F26A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`) ON DELETE CASCADE;

--
-- Contraintes pour la table `inscription_evenement`
--
ALTER TABLE `inscription_evenement`
  ADD CONSTRAINT `FK_6A20B5DE4C3B0C20` FOREIGN KEY (`id_user_inscription_evenemen`) REFERENCES `users` (`id_user`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_6A20B5DE9F14502C` FOREIGN KEY (`id_evenement_inscription_evenemen`) REFERENCES `evenements` (`id_evenement`) ON DELETE CASCADE;

--
-- Contraintes pour la table `ligne_commande`
--
ALTER TABLE `ligne_commande`
  ADD CONSTRAINT `FK_3170B74B50E4A1A9` FOREIGN KEY (`id_produit_ligne_commande`) REFERENCES `produits` (`id_produit`),
  ADD CONSTRAINT `FK_3170B74B9A406FC1` FOREIGN KEY (`id_commande_ligne_commande`) REFERENCES `commandes` (`id_commande`) ON DELETE CASCADE;

--
-- Contraintes pour la table `messages`
--
ALTER TABLE `messages`
  ADD CONSTRAINT `FK_DB021E9614399779` FOREIGN KEY (`parent_message_id`) REFERENCES `messages` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_DB021E963ECD64BD` FOREIGN KEY (`original_message_id`) REFERENCES `messages` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_DB021E969AC0396` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_DB021E96F624B39D` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id_user`) ON DELETE CASCADE;

--
-- Contraintes pour la table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `FK_6000B0D384C456BF` FOREIGN KEY (`related_conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_6000B0D3A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_6000B0D3D9B71B99` FOREIGN KEY (`related_message_id`) REFERENCES `messages` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `notification_evenement`
--
ALTER TABLE `notification_evenement`
  ADD CONSTRAINT `FK_NE_EVENT` FOREIGN KEY (`evenement_id`) REFERENCES `evenements` (`id_evenement`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_NE_USER` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`) ON DELETE CASCADE;

--
-- Contraintes pour la table `objectifs_hebdomadaires`
--
ALTER TABLE `objectifs_hebdomadaires`
  ADD CONSTRAINT `FK_FB12F8E1A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`);

--
-- Contraintes pour la table `post`
--
ALTER TABLE `post`
  ADD CONSTRAINT `FK_5A8A6C8DA67729A5` FOREIGN KEY (`id_author_post`) REFERENCES `users` (`id_user`);

--
-- Contraintes pour la table `post_likes`
--
ALTER TABLE `post_likes`
  ADD CONSTRAINT `post_likes_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`) ON DELETE CASCADE,
  ADD CONSTRAINT `post_likes_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `post` (`id_post`) ON DELETE CASCADE;

--
-- Contraintes pour la table `programmes_generes`
--
ALTER TABLE `programmes_generes`
  ADD CONSTRAINT `FK_4D719794A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`);

--
-- Contraintes pour la table `resultat_evenement`
--
ALTER TABLE `resultat_evenement`
  ADD CONSTRAINT `FK_7E6F29C59F14502C` FOREIGN KEY (`id_evenement_resultat_evenement`) REFERENCES `evenements` (`id_evenement`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_7E6F29C5B8340A8F` FOREIGN KEY (`published_by_resultat_evenement`) REFERENCES `users` (`id_user`);

--
-- Contraintes pour la table `session_participants`
--
ALTER TABLE `session_participants`
  ADD CONSTRAINT `FK_BFF4CD13A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_BFF4CD13DB8156B9` FOREIGN KEY (`training_session_id`) REFERENCES `training_sessions` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `taches_quotidiennes`
--
ALTER TABLE `taches_quotidiennes`
  ADD CONSTRAINT `FK_DB2CD843A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`);

--
-- Contraintes pour la table `training_sessions`
--
ALTER TABLE `training_sessions`
  ADD CONSTRAINT `FK_7D058E841CBE3BDD` FOREIGN KEY (`coach_user_id`) REFERENCES `users` (`id_user`) ON DELETE SET NULL,
  ADD CONSTRAINT `FK_7D058E849D8C922A` FOREIGN KEY (`gymnasium_id`) REFERENCES `gymnasia` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `user_gamification`
--
ALTER TABLE `user_gamification`
  ADD CONSTRAINT `user_gamification_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`) ON DELETE CASCADE;

--
-- Contraintes pour la table `user_recommendation`
--
ALTER TABLE `user_recommendation`
  ADD CONSTRAINT `user_recommendation_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
