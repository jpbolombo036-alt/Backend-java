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
import org.springframework.security.access.prepost.PreAuthorize;
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
    /**
     * Récupère l'historique des notifications (personnelles et globales).
     * Utilisé pour peupler le centre de notifications sur le dashboard.
     */
    public ResponseEntity<List<SystemNotificationDTO>> getUserNotifications(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        List<SystemNotificationDTO> notifications = notificationService.getUserNotifications(currentUser.getId());
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/unread")
    @Operation(summary = "Notifications non lues", description = "Retourne les notifications non lues pour l'utilisateur connecté")
    /**
     * Filtre uniquement les messages que l'utilisateur n'a pas encore consultés.
     * Idéal pour afficher des badges de notification (pastilles rouges).
     */
    public ResponseEntity<List<SystemNotificationDTO>> getUnreadNotifications(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        List<SystemNotificationDTO> notifications = notificationService.getUnreadNotifications(currentUser.getId());
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/unread-count")
    @Operation(summary = "Nombre de notifications non lues", description = "Retourne le nombre de notifications non lues")
    /**
     * Retourne juste le chiffre des messages non lus pour optimiser les appels réseau du header.
     */
    public ResponseEntity<Long> getUnreadCount(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        Long count = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(count);
    }
    
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Créer une notification", description = "Crée une nouvelle notification système (admin uniquement)")
    /**
     * Envoie une notification ciblée à un utilisateur spécifique.
     */
    public ResponseEntity<SystemNotificationDTO> createNotification(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @RequestBody CreateNotificationRequest request) {
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
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Créer une notification globale", description = "Crée une notification pour tous les utilisateurs (admin uniquement)")
    /**
     * Diffuse un message à l'ensemble des testeurs et administrateurs de la plateforme.
     */
    public ResponseEntity<SystemNotificationDTO> createGlobalNotification(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @RequestBody CreateGlobalNotificationRequest request) {
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
    /**
     * Action individuelle lorsqu'un utilisateur clique sur une notification précise.
     */
    public ResponseEntity<SystemNotificationDTO> markAsRead(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        SystemNotificationDTO notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }
    
    @PatchMapping("/read-all")
    @Operation(summary = "Tout marquer comme lu", description = "Marque toutes les notifications de l'utilisateur comme lues")
    /**
     * Permet de vider rapidement le centre de notifications.
     */
    public ResponseEntity<Void> markAllAsRead(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        notificationService.markAllAsReadForUser(currentUser.getId());
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Supprimer une notification", description = "Supprime une notification (admin uniquement)")
    /**
     * Suppression physique d'une notification obsolète de la base de données.
     */
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
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
