package com.dds.gestioncandidatures.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dds.gestioncandidatures.entity.Poste;

@Repository
public interface PosteRepository extends JpaRepository<Poste, Long> {

    List<Poste> findAllByOrderByDateCreationDesc();

    List<Poste> findByStatutOrderByDateCreationDesc(Poste.Statut statut);

    long countByStatut(Poste.Statut statut);

    Optional<Poste> findFirstByIntitulePosteIgnoreCaseAndStatut(String intitulePoste, Poste.Statut statut);
}

