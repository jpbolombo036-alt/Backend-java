package com.itaccess.service;

import com.itaccess.dto.SystemNotificationDTO;
import com.itaccess.entity.NotificationRead;
import com.itaccess.entity.SystemNotification;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.NotificationReadRepository;
import com.itaccess.repository.SystemNotificationRepository;
import com.itaccess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final NotificationReadRepository readRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String BROADCAST_DESTINATION = "/topic/notifications";

    public List<SystemNotificationDTO> getUserNotifications(Long userId) {
        return notificationRepository.findAllVisibleByUserId(userId).stream()
                .map(n -> toDTO(n, isRead(n.getId(), userId)))
                .collect(Collectors.toList());
    }

    public List<SystemNotificationDTO> getUnreadNotifications(Long userId) {
        return notificationRepository.findAllUnreadByUserId(userId).stream()
                .map(n -> toDTO(n, false))
                .collect(Collectors.toList());
    }

    public Long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public SystemNotificationDTO createNotification(String title, String message,
                                                   SystemNotification.NotificationType type,
                                                   Long targetUserId, Long createdBy, String actionUrl) {
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

        SystemNotificationDTO dto = toDTO(notificationRepository.save(notification), false);
        broadcast(dto);
        return dto;
    }

    @Transactional
    public SystemNotificationDTO createGlobalNotification(String title, String message,
                                                        SystemNotification.NotificationType type,
                                                        Long createdBy, String actionUrl) {
        return createNotification(title, message, type, null, createdBy, actionUrl);
    }

    @Transactional
    public SystemNotificationDTO markAsRead(Long notificationId, Long userId) {
        SystemNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification non trouvée avec l'ID: " + notificationId));

        if (!readRepository.existsByNotificationIdAndUserId(notificationId, userId)) {
            readRepository.save(NotificationRead.builder()
                    .notificationId(notificationId)
                    .userId(userId)
                    .build());
        }

        return toDTO(notification, true);
    }

    @Transactional
    public void markAllAsReadForUser(Long userId) {
        readRepository.markAllReadForUser(userId);
    }

    @Transactional
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteByCreatedAtBefore(cutoffDate);
    }

    private boolean isRead(Long notificationId, Long userId) {
        return readRepository.existsByNotificationIdAndUserId(notificationId, userId);
    }

    private void broadcast(SystemNotificationDTO dto) {
        try {
            messagingTemplate.convertAndSend(BROADCAST_DESTINATION, dto);
        } catch (Exception e) {
            // Diffusion temps réel best-effort : ne pas faire échouer la création
        }
    }

    private SystemNotificationDTO toDTO(SystemNotification notification, boolean read) {
        return SystemNotificationDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .read(read)
                .targetUserId(notification.getTargetUserId())
                .createdBy(notification.getCreatedBy())
                .createdAt(notification.getCreatedAt() != null ? notification.getCreatedAt().format(DATE_FORMATTER) : null)
                .actionUrl(notification.getActionUrl())
                .build();
    }
}
