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

import com.dds.gestioncandidatures.entity.Candidat;
import com.dds.gestioncandidatures.entity.Entreprise;
import com.dds.gestioncandidatures.entity.FichierCandidat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailSenderService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailSenderService.class);
    
    @Value("${email.host}")
    private String emailHost;
    
    @Value("${email.port}")
    private String emailPort;
    
    @Value("${email.username}")
    private String emailUsername;
    
    @Value("${email.password}")
    private String emailPassword;
    
    private final ObjectMapper objectMapper;
    
    public EmailSenderService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public void envoyerCandidatureParEmail(Candidat candidat, Entreprise entrepriseDestination, String commentaire) {
        try {
            // Construire le JSON de la candidature selon le schéma ProjetDDS.json
            Map<String, Object> candidatureJson = construireCandidatureJson(candidat);
            
            // Convertir en JSON
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(candidatureJson);
            
            // Envoyer l'email
            envoyerEmail(
                entrepriseDestination.getEmailEntreprise(),
                "Transfert de candidature - " + candidat.getNomComplet(),
                construireCorpsEmail(candidat, commentaire, jsonContent)
            );
            
            logger.info("Email de transfert envoyé à {} pour le candidat {}", 
                       entrepriseDestination.getEmailEntreprise(), candidat.getIdCandidature());
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email : {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }
    
    private Map<String, Object> construireCandidatureJson(Candidat candidat) {
        Map<String, Object> json = new HashMap<>();
        
        // Champs requis
        json.put("idCandidature", candidat.getIdCandidature());
        json.put("nom", candidat.getNom());
        json.put("prenom", candidat.getPrenom());
        json.put("mail", candidat.getEmail());
        json.put("telephone", candidat.getTelephone());
        json.put("dateCandidature", candidat.getDateCandidature().toString());
        
        // Champs optionnels
        if (candidat.getPosteVise() != null) {
            json.put("posteVise", candidat.getPosteVise());
        }
        
        if (candidat.getDateDisponibilite() != null) {
            json.put("dateDisponibilite", candidat.getDateDisponibilite().toString());
        }
        
        // Diplomes (vide si pas de données)
        json.put("diplomes", new ArrayList<>());
        
        // Experiences (vide si pas de données)
        json.put("experiences", new ArrayList<>());
        
        // Compétences (vide si pas de données)
        json.put("competences", new ArrayList<>());
        
        // Soft skills (vide si pas de données)
        json.put("softSkills", new ArrayList<>());
        
        // Permis de conduire (vide si pas de données)
        json.put("permisDeConduite", new ArrayList<>());
        
        // Langues (obligatoire avec au moins un élément)
        List<String> langues = new ArrayList<>();
        langues.add("Français");
        json.put("langues", langues);
        
        // Fichiers
        if (candidat.getFichiers() != null && !candidat.getFichiers().isEmpty()) {
            FichierCandidat fichier = candidat.getFichiers().get(0);
            Map<String, String> fichiers = new HashMap<>();
            fichiers.put("cv_filename", fichier.getCvFilename());
            if (fichier.getLmFilename() != null) {
                fichiers.put("lm_filename", fichier.getLmFilename());
            }
            json.put("fichiers", fichiers);
        } else {
            Map<String, String> fichiers = new HashMap<>();
            fichiers.put("cv_filename", "cv_" + candidat.getIdCandidature() + ".pdf");
            json.put("fichiers", fichiers);
        }
        
        // Message Candidat Entreprise
        Map<String, String> message = new HashMap<>();
        message.put("AutorisationStocker", candidat.getAutorisationStocker().name());
        message.put("AutorisationDiffuser", candidat.getAutorisationDiffuser().name());
        if (candidat.getPosteVise() != null) {
            message.put("PosteVise", candidat.getPosteVise());
        }
        json.put("Message_Candidat_Entreprise", message);
        
        return json;
    }
    
    private String construireCorpsEmail(Candidat candidat, String commentaire, String jsonContent) {
        StringBuilder corps = new StringBuilder();
        
        corps.append("Bonjour,\n\n");
        corps.append("Nous vous transférons une candidature qui pourrait correspondre à vos besoins.\n\n");
        
        corps.append("Candidat : ").append(candidat.getNomComplet()).append("\n");
        corps.append("Email : ").append(candidat.getEmail()).append("\n");
        corps.append("Téléphone : ").append(candidat.getTelephone()).append("\n");
        if (candidat.getPosteVise() != null) {
            corps.append("Poste visé : ").append(candidat.getPosteVise()).append("\n");
        }
        corps.append("\n");
        
        if (commentaire != null && !commentaire.trim().isEmpty()) {
            corps.append("Commentaire : ").append(commentaire).append("\n\n");
        }
        
        corps.append("Vous trouverez ci-dessous les informations complètes au format JSON :\n\n");
        corps.append("```json\n");
        corps.append(jsonContent);
        corps.append("\n```\n\n");
        
        corps.append("Cordialement,\n");
        corps.append("Le service RH\n");
        
        return corps.toString();
    }
    
    private void envoyerEmail(String destinataire, String sujet, String corps) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", emailHost);
        props.put("mail.smtp.port", emailPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", emailHost);
        
        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailUsername, emailPassword);
            }
        });
        
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(emailUsername));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
        message.setSubject(sujet);
        message.setText(corps);
        
        Transport.send(message);
    }
}

