-- Vues et procédures stockées

-- Vue complète des candidats
CREATE VIEW vue_candidats_complets AS
SELECT 
    c.id_candidature,
    c.nom,
    c.prenom,
    c.email,
    c.telephone,
    c.poste_vise,
    c.date_disponibilite,
    c.date_candidature,
    c.autorisation_stocker,
    c.autorisation_diffuser,
    c.date_expiration_autorisation,
    c.statut_candidature,
    f.cv_filename,
    f.lm_filename,
    f.date_upload,
    COUNT(DISTINCT d.id) as nb_diplomes,
    COUNT(DISTINCT e.id) as nb_experiences,
    COUNT(DISTINCT comp.id) as nb_competences,
    COUNT(DISTINCT l.id) as nb_langues
FROM candidats c
LEFT JOIN fichiers_candidat f ON c.id_candidature = f.id_candidature
LEFT JOIN diplomes d ON c.id_candidature = d.id_candidature
LEFT JOIN experiences e ON c.id_candidature = e.id_candidature
LEFT JOIN competences comp ON c.id_candidature = comp.id_candidature
LEFT JOIN langues l ON c.id_candidature = l.id_candidature
GROUP BY c.id_candidature, f.id;

-- Candidats avec autorisations expirées
CREATE VIEW vue_autorisations_expirees AS
SELECT 
    c.id_candidature,
    c.nom,
    c.prenom,
    c.email,
    c.date_expiration_autorisation,
    DATEDIFF(CURDATE(), c.date_expiration_autorisation) as jours_expiration
FROM candidats c
WHERE c.date_expiration_autorisation < CURDATE()
AND c.autorisation_stocker = 'O';

-- Matching candidat-poste
CREATE VIEW vue_matching_detaille AS
SELECT 
    m.id,
    c.nom,
    c.prenom,
    c.email,
    c.poste_vise as poste_candidat,
    p.intitule_poste,
    p.localisation,
    e.nom_entreprise,
    m.score_matching,
    m.statut as statut_matching,
    m.date_matching
FROM matching_candidat_poste m
JOIN candidats c ON m.id_candidature = c.id_candidature
JOIN postes_disponibles p ON m.id_poste = p.id
JOIN entreprises e ON p.id_entreprise = e.id_entreprise
ORDER BY m.score_matching DESC;

-- Transferts en cours
CREATE VIEW vue_transferts_en_cours AS
SELECT 
    t.id,
    c.nom,
    c.prenom,
    c.email,
    c.poste_vise,
    es.nom_entreprise as entreprise_source,
    ed.nom_entreprise as entreprise_destination,
    t.date_transfert,
    t.date_expiration,
    t.commentaire,
    DATEDIFF(t.date_expiration, CURDATE()) as jours_restants
FROM transferts_candidatures t
JOIN candidats c ON t.id_candidature = c.id_candidature
JOIN entreprises es ON t.entreprise_source = es.id_entreprise
JOIN entreprises ed ON t.entreprise_destination = ed.id_entreprise
WHERE t.statut_transfert = 'en_cours'
AND t.date_expiration > CURDATE();

-- Statistiques par entreprise
CREATE VIEW vue_stats_entreprises AS
SELECT 
    e.id_entreprise,
    e.nom_entreprise,
    COUNT(DISTINCT p.id) as nb_postes_ouverts,
    COUNT(DISTINCT t_source.id) as nb_transferts_envoyes,
    COUNT(DISTINCT t_dest.id) as nb_transferts_recus,
    COUNT(DISTINCT r.id) as nb_reponses_donnees
FROM entreprises e
LEFT JOIN postes_disponibles p ON e.id_entreprise = p.id_entreprise AND p.statut = 'ouvert'
LEFT JOIN transferts_candidatures t_source ON e.id_entreprise = t_source.entreprise_source
LEFT JOIN transferts_candidatures t_dest ON e.id_entreprise = t_dest.entreprise_destination
LEFT JOIN reponses_candidatures r ON e.id_entreprise = r.id_entreprise
WHERE e.statut = 'active'
GROUP BY e.id_entreprise, e.nom_entreprise;

DELIMITER //

-- Créer une nouvelle candidature
CREATE PROCEDURE CreerCandidature(
    IN p_id_candidature VARCHAR(50),
    IN p_nom VARCHAR(100),
    IN p_prenom VARCHAR(100),
    IN p_email VARCHAR(255),
    IN p_telephone VARCHAR(20),
    IN p_poste_vise VARCHAR(255),
    IN p_date_disponibilite DATE,
    IN p_autorisation_stocker ENUM('O', 'N'),
    IN p_autorisation_diffuser ENUM('O', 'N'),
    IN p_cv_filename VARCHAR(255),
    IN p_lm_filename VARCHAR(255)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- Insertion du candidat
    INSERT INTO candidats (
        id_candidature, nom, prenom, email, telephone, poste_vise, 
        date_disponibilite, date_candidature, autorisation_stocker, 
        autorisation_diffuser, date_expiration_autorisation, statut_candidature
    ) VALUES (
        p_id_candidature, p_nom, p_prenom, p_email, p_telephone, p_poste_vise,
        p_date_disponibilite, CURDATE(), p_autorisation_stocker, p_autorisation_diffuser,
        DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 'en_attente'
    );
    
    -- Insertion des fichiers
    INSERT INTO fichiers_candidat (
        id_candidature, cv_filename, lm_filename, date_upload
    ) VALUES (
        p_id_candidature, p_cv_filename, p_lm_filename, NOW()
    );
    
    -- Log de l'activité
    INSERT INTO logs_activite (
        id_candidature, action, details, utilisateur
    ) VALUES (
        p_id_candidature, 'CANDIDATURE_CREEE', 
        CONCAT('Nouvelle candidature créée pour ', p_prenom, ' ', p_nom), 
        'system'
    );
    
    COMMIT;
END //

-- Transférer une candidature
CREATE PROCEDURE TransfererCandidature(
    IN p_id_candidature VARCHAR(50),
    IN p_entreprise_source VARCHAR(50),
    IN p_entreprise_destination VARCHAR(50),
    IN p_commentaire TEXT,
    IN p_duree_jours INT
)
BEGIN
    DECLARE v_autorisation_diffuser ENUM('O', 'N');
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- Vérifier l'autorisation de diffusion
    SELECT autorisation_diffuser INTO v_autorisation_diffuser
    FROM candidats 
    WHERE id_candidature = p_id_candidature;
    
    IF v_autorisation_diffuser = 'N' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Le candidat n\'autorise pas la diffusion de ses données';
    END IF;
    
    -- Créer le transfert
    INSERT INTO transferts_candidatures (
        id_candidature, entreprise_source, entreprise_destination,
        statut_transfert, commentaire, date_expiration
    ) VALUES (
        p_id_candidature, p_entreprise_source, p_entreprise_destination,
        'en_cours', p_commentaire, DATE_ADD(NOW(), INTERVAL p_duree_jours DAY)
    );
    
    -- Log de l'activité
    INSERT INTO logs_activite (
        id_candidature, id_entreprise, action, details, utilisateur
    ) VALUES (
        p_id_candidature, p_entreprise_source, 'TRANSFERT_DEMANDE',
        CONCAT('Transfert demandé vers ', p_entreprise_destination), 'system'
    );
    
    COMMIT;
END //

-- Calculer le score de matching
CREATE PROCEDURE CalculerScoreMatching(
    IN p_id_candidature VARCHAR(50),
    IN p_id_poste INT,
    OUT p_score DECIMAL(5,2)
)
BEGIN
    DECLARE v_score_competences DECIMAL(5,2) DEFAULT 0;
    DECLARE v_score_experience DECIMAL(5,2) DEFAULT 0;
    DECLARE v_score_diplome DECIMAL(5,2) DEFAULT 0;
    DECLARE v_nb_competences_requises INT DEFAULT 0;
    DECLARE v_nb_competences_candidat INT DEFAULT 0;
    
    -- Score basé sur les compétences (50% du score total)
    SELECT COUNT(*) INTO v_nb_competences_requises
    FROM postes_disponibles p
    WHERE p.id = p_id_poste
    AND p.competences_requises IS NOT NULL;
    
    IF v_nb_competences_requises > 0 THEN
        SELECT COUNT(DISTINCT c.competence) INTO v_nb_competences_candidat
        FROM competences c
        JOIN postes_disponibles p ON p.id = p_id_poste
        WHERE c.id_candidature = p_id_candidature
        AND FIND_IN_SET(c.competence, REPLACE(p.competences_requises, ', ', ',')) > 0;
        
        SET v_score_competences = (v_nb_competences_candidat / v_nb_competences_requises) * 50;
    END IF;
    
    -- Score basé sur l'expérience (30% du score total)
    SELECT COUNT(*) * 15 INTO v_score_experience
    FROM experiences e
    WHERE e.id_candidature = p_id_candidature
    AND DATEDIFF(IFNULL(e.date_fin, CURDATE()), e.date_debut) >= 365
    LIMIT 2; -- Maximum 2 expériences comptées
    
    -- Score basé sur le diplôme (20% du score total)
    SELECT 
        CASE 
            WHEN MAX(CASE WHEN d.niveau = 'BAC+8' THEN 20
                         WHEN d.niveau = 'BAC+5' THEN 18
                         WHEN d.niveau = 'BAC+3' THEN 15
                         WHEN d.niveau = 'BAC+2' THEN 12
                         ELSE 8 END) IS NOT NULL 
            THEN MAX(CASE WHEN d.niveau = 'BAC+8' THEN 20
                         WHEN d.niveau = 'BAC+5' THEN 18
                         WHEN d.niveau = 'BAC+3' THEN 15
                         WHEN d.niveau = 'BAC+2' THEN 12
                         ELSE 8 END)
            ELSE 0 
        END INTO v_score_diplome
    FROM diplomes d
    WHERE d.id_candidature = p_id_candidature;
    
    -- Score final
    SET p_score = LEAST(100, v_score_competences + v_score_experience + v_score_diplome);
    
    -- Insérer ou mettre à jour le matching
    INSERT INTO matching_candidat_poste (id_candidature, id_poste, score_matching, statut)
    VALUES (p_id_candidature, p_id_poste, p_score, 'automatique')
    ON DUPLICATE KEY UPDATE 
        score_matching = p_score,
        date_matching = NOW();
        
END //

-- Nettoyer les données expirées
CREATE PROCEDURE NettoyerDonneesExpirees()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- Supprimer les candidatures avec autorisation expirée depuis plus de 30 jours
    DELETE FROM candidats 
    WHERE date_expiration_autorisation < DATE_SUB(CURDATE(), INTERVAL 30 DAY)
    AND autorisation_stocker = 'O';
    
    -- Marquer comme expirés les transferts dépassés
    UPDATE transferts_candidatures 
    SET statut_transfert = 'expire'
    WHERE date_expiration < NOW()
    AND statut_transfert = 'en_cours';
    
    -- Nettoyer les logs anciens (plus de 2 ans)
    DELETE FROM logs_activite 
    WHERE date_action < DATE_SUB(CURDATE(), INTERVAL 2 YEAR);
    
    COMMIT;
    
    -- Log de l'activité de nettoyage
    INSERT INTO logs_activite (action, details, utilisateur)
    VALUES ('NETTOYAGE_DONNEES', 'Nettoyage automatique des données expirées', 'system');
    
END //

-- Générer un rapport de candidatures
CREATE PROCEDURE GenererRapportCandidatures(
    IN p_date_debut DATE,
    IN p_date_fin DATE,
    IN p_id_entreprise VARCHAR(50)
)
BEGIN
    SELECT 
        'Résumé des candidatures' as type_rapport,
        COUNT(*) as total_candidatures,
        COUNT(CASE WHEN c.statut_candidature = 'en_attente' THEN 1 END) as en_attente,
        COUNT(CASE WHEN c.statut_candidature = 'accepte' THEN 1 END) as acceptees,
        COUNT(CASE WHEN c.statut_candidature = 'refuse' THEN 1 END) as refusees,
        COUNT(CASE WHEN c.autorisation_diffuser = 'O' THEN 1 END) as autorisation_diffusion
    FROM candidats c
    LEFT JOIN transferts_candidatures t ON c.id_candidature = t.id_candidature
    WHERE c.date_candidature BETWEEN p_date_debut AND p_date_fin
    AND (p_id_entreprise IS NULL OR t.entreprise_destination = p_id_entreprise OR t.entreprise_source = p_id_entreprise)
    
    UNION ALL
    
    SELECT 
        'Détail par poste' as type_rapport,
        p.id as poste_id,
        p.intitule_poste,
        COUNT(m.id_candidature) as nb_candidatures,
        AVG(m.score_matching) as score_moyen,
        MAX(m.score_matching) as meilleur_score
    FROM postes_disponibles p
    LEFT JOIN matching_candidat_poste m ON p.id = m.id_poste
    LEFT JOIN candidats c ON m.id_candidature = c.id_candidature
    WHERE (p_id_entreprise IS NULL OR p.id_entreprise = p_id_entreprise)
    AND (c.date_candidature IS NULL OR c.date_candidature BETWEEN p_date_debut AND p_date_fin)
    GROUP BY p.id, p.intitule_poste;
    
END //

DELIMITER ;

-- Index pour optimisation
CREATE INDEX idx_candidat_statut_date ON candidats(statut_candidature, date_candidature);
CREATE INDEX idx_transfert_statut_expiration ON transferts_candidatures(statut_transfert, date_expiration);
CREATE INDEX idx_matching_score ON matching_candidat_poste(score_matching DESC);
CREATE INDEX idx_poste_entreprise_statut ON postes_disponibles(id_entreprise, statut);

DELIMITER //

-- Trigger mise à jour timestamp
CREATE TRIGGER tr_candidats_update_timestamp
    BEFORE UPDATE ON candidats
    FOR EACH ROW
BEGIN
    SET NEW.updated_at = CURRENT_TIMESTAMP;
END //

-- Trigger vérification autorisations
CREATE TRIGGER tr_candidats_check_autorisation
    BEFORE INSERT ON candidats
    FOR EACH ROW
BEGIN
    IF NEW.autorisation_diffuser = 'O' AND NEW.autorisation_stocker = 'N' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Impossible d\'autoriser la diffusion sans autoriser le stockage';
    END IF;
END //

-- Trigger log suppressions
CREATE TRIGGER tr_candidats_delete_log
    BEFORE DELETE ON candidats
    FOR EACH ROW
BEGIN
    INSERT INTO logs_activite (id_candidature, action, details, utilisateur)
    VALUES (OLD.id_candidature, 'CANDIDATURE_SUPPRIMEE', 
            CONCAT('Suppression de la candidature de ', OLD.prenom, ' ', OLD.nom), 
            USER());
END //

DELIMITER ;
