package com.dds.gestioncandidatures.dto;

import com.dds.gestioncandidatures.entity.Entreprise;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EntrepriseForm {

    @NotBlank(message = "L'identifiant est obligatoire")
    @Size(max = 50, message = "L'identifiant ne doit pas dépasser 50 caractères")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "L'identifiant ne doit contenir que des lettres, chiffres, tirets ou underscores")
    private String idEntreprise;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 255, message = "Le nom ne doit pas dépasser 255 caractères")
    private String nomEntreprise;

    @Email(message = "L'adresse email n'est pas valide")
    @Size(max = 255, message = "L'email ne doit pas dépasser 255 caractères")
    private String emailEntreprise;

    private Entreprise.Statut statut = Entreprise.Statut.active;

    public String getIdEntreprise() {
        return idEntreprise;
    }

    public void setIdEntreprise(String idEntreprise) {
        this.idEntreprise = idEntreprise;
    }

    public String getNomEntreprise() {
        return nomEntreprise;
    }

    public void setNomEntreprise(String nomEntreprise) {
        this.nomEntreprise = nomEntreprise;
    }

    public String getEmailEntreprise() {
        return emailEntreprise;
    }

    public void setEmailEntreprise(String emailEntreprise) {
        this.emailEntreprise = emailEntreprise;
    }

    public Entreprise.Statut getStatut() {
        return statut;
    }

    public void setStatut(Entreprise.Statut statut) {
        this.statut = statut;
    }

    public static EntrepriseForm fromEntity(Entreprise entreprise) {
        EntrepriseForm form = new EntrepriseForm();
        form.setIdEntreprise(entreprise.getIdEntreprise());
        form.setNomEntreprise(entreprise.getNomEntreprise());
        form.setEmailEntreprise(entreprise.getEmailEntreprise());
        form.setStatut(entreprise.getStatut());
        return form;
    }
}

