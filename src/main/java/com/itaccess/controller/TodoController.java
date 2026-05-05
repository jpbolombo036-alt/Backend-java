package com.itaccess.controller;

import com.itaccess.dto.TodoDTO;
import com.itaccess.dto.TodoRequest;
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
    public ResponseEntity<List<TodoDTO>> getAllTodos(@Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        if ("admin".equals(currentUser.getRole())) {
            return ResponseEntity.ok(todoService.getAll());
        }
        return ResponseEntity.ok(todoService.getAllByUser(currentUser.getId()));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Tâche par ID", description = "Retourne une tâche par son ID (authentification requise)")
    public ResponseEntity<TodoDTO> getTodoById(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(todoService.getById(id));
    }
    
    @PostMapping
    @Operation(summary = "Créer une tâche", description = "Crée une nouvelle tâche (authentification requise)")
    public ResponseEntity<TodoDTO> createTodo(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody TodoRequest request) {
        TodoDTO created = todoService.create(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Modifier une tâche", description = "Modifie une tâche existante (authentification requise)")
    public ResponseEntity<TodoDTO> updateTodo(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @Valid @RequestBody TodoRequest request) {
        return ResponseEntity.ok(todoService.update(id, request));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une tâche", description = "Supprime une tâche (authentification requise)")
    public ResponseEntity<Void> deleteTodo(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        todoService.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Basculer l'état", description = "Marque une tâche comme terminée/non terminée (authentification requise)")
    public ResponseEntity<TodoDTO> toggleTodo(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(todoService.toggleComplete(id));
    }
}
