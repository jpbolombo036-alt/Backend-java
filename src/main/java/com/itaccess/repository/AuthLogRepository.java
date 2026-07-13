package com.itaccess.repository;

import com.itaccess.entity.AuthLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuthLogRepository extends JpaRepository<AuthLog, Long> {

    List<AuthLog> findByUsernameOrderByCreatedAtDesc(String username);

    List<AuthLog> findBySuccessFalseOrderByCreatedAtDesc();

    List<AuthLog> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAt);
}
