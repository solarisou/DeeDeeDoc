package com.dds.gestioncandidatures.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dds.gestioncandidatures.entity.PermisConduire;

@Repository
public interface PermisConduireRepository extends JpaRepository<PermisConduire, Integer> {
    List<PermisConduire> findByIdCandidature(String idCandidature);
}







