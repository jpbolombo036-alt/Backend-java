package com.itaccess.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlocNoteRequest {

    private String title;
    private String content;
    private Long applicationId;
    private Long sessionId;
    private Long testId;
    private String status;
}
