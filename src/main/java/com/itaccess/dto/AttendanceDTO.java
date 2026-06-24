package com.itaccess.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDTO {

    private Long id;

    private Long agentId;

    private String agentUsername;

    private LocalDate date;

    private LocalTime checkInTime;

    private LocalTime checkOutTime;

    private String status;

    private String reason;

    private Long createdBy;

    private String createdAt;
}
