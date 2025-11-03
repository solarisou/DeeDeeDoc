package com.dds.gestioncandidatures.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dds.gestioncandidatures.entity.FichierCandidat;

@Repository
public interface FichierCandidatRepository extends JpaRepository<FichierCandidat, Long> {
    
    List<FichierCandidat> findByCandidatIdCandidature(String idCandidature);
    
    FichierCandidat findFirstByCandidatIdCandidatureOrderByVersionDesc(String idCandidature);
}