package com.dds.gestioncandidatures.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dds.gestioncandidatures.service.EmailCandidatureProcessorService;
import com.dds.gestioncandidatures.service.EmailCandidatureProcessorService.ProcessingResult;

@Controller
@RequestMapping("/admin/email-scrutator")
public class EmailScrutatorController {
    
    @Autowired
    private EmailCandidatureProcessorService emailCandidatureProcessorService;
    
    @GetMapping
    public String afficherPageScrutateur(Model model) {
        return "admin/email-scrutator";
    }
    
    @PostMapping("/process")
    public String traiterEmails(RedirectAttributes redirectAttributes) {
        try {
            ProcessingResult result = emailCandidatureProcessorService.processUnreadEmails();
            StringBuilder message = new StringBuilder();
            
            if (result.getTotalEmails() == 0) {
                message.append("Aucun nouvel email à traiter.");
            } else {
                message.append("Traitement terminé\n");
                message.append("Emails traités : ").append(result.getTotalEmails()).append("\n");
                message.append("Candidatures ajoutées : ").append(result.getSuccessCount()).append("\n");
                
                if (result.getSkippedCount() > 0) {
                    message.append("Candidatures ignorées (déjà existantes) : ").append(result.getSkippedCount()).append("\n");
                }
                
                if (result.getErrorCount() > 0) {
                    message.append("Erreurs : ").append(result.getErrorCount());
                }
            }
            
            if (result.getSuccessCount() > 0) {
                redirectAttributes.addFlashAttribute("successList", result.getSuccessfulCandidatures());
            }
            
            if (result.getSkippedCount() > 0) {
                redirectAttributes.addFlashAttribute("skippedList", result.getSkippedCandidatures());
            }
            
            if (result.getErrorCount() > 0) {
                redirectAttributes.addFlashAttribute("errorList", result.getErrors());
            }
            
            if (result.getErrorCount() > 0 && result.getSuccessCount() == 0) {
                redirectAttributes.addFlashAttribute("error", message.toString());
            } else if (result.getErrorCount() > 0) {
                redirectAttributes.addFlashAttribute("warning", message.toString());
            } else {
                redirectAttributes.addFlashAttribute("success", message.toString());
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors du traitement des emails : " + e.getMessage());
        }
        
        return "redirect:/admin/email-scrutator";
    }
    
    @PostMapping("/test-connection")
    public String testerConnexion(RedirectAttributes redirectAttributes) {
        try {
            int count = emailCandidatureProcessorService.processUnreadEmails().getTotalEmails();
            redirectAttributes.addFlashAttribute("success", 
                "Connexion réussie ! " + count + " email(s) non lu(s) trouvé(s).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Échec de connexion : " + e.getMessage());
        }
        
        return "redirect:/admin/email-scrutator";
    }
}


