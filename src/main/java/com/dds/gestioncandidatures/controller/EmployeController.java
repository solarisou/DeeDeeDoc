package com.dds.gestioncandidatures.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dds.gestioncandidatures.entity.Candidat;
import com.dds.gestioncandidatures.entity.Entreprise;
import com.dds.gestioncandidatures.repository.CandidatRepository;
import com.dds.gestioncandidatures.repository.EntrepriseRepository;
import com.dds.gestioncandidatures.service.EmailSenderService;

@Controller
@RequestMapping("/employe")
public class EmployeController {
    
    @Autowired
    private CandidatRepository candidatRepository;
    
    @Autowired
    private EntrepriseRepository entrepriseRepository;
    
    @Autowired
    private EmailSenderService emailSenderService;
    
    @GetMapping("/dashboard")
    public String afficherDashboard(Model model) {
        List<Candidat> candidats = candidatRepository.findByOrderByDateCandidatureDesc();
        List<Entreprise> entreprises = entrepriseRepository.findByStatut(Entreprise.Statut.active);
        
        model.addAttribute("candidats", candidats);
        model.addAttribute("entreprises", entreprises);
        model.addAttribute("nbCandidats", candidats.size());
        model.addAttribute("nbEntreprises", entreprises.size());
        
        return "employe/dashboard";
    }
    
    @GetMapping("/candidatures")
    public String listerCandidatures(@RequestParam(required = false) String statut,
                                   @RequestParam(required = false) String recherche,
                                   Model model) {
        
        List<Candidat> candidats;
        
        if (recherche != null && !recherche.trim().isEmpty()) {
            candidats = candidatRepository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(
                recherche.trim(), recherche.trim());
        } else if (statut != null && !statut.isEmpty()) {
            candidats = candidatRepository.findByStatutCandidature(
                Candidat.StatutCandidature.valueOf(statut));
        } else {
            candidats = candidatRepository.findByOrderByDateCandidatureDesc();
        }
        
        model.addAttribute("candidats", candidats);
        model.addAttribute("statutSelectionne", statut);
        model.addAttribute("rechercheActuelle", recherche);
        
        return "employe/candidatures";
    }
    
    @GetMapping("/candidature/{id}")
    public String voirCandidature(@PathVariable String id, Model model) {
        Optional<Candidat> candidat = candidatRepository.findById(id);
        
        if (candidat.isPresent()) {
            model.addAttribute("candidat", candidat.get());
            
            // Charger la liste des entreprises pour le transfert
            List<Entreprise> entreprises = entrepriseRepository.findByStatut(Entreprise.Statut.active);
            model.addAttribute("entreprises", entreprises);
            
            return "employe/detail-candidature";
        } else {
            return "redirect:/employe/candidatures?error=Candidature non trouvée";
        }
    }
    
    @PostMapping("/candidature/{id}/statut")
    public String changerStatut(@PathVariable String id, 
                              @RequestParam String nouveauStatut) {
        
        Optional<Candidat> candidatOpt = candidatRepository.findById(id);
        
        if (candidatOpt.isPresent()) {
            Candidat candidat = candidatOpt.get();
            candidat.setStatutCandidature(Candidat.StatutCandidature.valueOf(nouveauStatut));
            candidatRepository.save(candidat);
        }
        
        return "redirect:/employe/candidature/" + id;
    }
    
    @GetMapping("/entreprises")
    public String listerEntreprises(Model model) {
        List<Entreprise> entreprises = entrepriseRepository.findAll();
        model.addAttribute("entreprises", entreprises);
        return "employe/entreprises";
    }
    
    @PostMapping("/candidature/{id}/transferer")
    public String transfererCandidature(@PathVariable String id,
                                       @RequestParam String entrepriseDestination,
                                       @RequestParam(required = false) String commentaire,
                                       RedirectAttributes redirectAttributes) {
        try {
            Optional<Candidat> candidatOpt = candidatRepository.findById(id);
            Optional<Entreprise> entrepriseOpt = entrepriseRepository.findById(entrepriseDestination);
            
            if (candidatOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Candidat non trouvé");
                return "redirect:/employe/candidatures";
            }
            
            if (entrepriseOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Entreprise destination non trouvée");
                return "redirect:/employe/candidature/" + id;
            }
            
            Candidat candidat = candidatOpt.get();
            Entreprise entreprise = entrepriseOpt.get();
            
            // Vérifier l'autorisation de diffusion
            if (candidat.getAutorisationDiffuser() != Candidat.Autorisation.O) {
                redirectAttributes.addFlashAttribute("error", 
                    "Le candidat n'a pas autorisé la diffusion de ses données");
                return "redirect:/employe/candidature/" + id;
            }
            
            // Vérifier que l'entreprise a un email
            if (entreprise.getEmailEntreprise() == null || entreprise.getEmailEntreprise().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", 
                    "L'entreprise destination n'a pas d'adresse email configurée");
                return "redirect:/employe/candidature/" + id;
            }
            
            // Envoyer l'email avec le JSON de la candidature
            emailSenderService.envoyerCandidatureParEmail(candidat, entreprise, commentaire);
            
            redirectAttributes.addFlashAttribute("success", 
                "Candidature transférée avec succès à " + entreprise.getNomEntreprise() + 
                " (" + entreprise.getEmailEntreprise() + ")");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors du transfert : " + e.getMessage());
        }
        
        return "redirect:/employe/candidature/" + id;
    }
}