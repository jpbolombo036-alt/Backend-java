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
public class ApkFileDTO {
    
    private Long id;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String version;
    private String packageName;
    private String description;
    private Long applicationId;
    private Long uploadedBy;
    private LocalDateTime uploadDate;
    private Integer downloadCount;
}
