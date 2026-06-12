package com.itaccess.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportGenerationDTO {

    private Long id;
    private String reportType;
    private String title;
    private String type;
    private String status;
    private LocalDateTime generatedAt;
    private Long generatedBy;
    private String generatedByUsername;
}
