package com.itaccess.repository;

import com.itaccess.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    List<User> findByIdIn(List<Long> ids);
<<<<<<< HEAD

    List<User> findByIdNot(Long id);
=======
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
}