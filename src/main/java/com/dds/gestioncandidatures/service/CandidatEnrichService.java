package com.dds.gestioncandidatures.service;

import com.dds.gestioncandidatures.dto.CandidatEnrichiDTO;
import com.dds.gestioncandidatures.entity.Candidat;
import com.dds.gestioncandidatures.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CandidatEnrichService {
    
    @Autowired
    private DiplomeRepository diplomeRepository;
    
    @Autowired
    private ExperienceRepository experienceRepository;
    
    @Autowired
    private LangueRepository langueRepository;
    
    @Autowired
    private CompetenceRepository competenceRepository;
    
    @Autowired
    private SoftSkillRepository softSkillRepository;
    
    @Autowired
    private PermisConduireRepository permisConduireRepository;
    
    @Autowired
    private FichierCandidatRepository fichierCandidatRepository;
    
    /**
     * Enrichit un candidat avec toutes ses données liées
     */
    public CandidatEnrichiDTO enrichirCandidat(Candidat candidat) {
        CandidatEnrichiDTO dto = new CandidatEnrichiDTO(candidat);
        
        String idCandidature = candidat.getIdCandidature();
        
        dto.setDiplomes(diplomeRepository.findByIdCandidature(idCandidature));
        dto.setExperiences(experienceRepository.findByIdCandidature(idCandidature));
        dto.setLangues(langueRepository.findByIdCandidature(idCandidature));
        dto.setCompetences(competenceRepository.findByIdCandidature(idCandidature));
        dto.setSoftSkills(softSkillRepository.findByIdCandidature(idCandidature));
        dto.setPermis(permisConduireRepository.findByIdCandidature(idCandidature));
        dto.setFichiers(fichierCandidatRepository.findByCandidatIdCandidature(idCandidature));
        
        return dto;
    }
    
    /**
     * Enrichit une liste de candidats
     */
    public List<CandidatEnrichiDTO> enrichirCandidats(List<Candidat> candidats) {
        return candidats.stream()
            .map(this::enrichirCandidat)
            .collect(Collectors.toList());
    }
}

