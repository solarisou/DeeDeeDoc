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
('0001', 'Développeur Full Stack', 'Développement d\'applications web modernes', 'JavaScript, React, Node.js, SQL', 35000.00, 45000.00, 'CDI', 'Paris', '2024-12-31', 'ouvert');

-- Insertion des candidats
-- Format ID : 4 chiffres entreprise + 4 chiffres incrémentés (0001, 0002, 0003, ...)
-- Exemple : 00010001 = entreprise 0001, candidat 0001
INSERT INTO candidats (id_candidature, nom, prenom, email, telephone, poste_vise, date_disponibilite, date_candidature, autorisation_stocker, autorisation_diffuser, date_expiration_autorisation, statut_candidature) VALUES
('00020001', 'GHERRAS', 'Salman Isfahani', 'isfahane2607@email.com', '0123456789', 'Développeur Full Stack', '2025-10-15', '2025-10-12', 'O', 'O', '2026-10-15', 'en_attente');

