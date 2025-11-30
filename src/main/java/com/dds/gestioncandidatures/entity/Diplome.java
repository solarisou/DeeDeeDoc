package com.dds.gestioncandidatures.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "diplomes")
public class Diplome {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "id_candidature", nullable = false)
    private String idCandidature;
    
    @Column(name = "nom_diplome", nullable = false)
    private String nomDiplome;
    
    @Column(name = "annee_obtention")
    private Integer anneeObtention;
    
    private String domaine;
    
    private String etablissement;
    
    @Enumerated(EnumType.STRING)
    private Niveau niveau;
    
    public enum Niveau {
        CAP, BEP, BAC, BAC_plus_2, BAC_plus_3, BAC_plus_5, BAC_plus_8, Autre
    }
    
    // Constructeurs
    public Diplome() {}
    
    public Diplome(String idCandidature, String nomDiplome) {
        this.idCandidature = idCandidature;
        this.nomDiplome = nomDiplome;
    }
    
    // Getters et Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getIdCandidature() { return idCandidature; }
    public void setIdCandidature(String idCandidature) { this.idCandidature = idCandidature; }
    
    public String getNomDiplome() { return nomDiplome; }
    public void setNomDiplome(String nomDiplome) { this.nomDiplome = nomDiplome; }
    
    public Integer getAnneeObtention() { return anneeObtention; }
    public void setAnneeObtention(Integer anneeObtention) { this.anneeObtention = anneeObtention; }
    
    public String getDomaine() { return domaine; }
    public void setDomaine(String domaine) { this.domaine = domaine; }
    
    public String getEtablissement() { return etablissement; }
    public void setEtablissement(String etablissement) { this.etablissement = etablissement; }
    
    public Niveau getNiveau() { return niveau; }
    public void setNiveau(Niveau niveau) { this.niveau = niveau; }
}

