package com.itaccess.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlocNoteDTO {

    private Long id;
    private String title;
    private String content;
    private Long applicationId;
    private Long sessionId;
    private Long testId;
    private String status;
    private Long createdBy;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
