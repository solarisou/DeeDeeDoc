package com.dds.gestioncandidatures.entity;

import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "permis_conduire")
public class PermisConduire {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "id_candidature", nullable = false)
    private String idCandidature;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "categorie_permis", nullable = false)
    private CategoriePermis categoriePermis;
    
    @Column(name = "date_obtention")
    private LocalDate dateObtention;
    
    @Column(name = "date_expiration")
    private LocalDate dateExpiration;
    
    public enum CategoriePermis {
        A, B, AM, C, D, F
    }
    
    // Constructeurs
    public PermisConduire() {}
    
    public PermisConduire(String idCandidature, CategoriePermis categoriePermis) {
        this.idCandidature = idCandidature;
        this.categoriePermis = categoriePermis;
    }
    
    // Getters et Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getIdCandidature() { return idCandidature; }
    public void setIdCandidature(String idCandidature) { this.idCandidature = idCandidature; }
    
    public CategoriePermis getCategoriePermis() { return categoriePermis; }
    public void setCategoriePermis(CategoriePermis categoriePermis) { this.categoriePermis = categoriePermis; }
    
    public LocalDate getDateObtention() { return dateObtention; }
    public void setDateObtention(LocalDate dateObtention) { this.dateObtention = dateObtention; }
    
    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }
}







