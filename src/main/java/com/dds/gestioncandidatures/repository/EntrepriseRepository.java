package com.dds.gestioncandidatures.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dds.gestioncandidatures.entity.Entreprise;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, String> {
    
    List<Entreprise> findByStatut(Entreprise.Statut statut);
    
    List<Entreprise> findByNomEntrepriseContainingIgnoreCase(String nom);
}