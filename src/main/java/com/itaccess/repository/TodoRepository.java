package com.itaccess.repository;

import com.itaccess.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
    
    List<Todo> findByCreatedBy(Long createdBy);
    
    List<Todo> findByCreatedByOrderByCreatedAtDesc(Long createdBy);
    
    @Query("SELECT DISTINCT t.createdBy FROM Todo t WHERE t.createdBy IS NOT NULL")
    List<Long> findDistinctCreatedBy();
<<<<<<< HEAD

    List<Todo> findByCreatedByInOrderByCreatedAtDesc(List<Long> createdByList);
=======
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
}
