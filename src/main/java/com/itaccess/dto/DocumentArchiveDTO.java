package com.itaccess.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentArchiveDTO {

    private Long id;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String contentType;
    private String title;
    private String description;
    private String category;
    private String tags;
    private String author;
    private Long uploadedBy;
    private String uploadedByUsername;
    private LocalDateTime uploadDate;
    private Integer downloadCount;
}
