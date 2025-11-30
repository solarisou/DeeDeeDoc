-- =====================================================
-- DONNÉES D'EXEMPLE POUR L'APPLICATION DE GESTION DE CANDIDATURES
-- =====================================================

-- Insertion des entreprises du consortium
INSERT INTO entreprises (id_entreprise, nom_entreprise, email_entreprise, statut) VALUES
('0001', 'ENTGROUP1', ' ... ', 'active'),
('0002', 'ENTGROUP2', 'projetddsgroupe2@gmail.com', 'active'),
('0003', 'ENTGROUP3', ' ...', 'active'),
('0004', 'ENTGROUP4', ' ... ', 'active'),
('0005', 'ENTGROUP5', ' ... ', 'active');

-- Insertion des postes disponibles
INSERT INTO postes_disponibles (id_entreprise, intitule_poste, description, competences_requises, salaire_min, salaire_max, type_contrat, localisation, date_expiration, statut) VALUES
('0002', 'Développeur Full Stack', 'Développement d\'applications web modernes', 'JavaScript, React, Node.js, SQL', 35000.00, 45000.00, 'CDI', NULL, '2025-12-31', 'ouvert'),
('0002', 'Chef de Projet Digital', 'Pilotage de projets digitaux pour nos clients internes et partenaires.', 'Gestion de projet, Scrum, Communication, Reporting', 42000.00, 52000.00, 'CDI', NULL, '2025-11-30', 'ouvert');

-- Insertion des candidats
-- Format ID : 4 chiffres entreprise + 4 chiffres incrémentés (0001, 0002, 0003, ...)
-- Exemple : 00010001 = entreprise 0001, candidat 0001
INSERT INTO candidats (id_candidature, nom, prenom, email, telephone, poste_vise, date_disponibilite, date_candidature, autorisation_stocker, autorisation_diffuser, date_expiration_autorisation, statut_candidature, type_candidature, id_poste) VALUES
('00020001', 'GHERRAS', 'Salman Isfahani', 'isfahane2607@email.com', '0123456789', 'Développeur Full Stack', '2025-10-15', '2025-10-12', 'O', 'O', '2026-10-15', 'en_attente', 'POSTE', 1),
('00020002', 'CHERIFI', 'Sarah', 'cherifisarah232@gmail.com', '0750541678', 'Stage suivi d\'une alternance - Développement Web Full-Stack', '2026-05-01', '2025-11-05', 'O', 'O', '2026-11-05', 'en_attente', 'SPONTANEE', NULL);

-- Insertion des diplômes
INSERT INTO diplomes (id_candidature, nom_diplome, annee_obtention, domaine, niveau) VALUES
('00020002', 'Master 1 Données et Systèmes Connectés (DSC)', 2024, 'Informatique', 'BAC_plus_5'),
('00020002', 'Licence 3 Informatique classique', 2024, 'Informatique', 'BAC_plus_3'),
('00020002', 'Licence 2 Informatique parcours Alternance', 2024, 'Informatique', 'BAC_plus_2'),
('00020002', 'Licence 1 Sciences pour l\'Ingénieur parcours informatique (SPI)', 2021, 'Informatique', 'BAC_plus_2'),
('00020002', 'Baccalauréat Techniques Mathématiques', 2019, 'Génie Mécanique', 'BAC');

-- Insertion des expériences professionnelles
INSERT INTO experiences (id_candidature, nom_entreprise, duree_experience, date_debut) VALUES
('00020002', 'Jeannette', '18 mois', '2023-06-01'),
('00020002', 'Buffalo Grill', '8 mois', '2022-10-01'),
('00020002', 'Le Time', '5 mois', '2022-05-01');

-- Insertion des compétences techniques
INSERT INTO competences (id_candidature, competence, niveau, categorie) VALUES
('00020002', 'HTML5', 'Avancé', 'Langage_programmation'),
('00020002', 'CSS3', 'Avancé', 'Langage_programmation'),
('00020002', 'JavaScript', 'Avancé', 'Langage_programmation'),
('00020002', 'PHP', 'Intermédiaire', 'Langage_programmation'),
('00020002', 'Java', 'Avancé', 'Langage_programmation'),
('00020002', 'Spring Boot', 'Intermédiaire', 'Framework'),
('00020002', 'MySQL', 'Avancé', 'Logiciel'),
('00020002', 'SQLite', 'Intermédiaire', 'Logiciel'),
('00020002', 'REST', 'Avancé', 'Technique'),
('00020002', 'JWT', 'Intermédiaire', 'Technique'),
('00020002', 'C', 'Intermédiaire', 'Langage_programmation'),
('00020002', 'Python', 'Avancé', 'Langage_programmation'),
('00020002', 'Lex', 'Débutant', 'Technique'),
('00020002', 'Yacc', 'Débutant', 'Technique'),
('00020002', 'Assembleur', 'Débutant', 'Langage_programmation'),
('00020002', 'OpenGL', 'Intermédiaire', 'Framework'),
('00020002', 'Git', 'Avancé', 'Logiciel'),
('00020002', 'GitHub', 'Avancé', 'Logiciel'),
('00020002', 'GitLab', 'Avancé', 'Logiciel'),
('00020002', 'Scrum', 'Intermédiaire', 'Technique'),
('00020002', 'Architecture MVC', 'Avancé', 'Technique'),
('00020002', 'LATEX', 'Intermédiaire', 'Logiciel');

-- Insertion des soft skills (compétences comportementales)
INSERT INTO soft_skills (id_candidature, soft_skill, niveau) VALUES
('00020002', 'Esprit entrepreneurial', 'Bon'),
('00020002', 'Autonomie et initiative', 'Excellent'),
('00020002', 'Rigueur et sens du détail', 'Excellent'),
('00020002', 'Communication', 'Bon'),
('00020002', 'Travail d\'équipe', 'Excellent'),
('00020002', 'Créativité et curiosité', 'Bon');

-- Insertion des langues
INSERT INTO langues (id_candidature, langue, niveau) VALUES
('00020002', 'Français', 'C2'),
('00020002', 'Anglais', 'B2');

-- Insertion du permis de conduire
INSERT INTO permis_conduire (id_candidature, categorie_permis) VALUES
('00020002', 'B');

-- Insertion des fichiers
INSERT INTO fichiers_candidat (id_candidature, cv_filename, cv_path) VALUES
('00020002', 'CV_CHERIFI_Sarah-3.pdf', 'uploads/CV_CHERIFI_Sarah-3.pdf');


