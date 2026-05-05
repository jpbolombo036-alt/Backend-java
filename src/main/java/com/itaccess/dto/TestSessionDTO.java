package com.itaccess.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSessionDTO {
    
    private Long id;
    private String nom;
    private String description;
    private Long applicationId;
    private String applicationNom;
    private String environnement;
    private String version;
    private String nomDocument;
    private LocalDateTime dateCreation;
    private String statut;
    private Long createdBy;
    private String createdByUsername;
    private List<TestDTO> tests;
    private Integer totalTests;
    private Integer testsOk;
    private Integer testsBug;
    private Integer testsEnCours;
}