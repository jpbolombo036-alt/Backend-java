package com.itaccess.service;

import com.itaccess.dto.SystemNotificationDTO;
import com.itaccess.entity.SystemNotification;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.SystemNotificationRepository;
import com.itaccess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemNotificationService {
    
    private final SystemNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    public List<SystemNotificationDTO> getUserNotifications(Long userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<SystemNotificationDTO> getUnreadNotifications(Long userId) {
        return notificationRepository.findAllUnreadByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
    
    @Transactional
    public SystemNotificationDTO createNotification(String title, String message, 
                                                   SystemNotification.NotificationType type,
                                                   Long targetUserId, Long createdBy, String actionUrl) {
        // Vérifier que l'utilisateur cible existe si spécifié
        if (targetUserId != null && !userRepository.existsById(targetUserId)) {
            throw new IllegalArgumentException("Target user not found");
        }
        
        SystemNotification notification = SystemNotification.builder()
                .title(title)
                .message(message)
                .type(type)
                .read(false)
                .targetUserId(targetUserId)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .actionUrl(actionUrl)
                .build();
        
        return toDTO(notificationRepository.save(notification));
    }
    
    @Transactional
    public SystemNotificationDTO createGlobalNotification(String title, String message,
                                                        SystemNotification.NotificationType type,
                                                        Long createdBy, String actionUrl) {
        return createNotification(title, message, type, null, createdBy, actionUrl);
    }
    
    @Transactional
    public SystemNotificationDTO markAsRead(Long notificationId) {
        SystemNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification non trouvée avec l'ID: " + notificationId));
        
        notification.setRead(true);
        return toDTO(notificationRepository.save(notification));
    }
    
    @Transactional
    public void markAllAsReadForUser(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
    
    @Transactional
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }
    
    @Transactional
    public void cleanupOldNotifications() {
        // Supprimer les notifications de plus de 30 jours
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteByCreatedAtBefore(cutoffDate);
    }
    
    private SystemNotificationDTO toDTO(SystemNotification notification) {
        return SystemNotificationDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .read(notification.getRead())
                .targetUserId(notification.getTargetUserId())
                .createdBy(notification.getCreatedBy())
                .createdAt(notification.getCreatedAt() != null ? notification.getCreatedAt().format(DATE_FORMATTER) : null)
                .actionUrl(notification.getActionUrl())
                .build();
    }
}
