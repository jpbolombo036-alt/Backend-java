package com.itaccess.controller;

import com.itaccess.entity.Bug;
import com.itaccess.repository.BugRepository;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bugs")
@RequiredArgsConstructor
@Tag(name = "Bugs", description = "Gestion des anomalies détectées lors des tests")
public class BugController {

    private final BugRepository bugRepository;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Déclarer un bug", description = "Crée un nouveau rapport d'anomalie lié à une étape de test")
    /**
     * Route critique : lie un bug à une étape de test spécifique (TestStep).
     */
    public ResponseEntity<Bug> createBug(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @RequestBody Bug bug) {
        // Enregistre qui a trouvé le bug
        if (bug.getAssignedTo() == null) {
            bug.setAssignedTo(currentUser.getId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(bugRepository.save(bug));
    }

    @GetMapping("/step/{testStepId}")
    @Operation(summary = "Bugs par étape", description = "Récupère tous les bugs liés à une étape de test spécifique")
    /**
     * Permet au développeur de voir tous les tickets ouverts pour un test en échec.
     */
    public ResponseEntity<List<Bug>> getBugsByStep(@PathVariable Long testStepId) {
        return ResponseEntity.ok(bugRepository.findByTestStepId(testStepId));
    }

    @GetMapping
    @Operation(summary = "Liste des bugs", description = "Retourne tous les bugs du système")
    /**
     * Vue globale pour le QA Manager afin de suivre l'état de santé des applications.
     */
    public ResponseEntity<List<Bug>> getAllBugs() {
        return ResponseEntity.ok(bugRepository.findAll());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Changer le statut", description = "Met à jour le statut du bug (ex: FIXED, CLOSED)")
    /**
     * Permet de faire avancer le workflow de résolution du bug.
     */
    public ResponseEntity<Bug> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Bug bug = bugRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bug non trouvé avec l'ID: " + id));
        bug.setStatus(status);
        return ResponseEntity.ok(bugRepository.save(bug));
    }
}