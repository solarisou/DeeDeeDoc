package com.dds.gestioncandidatures.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CandidatIdGeneratorService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * Génère un nouvel ID candidat au format [id_entreprise][sequence]
     * Exemple : 00010001, 00010002, etc.
     */
    public synchronized String genererNouvelId(String idEntreprise) {
        // Récupérer le dernier ID pour cette entreprise
        String pattern = idEntreprise + "%";
        
        Integer maxSequence = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(CAST(SUBSTRING(id_candidature, 5, 4) AS UNSIGNED)), 0) " +
            "FROM candidats WHERE id_candidature LIKE ?",
            Integer.class,
            pattern
        );
        
        // Incrémenter la séquence
        int nouvelleSequence = (maxSequence != null ? maxSequence : 0) + 1;
        
        // Formater l'ID : 4 chiffres entreprise + 4 chiffres séquence
        return String.format("%s%04d", idEntreprise, nouvelleSequence);
    }
    
    /**
     * Réinitialise le compteur (utilisé quand la base est vidée)
     * Le compteur redémarre automatiquement à 0001 pour chaque entreprise
     */
    public void reinitialiserCompteur() {
        // Rien à faire - le compteur se réinitialise automatiquement
        // car on cherche le MAX dans la table
    }
}

