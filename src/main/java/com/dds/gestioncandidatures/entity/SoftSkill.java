package com.dds.gestioncandidatures.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "soft_skills")
public class SoftSkill {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "id_candidature", nullable = false)
    private String idCandidature;
    
    @Column(name = "soft_skill", nullable = false)
    private String softSkill;
    
    @Enumerated(EnumType.STRING)
    private Niveau niveau;
    
    public enum Niveau {
        Faible, Moyen, Bon, Excellent
    }
    
    // Constructeurs
    public SoftSkill() {}
    
    public SoftSkill(String idCandidature, String softSkill) {
        this.idCandidature = idCandidature;
        this.softSkill = softSkill;
        this.niveau = Niveau.Bon;
    }
    
    // Getters et Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getIdCandidature() { return idCandidature; }
    public void setIdCandidature(String idCandidature) { this.idCandidature = idCandidature; }
    
    public String getSoftSkill() { return softSkill; }
    public void setSoftSkill(String softSkill) { this.softSkill = softSkill; }
    
    public Niveau getNiveau() { return niveau; }
    public void setNiveau(Niveau niveau) { this.niveau = niveau; }
}







