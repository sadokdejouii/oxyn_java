-- Module Planning — objectifs libres client + analyse IA locale + recommandations boutique
-- Exécuter sur la base `oxyn` (MariaDB / MySQL).

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `objectifs_client` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `texte_objectif` text NOT NULL,
  `reponse_ia` longtext DEFAULT NULL,
  `mots_cles` varchar(512) DEFAULT NULL,
  `ids_produits_recommandes` varchar(512) DEFAULT NULL,
  `date_enregistrement` datetime NOT NULL DEFAULT current_timestamp(),
  `intervention_encadrant` text DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_objectifs_client_user_date` (`user_id`,`date_enregistrement`),
  CONSTRAINT `fk_objectifs_client_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
