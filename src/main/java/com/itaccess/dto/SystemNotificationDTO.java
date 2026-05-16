package com.itaccess.dto;

import com.itaccess.entity.SystemNotification;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemNotificationDTO {
    
    private Long id;
    private String title;
    private String message;
    private SystemNotification.NotificationType type;
    private Boolean read;
    private Long targetUserId;
    private Long createdBy;
    private String createdAt;
    private String actionUrl;
}
