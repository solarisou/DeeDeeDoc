package com.dds.gestioncandidatures.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dds.gestioncandidatures.entity.Langue;

@Repository
public interface LangueRepository extends JpaRepository<Langue, Integer> {
    List<Langue> findByIdCandidature(String idCandidature);
}







