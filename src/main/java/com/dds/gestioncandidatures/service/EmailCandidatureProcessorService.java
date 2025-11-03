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
import com.dds.gestioncandidatures.entity.Candidat;
import com.dds.gestioncandidatures.entity.FichierCandidat;
import com.dds.gestioncandidatures.repository.CandidatRepository;
import com.dds.gestioncandidatures.repository.FichierCandidatRepository;
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
                        
                        saveAttachments(email, candidat);
                        
                        result.addSuccess(candidat.getIdCandidature());
                        logger.info("Candidature {} traitée avec succès", candidat.getIdCandidature());
                        
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
        
        candidat.setStatutCandidature(Candidat.StatutCandidature.en_attente);
        
        return candidat;
    }
    
    private LocalDate parseDate(String dateStr) {
        try {
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

