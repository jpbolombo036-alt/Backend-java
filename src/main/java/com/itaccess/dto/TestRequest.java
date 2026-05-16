package com.itaccess.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestRequest {
    
    @JsonProperty("sessionId")
    private Long sessionId;
    
    @JsonProperty("applicationId")
    private Long applicationId;
    
    @JsonProperty("applicationNom")
    private String applicationNom;
    
    private String version;
    private String environnement;
    
    @NotBlank(message = "La fonction est requise")
    private String fonction;
    
    private String precondition;
    private String etapes;
    
    @JsonProperty("resultatAttendu")
    private String resultatAttendu;
    
    @JsonProperty("resultatObtenu")
    private String resultatObtenu;
    
    @NotBlank(message = "Le statut est requis")
    private String statut;
    
    private String commentaires;
    
    @JsonProperty("image")
    private String image;
}