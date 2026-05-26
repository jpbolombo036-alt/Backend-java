package com.itaccess.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWithTodosDTO {
    
    private Long id;
    
    private String username;
    
    private String email;
    
    private String role;
    
    private Boolean isActive;
    
    private String profilePhoto;
    
    private String createdAt;
    
    private List<TodoDTO> todos;
}
