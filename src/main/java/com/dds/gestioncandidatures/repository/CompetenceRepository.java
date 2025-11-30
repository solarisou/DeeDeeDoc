package com.dds.gestioncandidatures.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dds.gestioncandidatures.entity.Competence;

@Repository
public interface CompetenceRepository extends JpaRepository<Competence, Integer> {
    List<Competence> findByIdCandidature(String idCandidature);
}







