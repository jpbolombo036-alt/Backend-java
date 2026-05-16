package com.itaccess.controller;

import com.itaccess.dto.SystemNotificationDTO;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.SystemNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system-notifications")
@RequiredArgsConstructor
@Tag(name = "System Notifications", description = "API de notifications système")
public class SystemNotificationController {
    
    private final SystemNotificationService notificationService;
    
    @GetMapping
    @Operation(summary = "Liste des notifications", description = "Retourne toutes les notifications pour l'utilisateur connecté")
    public ResponseEntity<List<SystemNotificationDTO>> getUserNotifications(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        List<SystemNotificationDTO> notifications = notificationService.getUserNotifications(currentUser.getId());
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/unread")
    @Operation(summary = "Notifications non lues", description = "Retourne les notifications non lues pour l'utilisateur connecté")
    public ResponseEntity<List<SystemNotificationDTO>> getUnreadNotifications(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        List<SystemNotificationDTO> notifications = notificationService.getUnreadNotifications(currentUser.getId());
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/unread-count")
    @Operation(summary = "Nombre de notifications non lues", description = "Retourne le nombre de notifications non lues")
    public ResponseEntity<Long> getUnreadCount(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        Long count = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(count);
    }
    
    @PostMapping
    @Operation(summary = "Créer une notification", description = "Crée une nouvelle notification système (admin uniquement)")
    public ResponseEntity<SystemNotificationDTO> createNotification(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @RequestBody CreateNotificationRequest request) {
        if (!"admin".equals(currentUser.getRole())) {
            return ResponseEntity.status(403).build();
        }
        
        SystemNotificationDTO notification = notificationService.createNotification(
            request.getTitle(),
            request.getMessage(),
            request.getType(),
            request.getTargetUserId(),
            currentUser.getId(),
            request.getActionUrl()
        );
        
        return ResponseEntity.ok(notification);
    }
    
    @PostMapping("/global")
    @Operation(summary = "Créer une notification globale", description = "Crée une notification pour tous les utilisateurs (admin uniquement)")
    public ResponseEntity<SystemNotificationDTO> createGlobalNotification(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @RequestBody CreateGlobalNotificationRequest request) {
        if (!"admin".equals(currentUser.getRole())) {
            return ResponseEntity.status(403).build();
        }
        
        SystemNotificationDTO notification = notificationService.createGlobalNotification(
            request.getTitle(),
            request.getMessage(),
            request.getType(),
            currentUser.getId(),
            request.getActionUrl()
        );
        
        return ResponseEntity.ok(notification);
    }
    
    @PatchMapping("/{id}/read")
    @Operation(summary = "Marquer comme lu", description = "Marque une notification comme lue")
    public ResponseEntity<SystemNotificationDTO> markAsRead(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        SystemNotificationDTO notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }
    
    @PatchMapping("/read-all")
    @Operation(summary = "Tout marquer comme lu", description = "Marque toutes les notifications de l'utilisateur comme lues")
    public ResponseEntity<Void> markAllAsRead(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        notificationService.markAllAsReadForUser(currentUser.getId());
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une notification", description = "Supprime une notification (admin uniquement)")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        if (!"admin".equals(currentUser.getRole())) {
            return ResponseEntity.status(403).build();
        }
        
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
    
    // DTOs pour les requêtes
    public static class CreateNotificationRequest {
        private String title;
        private String message;
        private com.itaccess.entity.SystemNotification.NotificationType type;
        private Long targetUserId;
        private String actionUrl;
        
        // Getters et Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public com.itaccess.entity.SystemNotification.NotificationType getType() { return type; }
        public void setType(com.itaccess.entity.SystemNotification.NotificationType type) { this.type = type; }
        public Long getTargetUserId() { return targetUserId; }
        public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
        public String getActionUrl() { return actionUrl; }
        public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    }
    
    public static class CreateGlobalNotificationRequest {
        private String title;
        private String message;
        private com.itaccess.entity.SystemNotification.NotificationType type;
        private String actionUrl;
        
        // Getters et Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public com.itaccess.entity.SystemNotification.NotificationType getType() { return type; }
        public void setType(com.itaccess.entity.SystemNotification.NotificationType type) { this.type = type; }
        public String getActionUrl() { return actionUrl; }
        public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    }
}
