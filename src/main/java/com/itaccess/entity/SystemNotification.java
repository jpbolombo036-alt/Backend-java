package com.itaccess.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemNotification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, length = 500)
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean read = false;
    
    @Column(name = "target_user_id")
    private Long targetUserId; // null si notification globale
    
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
@Column(name = "action_url")
    private String actionUrl;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum NotificationType {
        INFO, SUCCESS, WARNING, ERROR, SYSTEM
    }
}
