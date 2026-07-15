package com.itaccess.repository;

import com.itaccess.entity.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {

    boolean existsByNotificationIdAndUserId(Long notificationId, Long userId);

    @Modifying
    @Query("DELETE FROM NotificationRead r WHERE r.notificationId = :notificationId")
    void deleteByNotificationId(@Param("notificationId") Long notificationId);

    @Modifying
    @Query(value = """
            INSERT INTO user_notification_reads (notification_id, user_id)
            SELECT n.id, :userId FROM system_notifications n
            WHERE (n.target_user_id = :userId OR n.target_user_id IS NULL)
            AND n.id NOT IN (SELECT notification_id FROM user_notification_reads WHERE user_id = :userId)
            """, nativeQuery = true)
    void markAllReadForUser(@Param("userId") Long userId);
}
