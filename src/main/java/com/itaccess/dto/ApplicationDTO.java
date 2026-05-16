package com.itaccess.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDTO {
    
    private Long id;
    
    @NotBlank(message = "Le nom est requis")
    private String nom;
    
    private String description;
    private String version;
    private String environnement;
    private String dateCreation;
    private Long createdBy;
}