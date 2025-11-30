package com.dds.gestioncandidatures.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.dds.gestioncandidatures.service.EmailReaderService;
import com.dds.gestioncandidatures.service.EmailReaderService.EmailContent;

/**
 * Contrôleur de test pour afficher tous les emails
 */
@Controller
@RequestMapping("/admin/test-emails")
public class EmailTestController {
    
    @Autowired
    private EmailReaderService emailReaderService;
    
    /**
     * Affiche tous les emails (lus et non lus) avec leur titre et expéditeur
     */
    @GetMapping
    public String afficherTousLesEmails(Model model) {
        List<EmailInfo> emailInfos = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        
        try {
            // Lire tous les emails (lus et non lus)
            List<EmailContent> emails = emailReaderService.readAllEmails();
            
            for (EmailContent email : emails) {
                EmailInfo info = new EmailInfo();
                info.setTitre(email.getSubject());
                info.setExpediteur(email.getFrom());
                info.setDate(dateFormat.format(email.getSentDate()));
                
                // Extraire un aperçu du contenu (100 premiers caractères)
                String contenu = email.getContent();
                if (contenu.length() > 100) {
                    contenu = contenu.substring(0, 100) + "...";
                }
                info.setApercu(contenu);
                
                emailInfos.add(info);
            }
            
            model.addAttribute("success", emailInfos.size() + " email(s) trouvé(s)");
            
        } catch (Exception e) {
            model.addAttribute("error", "Erreur : " + e.getMessage());
            e.printStackTrace();
        }
        
        model.addAttribute("emails", emailInfos);
        return "admin/test-emails";
    }
    
    /**
     * Classe interne pour représenter les infos d'un email
     */
    public static class EmailInfo {
        private String titre;
        private String expediteur;
        private String date;
        private String apercu;
        
        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }
        
        public String getExpediteur() { return expediteur; }
        public void setExpediteur(String expediteur) { this.expediteur = expediteur; }
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public String getApercu() { return apercu; }
        public void setApercu(String apercu) { this.apercu = apercu; }
    }
}


