package com.dds.gestioncandidatures.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "entreprises")
public class Entreprise {
    
    @Id
    @Column(name = "id_entreprise")
    private String idEntreprise;
    
    @Column(name = "nom_entreprise", nullable = false)
    private String nomEntreprise;
    
    @Column(name = "email_entreprise")
    private String emailEntreprise;
    
    @Column(name = "date_ajout")
    private LocalDateTime dateAjout;
    
    @Enumerated(EnumType.STRING)
    private Statut statut = Statut.active;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum Statut {
        active, inactive
    }
    
    // Constructeurs
    public Entreprise() {}
    
    public Entreprise(String idEntreprise, String nomEntreprise) {
        this.idEntreprise = idEntreprise;
        this.nomEntreprise = nomEntreprise;
        this.dateAjout = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters et Setters
    public String getIdEntreprise() { return idEntreprise; }
    public void setIdEntreprise(String idEntreprise) { this.idEntreprise = idEntreprise; }
    
    public String getNomEntreprise() { return nomEntreprise; }
    public void setNomEntreprise(String nomEntreprise) { this.nomEntreprise = nomEntreprise; }
    
    public String getEmailEntreprise() { return emailEntreprise; }
    public void setEmailEntreprise(String emailEntreprise) { this.emailEntreprise = emailEntreprise; }
    
    public LocalDateTime getDateAjout() { return dateAjout; }
    public void setDateAjout(LocalDateTime dateAjout) { this.dateAjout = dateAjout; }
    
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (dateAjout == null) {
            dateAjout = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}