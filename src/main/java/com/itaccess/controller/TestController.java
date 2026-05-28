package com.itaccess.controller;

import com.itaccess.dto.TestDTO;
import com.itaccess.dto.TestRequest;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.TestService;
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
@RequestMapping("/tests")
@RequiredArgsConstructor
@Tag(name = "Tests", description = "")
public class TestController {
    
    private final TestService testService;
    
    @GetMapping
    @Operation(summary = "Liste des tests", description = "Retourne tous les tests")
    /**
     * Liste les étapes d'exécution. Peut être filtré par sessionId pour le dashboard.
     */
    public ResponseEntity<List<TestDTO>> getAllTests(
            @RequestParam(required = false) Long sessionId) {
        if (sessionId != null) {
            return ResponseEntity.ok(testService.getTestsBySessionId(sessionId));
        }
        return ResponseEntity.ok(testService.getAllTests());
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Test par ID", description = "Retourne un test par son ID")
    /**
     * Récupère une étape précise avec son statut et ses commentaires.
     */
    public ResponseEntity<TestDTO> getTestById(@PathVariable Long id) {
        return ResponseEntity.ok(testService.getTestById(id));
    }
    
    @PostMapping
    @Operation(summary = "Créer un test", description = "Crée un nouveau test (authentification requise)")
    /**
     * Enregistre le résultat d'un pas de test manuel.
     */
    public ResponseEntity<TestDTO> createTest(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody TestRequest request) {
<<<<<<< HEAD
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
=======
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
        TestDTO created = testService.createTest(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Modifier un test", description = "Modifie un test existant (authentification requise)")
    public ResponseEntity<TestDTO> updateTest(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody TestRequest request) {
<<<<<<< HEAD
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
=======
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
        return ResponseEntity.ok(testService.updateTest(id, request, currentUser.getId(), currentUser.getRole()));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un test", description = "Supprime un test (authentification requise)")
    public ResponseEntity<Void> deleteTest(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
<<<<<<< HEAD
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
=======
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
        testService.deleteTest(id, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.noContent().build();
    }
}