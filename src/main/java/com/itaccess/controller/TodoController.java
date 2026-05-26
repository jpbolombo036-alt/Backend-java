package com.itaccess.controller;

import com.itaccess.dto.TodoDTO;
import com.itaccess.dto.TodoRequest;
import com.itaccess.dto.UserWithTodosDTO;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.TodoService;
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
@RequestMapping("/todos")
@RequiredArgsConstructor
@Tag(name = "Todos", description = "")
public class TodoController {
    
    private final TodoService todoService;
    
    @GetMapping
    @Operation(summary = "Liste des tâches", description = "Retourne toutes les tâches (admin voit toutes les tâches)")
    /**
     * Filtre automatiquement les tâches : l'utilisateur ne voit que les siennes, l'admin voit tout.
     */
    public ResponseEntity<List<TodoDTO>> getAllTodos(@Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        if ("admin".equals(currentUser.getRole())) {
            return ResponseEntity.ok(todoService.getAll());
        }
        return ResponseEntity.ok(todoService.getAllByUser(currentUser.getId()));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Tâche par ID", description = "Retourne une tâche par son ID (authentification requise)")
    /**
     * Récupère le détail d'une tâche spécifique.
     */
    public ResponseEntity<TodoDTO> getTodoById(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(todoService.getById(id));
    }
    
    @PostMapping
    @Operation(summary = "Créer une tâche", description = "Crée une nouvelle tâche (authentification requise)")
    /**
     * Permet à tout utilisateur authentifié de créer une tâche personnelle.
     */
    public ResponseEntity<TodoDTO> createTodo(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody TodoRequest request) {
        TodoDTO created = todoService.create(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Modifier une tâche", description = "Modifie une tâche existante (authentification requise)")
    /**
     * Mise à jour sécurisée : vérifie que l'utilisateur est le propriétaire ou admin.
     */
    public ResponseEntity<TodoDTO> updateTodo(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody TodoRequest request) {
        return ResponseEntity.ok(todoService.update(id, request, currentUser.getId(), currentUser.getRole()));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une tâche", description = "Supprime une tâche (authentification requise)")
    public ResponseEntity<Void> deleteTodo(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        todoService.delete(id, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Basculer l'état", description = "Marque une tâche comme terminée/non terminée (authentification requise)")
    public ResponseEntity<TodoDTO> toggleTodo(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(todoService.toggleComplete(id, currentUser.getId(), currentUser.getRole()));
    }
    
    @GetMapping("/users")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Utilisateurs avec leurs tâches", description = "Retourne tous les utilisateurs qui ont des tâches avec leurs tâches respectives (admin uniquement)")
    public ResponseEntity<List<UserWithTodosDTO>> getUsersWithTodos(@Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(todoService.getUsersWithTodos());
    }
}
