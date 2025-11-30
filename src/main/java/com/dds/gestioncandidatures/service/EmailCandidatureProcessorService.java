package com.dds.gestioncandidatures.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dds.gestioncandidatures.dto.CandidatureEmailDTO;
import com.dds.gestioncandidatures.entity.*;
import com.dds.gestioncandidatures.repository.*;
import com.dds.gestioncandidatures.service.EmailReaderService.AttachmentInfo;
import com.dds.gestioncandidatures.service.EmailReaderService.EmailContent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class EmailCandidatureProcessorService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailCandidatureProcessorService.class);
    
    @Autowired
    private EmailReaderService emailReaderService;
    
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
    
    @Autowired
    private PosteRepository posteRepository;
    
    @Value("${app.upload.dir}")
    private String uploadDir;
    
    private final ObjectMapper objectMapper;
    
    public EmailCandidatureProcessorService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Transactional
    public ProcessingResult processUnreadEmails() {
        ProcessingResult result = new ProcessingResult();
        
        try {
            List<EmailContent> emails = emailReaderService.readUnreadEmails();
            result.setTotalEmails(emails.size());
            
            logger.info("Traitement de {} emails non lus", emails.size());
            
            for (EmailContent email : emails) {
                try {
                    String jsonContent = extractJsonFromEmailContent(email.getContent());
                    
                    if (jsonContent != null && !jsonContent.isEmpty()) {
                        CandidatureEmailDTO candidatureDTO = objectMapper.readValue(jsonContent, CandidatureEmailDTO.class);
                        
                        Candidat candidat = convertToCandidat(candidatureDTO, email);
                        
                        if (candidatRepository.existsById(candidat.getIdCandidature())) {
                            logger.warn("Candidature {} existe déjà, ignorée", candidat.getIdCandidature());
                            result.addSkipped(candidat.getIdCandidature());
                            continue;
                        }
                        
                        candidatRepository.save(candidat);
                        
                        // Sauvegarder toutes les données liées
                        saveDiplomes(candidatureDTO, candidat);
                        saveExperiences(candidatureDTO, candidat);
                        saveLangues(candidatureDTO, candidat);
                        saveCompetences(candidatureDTO, candidat);
                        saveSoftSkills(candidatureDTO, candidat);
                        savePermis(candidatureDTO, candidat);
                        
                        saveAttachments(email, candidat);
                        
                        result.addSuccess(candidat.getIdCandidature());
                        logger.info("Candidature {} traitée avec succès (avec toutes les données liées)", candidat.getIdCandidature());
                        
                    } else {
                        logger.warn("Aucun JSON trouvé dans l'email : {}", email.getSubject());
                        result.addError("Email sans JSON: " + email.getSubject());
                    }
                    
                } catch (Exception e) {
                    logger.error("Erreur lors du traitement de l'email : {}", email.getSubject(), e);
                    result.addError("Erreur email '" + email.getSubject() + "': " + e.getMessage());
                }
            }
            
            if (!emails.isEmpty()) {
                emailReaderService.markEmailsAsRead(emails.size());
            }
            
        } catch (Exception e) {
            logger.error("Erreur globale lors du traitement des emails", e);
            result.addError("Erreur globale: " + e.getMessage());
        }
        
        return result;
    }
    
    private String extractJsonFromEmailContent(String content) {
        Pattern pattern = Pattern.compile("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            String json = matcher.group();
            json = json.replaceAll("<[^>]*>", "");
            return json.trim();
        }
        
        return null;
    }
    
    private Candidat convertToCandidat(CandidatureEmailDTO dto, EmailContent email) {
        Candidat candidat = new Candidat();
        
        candidat.setIdCandidature(dto.getIdCandidature());
        candidat.setNom(dto.getNom());
        candidat.setPrenom(dto.getPrenom());
        candidat.setEmail(dto.getMail() != null ? dto.getMail() : extractEmailFromSender(email.getFrom()));
        candidat.setTelephone(dto.getTelephone());
        candidat.setPosteVise(dto.getPosteVise());
        candidat.setTypeCandidature(Candidat.TypeCandidature.SPONTANEE);
        
        if (dto.getDateDisponibilite() != null) {
            candidat.setDateDisponibilite(parseDate(dto.getDateDisponibilite()));
        }
        
        if (dto.getDateCandidature() != null) {
            candidat.setDateCandidature(parseDate(dto.getDateCandidature()));
        } else {
            candidat.setDateCandidature(LocalDate.now());
        }
        
        if (dto.getMessageCandidatEntreprise() != null) {
            String autoStoc = dto.getMessageCandidatEntreprise().getAutorisationStocker();
            String autoDiff = dto.getMessageCandidatEntreprise().getAutorisationDiffuser();
            
            if ("O".equalsIgnoreCase(autoStoc)) {
                candidat.setAutorisationStocker(Candidat.Autorisation.O);
                candidat.setDateExpirationAutorisation(LocalDate.now().plusYears(2));
            }
            
            if ("O".equalsIgnoreCase(autoDiff)) {
                candidat.setAutorisationDiffuser(Candidat.Autorisation.O);
            }
            
            if (candidat.getPosteVise() == null) {
                candidat.setPosteVise(dto.getMessageCandidatEntreprise().getPosteVise());
            }
        }

        if (candidat.getPosteVise() != null && !candidat.getPosteVise().isBlank()) {
            posteRepository.findFirstByIntitulePosteIgnoreCaseAndStatut(
                    candidat.getPosteVise(),
                    Poste.Statut.ouvert)
                .ifPresent(candidat::setPoste);
        }
        
        candidat.setStatutCandidature(Candidat.StatutCandidature.en_attente);
        
        return candidat;
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDate.now();
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
                }
            }
        } catch (Exception e) {
            logger.warn("Impossible de parser la date : {}", dateStr);
        }
        
        return LocalDate.now();
    }
    
    private String extractEmailFromSender(String from) {
        Pattern pattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher matcher = pattern.matcher(from);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return from;
    }
    
    private void saveAttachments(EmailContent email, Candidat candidat) {
        if (email.getAttachments() == null || email.getAttachments().isEmpty()) {
            return;
        }
        
        File uploadFolder = new File(uploadDir);
        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs();
        }
        
        for (AttachmentInfo attachment : email.getAttachments()) {
            try {
                String fileName = attachment.getFileName();
                
                if (fileName == null || (!fileName.toLowerCase().endsWith(".pdf"))) {
                    continue;
                }
                
                String uniqueFileName = candidat.getIdCandidature() + "_" + System.currentTimeMillis() + "_" + fileName;
                String filePath = uploadDir + uniqueFileName;
                File file = new File(filePath);
                
                long fileSize = 0;
                try (InputStream is = attachment.getInputStream();
                     FileOutputStream fos = new FileOutputStream(file)) {
                    
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        fileSize += bytesRead;
                    }
                }
                
                FichierCandidat fichierCandidat = fichierCandidatRepository.findFirstByCandidatIdCandidatureOrderByVersionDesc(candidat.getIdCandidature());
                if (fichierCandidat == null) {
                    fichierCandidat = new FichierCandidat();
                }
                
                fichierCandidat.setCandidat(candidat);
                
                if (fileName.toLowerCase().contains("cv")) {
                    fichierCandidat.setCvFilename(uniqueFileName);
                    fichierCandidat.setCvPath(filePath);
                    fichierCandidat.setCvSizeBytes(fileSize);
                } else if (fileName.toLowerCase().contains("lm") || 
                           fileName.toLowerCase().contains("lettre") ||
                           fileName.toLowerCase().contains("motivation")) {
                    fichierCandidat.setLmFilename(uniqueFileName);
                    fichierCandidat.setLmPath(filePath);
                    fichierCandidat.setLmSizeBytes(fileSize);
                } else {
                    fichierCandidat.setCvFilename(uniqueFileName);
                    fichierCandidat.setCvPath(filePath);
                    fichierCandidat.setCvSizeBytes(fileSize);
                }
                
                fichierCandidatRepository.save(fichierCandidat);
                
                logger.info("Fichier sauvegardé : {}", uniqueFileName);
                
            } catch (IOException e) {
                logger.error("Erreur lors de la sauvegarde du fichier : {}", attachment.getFileName(), e);
            }
        }
    }
    
    private void saveDiplomes(CandidatureEmailDTO dto, Candidat candidat) {
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
                        logger.warn("Année diplôme invalide : {}", diplomeDTO.getAnneeObtention());
                    }
                }
                
                diplome.setDomaine(diplomeDTO.getDomaine());
                
                // Déterminer le niveau à partir du nom du diplôme
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
                logger.debug("Diplôme sauvegardé : {}", diplome.getNomDiplome());
            } catch (Exception e) {
                logger.error("Erreur lors de la sauvegarde du diplôme : {}", diplomeDTO.getNomDiplome(), e);
            }
        }
    }
    
    private void saveExperiences(CandidatureEmailDTO dto, Candidat candidat) {
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
                logger.debug("Expérience sauvegardée : {}", experience.getNomEntreprise());
            } catch (Exception e) {
                logger.error("Erreur lors de la sauvegarde de l'expérience : {}", expDTO.getNomEntreprise(), e);
            }
        }
    }
    
    private void saveLangues(CandidatureEmailDTO dto, Candidat candidat) {
        if (dto.getLangues() == null || dto.getLangues().isEmpty()) {
            return;
        }
        
        for (String langueStr : dto.getLangues()) {
            try {
                Langue langue = new Langue();
                langue.setIdCandidature(candidat.getIdCandidature());
                
                // Parser le format "Langue-Niveau" ou juste "Langue"
                String langueNom;
                String niveau = "B1"; // Par défaut B1 si non spécifié
                
                if (langueStr.contains("-")) {
                    // Format "Langue-Niveau"
                    String[] parts = langueStr.split("-", 2);
                    langueNom = parts[0].trim();
                    niveau = parts[1].trim();
                } else {
                    // Format simple "Langue"
                    langueNom = langueStr.trim();
                    // Pour Français, niveau C2 par défaut, sinon B2
                    if (langueNom.equalsIgnoreCase("Français")) {
                        niveau = "C2";
                    } else {
                        niveau = "B2";
                    }
                }
                
                // Mapper le nom de la langue
                try {
                    langue.setLangue(Langue.LangueType.valueOf(langueNom));
                } catch (IllegalArgumentException e) {
                    // Si la langue n'est pas dans l'enum, utiliser Français par défaut
                    logger.warn("Langue non reconnue : {}, utilisation de Français", langueNom);
                    langue.setLangue(Langue.LangueType.Français);
                }
                
                // Mapper le niveau
                try {
                    langue.setNiveau(Langue.Niveau.valueOf(niveau));
                } catch (IllegalArgumentException e) {
                    // Si le niveau n'est pas valide, utiliser B1 par défaut
                    logger.warn("Niveau non reconnu : {}, utilisation de B1", niveau);
                    langue.setNiveau(Langue.Niveau.B1);
                }
                
                langueRepository.save(langue);
                logger.debug("Langue sauvegardée : {} ({})", langue.getLangue(), langue.getNiveau());
            } catch (Exception e) {
                logger.error("Erreur lors de la sauvegarde de la langue : {}", langueStr, e);
            }
        }
    }
    
    private void saveCompetences(CandidatureEmailDTO dto, Candidat candidat) {
        if (dto.getCompetences() == null || dto.getCompetences().isEmpty()) {
            return;
        }
        
        for (String competenceStr : dto.getCompetences()) {
            try {
                Competence competence = new Competence();
                competence.setIdCandidature(candidat.getIdCandidature());
                competence.setCompetence(competenceStr);
                competence.setNiveau(Competence.Niveau.Intermédiaire); // Par défaut
                
                // Déterminer la catégorie selon le nom
                String comp = competenceStr.toUpperCase();
                if (comp.contains("SPRING") || comp.contains("REACT") || comp.contains("ANGULAR")) {
                    competence.setCategorie(Competence.Categorie.Framework);
                } else if (comp.contains("JAVA") || comp.contains("PYTHON") || comp.contains("JAVASCRIPT") || 
                           comp.contains("C++") || comp.contains("C#")) {
                    competence.setCategorie(Competence.Categorie.Langage_programmation);
                } else if (comp.contains("GIT") || comp.contains("DOCKER") || comp.contains("MYSQL") || 
                           comp.contains("POSTGRESQL")) {
                    competence.setCategorie(Competence.Categorie.Logiciel);
                } else {
                    competence.setCategorie(Competence.Categorie.Technique);
                }
                
                competenceRepository.save(competence);
                logger.debug("Compétence sauvegardée : {}", competence.getCompetence());
            } catch (Exception e) {
                logger.error("Erreur lors de la sauvegarde de la compétence : {}", competenceStr, e);
            }
        }
    }
    
    private void saveSoftSkills(CandidatureEmailDTO dto, Candidat candidat) {
        if (dto.getSoftSkills() == null || dto.getSoftSkills().isEmpty()) {
            return;
        }
        
        for (String softSkillStr : dto.getSoftSkills()) {
            try {
                SoftSkill softSkill = new SoftSkill();
                softSkill.setIdCandidature(candidat.getIdCandidature());
                softSkill.setSoftSkill(softSkillStr);
                softSkill.setNiveau(SoftSkill.Niveau.Bon); // Par défaut
                
                softSkillRepository.save(softSkill);
                logger.debug("Soft skill sauvegardé : {}", softSkill.getSoftSkill());
            } catch (Exception e) {
                logger.error("Erreur lors de la sauvegarde du soft skill : {}", softSkillStr, e);
            }
        }
    }
    
    private void savePermis(CandidatureEmailDTO dto, Candidat candidat) {
        if (dto.getPermisDeConduite() == null || dto.getPermisDeConduite().isEmpty()) {
            return;
        }
        
        for (String permisStr : dto.getPermisDeConduite()) {
            try {
                PermisConduire permis = new PermisConduire();
                permis.setIdCandidature(candidat.getIdCandidature());
                
                // Mapper le permis
                try {
                    permis.setCategoriePermis(PermisConduire.CategoriePermis.valueOf(permisStr.trim()));
                } catch (IllegalArgumentException e) {
                    logger.warn("Permis non reconnu : {}, utilisation de B par défaut", permisStr);
                    permis.setCategoriePermis(PermisConduire.CategoriePermis.B);
                }
                
                permisConduireRepository.save(permis);
                logger.debug("Permis sauvegardé : {}", permis.getCategoriePermis());
            } catch (Exception e) {
                logger.error("Erreur lors de la sauvegarde du permis : {}", permisStr, e);
            }
        }
    }
    
    public static class ProcessingResult {
        private int totalEmails;
        private final List<String> successfulCandidatures = new ArrayList<>();
        private final List<String> skippedCandidatures = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        
        public int getTotalEmails() { return totalEmails; }
        public void setTotalEmails(int totalEmails) { this.totalEmails = totalEmails; }
        
        public List<String> getSuccessfulCandidatures() { return successfulCandidatures; }
        public void addSuccess(String id) { this.successfulCandidatures.add(id); }
        
        public List<String> getSkippedCandidatures() { return skippedCandidatures; }
        public void addSkipped(String id) { this.skippedCandidatures.add(id); }
        
        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }
        
        public int getSuccessCount() { return successfulCandidatures.size(); }
        public int getSkippedCount() { return skippedCandidatures.size(); }
        public int getErrorCount() { return errors.size(); }
    }
}

