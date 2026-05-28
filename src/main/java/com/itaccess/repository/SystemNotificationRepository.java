package com.itaccess.repository;

import com.itaccess.entity.SystemNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {
    
    @Query("SELECT n FROM SystemNotification n WHERE n.targetUserId = :userId OR n.targetUserId IS NULL ORDER BY n.createdAt DESC")
    List<SystemNotification> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT n FROM SystemNotification n WHERE (n.targetUserId = :userId OR n.targetUserId IS NULL) AND n.read = false ORDER BY n.createdAt DESC")
    List<SystemNotification> findAllUnreadByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT COUNT(n) FROM SystemNotification n WHERE (n.targetUserId = :userId OR n.targetUserId IS NULL) AND n.read = false")
    Long countUnreadByUserId(Long userId);
    
    @Query("SELECT n FROM SystemNotification n WHERE (n.targetUserId = :userId OR n.targetUserId IS NULL) AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<SystemNotification> findRecentByUserId(Long userId, LocalDateTime since);
    
    @Modifying
    @Query("UPDATE SystemNotification n SET n.read = true WHERE (n.targetUserId = :userId OR n.targetUserId IS NULL) AND n.read = false")
    void markAllAsReadByUserId(Long userId);

    void deleteByCreatedAtBefore(LocalDateTime date);
}
