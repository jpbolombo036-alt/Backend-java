package com.itaccess.service;

// import com.itaccess.entity.AuditLog; // Entité à créer si vous voulez persister l'audit
// import com.itaccess.repository.AuditLogRepository; // Repository à créer
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service pour enregistrer les actions importantes des utilisateurs à des fins d'audit.
 * Les actions sont loggées et peuvent être persistées en base de données.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    // Décommenter et créer AuditLogRepository si vous souhaitez persister les logs d'audit
    // private final AuditLogRepository auditLogRepository;

    /**
     * Enregistre une action utilisateur dans les logs et potentiellement en base de données.
     * @param action Description de l'action (ex: "DELETE_APK", "UPDATE_USER_ROLE").
     * @param details Détails supplémentaires sur l'action (ex: "Fichier: mon_apk.apk").
     * @param userId ID de l'utilisateur qui a effectué l'action.
     */
    public void logAction(String action, String details, Long userId) {
        String message = String.format("AUDIT - User[%d] Action[%s]: %s", userId, action, details);
        log.info(message);
        
        // Exemple de persistance (décommenter après avoir créé l'entité AuditLog et son repository)
        // AuditLog logEntry = AuditLog.builder()
        //     .action(action)
        //     .details(details)
        //     .userId(userId)
        //     .timestamp(LocalDateTime.now())
        //     .build();
        // auditLogRepository.save(logEntry);
    }
}