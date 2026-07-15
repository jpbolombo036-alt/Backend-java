package com.itaccess.repository;

import com.itaccess.entity.NotificationRead;
import com.itaccess.entity.SystemNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {

    @Query("SELECT n FROM SystemNotification n WHERE n.targetUserId = :userId OR n.targetUserId IS NULL ORDER BY n.createdAt DESC")
    List<SystemNotification> findAllVisibleByUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM SystemNotification n " +
           "WHERE (n.targetUserId = :userId OR n.targetUserId IS NULL) " +
           "AND n.id NOT IN (SELECT r.notificationId FROM NotificationRead r WHERE r.userId = :userId) " +
           "ORDER BY n.createdAt DESC")
    List<SystemNotification> findAllUnreadByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM SystemNotification n " +
           "WHERE (n.targetUserId = :userId OR n.targetUserId IS NULL) " +
           "AND n.id NOT IN (SELECT r.notificationId FROM NotificationRead r WHERE r.userId = :userId)")
    Long countUnreadByUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM SystemNotification n WHERE (n.targetUserId = :userId OR n.targetUserId IS NULL) AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<SystemNotification> findRecentByUserId(@Param("userId") Long userId, LocalDateTime since);

    void deleteByCreatedAtBefore(LocalDateTime date);
}
