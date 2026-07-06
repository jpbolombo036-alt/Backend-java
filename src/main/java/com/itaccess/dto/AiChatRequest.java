package com.itaccess.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatRequest {

    private String conversationId;

    private List<AiMessage> messages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiMessage {
        private String role;    // "user", "assistant", "system"
        private String content;
    }
}
