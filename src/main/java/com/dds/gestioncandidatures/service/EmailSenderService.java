package com.dds.gestioncandidatures.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dds.gestioncandidatures.dto.CandidatEnrichiDTO;
import com.dds.gestioncandidatures.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;

@Service
public class EmailSenderService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailSenderService.class);
    
    @Value("${smtp.host}")
    private String smtpHost;
    
    @Value("${smtp.port}")
    private String smtpPort;
    
    @Value("${smtp.username}")
    private String smtpUsername;
    
    @Value("${smtp.password}")
    private String smtpPassword;
    
    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;
    
    @Autowired
    private CandidatEnrichService candidatEnrichService;
    
    private final ObjectMapper objectMapper;
    
    public EmailSenderService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public void envoyerCandidatureParEmail(Candidat candidat, Entreprise entrepriseDestination, String commentaire) {
        try {
            // Enrichir le candidat avec toutes ses données
            CandidatEnrichiDTO candidatEnrichi = candidatEnrichService.enrichirCandidat(candidat);
            
            // Construire le JSON complet de la candidature
            Map<String, Object> candidatureJson = construireCandidatureJsonComplet(candidatEnrichi);
            
            // Convertir en JSON
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(candidatureJson);
            
            // Préparer les pièces jointes
            List<File> pieceJointes = new ArrayList<>();
            List<String> nomsAttachments = new ArrayList<>();
            
            if (candidatEnrichi.getFichiers() != null && !candidatEnrichi.getFichiers().isEmpty()) {
                FichierCandidat fichier = candidatEnrichi.getFichiers().get(0);
                
                // Ajouter le CV
                File cvFile = new File(uploadDir + fichier.getCvFilename());
                if (cvFile.exists()) {
                    pieceJointes.add(cvFile);
                    nomsAttachments.add(fichier.getCvFilename());
                    logger.info("CV trouvé : {}", cvFile.getAbsolutePath());
                } else {
                    logger.warn("CV non trouvé : {}", cvFile.getAbsolutePath());
                }
                
                // Ajouter la lettre de motivation si elle existe
                if (fichier.getLmFilename() != null) {
                    File lmFile = new File(uploadDir + fichier.getLmFilename());
                    if (lmFile.exists()) {
                        pieceJointes.add(lmFile);
                        nomsAttachments.add(fichier.getLmFilename());
                        logger.info("LM trouvée : {}", lmFile.getAbsolutePath());
                    } else {
                        logger.warn("LM non trouvée : {}", lmFile.getAbsolutePath());
                    }
                }
            }
            
            // Envoyer l'email avec pièces jointes
            envoyerEmail(
                entrepriseDestination.getEmailEntreprise(),
                "Transfert de candidature - " + candidat.getNomComplet(),
                construireCorpsEmail(candidat, commentaire, jsonContent, nomsAttachments),
                pieceJointes
            );
            
            logger.info("Email de transfert envoyé à {} pour le candidat {}", 
                       entrepriseDestination.getEmailEntreprise(), candidat.getIdCandidature());
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email : {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }
    
    private Map<String, Object> construireCandidatureJsonComplet(CandidatEnrichiDTO candidatEnrichi) {
        Candidat candidat = candidatEnrichi.getCandidat();
        Map<String, Object> json = new HashMap<>();
        
        // Champs requis
        json.put("idCandidature", candidat.getIdCandidature());
        json.put("nom", candidat.getNom());
        json.put("prenom", candidat.getPrenom());
        json.put("mail", candidat.getEmail());
        json.put("telephone", candidat.getTelephone());
        json.put("dateCandidature", candidat.getDateCandidature().toString());
        
        // Champs optionnels
        String posteLibelle = candidat.getIntitulePoste();
        if (posteLibelle != null && !posteLibelle.isEmpty()) {
            json.put("posteVise", posteLibelle);
        }
        
        if (candidat.getDateDisponibilite() != null) {
            json.put("dateDisponibilite", candidat.getDateDisponibilite().toString());
        }
        
        // Diplomes
        List<Map<String, Object>> diplomes = new ArrayList<>();
        if (candidatEnrichi.getDiplomes() != null) {
            for (Diplome diplome : candidatEnrichi.getDiplomes()) {
                Map<String, Object> d = new HashMap<>();
                d.put("nomDiplome", diplome.getNomDiplome());
                if (diplome.getAnneeObtention() != null) {
                    d.put("anneeObtention", diplome.getAnneeObtention().toString());
                }
                if (diplome.getDomaine() != null) {
                    d.put("domaine", diplome.getDomaine());
                }
                if (diplome.getEtablissement() != null) {
                    d.put("etablissement", diplome.getEtablissement());
                }
                if (diplome.getNiveau() != null) {
                    d.put("niveau", diplome.getNiveau().toString());
                }
                diplomes.add(d);
            }
        }
        json.put("diplomes", diplomes);
        
        // Experiences
        List<Map<String, Object>> experiences = new ArrayList<>();
        if (candidatEnrichi.getExperiences() != null) {
            for (Experience exp : candidatEnrichi.getExperiences()) {
                Map<String, Object> e = new HashMap<>();
                e.put("nomEntreprise", exp.getNomEntreprise());
                if (exp.getPoste() != null) {
                    e.put("poste", exp.getPoste());
                }
                if (exp.getDureeExperience() != null) {
                    e.put("dureeExperience", exp.getDureeExperience());
                }
                if (exp.getDateDebut() != null) {
                    e.put("dateDebut", exp.getDateDebut().toString());
                }
                if (exp.getDateFin() != null) {
                    e.put("dateFin", exp.getDateFin().toString());
                }
                if (exp.getDescription() != null) {
                    e.put("description", exp.getDescription());
                }
                if (exp.getSecteurActivite() != null) {
                    e.put("secteurActivite", exp.getSecteurActivite());
                }
                experiences.add(e);
            }
        }
        json.put("experiences", experiences);
        
        // Compétences techniques
        List<String> competences = new ArrayList<>();
        if (candidatEnrichi.getCompetences() != null) {
            for (Competence comp : candidatEnrichi.getCompetences()) {
                competences.add(comp.getCompetence());
            }
        }
        json.put("competences", competences);
        
        // Soft skills
        List<String> softSkills = new ArrayList<>();
        if (candidatEnrichi.getSoftSkills() != null) {
            for (SoftSkill skill : candidatEnrichi.getSoftSkills()) {
                softSkills.add(skill.getSoftSkill());
            }
        }
        json.put("softSkills", softSkills);
        
        // Permis de conduire
        List<String> permis = new ArrayList<>();
        if (candidatEnrichi.getPermis() != null) {
            for (PermisConduire p : candidatEnrichi.getPermis()) {
                permis.add(p.getCategoriePermis().toString());
            }
        }
        json.put("permisDeConduite", permis);
        
        // Langues
        List<String> langues = new ArrayList<>();
        if (candidatEnrichi.getLangues() != null) {
            for (Langue langue : candidatEnrichi.getLangues()) {
                langues.add(langue.getLangue() + "-" + langue.getNiveau());
            }
        }
        json.put("langues", langues.isEmpty() ? List.of("Français") : langues);
        
        // Fichiers
        Map<String, String> fichiers = new HashMap<>();
        if (candidatEnrichi.getFichiers() != null && !candidatEnrichi.getFichiers().isEmpty()) {
            FichierCandidat fichier = candidatEnrichi.getFichiers().get(0);
            fichiers.put("cv_filename", fichier.getCvFilename());
            if (fichier.getLmFilename() != null) {
                fichiers.put("lm_filename", fichier.getLmFilename());
            }
        } else {
            fichiers.put("cv_filename", "cv_" + candidat.getIdCandidature() + ".pdf");
        }
        json.put("fichiers", fichiers);
        
        // Message Candidat Entreprise
        Map<String, String> message = new HashMap<>();
        message.put("AutorisationStocker", candidat.getAutorisationStocker().name());
        message.put("AutorisationDiffuser", candidat.getAutorisationDiffuser().name());
        if (posteLibelle != null && !posteLibelle.isEmpty()) {
            message.put("PosteVise", posteLibelle);
        }
        json.put("Message_Candidat_Entreprise", message);
        
        return json;
    }
    
    private String construireCorpsEmail(Candidat candidat, String commentaire, String jsonContent, List<String> nomsAttachments) {
        StringBuilder corps = new StringBuilder();
        
        corps.append("Bonjour,\n\n");
        corps.append("Nous vous transférons une candidature qui pourrait correspondre à vos besoins.\n\n");
        
        corps.append("========== INFORMATIONS DU CANDIDAT ==========\n\n");
        corps.append("Candidat : ").append(candidat.getNomComplet()).append("\n");
        corps.append("Email : ").append(candidat.getEmail()).append("\n");
        corps.append("Téléphone : ").append(candidat.getTelephone()).append("\n");
        String posteLibelle = candidat.getIntitulePoste();
        if (posteLibelle != null && !posteLibelle.isEmpty()) {
            corps.append("Poste visé : ").append(posteLibelle).append("\n");
        }
        if (candidat.getDateDisponibilite() != null) {
            corps.append("Date de disponibilité : ").append(candidat.getDateDisponibilite()).append("\n");
        }
        corps.append("\n");
        
        if (commentaire != null && !commentaire.trim().isEmpty()) {
            corps.append("========== COMMENTAIRE ==========\n");
            corps.append(commentaire).append("\n\n");
        }
        
        if (!nomsAttachments.isEmpty()) {
            corps.append("========== PIÈCES JOINTES ==========\n");
            for (String nom : nomsAttachments) {
                corps.append("- ").append(nom).append("\n");
            }
            corps.append("\n");
        }
        
        corps.append("========== DONNÉES COMPLÈTES (JSON) ==========\n\n");
        corps.append(jsonContent);
        corps.append("\n\n");
        
        corps.append("Cordialement,\n");
        corps.append("Le service RH - Gestion des Candidatures\n");
        
        return corps.toString();
    }
    
    private void envoyerEmail(String destinataire, String sujet, String corps, List<File> pieceJointes) throws MessagingException {
        logger.info("Tentative d'envoi d'email à {} depuis {} avec {} pièce(s) jointe(s)", 
                   destinataire, smtpUsername, pieceJointes.size());
        
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", smtpHost);
        
        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });
        
        // Activer le debug pour voir les logs SMTP
        session.setDebug(true);
        
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(smtpUsername));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
        message.setSubject(sujet);
        
        // Créer le contenu multipart (texte + pièces jointes)
        Multipart multipart = new MimeMultipart();
        
        // Partie texte
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(corps, "UTF-8");
        multipart.addBodyPart(textPart);
        
        // Ajouter les pièces jointes
        for (File fichier : pieceJointes) {
            try {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.attachFile(fichier);
                multipart.addBodyPart(attachmentPart);
                logger.info("Pièce jointe ajoutée : {} ({} octets)", 
                           fichier.getName(), fichier.length());
            } catch (Exception e) {
                logger.error("Erreur lors de l'ajout de la pièce jointe {} : {}", 
                            fichier.getName(), e.getMessage());
            }
        }
        
        // Attacher le multipart au message
        message.setContent(multipart);
        
        Transport.send(message);
        
        logger.info("Email envoyé avec succès à {} avec {} pièce(s) jointe(s)", 
                   destinataire, pieceJointes.size());
    }
    
    /**
     * Envoie un email de notification au candidat pour l'informer de la décision
     */
    public void envoyerNotificationCandidat(Candidat candidat, Candidat.StatutCandidature nouveauStatut) {
        try {
            String sujet;
            String corps;
            String posteLibelle = candidat.getIntitulePoste();
            if (posteLibelle == null || posteLibelle.isEmpty()) {
                posteLibelle = "votre candidature";
            }
            
            if (nouveauStatut == Candidat.StatutCandidature.accepte) {
                sujet = "Candidature acceptée - " + posteLibelle;
                corps = construireCorpsEmailAcceptation(candidat);
            } else if (nouveauStatut == Candidat.StatutCandidature.refuse) {
                sujet = "Candidature - " + posteLibelle;
                corps = construireCorpsEmailRefus(candidat);
            } else {
                // Pas d'email pour les autres statuts
                return;
            }
            
            envoyerEmail(candidat.getEmail(), sujet, corps, new ArrayList<>());
            
            logger.info("Email de notification envoyé à {} pour le statut {}", 
                       candidat.getEmail(), nouveauStatut);
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification au candidat {} : {}", 
                        candidat.getIdCandidature(), e.getMessage(), e);
            // On ne fait pas échouer le changement de statut si l'email échoue
        }
    }
    
    private String construireCorpsEmailAcceptation(Candidat candidat) {
        StringBuilder corps = new StringBuilder();
        
        corps.append("Bonjour ").append(candidat.getPrenom()).append(" ").append(candidat.getNom()).append(",\n\n");
        corps.append("Nous avons le plaisir de vous informer que votre candidature");
        String posteLibelle = candidat.getIntitulePoste();
        if (posteLibelle != null && !posteLibelle.isEmpty()) {
            corps.append(" pour le poste de ").append(posteLibelle);
        }
        corps.append(" a été retenue.\n\n");
        
        corps.append("Votre profil et votre expérience correspondent à nos attentes et nous souhaiterions vous rencontrer pour discuter de la suite du processus de recrutement.\n\n");
        
        corps.append("Un membre de notre équipe vous contactera prochainement pour convenir d'un rendez-vous.\n\n");
        
        corps.append("Nous restons à votre disposition pour toute question.\n\n");
        
        corps.append("Cordialement,\n");
        corps.append("Le service RH\n");
        corps.append("Gestion des Candidatures\n");
        
        return corps.toString();
    }
    
    private String construireCorpsEmailRefus(Candidat candidat) {
        StringBuilder corps = new StringBuilder();
        
        corps.append("Bonjour ").append(candidat.getPrenom()).append(" ").append(candidat.getNom()).append(",\n\n");
        corps.append("Nous vous remercions de l'intérêt que vous avez porté à notre entreprise");
        String posteLibelle = candidat.getIntitulePoste();
        if (posteLibelle != null && !posteLibelle.isEmpty()) {
            corps.append(" et de votre candidature pour le poste de ").append(posteLibelle);
        }
        corps.append(".\n\n");
        
        corps.append("Après avoir étudié attentivement votre profil, nous avons le regret de vous informer que nous ne pourrons pas donner suite favorable à votre candidature à ce stade.\n\n");
        
        corps.append("Votre candidature a été examinée avec attention, mais nous avons retenu d'autres profils qui correspondent davantage aux besoins actuels du poste.\n\n");
        
        corps.append("Nous vous souhaitons beaucoup de succès dans vos démarches et nous restons à votre écoute pour d'éventuelles opportunités futures.\n\n");
        
        corps.append("Cordialement,\n");
        corps.append("Le service RH\n");
        corps.append("Gestion des Candidatures\n");
        
        return corps.toString();
    }
}

