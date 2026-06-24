package com.itaccess.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDashboardDTO {

    private LocalDate date;

    private int totalPresent;

    private int totalAbsent;

    private int totalLate;

    private int totalOnLeave;

    private int totalAgents;

    private List<AttendanceReportDTO> attendances;

    private Map<String, Integer> statusDistribution;

    private double attendanceRate;
}
