package com.itaccess.repository;

import com.itaccess.entity.SystemNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {
    
    List<SystemNotification> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);
    
    List<SystemNotification> findByTargetUserIdIsNullOrderByCreatedAtDesc();
    
    List<SystemNotification> findByTargetUserIdAndReadFalseOrderByCreatedAtDesc(Long targetUserId);
    
    List<SystemNotification> findByTargetUserIdIsNullAndReadFalseOrderByCreatedAtDesc();
    
    @Query("SELECT COUNT(n) FROM SystemNotification n WHERE (n.targetUserId = :userId OR n.targetUserId IS NULL) AND n.read = false")
    Long countUnreadByUserId(Long userId);
    
    @Query("SELECT n FROM SystemNotification n WHERE (n.targetUserId = :userId OR n.targetUserId IS NULL) AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<SystemNotification> findRecentByUserId(Long userId, LocalDateTime since);
    
    void deleteByCreatedAtBefore(LocalDateTime date);
}
