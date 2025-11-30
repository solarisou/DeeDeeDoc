package com.dds.gestioncandidatures.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.dds.gestioncandidatures.entity.Candidat;

@Repository
public interface CandidatRepository extends JpaRepository<Candidat, String> {
    
    List<Candidat> findByStatutCandidature(Candidat.StatutCandidature statut);
    
    List<Candidat> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(String nom, String prenom);
    
    @Query("SELECT c FROM Candidat c WHERE c.posteVise LIKE %?1%")
    List<Candidat> findByPosteViseContaining(String poste);
    
    List<Candidat> findByOrderByDateCandidatureDesc();

    List<Candidat> findByPosteId(Long posteId);
}