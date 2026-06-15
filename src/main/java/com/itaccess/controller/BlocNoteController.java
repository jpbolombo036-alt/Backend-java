package com.itaccess.controller;

import com.itaccess.dto.BlocNoteDTO;
import com.itaccess.dto.BlocNoteRequest;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.BlocNoteService;
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
@RequestMapping("/bloc-notes")
@RequiredArgsConstructor
@Tag(name = "Bloc Notes", description = "Gestion des notes de qualification QA")
public class BlocNoteController {

    private final BlocNoteService blocNoteService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Liste des notes", description = "Retourne les notes de l'utilisateur ou toutes les notes pour un administrateur")
    public ResponseEntity<List<BlocNoteDTO>> getNotes(@Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(blocNoteService.getAll(currentUser));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Note par ID", description = "Retourne une note spécifique")
    public ResponseEntity<BlocNoteDTO> getNote(@PathVariable Long id) {
        return ResponseEntity.ok(blocNoteService.get(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Créer une note", description = "Crée une nouvelle note QA")
    public ResponseEntity<BlocNoteDTO> createNote(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody BlocNoteRequest request) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(blocNoteService.create(request, currentUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Modifier une note", description = "Met à jour une note QA existante")
    public ResponseEntity<BlocNoteDTO> updateNote(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody BlocNoteRequest request) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(blocNoteService.update(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Supprimer une note", description = "Supprime une note QA")
    public ResponseEntity<Void> deleteNote(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        blocNoteService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
