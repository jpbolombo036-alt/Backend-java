package com.itaccess.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRequest {
    
    private Long receiverId;
    private String content;
    private MultipartFile attachment;
}
