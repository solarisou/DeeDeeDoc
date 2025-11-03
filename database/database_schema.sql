-- Base de données pour gestion de candidatures
-- Stockage des CV et lettres de motivation
-- Transfert entre entreprises du groupe


-- Entreprises du groupe
CREATE TABLE entreprises (
    id_entreprise VARCHAR(50) PRIMARY KEY,
    nom_entreprise VARCHAR(255) NOT NULL,
    email_entreprise VARCHAR(255),
    date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut ENUM('active', 'inactive') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Candidats
CREATE TABLE candidats (
    id_candidature VARCHAR(50) PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    telephone VARCHAR(20) NOT NULL,
    poste_vise VARCHAR(255),
    date_disponibilite DATE,
    date_candidature DATE NOT NULL,
    autorisation_stocker ENUM('O', 'N') NOT NULL DEFAULT 'N',
    autorisation_diffuser ENUM('O', 'N') NOT NULL DEFAULT 'N',
    date_expiration_autorisation DATE,
    statut_candidature ENUM('en_attente', 'accepte', 'refuse', 'archive') DEFAULT 'en_attente',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_email (email),
    INDEX idx_nom_prenom (nom, prenom),
    INDEX idx_date_candidature (date_candidature),
    INDEX idx_statut (statut_candidature)
);

-- Fichiers CV et lettres de motivation
CREATE TABLE fichiers_candidat (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_candidature VARCHAR(50) NOT NULL,
    cv_filename VARCHAR(255) NOT NULL,
    cv_path VARCHAR(500),
    cv_size_bytes INT,
    lm_filename VARCHAR(255),
    lm_path VARCHAR(500),
    lm_size_bytes INT,
    date_upload TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INT DEFAULT 1,
    
    FOREIGN KEY (id_candidature) REFERENCES candidats(id_candidature) ON DELETE CASCADE,
    INDEX idx_candidature (id_candidature)
);

-- Diplômes
CREATE TABLE diplomes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_candidature VARCHAR(50) NOT NULL,
    nom_diplome VARCHAR(255) NOT NULL,
    annee_obtention YEAR,
    domaine VARCHAR(255),
    etablissement VARCHAR(255),
    niveau ENUM('CAP', 'BEP', 'BAC', 'BAC+2', 'BAC+3', 'BAC+5', 'BAC+8', 'Autre'),
    
    FOREIGN KEY (id_candidature) REFERENCES candidats(id_candidature) ON DELETE CASCADE,
    INDEX idx_candidature (id_candidature),
    INDEX idx_niveau (niveau),
    INDEX idx_domaine (domaine)
);

-- Expériences professionnelles
CREATE TABLE experiences (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_candidature VARCHAR(50) NOT NULL,
    nom_entreprise VARCHAR(255) NOT NULL,
    poste VARCHAR(255),
    duree_experience VARCHAR(100),
    date_debut DATE,
    date_fin DATE,
    description TEXT,
    secteur_activite VARCHAR(255),
    
    FOREIGN KEY (id_candidature) REFERENCES candidats(id_candidature) ON DELETE CASCADE,
    INDEX idx_candidature (id_candidature),
    INDEX idx_entreprise (nom_entreprise),
    INDEX idx_secteur (secteur_activite)
);

-- Compétences techniques
CREATE TABLE competences (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_candidature VARCHAR(50) NOT NULL,
    competence VARCHAR(255) NOT NULL,
    niveau ENUM('Débutant', 'Intermédiaire', 'Avancé', 'Expert') DEFAULT 'Intermédiaire',
    categorie ENUM('Technique', 'Logiciel', 'Langage_programmation', 'Framework', 'Autre') DEFAULT 'Technique',
    
    FOREIGN KEY (id_candidature) REFERENCES candidats(id_candidature) ON DELETE CASCADE,
    INDEX idx_candidature (id_candidature),
    INDEX idx_competence (competence),
    INDEX idx_categorie (categorie)
);

-- Compétences comportementales
CREATE TABLE soft_skills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_candidature VARCHAR(50) NOT NULL,
    soft_skill VARCHAR(255) NOT NULL,
    niveau ENUM('Faible', 'Moyen', 'Bon', 'Excellent') DEFAULT 'Bon',
    
    FOREIGN KEY (id_candidature) REFERENCES candidats(id_candidature) ON DELETE CASCADE,
    INDEX idx_candidature (id_candidature),
    INDEX idx_soft_skill (soft_skill)
);

-- Permis de conduire
CREATE TABLE permis_conduire (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_candidature VARCHAR(50) NOT NULL,
    categorie_permis ENUM('A', 'B', 'AM', 'C', 'D', 'F') NOT NULL,
    date_obtention DATE,
    date_expiration DATE,
    
    FOREIGN KEY (id_candidature) REFERENCES candidats(id_candidature) ON DELETE CASCADE,
    INDEX idx_candidature (id_candidature),
    INDEX idx_categorie (categorie_permis)
);

-- Langues
CREATE TABLE langues (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_candidature VARCHAR(50) NOT NULL,
    langue ENUM('Français', 'Anglais', 'Espagnol', 'Allemand', 'Italien', 'Chinois', 'Japonais', 'Russe', 'Arabe', 'Portugais') NOT NULL,
    niveau ENUM('A1', 'A2', 'B1', 'B2', 'C1', 'C2') NOT NULL,
    certification VARCHAR(255),
    
    FOREIGN KEY (id_candidature) REFERENCES candidats(id_candidature) ON DELETE CASCADE,
    INDEX idx_candidature (id_candidature),
    INDEX idx_langue (langue),
    INDEX idx_niveau (niveau)
);

-- Transferts entre entreprises
CREATE TABLE transferts_candidatures (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_candidature VARCHAR(50) NOT NULL,
    entreprise_source VARCHAR(50) NOT NULL,
    entreprise_destination VARCHAR(50) NOT NULL,
    date_transfert TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut_transfert ENUM('en_cours', 'accepte', 'refuse', 'expire') DEFAULT 'en_cours',
    commentaire TEXT,
    date_expiration TIMESTAMP,
    
    FOREIGN KEY (id_candidature) REFERENCES candidats(id_candidature) ON DELETE CASCADE,
    FOREIGN KEY (entreprise_source) REFERENCES entreprises(id_entreprise),
    FOREIGN KEY (entreprise_destination) REFERENCES entreprises(id_entreprise),
    INDEX idx_candidature (id_candidature),
    INDEX idx_source (entreprise_source),
    INDEX idx_destination (entreprise_destination),
    INDEX idx_date_transfert (date_transfert)
);

-- Réponses aux candidatures
CREATE TABLE reponses_candidatures (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_candidature VARCHAR(50) NOT NULL,
    id_entreprise VARCHAR(50) NOT NULL,
    statut_reponse ENUM('O', 'N', 'Autre') NOT NULL,
    commentaire TEXT,
    intitule_poste VARCHAR(255),
    date_decision DATE NOT NULL,
    date_reponse TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (id_candidature) REFERENCES candidats(id_candidature) ON DELETE CASCADE,
    FOREIGN KEY (id_entreprise) REFERENCES entreprises(id_entreprise),
    INDEX idx_candidature (id_candidature),
    INDEX idx_entreprise (id_entreprise),
    INDEX idx_statut (statut_reponse)
);

-- Renouvellements d'autorisation
CREATE TABLE renouvellements_autorisation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_candidature VARCHAR(50) NOT NULL,
    renouvellement_stocker ENUM('O', 'N') NOT NULL,
    renouvellement_diffuser ENUM('O', 'N') NOT NULL,
    date_demande TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_reponse TIMESTAMP,
    statut ENUM('en_attente', 'accepte', 'refuse') DEFAULT 'en_attente',
    
    FOREIGN KEY (id_candidature) REFERENCES candidats(id_candidature) ON DELETE CASCADE,
    INDEX idx_candidature (id_candidature),
    INDEX idx_date_demande (date_demande)
);

-- Postes disponibles
CREATE TABLE postes_disponibles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_entreprise VARCHAR(50) NOT NULL,
    intitule_poste VARCHAR(255) NOT NULL,
    description TEXT,
    competences_requises TEXT,
    salaire_min DECIMAL(10,2),
    salaire_max DECIMAL(10,2),
    type_contrat ENUM('CDI', 'CDD', 'Stage', 'Alternance', 'Freelance') DEFAULT 'CDI',
    localisation VARCHAR(255),
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_expiration DATE,
    statut ENUM('ouvert', 'ferme', 'pourvue') DEFAULT 'ouvert',
    
    FOREIGN KEY (id_entreprise) REFERENCES entreprises(id_entreprise),
    INDEX idx_entreprise (id_entreprise),
    INDEX idx_intitule (intitule_poste),
    INDEX idx_statut (statut)
);

-- Matching candidat-poste
CREATE TABLE matching_candidat_poste (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_candidature VARCHAR(50) NOT NULL,
    id_poste INT NOT NULL,
    score_matching DECIMAL(5,2) DEFAULT 0.00,
    date_matching TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut ENUM('automatique', 'manuel', 'valide', 'rejete') DEFAULT 'automatique',
    
    FOREIGN KEY (id_candidature) REFERENCES candidats(id_candidature) ON DELETE CASCADE,
    FOREIGN KEY (id_poste) REFERENCES postes_disponibles(id),
    INDEX idx_candidature (id_candidature),
    INDEX idx_poste (id_poste),
    INDEX idx_score (score_matching)
);

-- Logs d'activité
CREATE TABLE logs_activite (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_candidature VARCHAR(50),
    id_entreprise VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    details TEXT,
    date_action TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    utilisateur VARCHAR(100),
    
    FOREIGN KEY (id_candidature) REFERENCES candidats(id_candidature) ON DELETE SET NULL,
    FOREIGN KEY (id_entreprise) REFERENCES entreprises(id_entreprise) ON DELETE SET NULL,
    INDEX idx_candidature (id_candidature),
    INDEX idx_entreprise (id_entreprise),
    INDEX idx_date_action (date_action),
    INDEX idx_action (action)
);
