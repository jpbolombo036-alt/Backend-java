package com.itaccess.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestDTO {
    
    private Long id;
    private Long sessionId;
    private Long applicationId;
    private String applicationNom;
    private String version;
    private String environnement;
    private String fonction;
    private String precondition;
    private String etapes;
    private String resultatAttendu;
    private String resultatObtenu;
    private String statut;
    private String commentaires;
    private Long createdBy;
}