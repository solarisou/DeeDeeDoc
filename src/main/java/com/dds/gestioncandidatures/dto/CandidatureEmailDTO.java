package com.dds.gestioncandidatures.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO pour représenter les candidatures reçues par email au format JSON
 */
public class CandidatureEmailDTO {
    
    @JsonProperty("idCandidature")
    private String idCandidature;
    
    @JsonProperty("nom")
    private String nom;
    
    @JsonProperty("prenom")
    private String prenom;
    
    @JsonProperty("mail")
    private String mail;
    
    @JsonProperty("telephone")
    private String telephone;
    
    @JsonProperty("diplomes")
    private List<Diplome> diplomes;
    
    @JsonProperty("experiences")
    private List<Experience> experiences;
    
    @JsonProperty("posteVise")
    private String posteVise;
    
    @JsonProperty("dateDisponibilite")
    private String dateDisponibilite;
    
    @JsonProperty("competences")
    private List<String> competences;
    
    @JsonProperty("softSkills")
    private List<String> softSkills;
    
    @JsonProperty("permisDeConduite")
    private List<String> permisDeConduite;
    
    @JsonProperty("langues")
    private List<String> langues;
    
    @JsonProperty("dateCandidature")
    private String dateCandidature;
    
    @JsonProperty("fichiers")
    private Fichiers fichiers;
    
    @JsonProperty("Message_Candidat_Entreprise")
    private MessageCandidatEntreprise messageCandidatEntreprise;
    
    // Classes internes
    public static class Diplome {
        @JsonProperty("nomDiplome")
        private String nomDiplome;
        
        @JsonProperty("anneeObtention")
        private String anneeObtention;
        
        @JsonProperty("domaine")
        private String domaine;
        
        // Getters et Setters
        public String getNomDiplome() { return nomDiplome; }
        public void setNomDiplome(String nomDiplome) { this.nomDiplome = nomDiplome; }
        
        public String getAnneeObtention() { return anneeObtention; }
        public void setAnneeObtention(String anneeObtention) { this.anneeObtention = anneeObtention; }
        
        public String getDomaine() { return domaine; }
        public void setDomaine(String domaine) { this.domaine = domaine; }
    }
    
    public static class Experience {
        @JsonProperty("nomEntreprise")
        private String nomEntreprise;
        
        @JsonProperty("dureeExperience")
        private String dureeExperience;
        
        @JsonProperty("dateDebut")
        private String dateDebut;
        
        // Getters et Setters
        public String getNomEntreprise() { return nomEntreprise; }
        public void setNomEntreprise(String nomEntreprise) { this.nomEntreprise = nomEntreprise; }
        
        public String getDureeExperience() { return dureeExperience; }
        public void setDureeExperience(String dureeExperience) { this.dureeExperience = dureeExperience; }
        
        public String getDateDebut() { return dateDebut; }
        public void setDateDebut(String dateDebut) { this.dateDebut = dateDebut; }
    }
    
    public static class Fichiers {
        @JsonProperty("cv_filename")
        private String cvFilename;
        
        @JsonProperty("lm_filename")
        private String lmFilename;
        
        // Getters et Setters
        public String getCvFilename() { return cvFilename; }
        public void setCvFilename(String cvFilename) { this.cvFilename = cvFilename; }
        
        public String getLmFilename() { return lmFilename; }
        public void setLmFilename(String lmFilename) { this.lmFilename = lmFilename; }
    }
    
    public static class MessageCandidatEntreprise {
        @JsonProperty("CV")
        private String cv;
        
        @JsonProperty("lettreDeMotivation")
        private String lettreDeMotivation;
        
        @JsonProperty("AutorisationStocker")
        private String autorisationStocker;
        
        @JsonProperty("AutorisationDiffuser")
        private String autorisationDiffuser;
        
        @JsonProperty("PosteVise")
        private String posteVise;
        
        // Getters et Setters
        public String getCv() { return cv; }
        public void setCv(String cv) { this.cv = cv; }
        
        public String getLettreDeMotivation() { return lettreDeMotivation; }
        public void setLettreDeMotivation(String lettreDeMotivation) { this.lettreDeMotivation = lettreDeMotivation; }
        
        public String getAutorisationStocker() { return autorisationStocker; }
        public void setAutorisationStocker(String autorisationStocker) { this.autorisationStocker = autorisationStocker; }
        
        public String getAutorisationDiffuser() { return autorisationDiffuser; }
        public void setAutorisationDiffuser(String autorisationDiffuser) { this.autorisationDiffuser = autorisationDiffuser; }
        
        public String getPosteVise() { return posteVise; }
        public void setPosteVise(String posteVise) { this.posteVise = posteVise; }
    }
    
    // Getters et Setters principaux
    public String getIdCandidature() { return idCandidature; }
    public void setIdCandidature(String idCandidature) { this.idCandidature = idCandidature; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    
    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }
    
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    
    public List<Diplome> getDiplomes() { return diplomes; }
    public void setDiplomes(List<Diplome> diplomes) { this.diplomes = diplomes; }
    
    public List<Experience> getExperiences() { return experiences; }
    public void setExperiences(List<Experience> experiences) { this.experiences = experiences; }
    
    public String getPosteVise() { return posteVise; }
    public void setPosteVise(String posteVise) { this.posteVise = posteVise; }
    
    public String getDateDisponibilite() { return dateDisponibilite; }
    public void setDateDisponibilite(String dateDisponibilite) { this.dateDisponibilite = dateDisponibilite; }
    
    public List<String> getCompetences() { return competences; }
    public void setCompetences(List<String> competences) { this.competences = competences; }
    
    public List<String> getSoftSkills() { return softSkills; }
    public void setSoftSkills(List<String> softSkills) { this.softSkills = softSkills; }
    
    public List<String> getPermisDeConduite() { return permisDeConduite; }
    public void setPermisDeConduite(List<String> permisDeConduite) { this.permisDeConduite = permisDeConduite; }
    
    public List<String> getLangues() { return langues; }
    public void setLangues(List<String> langues) { this.langues = langues; }
    
    public String getDateCandidature() { return dateCandidature; }
    public void setDateCandidature(String dateCandidature) { this.dateCandidature = dateCandidature; }
    
    public Fichiers getFichiers() { return fichiers; }
    public void setFichiers(Fichiers fichiers) { this.fichiers = fichiers; }
    
    public MessageCandidatEntreprise getMessageCandidatEntreprise() { return messageCandidatEntreprise; }
    public void setMessageCandidatEntreprise(MessageCandidatEntreprise messageCandidatEntreprise) { 
        this.messageCandidatEntreprise = messageCandidatEntreprise; 
    }
}


