package com.itaccess.service;

import com.itaccess.dto.SystemNotificationDTO;
import com.itaccess.entity.SystemNotification;
import com.itaccess.repository.SystemNotificationRepository;
import com.itaccess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemNotificationService {
    
    private final SystemNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    
    public List<SystemNotificationDTO> getUserNotifications(Long userId) {
        List<SystemNotification> notifications = notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(userId);
        List<SystemNotification> globalNotifications = notificationRepository.findByTargetUserIdIsNullOrderByCreatedAtDesc();
        
        // Fusionner les notifications personnelles et globales
        notifications.addAll(globalNotifications);
        
        // Trier par date
        notifications.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        return notifications.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<SystemNotificationDTO> getUnreadNotifications(Long userId) {
        List<SystemNotification> notifications = notificationRepository.findByTargetUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        List<SystemNotification> globalNotifications = notificationRepository.findByTargetUserIdIsNullAndReadFalseOrderByCreatedAtDesc();
        
        // Fusionner les notifications personnelles et globales non lues
        notifications.addAll(globalNotifications);
        
        // Trier par date
        notifications.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        return notifications.stream()
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
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        notification.setRead(true);
        return toDTO(notificationRepository.save(notification));
    }
    
    @Transactional
    public void markAllAsReadForUser(Long userId) {
        List<SystemNotification> unreadNotifications = notificationRepository.findByTargetUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        List<SystemNotification> unreadGlobalNotifications = notificationRepository.findByTargetUserIdIsNullAndReadFalseOrderByCreatedAtDesc();
        
        // Marquer toutes les notifications non lues comme lues
        unreadNotifications.forEach(n -> n.setRead(true));
        unreadGlobalNotifications.forEach(n -> n.setRead(true));
        
        notificationRepository.saveAll(unreadNotifications);
        notificationRepository.saveAll(unreadGlobalNotifications);
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
                .createdAt(notification.getCreatedAt().toString())
                .actionUrl(notification.getActionUrl())
                .build();
    }
}
