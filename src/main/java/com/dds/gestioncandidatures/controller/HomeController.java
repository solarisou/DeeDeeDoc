package com.dds.gestioncandidatures.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.dds.gestioncandidatures.entity.Poste;
import com.dds.gestioncandidatures.repository.PosteRepository;

@Controller
public class HomeController {
    
    @Autowired
    private PosteRepository posteRepository;
    
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("postesOuverts", posteRepository.findByStatutOrderByDateCreationDesc(Poste.Statut.ouvert));
        return "index";
    }
}