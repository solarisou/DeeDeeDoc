package com.dds.gestioncandidatures.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "candidats")
public class Candidat {
    
    @Id
    @Column(name = "id_candidature")
    private String idCandidature;
    
    @NotBlank
    @Column(nullable = false)
    private String nom;
    
    @NotBlank
    @Column(nullable = false)
    private String prenom;
    
    @Email
    @NotBlank
    @Column(nullable = false)
    private String email;
    
    @NotBlank
    @Column(nullable = false)
    private String telephone;
    
    @Column(name = "poste_vise")
    private String posteVise;
    
    @Column(name = "date_disponibilite")
    private LocalDate dateDisponibilite;
    
    @Column(name = "date_candidature", nullable = false)
    private LocalDate dateCandidature;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "autorisation_stocker", nullable = false)
    private Autorisation autorisationStocker = Autorisation.N;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "autorisation_diffuser", nullable = false)
    private Autorisation autorisationDiffuser = Autorisation.N;
    
    @Column(name = "date_expiration_autorisation")
    private LocalDate dateExpirationAutorisation;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_candidature")
    private StatutCandidature statutCandidature = StatutCandidature.en_attente;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "candidat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FichierCandidat> fichiers;
    
    public enum Autorisation {
        O, N
    }
    
    public enum StatutCandidature {
        en_attente, accepte, refuse, archive
    }
    
    // Constructeurs
    public Candidat() {}
    
    public Candidat(String idCandidature, String nom, String prenom, String email, String telephone) {
        this.idCandidature = idCandidature;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.dateCandidature = LocalDate.now();
    }
    
    // Getters et Setters
    public String getIdCandidature() { return idCandidature; }
    public void setIdCandidature(String idCandidature) { this.idCandidature = idCandidature; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    
    public String getPosteVise() { return posteVise; }
    public void setPosteVise(String posteVise) { this.posteVise = posteVise; }
    
    public LocalDate getDateDisponibilite() { return dateDisponibilite; }
    public void setDateDisponibilite(LocalDate dateDisponibilite) { this.dateDisponibilite = dateDisponibilite; }
    
    public LocalDate getDateCandidature() { return dateCandidature; }
    public void setDateCandidature(LocalDate dateCandidature) { this.dateCandidature = dateCandidature; }
    
    public Autorisation getAutorisationStocker() { return autorisationStocker; }
    public void setAutorisationStocker(Autorisation autorisationStocker) { this.autorisationStocker = autorisationStocker; }
    
    public Autorisation getAutorisationDiffuser() { return autorisationDiffuser; }
    public void setAutorisationDiffuser(Autorisation autorisationDiffuser) { this.autorisationDiffuser = autorisationDiffuser; }
    
    public LocalDate getDateExpirationAutorisation() { return dateExpirationAutorisation; }
    public void setDateExpirationAutorisation(LocalDate dateExpirationAutorisation) { this.dateExpirationAutorisation = dateExpirationAutorisation; }
    
    public StatutCandidature getStatutCandidature() { return statutCandidature; }
    public void setStatutCandidature(StatutCandidature statutCandidature) { this.statutCandidature = statutCandidature; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<FichierCandidat> getFichiers() { return fichiers; }
    public void setFichiers(List<FichierCandidat> fichiers) { this.fichiers = fichiers; }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (dateCandidature == null) {
            dateCandidature = LocalDate.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public String getNomComplet() {
        return prenom + " " + nom;
    }
}