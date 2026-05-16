package com.itaccess.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompteRequest {
    
    @NotNull(message = "L'ID de l'application est requis")
    private Long applicationId;
    
    @NotBlank(message = "Le nom d'utilisateur est requis")
    private String username;
    
    private String code;
    private String role;
    private String commentaire;
}