package com.dds.gestioncandidatures.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dds.gestioncandidatures.entity.SoftSkill;

@Repository
public interface SoftSkillRepository extends JpaRepository<SoftSkill, Integer> {
    List<SoftSkill> findByIdCandidature(String idCandidature);
}







