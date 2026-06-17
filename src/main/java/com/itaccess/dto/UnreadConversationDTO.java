package com.itaccess.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnreadConversationDTO {
    private Long userId;
    private String username;
    private Long unreadCount;
    private MessageDTO lastMessage;
}
