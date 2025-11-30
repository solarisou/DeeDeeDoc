-- Script pour corriger l'enum niveau dans la table diplomes
-- Modification de l'ENUM pour utiliser des noms compatibles avec Java

ALTER TABLE diplomes 
MODIFY COLUMN niveau ENUM('CAP', 'BEP', 'BAC', 'BAC_plus_2', 'BAC_plus_3', 'BAC_plus_5', 'BAC_plus_8', 'Autre');

-- Si vous avez déjà des données avec l'ancien format, mettez-les à jour
UPDATE diplomes SET niveau = 'BAC_plus_2' WHERE niveau = 'BAC+2';
UPDATE diplomes SET niveau = 'BAC_plus_3' WHERE niveau = 'BAC+3';
UPDATE diplomes SET niveau = 'BAC_plus_5' WHERE niveau = 'BAC+5';
UPDATE diplomes SET niveau = 'BAC_plus_8' WHERE niveau = 'BAC+8';







