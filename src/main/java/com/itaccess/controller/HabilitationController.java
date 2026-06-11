package com.itaccess.controller;

import com.itaccess.dto.HabilitationDTO;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.HabilitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/habilitations")
@RequiredArgsConstructor
@Tag(name = "Habilitations", description = "Endpoints pour la gestion des habilitations")
public class HabilitationController {
    
    private final HabilitationService habilitationService;
    
    @GetMapping
    @Operation(summary = "Liste des habilitations", description = "Retourne toutes les habilitations")
    public ResponseEntity<List<HabilitationDTO>> getAllHabilitations() {
        return ResponseEntity.ok(habilitationService.getAllHabilitations());
    }
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Créer une habilitation", description = "Crée une nouvelle habilitation (authentification requise)")
    public ResponseEntity<HabilitationDTO> createHabilitation(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @RequestBody HabilitationDTO dto) {
        HabilitationDTO created = habilitationService.createHabilitation(dto, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Supprimer une habilitation", description = "Supprime une habilitation (authentification requise)")
    public ResponseEntity<Void> deleteHabilitation(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        habilitationService.deleteHabilitation(id, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.noContent().build();
    }
}