package com.dds.gestioncandidatures.service;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dds.gestioncandidatures.dto.CandidatureEmailDTO;
import com.dds.gestioncandidatures.entity.Candidat;
import com.dds.gestioncandidatures.entity.Diplome;
import com.dds.gestioncandidatures.entity.Experience;
import com.dds.gestioncandidatures.entity.FichierCandidat;
import com.dds.gestioncandidatures.repository.CandidatRepository;
import com.dds.gestioncandidatures.repository.CompetenceRepository;
import com.dds.gestioncandidatures.repository.DiplomeRepository;
import com.dds.gestioncandidatures.repository.ExperienceRepository;
import com.dds.gestioncandidatures.repository.FichierCandidatRepository;
import com.dds.gestioncandidatures.repository.LangueRepository;
import com.dds.gestioncandidatures.repository.PermisConduireRepository;
import com.dds.gestioncandidatures.repository.SoftSkillRepository;

@Service
public class CandidatSaveService {
    
    @Autowired
    private CandidatRepository candidatRepository;
    
    @Autowired
    private FichierCandidatRepository fichierCandidatRepository;
    
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
    
    @Value("${app.upload.dir}")
    private String uploadDir;
    
    @Transactional
    public void sauvegarderCandidatComplet(Candidat candidat, CandidatureEmailDTO candidatureDTO, 
                                           String cvFileName, String lmFileName, Long cvSize, Long lmSize) {
        // Sauvegarder le candidat dans la base de données
        candidatRepository.save(candidat);
        System.out.println("Candidat sauvegardé avec succès: " + candidat.getIdCandidature());
        
        // Sauvegarder les fichiers
        sauvegarderFichiers(candidat, cvFileName, lmFileName, cvSize, lmSize);
        
        // Sauvegarder les données enrichies si le JSON est disponible
        if (candidatureDTO != null) {
            sauvegarderDiplomes(candidatureDTO, candidat);
            sauvegarderExperiences(candidatureDTO, candidat);
            sauvegarderLangues(candidatureDTO, candidat);
            sauvegarderCompetences(candidatureDTO, candidat);
            sauvegarderSoftSkills(candidatureDTO, candidat);
            sauvegarderPermis(candidatureDTO, candidat);
        }
    }
    
    private void sauvegarderFichiers(Candidat candidat, String cvFileName, String lmFileName, Long cvSize, Long lmSize) {
        FichierCandidat fichierCandidat = new FichierCandidat();
        fichierCandidat.setCandidat(candidat);
        fichierCandidat.setCvFilename(cvFileName);
        fichierCandidat.setCvPath(Paths.get(uploadDir, cvFileName).toString());
        fichierCandidat.setCvSizeBytes(cvSize);
        
        if (lmFileName != null && !lmFileName.isEmpty()) {
            fichierCandidat.setLmFilename(lmFileName);
            fichierCandidat.setLmPath(Paths.get(uploadDir, lmFileName).toString());
            fichierCandidat.setLmSizeBytes(lmSize);
        }
        
        fichierCandidatRepository.save(fichierCandidat);
    }
    
    private void sauvegarderDiplomes(CandidatureEmailDTO dto, Candidat candidat) {
        if (dto.getDiplomes() == null || dto.getDiplomes().isEmpty()) {
            return;
        }
        
        for (CandidatureEmailDTO.Diplome diplomeDTO : dto.getDiplomes()) {
            try {
                Diplome diplome = new Diplome();
                diplome.setIdCandidature(candidat.getIdCandidature());
                diplome.setNomDiplome(diplomeDTO.getNomDiplome());
                
                if (diplomeDTO.getAnneeObtention() != null && !diplomeDTO.getAnneeObtention().isEmpty()) {
                    try {
                        diplome.setAnneeObtention(Integer.parseInt(diplomeDTO.getAnneeObtention()));
                    } catch (NumberFormatException e) {
                        // Ignorer si l'année n'est pas valide
                    }
                }
                
                diplome.setDomaine(diplomeDTO.getDomaine());
                
                // Déterminer le niveau
                String nomDiplome = diplomeDTO.getNomDiplome().toUpperCase();
                if (nomDiplome.contains("MASTER") || nomDiplome.contains("BAC+5")) {
                    diplome.setNiveau(Diplome.Niveau.BAC_plus_5);
                } else if (nomDiplome.contains("LICENCE") || nomDiplome.contains("BAC+3")) {
                    diplome.setNiveau(Diplome.Niveau.BAC_plus_3);
                } else if (nomDiplome.contains("BAC+2") || nomDiplome.contains("BTS") || nomDiplome.contains("DUT")) {
                    diplome.setNiveau(Diplome.Niveau.BAC_plus_2);
                } else if (nomDiplome.contains("BAC") || nomDiplome.contains("BACCALAUREAT")) {
                    diplome.setNiveau(Diplome.Niveau.BAC);
                } else {
                    diplome.setNiveau(Diplome.Niveau.Autre);
                }
                
                diplomeRepository.save(diplome);
            } catch (Exception e) {
                System.err.println("Erreur lors de la sauvegarde du diplôme: " + e.getMessage());
            }
        }
    }
    
    private void sauvegarderExperiences(CandidatureEmailDTO dto, Candidat candidat) {
        if (dto.getExperiences() == null || dto.getExperiences().isEmpty()) {
            return;
        }
        
        for (CandidatureEmailDTO.Experience expDTO : dto.getExperiences()) {
            try {
                Experience experience = new Experience();
                experience.setIdCandidature(candidat.getIdCandidature());
                experience.setNomEntreprise(expDTO.getNomEntreprise());
                experience.setDureeExperience(expDTO.getDureeExperience());
                
                if (expDTO.getDateDebut() != null && !expDTO.getDateDebut().isEmpty()) {
                    experience.setDateDebut(parseDate(expDTO.getDateDebut()));
                }
                
                experienceRepository.save(experience);
            } catch (Exception e) {
                System.err.println("Erreur lors de la sauvegarde de l'expérience: " + e.getMessage());
            }
        }
    }
    
    private void sauvegarderLangues(CandidatureEmailDTO dto, Candidat candidat) {
        if (dto.getLangues() == null || dto.getLangues().isEmpty()) {
            return;
        }
        
        for (String langueStr : dto.getLangues()) {
            try {
                com.dds.gestioncandidatures.entity.Langue langue = new com.dds.gestioncandidatures.entity.Langue();
                langue.setIdCandidature(candidat.getIdCandidature());
                
                // Parser la langue (format: "Français - bilingue" ou "Anglais - B2")
                String[] parts = langueStr.split(" - ");
                String langueNom = parts[0].trim();
                String niveauStr = parts.length > 1 ? parts[1].trim() : "B2";
                
                // Convertir en LangueType
                com.dds.gestioncandidatures.entity.Langue.LangueType langueType;
                try {
                    langueType = com.dds.gestioncandidatures.entity.Langue.LangueType.valueOf(langueNom);
                } catch (IllegalArgumentException e) {
                    langueType = com.dds.gestioncandidatures.entity.Langue.LangueType.Français;
                }
                
                // Convertir le niveau
                com.dds.gestioncandidatures.entity.Langue.Niveau niveau;
                try {
                    if (niveauStr.toUpperCase().contains("BILINGUE") || niveauStr.toUpperCase().contains("NATIF")) {
                        niveau = com.dds.gestioncandidatures.entity.Langue.Niveau.C2;
                    } else if (niveauStr.toUpperCase().startsWith("C")) {
                        niveau = com.dds.gestioncandidatures.entity.Langue.Niveau.valueOf(niveauStr.toUpperCase());
                    } else if (niveauStr.toUpperCase().startsWith("B")) {
                        niveau = com.dds.gestioncandidatures.entity.Langue.Niveau.valueOf(niveauStr.toUpperCase());
                    } else if (niveauStr.toUpperCase().startsWith("A")) {
                        niveau = com.dds.gestioncandidatures.entity.Langue.Niveau.valueOf(niveauStr.toUpperCase());
                    } else {
                        niveau = com.dds.gestioncandidatures.entity.Langue.Niveau.B2;
                    }
                } catch (IllegalArgumentException e) {
                    niveau = com.dds.gestioncandidatures.entity.Langue.Niveau.B2;
                }
                
                langue.setLangue(langueType);
                langue.setNiveau(niveau);
                langueRepository.save(langue);
            } catch (Exception e) {
                System.err.println("Erreur lors de la sauvegarde de la langue: " + e.getMessage());
            }
        }
    }
    
    private void sauvegarderCompetences(CandidatureEmailDTO dto, Candidat candidat) {
        if (dto.getCompetences() == null || dto.getCompetences().isEmpty()) {
            return;
        }
        
        for (String competenceStr : dto.getCompetences()) {
            try {
                com.dds.gestioncandidatures.entity.Competence competence = new com.dds.gestioncandidatures.entity.Competence();
                competence.setIdCandidature(candidat.getIdCandidature());
                competence.setCompetence(competenceStr);
                competenceRepository.save(competence);
            } catch (Exception e) {
                System.err.println("Erreur lors de la sauvegarde de la compétence: " + e.getMessage());
            }
        }
    }
    
    private void sauvegarderSoftSkills(CandidatureEmailDTO dto, Candidat candidat) {
        if (dto.getSoftSkills() == null || dto.getSoftSkills().isEmpty()) {
            return;
        }
        
        for (String softSkillStr : dto.getSoftSkills()) {
            try {
                com.dds.gestioncandidatures.entity.SoftSkill softSkill = new com.dds.gestioncandidatures.entity.SoftSkill();
                softSkill.setIdCandidature(candidat.getIdCandidature());
                softSkill.setSoftSkill(softSkillStr);
                softSkillRepository.save(softSkill);
            } catch (Exception e) {
                System.err.println("Erreur lors de la sauvegarde du soft skill: " + e.getMessage());
            }
        }
    }
    
    private void sauvegarderPermis(CandidatureEmailDTO dto, Candidat candidat) {
        if (dto.getPermisDeConduite() == null || dto.getPermisDeConduite().isEmpty()) {
            return;
        }
        
        for (String permisStr : dto.getPermisDeConduite()) {
            try {
                com.dds.gestioncandidatures.entity.PermisConduire permis = new com.dds.gestioncandidatures.entity.PermisConduire();
                permis.setIdCandidature(candidat.getIdCandidature());
                
                // Convertir la chaîne en enum CategoriePermis
                com.dds.gestioncandidatures.entity.PermisConduire.CategoriePermis categoriePermis;
                try {
                    categoriePermis = com.dds.gestioncandidatures.entity.PermisConduire.CategoriePermis.valueOf(permisStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    categoriePermis = com.dds.gestioncandidatures.entity.PermisConduire.CategoriePermis.B;
                }
                
                permis.setCategoriePermis(categoriePermis);
                permisConduireRepository.save(permis);
            } catch (Exception e) {
                System.err.println("Erreur lors de la sauvegarde du permis: " + e.getMessage());
            }
        }
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Gérer le format "YYYY-MM" (année-mois)
            if (dateStr.matches("\\d{4}-\\d{2}")) {
                String[] parts = dateStr.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                return LocalDate.of(year, month, 1); // Premier jour du mois
            }
            
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ISO_DATE
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDate.parse(dateStr, formatter);
                } catch (Exception e) {
                    // Continuer avec le prochain format
                }
            }
        } catch (Exception e) {
            System.err.println("Impossible de parser la date : " + dateStr);
        }
        
        return null;
    }
}

