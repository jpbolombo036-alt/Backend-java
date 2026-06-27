package com.itaccess.controller;

import com.itaccess.dto.TestSessionDTO;
import com.itaccess.dto.TestSessionRequest;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.TestSessionService;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/test-sessions")
@RequiredArgsConstructor
@Tag(name = "Test Sessions", description = "")
public class TestSessionController {
    
    private final TestSessionService testSessionService;
    
    @GetMapping
    @Operation(summary = "Liste des sessions", description = "Retourne toutes les sessions de test (admin voit toutes, utilisateur voit les siennes)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TestSessionDTO>> getAllTestSessions(@Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        if ("admin".equals(currentUser.getRole())) {
            return ResponseEntity.ok(testSessionService.getAllTestSessions());
        }
        return ResponseEntity.ok(testSessionService.getTestSessionsByUser(currentUser.getId()));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Session par ID", description = "Retourne une session de test par son ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TestSessionDTO> getTestSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(testSessionService.getTestSessionById(id));
    }
    
    @PostMapping
    @Operation(summary = "Créer une session", description = "Crée une nouvelle session de test")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TestSessionDTO> createTestSession(
            @Valid @RequestBody TestSessionRequest request,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        TestSessionDTO created = testSessionService.createTestSession(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Modifier une session", description = "Modifie une session de test existante")
    public ResponseEntity<TestSessionDTO> updateTestSession(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody TestSessionRequest request) {        
        return ResponseEntity.ok(testSessionService.updateTestSession(id, request, currentUser.getId(), currentUser.getRole()));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Supprimer une session", description = "Supprime une session de test")
    public ResponseEntity<Void> deleteTestSession(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        testSessionService.deleteTestSession(id, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}/export")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Exporter la session", description = "Retourne les données de la session pour export PDF/Word")
    public ResponseEntity<TestSessionDTO> exportTestSession(@PathVariable Long id) {
        return ResponseEntity.ok(testSessionService.getTestSessionById(id));
    }
    
    @PostMapping("/{id}/request-close")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Demander cloture session", description = "Ferme une session de test et notifie tous les admins")
    public ResponseEntity<TestSessionDTO> requestCloseSession(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(testSessionService.requestCloseSession(id, currentUser.getId()));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Réouvrir une session", description = "Réouvre une session de test cloturée (admin uniquement)")
    public ResponseEntity<TestSessionDTO> reopenSession(
            @PathVariable Long id) {
        return ResponseEntity.ok(testSessionService.reopenSession(id));
    }
}