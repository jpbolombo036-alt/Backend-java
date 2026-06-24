package com.itaccess.controller;

import com.itaccess.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/attendance-dashboard")
@RequiredArgsConstructor
@Tag(name = "AttendanceDashboard", description = "Tableau de bord des présences")
public class AttendanceDashboardController {

    private final AttendanceService attendanceService;

    @GetMapping("/today")
    @Operation(summary = "Stats du jour", description = "Retourne les statistiques de présence du jour")
    public ResponseEntity<Object> getTodayStats() {
        return ResponseEntity.ok(attendanceService.getDashboardStats());
    }
}
