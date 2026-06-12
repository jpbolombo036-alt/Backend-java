package com.itaccess.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDefinitionDTO {

    private String id;
    private String title;
    private String description;
    private String lastGenerated;
}
