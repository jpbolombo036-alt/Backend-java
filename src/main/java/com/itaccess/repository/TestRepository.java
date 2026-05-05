package com.itaccess.repository;

import com.itaccess.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
    
    List<Test> findBySessionId(Long sessionId);
    
    List<Test> findByApplicationId(Long applicationId);
    
    @Modifying
    void deleteBySessionId(Long sessionId);
}