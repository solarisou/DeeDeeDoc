package com.dds.gestioncandidatures.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dds.gestioncandidatures.entity.Diplome;

@Repository
public interface DiplomeRepository extends JpaRepository<Diplome, Integer> {
    List<Diplome> findByIdCandidature(String idCandidature);
}







