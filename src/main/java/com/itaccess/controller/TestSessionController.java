package com.itaccess.controller;

import com.itaccess.dto.TestSessionDTO;
import com.itaccess.dto.TestSessionRequest;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.TestSessionService;
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
    public ResponseEntity<List<TestSessionDTO>> getAllTestSessions(@Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        if ("admin".equals(currentUser.getRole())) {
            return ResponseEntity.ok(testSessionService.getAllTestSessions());
        }
        return ResponseEntity.ok(testSessionService.getTestSessionsByUser(currentUser.getId()));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Session par ID", description = "Retourne une session de test par son ID")
    public ResponseEntity<TestSessionDTO> getTestSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(testSessionService.getTestSessionById(id));
    }
    
    @PostMapping
    @Operation(summary = "Créer une session", description = "Crée une nouvelle session de test")
    public ResponseEntity<TestSessionDTO> createTestSession(
            @Valid @RequestBody TestSessionRequest request,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        TestSessionDTO created = testSessionService.createTestSession(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Modifier une session", description = "Modifie une session de test existante")
    public ResponseEntity<TestSessionDTO> updateTestSession(
            @PathVariable Long id,
            @Valid @RequestBody TestSessionRequest request) {
        return ResponseEntity.ok(testSessionService.updateTestSession(id, request));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une session", description = "Supprime une session de test")
    public ResponseEntity<Void> deleteTestSession(@PathVariable Long id) {
        testSessionService.deleteTestSession(id);
        return ResponseEntity.noContent().build();
    }
}