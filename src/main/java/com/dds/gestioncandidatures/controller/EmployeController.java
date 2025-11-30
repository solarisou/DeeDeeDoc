package com.dds.gestioncandidatures.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dds.gestioncandidatures.dto.EntrepriseForm;
import com.dds.gestioncandidatures.dto.PosteForm;
import com.dds.gestioncandidatures.entity.Candidat;
import com.dds.gestioncandidatures.entity.Entreprise;
import com.dds.gestioncandidatures.entity.Poste;
import com.dds.gestioncandidatures.repository.CandidatRepository;
import com.dds.gestioncandidatures.repository.EntrepriseRepository;
import com.dds.gestioncandidatures.repository.PosteRepository;
import com.dds.gestioncandidatures.service.CandidatEnrichService;
import com.dds.gestioncandidatures.service.EmailSenderService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/employe")
public class EmployeController {
    
    private static final String ENTREPRISE_PRINCIPALE_ID = "0002";
    
    @Autowired
    private CandidatRepository candidatRepository;
    
    @Autowired
    private EntrepriseRepository entrepriseRepository;
    
    @Autowired
    private EmailSenderService emailSenderService;
    
    @Autowired
    private PosteRepository posteRepository;
    
    @Autowired
    private CandidatEnrichService candidatEnrichService;
    
    @GetMapping("/dashboard")
    public String afficherDashboard(Model model) {
        List<Candidat> candidats = candidatRepository.findByOrderByDateCandidatureDesc();
        long nbPostesOuverts = posteRepository.countByStatut(Poste.Statut.ouvert);
        
        // Calculer les KPI par statut
        long nbEnAttente = candidats.stream()
            .filter(c -> c.getStatutCandidature() == Candidat.StatutCandidature.en_attente)
            .count();
        
        long nbAcceptes = candidats.stream()
            .filter(c -> c.getStatutCandidature() == Candidat.StatutCandidature.accepte)
            .count();
        
        long nbRefuses = candidats.stream()
            .filter(c -> c.getStatutCandidature() == Candidat.StatutCandidature.refuse)
            .count();
        
        model.addAttribute("candidats", candidats);
        model.addAttribute("nbCandidats", candidats.size());
        model.addAttribute("nbEnAttente", nbEnAttente);
        model.addAttribute("nbAcceptes", nbAcceptes);
        model.addAttribute("nbRefuses", nbRefuses);
        model.addAttribute("nbPostesOuverts", nbPostesOuverts);
        
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
        
        // Enrichir les candidats avec leurs données liées
        var candidatsEnrichis = candidatEnrichService.enrichirCandidats(candidats);
        
        // Charger la liste des entreprises pour le transfert multiple
        List<Entreprise> entreprises = entrepriseRepository.findByStatut(Entreprise.Statut.active);
        
        model.addAttribute("candidatsEnrichis", candidatsEnrichis);
        model.addAttribute("entreprises", entreprises);
        model.addAttribute("statutSelectionne", statut);
        model.addAttribute("rechercheActuelle", recherche);
        
        return "employe/candidatures";
    }
    
    @GetMapping("/candidature/{id}")
    public String voirCandidature(@PathVariable String id, Model model) {
        Optional<Candidat> candidat = candidatRepository.findById(id);
        
        if (candidat.isPresent()) {
            // Enrichir le candidat avec toutes ses données
            var candidatEnrichi = candidatEnrichService.enrichirCandidat(candidat.get());
            
            model.addAttribute("candidat", candidat.get());
            model.addAttribute("candidatEnrichi", candidatEnrichi);
            
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
                              @RequestParam String nouveauStatut,
                              RedirectAttributes redirectAttributes) {
        
        Optional<Candidat> candidatOpt = candidatRepository.findById(id);
        
        if (candidatOpt.isPresent()) {
            Candidat candidat = candidatOpt.get();
            Candidat.StatutCandidature ancienStatut = candidat.getStatutCandidature();
            Candidat.StatutCandidature nouveauStatutEnum = Candidat.StatutCandidature.valueOf(nouveauStatut);
            
            candidat.setStatutCandidature(nouveauStatutEnum);
            candidatRepository.save(candidat);
            
            // Envoyer un email de notification si le statut change vers accepte ou refuse
            if (ancienStatut != nouveauStatutEnum && 
                (nouveauStatutEnum == Candidat.StatutCandidature.accepte || 
                 nouveauStatutEnum == Candidat.StatutCandidature.refuse)) {
                try {
                    emailSenderService.envoyerNotificationCandidat(candidat, nouveauStatutEnum);
                    redirectAttributes.addFlashAttribute("success", 
                        "Statut mis à jour et email de notification envoyé à " + candidat.getEmail());
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("warning", 
                        "Statut mis à jour mais l'email n'a pas pu être envoyé : " + e.getMessage());
                }
            } else {
                redirectAttributes.addFlashAttribute("success", "Statut mis à jour avec succès");
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Candidature non trouvée");
        }
        
        return "redirect:/employe/candidature/" + id;
    }
    
    @GetMapping("/entreprises")
    public String listerEntreprises(@RequestParam(value = "edit", required = false) String editId,
                                    Model model) {
        List<Entreprise> entreprises = entrepriseRepository.findAll();
        model.addAttribute("entreprises", entreprises);
        model.addAttribute("statutsEntreprise", Entreprise.Statut.values());

        if (!model.containsAttribute("entrepriseForm")) {
            EntrepriseForm form;
            if (editId != null && !editId.isBlank()) {
                form = entrepriseRepository.findById(editId)
                    .map(EntrepriseForm::fromEntity)
                    .orElseGet(EntrepriseForm::new);
            } else {
                form = new EntrepriseForm();
            }
            model.addAttribute("entrepriseForm", form);
        }

        return "employe/entreprises";
    }

    @PostMapping("/entreprises/enregistrer")
    public String enregistrerEntreprise(@Valid @ModelAttribute("entrepriseForm") EntrepriseForm entrepriseForm,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.entrepriseForm", bindingResult);
            redirectAttributes.addFlashAttribute("entrepriseForm", entrepriseForm);
            redirectAttributes.addFlashAttribute("error", "Merci de corriger les erreurs du formulaire entreprise.");
            String editParam = entrepriseForm.getIdEntreprise() != null ? entrepriseForm.getIdEntreprise() : "";
            return "redirect:/employe/entreprises?edit=" + editParam;
        }

        Entreprise entreprise = entrepriseRepository.findById(entrepriseForm.getIdEntreprise())
            .orElseGet(Entreprise::new);

        boolean creation = entreprise.getIdEntreprise() == null;

        entreprise.setIdEntreprise(entrepriseForm.getIdEntreprise());
        entreprise.setNomEntreprise(entrepriseForm.getNomEntreprise());
        entreprise.setEmailEntreprise(entrepriseForm.getEmailEntreprise());
        entreprise.setStatut(entrepriseForm.getStatut());

        entrepriseRepository.save(entreprise);

        redirectAttributes.addFlashAttribute("success",
            creation ? "Entreprise créée avec succès." : "Entreprise mise à jour avec succès.");
        return "redirect:/employe/entreprises";
    }

    @PostMapping("/entreprises/{id}/statut")
    public String modifierStatutEntreprise(@PathVariable String id,
                                           @RequestParam("statut") Entreprise.Statut statut,
                                           RedirectAttributes redirectAttributes) {
        return entrepriseRepository.findById(id)
            .map(entreprise -> {
                entreprise.setStatut(statut);
                entrepriseRepository.save(entreprise);
                redirectAttributes.addFlashAttribute("success",
                    "Statut de l'entreprise \"" + entreprise.getNomEntreprise() + "\" mis à jour.");
                return "redirect:/employe/entreprises";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("error", "Entreprise introuvable.");
                return "redirect:/employe/entreprises";
            });
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
    
    @PostMapping("/candidatures/transferer-multiple")
    public String transfererCandidaturesMultiples(@RequestParam String candidatIds,
                                                   @RequestParam String entrepriseDestination,
                                                   @RequestParam(required = false) String commentaire,
                                                   RedirectAttributes redirectAttributes) {
        try {
            Optional<Entreprise> entrepriseOpt = entrepriseRepository.findById(entrepriseDestination);
            
            if (entrepriseOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Entreprise destination non trouvée");
                return "redirect:/employe/candidatures";
            }
            
            Entreprise entreprise = entrepriseOpt.get();
            String[] ids = candidatIds.split(",");
            int nbTransferes = 0;
            int nbEchecs = 0;
            
            for (String id : ids) {
                Optional<Candidat> candidatOpt = candidatRepository.findById(id.trim());
                
                if (candidatOpt.isPresent()) {
                    Candidat candidat = candidatOpt.get();
                    
                    // Vérifier l'autorisation de diffusion
                    if (candidat.getAutorisationDiffuser() == Candidat.Autorisation.O) {
                        try {
                            emailSenderService.envoyerCandidatureParEmail(candidat, entreprise, commentaire);
                            nbTransferes++;
                        } catch (Exception e) {
                            nbEchecs++;
                        }
                    } else {
                        nbEchecs++;
                    }
                }
            }
            
            if (nbTransferes > 0) {
                redirectAttributes.addFlashAttribute("success", 
                    nbTransferes + " candidature(s) transférée(s) avec succès à " + entreprise.getNomEntreprise());
            }
            
            if (nbEchecs > 0) {
                redirectAttributes.addFlashAttribute("warning", 
                    nbEchecs + " candidature(s) non transférée(s) (autorisation manquante ou erreur)");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors du transfert multiple : " + e.getMessage());
        }
        
        return "redirect:/employe/candidatures";
    }

    @GetMapping("/postes")
    public String gererPostes(Model model) {
        if (!model.containsAttribute("posteForm")) {
            model.addAttribute("posteForm", new PosteForm());
        }
        model.addAttribute("postes", posteRepository.findAllByOrderByDateCreationDesc());
        model.addAttribute("typesContrat", Poste.TypeContrat.values());
        model.addAttribute("statutsPoste", Poste.Statut.values());
        return "employe/postes";
    }

    @PostMapping("/postes")
    public String creerPoste(@Valid @ModelAttribute("posteForm") PosteForm posteForm,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (posteForm.getSalaireMin() != null && posteForm.getSalaireMax() != null
            && posteForm.getSalaireMax().compareTo(posteForm.getSalaireMin()) < 0) {
            bindingResult.rejectValue("salaireMax", "salaireMaxInvalid", "Le salaire max doit être supérieur ou égal au salaire min.");
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.posteForm", bindingResult);
            redirectAttributes.addFlashAttribute("posteForm", posteForm);
            redirectAttributes.addFlashAttribute("error", "Merci de corriger les erreurs du formulaire.");
            return "redirect:/employe/postes";
        }

        Entreprise entreprise = entrepriseRepository.findById(ENTREPRISE_PRINCIPALE_ID)
            .orElseGet(() -> entrepriseRepository.findAll().stream()
                .filter(e -> ENTREPRISE_PRINCIPALE_ID.equals(e.getIdEntreprise()))
                .findFirst()
                .orElse(null));

        if (entreprise == null) {
            redirectAttributes.addFlashAttribute("error", "Entreprise principale introuvable (ID 0002).");
            return "redirect:/employe/postes";
        }

        Poste poste = new Poste();
        poste.setEntreprise(entreprise);
        poste.setIntitulePoste(posteForm.getIntitule());
        poste.setDescription(posteForm.getDescription());
        poste.setCompetencesRequises(posteForm.getCompetences());
        poste.setSalaireMin(posteForm.getSalaireMin());
        poste.setSalaireMax(posteForm.getSalaireMax());
        poste.setTypeContrat(posteForm.getTypeContrat());
        poste.setLocalisation(null);
        poste.setDateExpiration(posteForm.getDateExpiration());
        poste.setStatut(Poste.Statut.ouvert);

        posteRepository.save(poste);

        redirectAttributes.addFlashAttribute("success", "Le poste \"" + poste.getIntitulePoste() + "\" a été créé.");
        return "redirect:/employe/postes";
    }

    @PostMapping("/postes/{id}/statut")
    public String modifierStatutPoste(@PathVariable Long id,
                                      @RequestParam("statut") Poste.Statut statut,
                                      RedirectAttributes redirectAttributes) {
        return posteRepository.findById(id)
            .map(poste -> {
                poste.setStatut(statut);
                posteRepository.save(poste);
                redirectAttributes.addFlashAttribute("success",
                    "Statut du poste \"" + poste.getIntitulePoste() + "\" mis à jour en " + statut + ".");
                return "redirect:/employe/postes";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("error", "Poste introuvable.");
                return "redirect:/employe/postes";
            });
    }
}