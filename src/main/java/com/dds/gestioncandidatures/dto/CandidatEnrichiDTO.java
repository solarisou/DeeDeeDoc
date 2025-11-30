package com.dds.gestioncandidatures.dto;

import com.dds.gestioncandidatures.entity.*;
import java.util.List;
import java.util.stream.Collectors;

public class CandidatEnrichiDTO {
    
    private Candidat candidat;
    private List<Diplome> diplomes;
    private List<Experience> experiences;
    private List<Langue> langues;
    private List<Competence> competences;
    private List<SoftSkill> softSkills;
    private List<PermisConduire> permis;
    private List<FichierCandidat> fichiers;
    
    public CandidatEnrichiDTO(Candidat candidat) {
        this.candidat = candidat;
    }
    
    // Méthodes d'aide pour l'affichage dans le template
    public String getDiplomesResume() {
        if (diplomes == null || diplomes.isEmpty()) return "-";
        return diplomes.stream()
            .map(d -> d.getNomDiplome() + (d.getNiveau() != null ? " (" + d.getNiveau() + ")" : ""))
            .collect(Collectors.joining(", "));
    }
    
    public String getExperiencesResume() {
        if (experiences == null || experiences.isEmpty()) return "-";
        return experiences.stream()
            .map(e -> {
                String result = e.getNomEntreprise();
                if (e.getPoste() != null && !e.getPoste().isEmpty()) {
                    result = e.getPoste() + " chez " + result;
                }
                if (e.getDureeExperience() != null && !e.getDureeExperience().isEmpty()) {
                    result += " (" + e.getDureeExperience() + ")";
                }
                return result;
            })
            .collect(Collectors.joining(", "));
    }
    
    public String getLanguesResume() {
        if (langues == null || langues.isEmpty()) return "-";
        return langues.stream()
            .map(l -> l.getLangue() + " (" + l.getNiveau() + ")")
            .collect(Collectors.joining(", "));
    }
    
    public String getCompetencesResume() {
        if (competences == null || competences.isEmpty()) return "-";
        return competences.stream()
            .limit(5) // Limiter à 5 compétences
            .map(c -> c.getCompetence())
            .collect(Collectors.joining(", ")) + (competences.size() > 5 ? "..." : "");
    }
    
    public String getSoftSkillsResume() {
        if (softSkills == null || softSkills.isEmpty()) return "-";
        return softSkills.stream()
            .limit(3) // Limiter à 3 soft skills
            .map(s -> s.getSoftSkill())
            .collect(Collectors.joining(", ")) + (softSkills.size() > 3 ? "..." : "");
    }
    
    public String getPermisResume() {
        if (permis == null || permis.isEmpty()) return "-";
        return permis.stream()
            .map(p -> "Permis " + p.getCategoriePermis())
            .collect(Collectors.joining(", "));
    }
    
    public int getNbDiplomes() {
        return diplomes != null ? diplomes.size() : 0;
    }
    
    public int getNbExperiences() {
        return experiences != null ? experiences.size() : 0;
    }
    
    public int getNbLangues() {
        return langues != null ? langues.size() : 0;
    }
    
    public int getNbCompetences() {
        return competences != null ? competences.size() : 0;
    }
    
    public int getNbSoftSkills() {
        return softSkills != null ? softSkills.size() : 0;
    }
    
    public int getNbPermis() {
        return permis != null ? permis.size() : 0;
    }
    
    public int getNbFichiers() {
        return fichiers != null ? fichiers.size() : 0;
    }
    
    public String getIntitulePoste() {
        String intitule = candidat.getIntitulePoste();
        return (intitule != null && !intitule.isEmpty()) ? intitule : "Non spécifié";
    }

    public String getTypeCandidatureLibelle() {
        if (candidat.getTypeCandidature() == null) {
            return "Non renseigné";
        }
        return switch (candidat.getTypeCandidature()) {
            case POSTE -> "Candidature sur poste";
            case SPONTANEE -> "Candidature spontanée";
        };
    }

    public boolean isCandidatureSpontanee() {
        return candidat.getTypeCandidature() == com.dds.gestioncandidatures.entity.Candidat.TypeCandidature.SPONTANEE;
    }
    
    // Getters et Setters
    public Candidat getCandidat() { return candidat; }
    public void setCandidat(Candidat candidat) { this.candidat = candidat; }
    
    public List<Diplome> getDiplomes() { return diplomes; }
    public void setDiplomes(List<Diplome> diplomes) { this.diplomes = diplomes; }
    
    public List<Experience> getExperiences() { return experiences; }
    public void setExperiences(List<Experience> experiences) { this.experiences = experiences; }
    
    public List<Langue> getLangues() { return langues; }
    public void setLangues(List<Langue> langues) { this.langues = langues; }
    
    public List<Competence> getCompetences() { return competences; }
    public void setCompetences(List<Competence> competences) { this.competences = competences; }
    
    public List<SoftSkill> getSoftSkills() { return softSkills; }
    public void setSoftSkills(List<SoftSkill> softSkills) { this.softSkills = softSkills; }
    
    public List<PermisConduire> getPermis() { return permis; }
    public void setPermis(List<PermisConduire> permis) { this.permis = permis; }
    
    public List<FichierCandidat> getFichiers() { return fichiers; }
    public void setFichiers(List<FichierCandidat> fichiers) { this.fichiers = fichiers; }
}

