package com.itaccess.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "user_notification_reads",
        uniqueConstraints = @UniqueConstraint(columnNames = {"notification_id", "user_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
