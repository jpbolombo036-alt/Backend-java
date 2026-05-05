package com.itaccess.service;

import com.itaccess.dto.TodoDTO;
import com.itaccess.dto.TodoRequest;
import com.itaccess.entity.Todo;
import com.itaccess.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TodoService {
    
    private final TodoRepository todoRepository;
    
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
                .orElseThrow(() -> new RuntimeException("Todo not found"));
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
    public TodoDTO update(Long id, TodoRequest request) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo not found"));
        
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
    public void delete(Long id) {
        todoRepository.deleteById(id);
    }
    
    @Transactional
    public TodoDTO toggleComplete(Long id) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo not found"));
        
        todo.setCompleted(!todo.getCompleted());
        return toDTO(todoRepository.save(todo));
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
