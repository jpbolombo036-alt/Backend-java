package com.itaccess.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentArchiveRequest {

    @NotBlank(message = "Le titre du document est requis")
    private String title;

    private String description;
    private String category;
    private String tags;
    private String author;
}
