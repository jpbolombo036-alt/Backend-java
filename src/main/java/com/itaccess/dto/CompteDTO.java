package com.itaccess.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompteDTO {
    
    private Long id;
    private Long applicationId;
    private String username;
    private String code;
    private String role;
    private String commentaire;
    private Long createdBy;
    private ApplicationInfoDTO application;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApplicationInfoDTO {
        private Long id;
        private String nom;
    }
}