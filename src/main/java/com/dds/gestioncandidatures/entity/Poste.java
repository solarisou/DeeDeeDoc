package com.dds.gestioncandidatures.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "postes_disponibles")
public class Poste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entreprise", nullable = false)
    private Entreprise entreprise;

    @Column(name = "intitule_poste", nullable = false)
    private String intitulePoste;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "competences_requises", columnDefinition = "TEXT")
    private String competencesRequises;

    @Column(name = "salaire_min")
    private BigDecimal salaireMin;

    @Column(name = "salaire_max")
    private BigDecimal salaireMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false)
    private TypeContrat typeContrat = TypeContrat.CDI;

    @Column
    private String localisation;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.ouvert;

    public enum Statut {
        ouvert,
        ferme,
        pourvue
    }

    public enum TypeContrat {
        CDI,
        CDD,
        Stage,
        Alternance,
        Freelance
    }

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Entreprise getEntreprise() {
        return entreprise;
    }

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
    }

    public String getIntitulePoste() {
        return intitulePoste;
    }

    public void setIntitulePoste(String intitulePoste) {
        this.intitulePoste = intitulePoste;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCompetencesRequises() {
        return competencesRequises;
    }

    public void setCompetencesRequises(String competencesRequises) {
        this.competencesRequises = competencesRequises;
    }

    public BigDecimal getSalaireMin() {
        return salaireMin;
    }

    public void setSalaireMin(BigDecimal salaireMin) {
        this.salaireMin = salaireMin;
    }

    public BigDecimal getSalaireMax() {
        return salaireMax;
    }

    public void setSalaireMax(BigDecimal salaireMax) {
        this.salaireMax = salaireMax;
    }

    public TypeContrat getTypeContrat() {
        return typeContrat;
    }

    public void setTypeContrat(TypeContrat typeContrat) {
        this.typeContrat = typeContrat;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDate getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDate dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public boolean estOuvert() {
        return Statut.ouvert.equals(this.statut);
    }
}

