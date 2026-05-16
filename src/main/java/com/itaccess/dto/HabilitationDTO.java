package com.itaccess.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabilitationDTO {
    
    private Long id;
    private Long compteId;
    private String permission;
}