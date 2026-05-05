package com.itaccess.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoDTO {
    
    private Long id;
    private String title;
    private String description;
    private Boolean completed;
    private String priority;
    private String dueDate;
    private LocalDateTime createdAt;
    private Long createdBy;
}
