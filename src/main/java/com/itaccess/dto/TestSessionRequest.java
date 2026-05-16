package com.itaccess.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSessionRequest {
    
    @NotBlank(message = "Le nom est requis")
    private String nom;
    
    private String description;
    private Long applicationId;
    private String environnement;
    private String version;
    
    @JsonProperty("nom_document")
    private String nomDocument;
    
    private String statut;
}