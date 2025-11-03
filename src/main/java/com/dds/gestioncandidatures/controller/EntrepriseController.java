package com.dds.gestioncandidatures.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.dds.gestioncandidatures.entity.Entreprise;
import com.dds.gestioncandidatures.repository.EntrepriseRepository;

@Controller
@RequestMapping("/entreprises")
public class EntrepriseController {
    
    @Autowired
    private EntrepriseRepository entrepriseRepository;
    
    @GetMapping
    public String listerEntreprises(Model model) {
        List<Entreprise> entreprises = entrepriseRepository.findByStatut(Entreprise.Statut.active);
        model.addAttribute("entreprises", entreprises);
        return "entreprise/liste";
    }
    
    @GetMapping("/{id}")
    public String voirEntreprise(@PathVariable String id, Model model) {
        entrepriseRepository.findById(id).ifPresent(entreprise -> 
            model.addAttribute("entreprise", entreprise)
        );
        return "entreprise/detail";
    }
}
