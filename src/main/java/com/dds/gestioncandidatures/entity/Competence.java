package com.dds.gestioncandidatures.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "competences")
public class Competence {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "id_candidature", nullable = false)
    private String idCandidature;
    
    @Column(nullable = false)
    private String competence;
    
    @Enumerated(EnumType.STRING)
    private Niveau niveau;
    
    @Enumerated(EnumType.STRING)
    private Categorie categorie;
    
    public enum Niveau {
        Débutant, Intermédiaire, Avancé, Expert
    }
    
    public enum Categorie {
        Technique, Logiciel, Langage_programmation, Framework, Autre
    }
    
    // Constructeurs
    public Competence() {}
    
    public Competence(String idCandidature, String competence) {
        this.idCandidature = idCandidature;
        this.competence = competence;
        this.niveau = Niveau.Intermédiaire;
        this.categorie = Categorie.Technique;
    }
    
    // Getters et Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getIdCandidature() { return idCandidature; }
    public void setIdCandidature(String idCandidature) { this.idCandidature = idCandidature; }
    
    public String getCompetence() { return competence; }
    public void setCompetence(String competence) { this.competence = competence; }
    
    public Niveau getNiveau() { return niveau; }
    public void setNiveau(Niveau niveau) { this.niveau = niveau; }
    
    public Categorie getCategorie() { return categorie; }
    public void setCategorie(Categorie categorie) { this.categorie = categorie; }
}







