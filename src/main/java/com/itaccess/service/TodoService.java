package com.itaccess.service;

import com.itaccess.dto.TodoDTO;
import com.itaccess.dto.TodoRequest;
import com.itaccess.dto.UserWithTodosDTO;
import com.itaccess.entity.Todo;
import com.itaccess.entity.User;
<<<<<<< HEAD
import com.itaccess.exception.ResourceNotFoundException;
=======
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
import com.itaccess.repository.TodoRepository;
import com.itaccess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

<<<<<<< HEAD
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
=======
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TodoService {
    
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
<<<<<<< HEAD

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
=======
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
    
    public List<TodoDTO> getAll() {
        return todoRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<TodoDTO> getAllByUser(Long userId) {
        return todoRepository.findByCreatedByOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public TodoDTO getById(Long id) {
        Todo todo = todoRepository.findById(id)
<<<<<<< HEAD
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'ID: " + id));
=======
                .orElseThrow(() -> new RuntimeException("Todo not found"));
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
        return toDTO(todo);
    }
    
    @Transactional
    public TodoDTO create(TodoRequest request, Long userId) {
        Todo todo = Todo.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .completed(request.getCompleted() != null ? request.getCompleted() : false)
                .priority(request.getPriority() != null ? request.getPriority() : "normal")
                .dueDate(request.getDueDate())
                .createdBy(userId)
                .build();
        
        return toDTO(todoRepository.save(todo));
    }
    
    @Transactional
    public TodoDTO update(Long id, TodoRequest request, Long userId, String userRole) {
        Todo todo = todoRepository.findById(id)
<<<<<<< HEAD
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'ID: " + id));
=======
                .orElseThrow(() -> new RuntimeException("Todo not found"));
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
        
        if (!"admin".equals(userRole) && !todo.getCreatedBy().equals(userId)) {
            throw new SecurityException("Non autorisé à modifier cette tâche");
        }

        if (request.getTitle() != null) {
            todo.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            todo.setDescription(request.getDescription());
        }
        if (request.getCompleted() != null) {
            todo.setCompleted(request.getCompleted());
        }
        if (request.getPriority() != null) {
            todo.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            todo.setDueDate(request.getDueDate());
        }
        
        return toDTO(todoRepository.save(todo));
    }
    
    @Transactional
    public void delete(Long id, Long userId, String userRole) {
        Todo todo = todoRepository.findById(id)
<<<<<<< HEAD
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'ID: " + id));
=======
                .orElseThrow(() -> new RuntimeException("Todo not found"));
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0

        if (!"admin".equals(userRole) && !todo.getCreatedBy().equals(userId)) {
            throw new SecurityException("Non autorisé à supprimer cette tâche");
        }
        todoRepository.deleteById(id);
    }
    
    @Transactional
    public TodoDTO toggleComplete(Long id, Long userId, String userRole) {
        Todo todo = todoRepository.findById(id)
<<<<<<< HEAD
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'ID: " + id));
=======
                .orElseThrow(() -> new RuntimeException("Todo not found"));
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
        
        if (!"admin".equals(userRole) && !todo.getCreatedBy().equals(userId)) {
            throw new SecurityException("Non autorisé à modifier cette tâche");
        }

        todo.setCompleted(!todo.getCompleted());
        return toDTO(todoRepository.save(todo));
    }
    
    public List<UserWithTodosDTO> getUsersWithTodos() {
        List<Long> userIds = todoRepository.findDistinctCreatedBy();
<<<<<<< HEAD
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<User> users = userRepository.findByIdIn(userIds);
        
        // Optimize: Fetch all todos in one query and group them by user ID in memory
        List<Todo> allTodos = todoRepository.findByCreatedByInOrderByCreatedAtDesc(userIds);
        Map<Long, List<TodoDTO>> todosByUserId = allTodos.stream()
                .map(this::toDTO)
                .collect(Collectors.groupingBy(TodoDTO::getCreatedBy));

        return users.stream()
                .map(user -> UserWithTodosDTO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .isActive(user.getIsActive())
                        .profilePhoto(user.getProfilePhoto())
                        .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                        .todos(todosByUserId.getOrDefault(user.getId(), Collections.emptyList()))
                        .build())
=======
        List<User> users = userRepository.findByIdIn(userIds);
        
        return users.stream()
                .map(user -> {
                    List<TodoDTO> userTodos = todoRepository.findByCreatedByOrderByCreatedAtDesc(user.getId())
                            .stream()
                            .map(this::toDTO)
                            .collect(Collectors.toList());
                    
                    return UserWithTodosDTO.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .isActive(user.getIsActive())
                            .profilePhoto(user.getProfilePhoto())
                            .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                            .todos(userTodos)
                            .build();
                })
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
                .collect(Collectors.toList());
    }
    
    private TodoDTO toDTO(Todo todo) {
        return TodoDTO.builder()
                .id(todo.getId())
                .title(todo.getTitle())
                .description(todo.getDescription())
                .completed(todo.getCompleted())
                .priority(todo.getPriority())
                .dueDate(todo.getDueDate())
                .createdAt(todo.getCreatedAt())
                .createdBy(todo.getCreatedBy())
                .build();
    }
}
