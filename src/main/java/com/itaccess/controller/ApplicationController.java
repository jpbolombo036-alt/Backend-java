package com.itaccess.controller;

import com.itaccess.dto.ApplicationDTO;
import com.itaccess.dto.PageResponse;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "")
public class ApplicationController {
    
    private final ApplicationService applicationService;
    
    @GetMapping
    @Operation(summary = "Liste des applications", description = "Retourne toutes les applications avec pagination")
    public ResponseEntity<PageResponse<ApplicationDTO>> getAllApplications(
            @Parameter(description = "Numéro de page (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(applicationService.getAllApplications(page, size, sortBy, sortDir));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Application par ID", description = "Retourne une application par son ID")
    public ResponseEntity<ApplicationDTO> getApplicationById(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getApplicationById(id));
    }
    
    @PostMapping
    @Operation(summary = "Créer une application", description = "Crée une nouvelle application")
    public ResponseEntity<ApplicationDTO> createApplication(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody ApplicationDTO dto) {
        ApplicationDTO created = applicationService.createApplication(dto, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Modifier une application", description = "Modifie une application existante")
    public ResponseEntity<ApplicationDTO> updateApplication(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody ApplicationDTO dto) {
        return ResponseEntity.ok(applicationService.updateApplication(id, dto, currentUser.getId(), currentUser.getRole()));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une application", description = "Supprime une application")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        applicationService.deleteApplication(id, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.noContent().build();
    }
}