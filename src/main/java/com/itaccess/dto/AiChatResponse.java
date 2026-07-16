package com.itaccess.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatResponse {

    private String reply;
    private int tokensUsed;
    private String model;
    private boolean error;
    private String errorMessage;
    private Long conversationId;
}
