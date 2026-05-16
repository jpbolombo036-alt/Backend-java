package com.itaccess.repository;

import com.itaccess.entity.TestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TestSessionRepository extends JpaRepository<TestSession, Long> {
    
    List<TestSession> findByApplicationId(Long applicationId);
    
    List<TestSession> findByCreatedBy(Long createdBy);
    
    List<TestSession> findByOrderByDateCreationDesc();
}