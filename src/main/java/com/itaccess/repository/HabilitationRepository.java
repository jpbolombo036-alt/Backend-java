package com.itaccess.repository;

import com.itaccess.entity.Habilitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HabilitationRepository extends JpaRepository<Habilitation, Long> {
    
    List<Habilitation> findByCompteId(Long compteId);
}