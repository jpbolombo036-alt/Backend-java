package com.itaccess.dto;

import jakarta.validation.constraints.*;
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
    
    private String fonction;
    
    private String precondition;
    private String etapes;
    
    @JsonProperty("resultatAttendu")
    private String resultatAttendu;
    
    @JsonProperty("resultatObtenu")
    private String resultatObtenu;
    
    private String statut;
    
    private String commentaires;
    
    @JsonProperty("image")
    private String image;

    private Boolean resolved;
}