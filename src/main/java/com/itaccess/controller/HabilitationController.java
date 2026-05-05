package com.itaccess.controller;

import com.itaccess.dto.HabilitationDTO;
import com.itaccess.service.HabilitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @Operation(summary = "Créer une habilitation", description = "Crée une nouvelle habilitation (authentification requise)")
    public ResponseEntity<HabilitationDTO> createHabilitation(@RequestBody HabilitationDTO dto) {
        HabilitationDTO created = habilitationService.createHabilitation(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une habilitation", description = "Supprime une habilitation (authentification requise)")
    public ResponseEntity<Void> deleteHabilitation(@PathVariable Long id) {
        habilitationService.deleteHabilitation(id);
        return ResponseEntity.noContent().build();
    }
}