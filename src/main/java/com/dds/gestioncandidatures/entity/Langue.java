package com.dds.gestioncandidatures.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "langues")
public class Langue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "id_candidature", nullable = false)
    private String idCandidature;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LangueType langue;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Niveau niveau;
    
    private String certification;
    
    public enum LangueType {
        Français, Anglais, Espagnol, Allemand, Italien, Chinois, Japonais, Russe, Arabe, Portugais
    }
    
    public enum Niveau {
        A1, A2, B1, B2, C1, C2
    }
    
    // Constructeurs
    public Langue() {}
    
    public Langue(String idCandidature, LangueType langue, Niveau niveau) {
        this.idCandidature = idCandidature;
        this.langue = langue;
        this.niveau = niveau;
    }
    
    // Getters et Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getIdCandidature() { return idCandidature; }
    public void setIdCandidature(String idCandidature) { this.idCandidature = idCandidature; }
    
    public LangueType getLangue() { return langue; }
    public void setLangue(LangueType langue) { this.langue = langue; }
    
    public Niveau getNiveau() { return niveau; }
    public void setNiveau(Niveau niveau) { this.niveau = niveau; }
    
    public String getCertification() { return certification; }
    public void setCertification(String certification) { this.certification = certification; }
}







