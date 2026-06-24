package com.itaccess.controller;

import com.itaccess.dto.ApplicationLinkDTO;
import com.itaccess.dto.PageResponse;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.ApplicationLinkService;
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
@RequestMapping("/application-links")
@RequiredArgsConstructor
@Tag(name = "ApplicationLinks", description = "Gestion des liens web des applications")
public class ApplicationLinkController {

    private final ApplicationLinkService applicationLinkService;

    @GetMapping
    @Operation(summary = "Liste des liens d'application", description = "Retourne tous les liens d'application avec pagination")
    public ResponseEntity<PageResponse<ApplicationLinkDTO>> getAllApplicationLinks(
            @Parameter(description = "Numéro de page (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "25") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(applicationLinkService.getAllApplicationLinks(page, size, sortBy, sortDir));
    }

    @GetMapping("/applications/{applicationId}")
    @Operation(summary = "Liens d'une application", description = "Retourne tous les liens d'une application")
    public ResponseEntity<List<ApplicationLinkDTO>> getApplicationLinksByApplicationId(@PathVariable Long applicationId) {
        return ResponseEntity.ok(applicationLinkService.getApplicationLinksByApplicationId(applicationId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lien d'application par ID", description = "Retourne un lien d'application par son ID")
    public ResponseEntity<ApplicationLinkDTO> getApplicationLinkById(@PathVariable Long id) {
        return ResponseEntity.ok(applicationLinkService.getApplicationLinkById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Créer un lien d'application", description = "Crée un nouveau lien web associé à une application")
    public ResponseEntity<ApplicationLinkDTO> createApplicationLink(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody ApplicationLinkDTO dto) {
        ApplicationLinkDTO created = applicationLinkService.createApplicationLink(dto, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Modifier un lien d'application", description = "Modifie un lien d'application existant")
    public ResponseEntity<ApplicationLinkDTO> updateApplicationLink(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody ApplicationLinkDTO dto) {
        return ResponseEntity.ok(applicationLinkService.updateApplicationLink(id, dto, currentUser.getId(), currentUser.getRole()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Supprimer un lien d'application", description = "Supprime un lien d'application")
    public ResponseEntity<Void> deleteApplicationLink(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        applicationLinkService.deleteApplicationLink(id, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.noContent().build();
    }
}
