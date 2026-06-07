package com.itaccess.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {
    private Integer applications;
    private Integer sessions;
    private Integer tests;
    private Integer users;
    private Integer accounts;
    private Integer testsOk;
    private Integer testsBug;
    private Integer testsEnCours;
    private Integer testsRateOk;
    private Integer testsRateBug;
    private Integer testsRatePending;
    private Integer activeAccounts;
    private Integer recentSessions;
    private Integer bugReports;
}