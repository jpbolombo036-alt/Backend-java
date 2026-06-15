package com.itaccess.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationLinkDTO {

    private Long id;

    @NotNull(message = "L'identifiant de l'application est requis")
    private Long applicationId;

    @NotBlank(message = "Le nom du lien est requis")
    @Size(max = 100, message = "Le nom du lien doit contenir au maximum 100 caractères")
    private String nom;

    @NotBlank(message = "L'URL du lien est requise")
    @Size(max = 500, message = "L'URL doit contenir au maximum 500 caractères")
    private String url;

    @Size(max = 100, message = "Le type doit contenir au maximum 100 caractères")
    private String type;

    private String description;
    private String dateCreation;
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
