package com.dds.gestioncandidatures.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.dds.gestioncandidatures.entity.Poste;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PosteForm {

    @NotBlank
    @Size(max = 255)
    private String intitule;

    @Size(max = 5000)
    private String description;

    @Size(max = 5000)
    private String competences;

    @DecimalMin(value = "0.0", inclusive = true, message = "Le salaire minimum doit être positif")
    private BigDecimal salaireMin;

    @DecimalMin(value = "0.0", inclusive = true, message = "Le salaire maximum doit être positif")
    private BigDecimal salaireMax;

    @NotNull
    private Poste.TypeContrat typeContrat = Poste.TypeContrat.CDI;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateExpiration;

    public String getIntitule() {
        return intitule;
    }

    public void setIntitule(String intitule) {
        this.intitule = intitule;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCompetences() {
        return competences;
    }

    public void setCompetences(String competences) {
        this.competences = competences;
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

    public Poste.TypeContrat getTypeContrat() {
        return typeContrat;
    }

    public void setTypeContrat(Poste.TypeContrat typeContrat) {
        this.typeContrat = typeContrat;
    }

    public LocalDate getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDate dateExpiration) {
        this.dateExpiration = dateExpiration;
    }
}

