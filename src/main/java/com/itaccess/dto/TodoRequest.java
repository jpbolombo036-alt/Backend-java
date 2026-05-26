package com.itaccess.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoRequest {
    
    @NotBlank(message = "Le titre est requis")
    private String title;
    private String description;
    private Boolean completed;
    private String priority;
    private String dueDate;
}
