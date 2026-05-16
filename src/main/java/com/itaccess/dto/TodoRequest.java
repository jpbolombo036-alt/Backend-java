package com.itaccess.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoRequest {
    
    private String title;
    private String description;
    private Boolean completed;
    private String priority;
    private String dueDate;
}
