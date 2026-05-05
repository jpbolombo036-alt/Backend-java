package com.itaccess.controller;

import com.itaccess.dto.MessageDTO;
import com.itaccess.dto.MessageRequest;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "API de messagerie entre utilisateurs")
public class MessageController {
    
    private final MessageService messageService;
    
    @GetMapping
    @Operation(summary = "Liste des messages", description = "Retourne tous les messages (admin voit tous les messages)")
    public ResponseEntity<List<MessageDTO>> getAllMessages(@Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        if ("admin".equals(currentUser.getRole())) {
            return ResponseEntity.ok(messageService.getAll());
        }
        return ResponseEntity.ok(messageService.getUnreadMessages(currentUser.getId()));
    }
    
    @GetMapping("/conversation/{userId}")
    @Operation(summary = "Conversation avec un utilisateur", description = "Retourne tous les messages entre l'utilisateur actuel et l'utilisateur spécifié")
    public ResponseEntity<List<MessageDTO>> getConversation(
            @PathVariable Long userId,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(messageService.getConversation(currentUser.getId(), userId));
    }
    
    @PostMapping
    @Operation(summary = "Envoyer un message", description = "Envoie un nouveau message à un utilisateur")
    public ResponseEntity<MessageDTO> createMessage(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody MessageRequest request) {
        MessageDTO created = messageService.create(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PatchMapping("/{id}/read")
    @Operation(summary = "Marquer comme lu", description = "Marque un message comme lu")
    public ResponseEntity<MessageDTO> markAsRead(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(messageService.markAsRead(id));
    }
    
    @GetMapping("/unread-count")
    @Operation(summary = "Nombre de messages non lus", description = "Retourne le nombre de messages non lus pour l'utilisateur actuel")
    public ResponseEntity<Long> getUnreadCount(@Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(messageService.getUnreadCount(currentUser.getId()));
    }
    
    @GetMapping("/unread-by-user")
    @Operation(summary = "Messages non lus par utilisateur", description = "Retourne le nombre de messages non lus groupés par expéditeur")
    public ResponseEntity<java.util.Map<Long, Long>> getUnreadByUser(@Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(messageService.getUnreadByUser(currentUser.getId()));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un message", description = "Supprime un message (admin uniquement)")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        if (!"admin".equals(currentUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        messageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
