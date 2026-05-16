package com.itaccess.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentUserResponse {
    
    private Long id;
    private String username;
    private String email;
    private String role;
    private Boolean isActive;
}