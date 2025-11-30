package com.dds.gestioncandidatures.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "fichiers_candidat")
public class FichierCandidat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_candidature", nullable = false)
    private Candidat candidat;
    
    @Column(name = "cv_filename", nullable = false)
    private String cvFilename;
    
    @Column(name = "cv_path")
    private String cvPath;
    
    @Column(name = "cv_size_bytes")
    private Long cvSizeBytes;
    
    @Column(name = "lm_filename")
    private String lmFilename;
    
    @Column(name = "lm_path")
    private String lmPath;
    
    @Column(name = "lm_size_bytes")
    private Long lmSizeBytes;
    
    @Column(name = "date_upload")
    private LocalDateTime dateUpload;
    
    @Column(name = "version")
    private Integer version = 1;
    
    // Constructeurs
    public FichierCandidat() {}
    
    public FichierCandidat(Candidat candidat, String cvFilename, String cvPath) {
        this.candidat = candidat;
        this.cvFilename = cvFilename;
        this.cvPath = cvPath;
        this.dateUpload = LocalDateTime.now();
    }
    
    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Candidat getCandidat() { return candidat; }
    public void setCandidat(Candidat candidat) { this.candidat = candidat; }
    
    public String getCvFilename() { return cvFilename; }
    public void setCvFilename(String cvFilename) { this.cvFilename = cvFilename; }
    
    public String getCvPath() { return cvPath; }
    public void setCvPath(String cvPath) { this.cvPath = cvPath; }
    
    public Long getCvSizeBytes() { return cvSizeBytes; }
    public void setCvSizeBytes(Long cvSizeBytes) { this.cvSizeBytes = cvSizeBytes; }
    
    public String getLmFilename() { return lmFilename; }
    public void setLmFilename(String lmFilename) { this.lmFilename = lmFilename; }
    
    public String getLmPath() { return lmPath; }
    public void setLmPath(String lmPath) { this.lmPath = lmPath; }
    
    public Long getLmSizeBytes() { return lmSizeBytes; }
    public void setLmSizeBytes(Long lmSizeBytes) { this.lmSizeBytes = lmSizeBytes; }
    
    public LocalDateTime getDateUpload() { return dateUpload; }
    public void setDateUpload(LocalDateTime dateUpload) { this.dateUpload = dateUpload; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    
    @PrePersist
    protected void onCreate() {
        if (dateUpload == null) {
            dateUpload = LocalDateTime.now();
        }
    }
}