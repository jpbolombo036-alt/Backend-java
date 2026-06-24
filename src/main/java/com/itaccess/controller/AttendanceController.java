package com.itaccess.controller;

import com.itaccess.dto.AttendanceDTO;
import com.itaccess.dto.PageResponse;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/attendances")
@RequiredArgsConstructor
@Tag(name = "Attendances", description = "Gestion des présences et pointages des agents")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    @Operation(summary = "Liste des présences", description = "Retourne toutes les présences avec pagination")
    public ResponseEntity<PageResponse<AttendanceDTO>> getAllAttendances(
            @Parameter(description = "Numéro de page (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "25") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "date") String sortBy,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(attendanceService.getAllAttendances(page, size, sortBy, sortDir));
    }

    @GetMapping("/agent/{agentId}")
    @Operation(summary = "Présences d'un agent", description = "Retourne toutes les présences d'un agent avec pagination")
    public ResponseEntity<PageResponse<AttendanceDTO>> getAttendancesByAgent(
            @PathVariable Long agentId,
            @Parameter(description = "Numéro de page (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "25") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "date") String sortBy,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(attendanceService.getAttendancesByAgent(agentId, page, size, sortBy, sortDir));
    }

    @GetMapping("/date/{date}")
    @Operation(summary = "Présences par date", description = "Retourne toutes les présences d'une date donnée avec pagination")
    public ResponseEntity<PageResponse<AttendanceDTO>> getAttendancesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Numéro de page (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "25") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "agentUsername") String sortBy,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(attendanceService.getAttendancesByDate(date, page, size, sortBy, sortDir));
    }

    @GetMapping("/agent/{agentId}/range")
    @Operation(summary = "Présences par période", description = "Retourne les présences d'un agent entre deux dates")
    public ResponseEntity<List<AttendanceDTO>> getAttendancesByAgentAndDateRange(
            @PathVariable Long agentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(attendanceService.getAttendancesByAgentAndDateRange(agentId, start, end));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Présence par ID", description = "Retourne une présence par son ID")
    public ResponseEntity<AttendanceDTO> getAttendanceById(@PathVariable Long id) {
        return ResponseEntity.ok(attendanceService.getAttendanceById(id));
    }

    @PostMapping("/check-in")
    @Operation(summary = "Pointer l'arrivée", description = "Enregistre l'heure d'arrivée de l'agent connecté")
    public ResponseEntity<AttendanceDTO> checkIn(@Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(attendanceService.checkIn(currentUser.getId(), currentUser.getUsername()));
    }

    @PostMapping("/check-out")
    @Operation(summary = "Pointer le départ", description = "Enregistre l'heure de départ de l'agent connecté")
    public ResponseEntity<AttendanceDTO> checkOut(@Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(attendanceService.checkOut(currentUser.getId()));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Créer une présence", description = "Crée un enregistrement de présence manuel")
    public ResponseEntity<AttendanceDTO> createAttendance(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody AttendanceDTO dto) {
        AttendanceDTO created = attendanceService.createAttendance(dto, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Modifier une présence", description = "Met à jour une présence existante")
    public ResponseEntity<AttendanceDTO> updateAttendance(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody AttendanceDTO dto) {
        return ResponseEntity.ok(attendanceService.updateAttendance(id, dto, currentUser.getId(), currentUser.getRole()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Supprimer une présence", description = "Supprime une présence")
    public ResponseEntity<Void> deleteAttendance(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        attendanceService.deleteAttendance(id, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.noContent().build();
    }
}
