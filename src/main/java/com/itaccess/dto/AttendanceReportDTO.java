package com.itaccess.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceReportDTO {

    private Long agentId;

    private String agentUsername;

    private LocalDate date;

    private String checkInTime;

    private String checkOutTime;

    private String status;

    private String reason;

    private String duration;
}
