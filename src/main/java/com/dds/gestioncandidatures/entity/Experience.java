package com.dds.gestioncandidatures.entity;

import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "experiences")
public class Experience {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "id_candidature", nullable = false)
    private String idCandidature;
    
    @Column(name = "nom_entreprise", nullable = false)
    private String nomEntreprise;
    
    private String poste;
    
    @Column(name = "duree_experience")
    private String dureeExperience;
    
    @Column(name = "date_debut")
    private LocalDate dateDebut;
    
    @Column(name = "date_fin")
    private LocalDate dateFin;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "secteur_activite")
    private String secteurActivite;
    
    // Constructeurs
    public Experience() {}
    
    public Experience(String idCandidature, String nomEntreprise, String poste) {
        this.idCandidature = idCandidature;
        this.nomEntreprise = nomEntreprise;
        this.poste = poste;
    }
    
    // Getters et Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getIdCandidature() { return idCandidature; }
    public void setIdCandidature(String idCandidature) { this.idCandidature = idCandidature; }
    
    public String getNomEntreprise() { return nomEntreprise; }
    public void setNomEntreprise(String nomEntreprise) { this.nomEntreprise = nomEntreprise; }
    
    public String getPoste() { return poste; }
    public void setPoste(String poste) { this.poste = poste; }
    
    public String getDureeExperience() { return dureeExperience; }
    public void setDureeExperience(String dureeExperience) { this.dureeExperience = dureeExperience; }
    
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getSecteurActivite() { return secteurActivite; }
    public void setSecteurActivite(String secteurActivite) { this.secteurActivite = secteurActivite; }
}







