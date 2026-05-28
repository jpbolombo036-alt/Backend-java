package com.itaccess.repository;

import com.itaccess.entity.SystemNotification;
import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.Modifying;
=======
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {
    
<<<<<<< HEAD
    @Query("SELECT n FROM SystemNotification n WHERE n.targetUserId = :userId OR n.targetUserId IS NULL ORDER BY n.createdAt DESC")
    List<SystemNotification> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT n FROM SystemNotification n WHERE (n.targetUserId = :userId OR n.targetUserId IS NULL) AND n.read = false ORDER BY n.createdAt DESC")
    List<SystemNotification> findAllUnreadByUserIdOrderByCreatedAtDesc(Long userId);
=======
    List<SystemNotification> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);
    
    List<SystemNotification> findByTargetUserIdIsNullOrderByCreatedAtDesc();
    
    List<SystemNotification> findByTargetUserIdAndReadFalseOrderByCreatedAtDesc(Long targetUserId);
    
    List<SystemNotification> findByTargetUserIdIsNullAndReadFalseOrderByCreatedAtDesc();
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
    
    @Query("SELECT COUNT(n) FROM SystemNotification n WHERE (n.targetUserId = :userId OR n.targetUserId IS NULL) AND n.read = false")
    Long countUnreadByUserId(Long userId);
    
    @Query("SELECT n FROM SystemNotification n WHERE (n.targetUserId = :userId OR n.targetUserId IS NULL) AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<SystemNotification> findRecentByUserId(Long userId, LocalDateTime since);
    
<<<<<<< HEAD
    @Modifying
    @Query("UPDATE SystemNotification n SET n.read = true WHERE (n.targetUserId = :userId OR n.targetUserId IS NULL) AND n.read = false")
    void markAllAsReadByUserId(Long userId);

=======
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
    void deleteByCreatedAtBefore(LocalDateTime date);
}
