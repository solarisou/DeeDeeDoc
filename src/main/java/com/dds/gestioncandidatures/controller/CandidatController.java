package com.dds.gestioncandidatures.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dds.gestioncandidatures.entity.Candidat;
import com.dds.gestioncandidatures.service.CandidatIdGeneratorService;
import com.dds.gestioncandidatures.service.CvParserService;
import com.dds.gestioncandidatures.service.FileStorageService;

@Controller
@RequestMapping("/candidat")
public class CandidatController {
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private CvParserService cvParserService;
    
    @Autowired
    private CandidatIdGeneratorService candidatIdGeneratorService;
    
    @GetMapping("/deposer")
    public String afficherFormulaireDepot() {
        return "candidat/deposer-simple";
    }
    
    @PostMapping("/deposer")
    public String deposerCandidature(@RequestParam("entrepriseId") String entrepriseId,
                                   @RequestParam("cvFile") MultipartFile cvFile,
                                   @RequestParam(value = "lmFile", required = false) MultipartFile lmFile,
                                   @RequestParam("autorisationStocker") String autorisationStocker,
                                   @RequestParam("autorisationDiffuser") String autorisationDiffuser,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        
        if (cvFile.isEmpty()) {
            model.addAttribute("error", "Le CV est obligatoire");
            return "candidat/deposer-simple";
        }
        
        try {
            // Générer un ID avec incrémentation automatique
            String candidatId = candidatIdGeneratorService.genererNouvelId(entrepriseId);
            
            String cvFileName = fileStorageService.storeFile(cvFile, candidatId, "CV");
            String lmFileName = null;
            
            if (lmFile != null && !lmFile.isEmpty()) {
                lmFileName = fileStorageService.storeFile(lmFile, candidatId, "LM");
            }
            
            Map<String, Object> resultTraitement = cvParserService.traiterFichiers(cvFileName, lmFileName);
            
            Candidat candidatTemp = new Candidat();
            candidatTemp.setIdCandidature(candidatId);
            candidatTemp.setDateCandidature(LocalDate.now());
            candidatTemp.setAutorisationStocker(Candidat.Autorisation.valueOf(autorisationStocker));
            candidatTemp.setAutorisationDiffuser(Candidat.Autorisation.valueOf(autorisationDiffuser));
            
            Map<String, Object> infosAffichage = new HashMap<>();
            infosAffichage.put("cvFileName", cvFile.getOriginalFilename());
            infosAffichage.put("cvSize", cvFile.getSize());
            
            if (lmFile != null && !lmFile.isEmpty()) {
                infosAffichage.put("lmFileName", lmFile.getOriginalFilename());
                infosAffichage.put("lmSize", lmFile.getSize());
            } else {
                infosAffichage.put("lmFileName", null);
                infosAffichage.put("lmSize", null);
            }
            
            infosAffichage.put("traitementStatus", resultTraitement.get("status"));
            infosAffichage.put("message", "Fichiers stockés avec succès. Traitement Python exécuté.");
            infosAffichage.put("output", resultTraitement.get("output"));
            
            model.addAttribute("candidat", candidatTemp);
            model.addAttribute("infosExtraites", infosAffichage);
            model.addAttribute("success", "Fichiers traités avec succès ! Le script Python a été exécuté.");
            
            return "candidat/validation-infos";
            
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors du traitement des fichiers: " + e.getMessage());
            return "candidat/deposer-simple";
        }
    }
    
    @GetMapping("/confirmation")
    public String afficherConfirmation() {
        return "candidat/confirmation";
    }
}
