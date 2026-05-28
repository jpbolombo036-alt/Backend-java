package com.itaccess.repository;

import com.itaccess.entity.TestStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<TestStep, Long> {
    
    List<TestStep> findBySessionId(Long sessionId);

<<<<<<< HEAD
    List<TestStep> findBySessionIdIn(List<Long> sessionIds);

=======
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
    @Query("SELECT MAX(t.testNumber) FROM TestStep t WHERE t.sessionId = :sessionId")
    Optional<Long> findMaxTestNumberBySessionId(Long sessionId);
    
    List<TestStep> findByApplicationId(Long applicationId);
    
    @Modifying
    void deleteBySessionId(Long sessionId);
}