package com.itaccess.service;

import com.itaccess.dto.PageResponse;
import com.itaccess.dto.AttendanceDTO;
import com.itaccess.dto.AttendanceReportDTO;
import com.itaccess.dto.AttendanceDashboardDTO;
import com.itaccess.entity.Attendance;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.AttendanceRepository;
import com.itaccess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public PageResponse<AttendanceDTO> getAllAttendances(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Attendance> attendancePage = attendanceRepository.findAll(pageable);

        List<AttendanceDTO> content = attendancePage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResponse.of(content, attendancePage.getNumber(), attendancePage.getSize(), attendancePage.getTotalElements());
    }

    public List<AttendanceDTO> getAttendancesByAgent(Long agentId) {
        return attendanceRepository.findByAgentIdOrderByDateDesc(agentId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PageResponse<AttendanceDTO> getAttendancesByAgent(Long agentId, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Attendance> attendancePage = attendanceRepository.findByAgentId(agentId, pageable);

        List<AttendanceDTO> content = attendancePage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResponse.of(content, attendancePage.getNumber(), attendancePage.getSize(), attendancePage.getTotalElements());
    }

    public List<AttendanceDTO> getAttendancesByDate(LocalDate date) {
        return attendanceRepository.findByDate(date).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PageResponse<AttendanceDTO> getAttendancesByDate(LocalDate date, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Attendance> attendancePage = attendanceRepository.findByDate(date, pageable);

        List<AttendanceDTO> content = attendancePage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResponse.of(content, attendancePage.getNumber(), attendancePage.getSize(), attendancePage.getTotalElements());
    }

    public List<AttendanceDTO> getAttendancesByAgentAndDateRange(Long agentId, LocalDate start, LocalDate end) {
        return attendanceRepository.findByAgentIdAndDateBetween(agentId, start, end).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public AttendanceDTO getAttendanceById(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Présence non trouvée avec l'ID: " + id));
        return toDTO(attendance);
    }

    @Transactional
    public AttendanceDTO checkIn(Long agentId, String agentUsername) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<Attendance> existing = attendanceRepository.findByAgentIdAndDateBetween(agentId, today, today);
        if (!existing.isEmpty()) {
            Attendance attendance = existing.get(0);
            if (attendance.getCheckInTime() != null) {
                return toDTO(attendance);
            }
            attendance.setCheckInTime(now);
            attendance.setStatus(now.isAfter(LocalTime.of(9, 0)) ? "LATE" : "PRESENT");
            Attendance saved = attendanceRepository.save(attendance);
            return toDTO(saved);
        }

        Attendance attendance = Attendance.builder()
                .agentId(agentId)
                .agentUsername(agentUsername)
                .date(today)
                .checkInTime(now)
                .status(now.isAfter(LocalTime.of(9, 0)) ? "LATE" : "PRESENT")
                .createdBy(agentId)
                .build();

        Attendance saved = attendanceRepository.save(attendance);
        return toDTO(saved);
    }

    @Transactional
    public AttendanceDTO checkOut(Long agentId) {
        LocalDate today = LocalDate.now();
        List<Attendance> existing = attendanceRepository.findByAgentIdAndDateBetween(agentId, today, today);
        if (existing.isEmpty()) {
            throw new ResourceNotFoundException("Aucun pointage d'arrivée trouvé pour aujourd'hui. Veuillez d'abord pointer votre arrivée.");
        }
        Attendance attendance = existing.get(0);
        if (attendance.getCheckOutTime() != null) {
            return toDTO(attendance);
        }
        attendance.setCheckOutTime(LocalTime.now());
        Attendance saved = attendanceRepository.save(attendance);
        return toDTO(saved);
    }

    @Transactional
    public AttendanceDTO createAttendance(AttendanceDTO dto, Long createdBy) {
        Attendance attendance = Attendance.builder()
                .agentId(dto.getAgentId())
                .agentUsername(userRepository.findById(dto.getAgentId())
                        .map(user -> user.getUsername())
                        .orElse(dto.getAgentUsername()))
                .date(dto.getDate())
                .checkInTime(dto.getCheckInTime())
                .checkOutTime(dto.getCheckOutTime())
                .status(dto.getStatus() != null ? dto.getStatus() : "PRESENT")
                .reason(dto.getReason())
                .createdBy(createdBy)
                .build();

        Attendance saved = attendanceRepository.save(attendance);
        return toDTO(saved);
    }

    @Transactional
    public AttendanceDTO updateAttendance(Long id, AttendanceDTO dto, Long userId, String userRole) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Présence non trouvée avec l'ID: " + id));

        if (!"admin".equals(userRole) && !attendance.getCreatedBy().equals(userId)) {
            throw new SecurityException("Non autorisé à modifier cette présence");
        }

        if (dto.getAgentId() != null) {
            attendance.setAgentId(dto.getAgentId());
        }
        if (dto.getAgentUsername() != null) {
            attendance.setAgentUsername(dto.getAgentUsername());
        }
        if (dto.getDate() != null) {
            attendance.setDate(dto.getDate());
        }
        if (dto.getCheckInTime() != null) {
            attendance.setCheckInTime(dto.getCheckInTime());
            if (attendance.getStatus() == null || "PRESENT".equals(attendance.getStatus())) {
                attendance.setStatus(dto.getCheckInTime().isAfter(LocalTime.of(9, 0)) ? "LATE" : "PRESENT");
            }
        }
        if (dto.getCheckOutTime() != null) {
            attendance.setCheckOutTime(dto.getCheckOutTime());
        }
        if (dto.getStatus() != null) {
            attendance.setStatus(dto.getStatus());
        }
        if (dto.getReason() != null) {
            attendance.setReason(dto.getReason());
        }

        Attendance updated = attendanceRepository.save(attendance);
        return toDTO(updated);
    }

    @Transactional
    public void deleteAttendance(Long id, Long userId, String userRole) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Présence non trouvée avec l'ID: " + id));

        if (!"admin".equals(userRole) && !attendance.getCreatedBy().equals(userId)) {
            throw new SecurityException("Non autorisé à supprimer cette présence");
        }

        attendanceRepository.deleteById(id);
    }

    public Object getDashboardStats() {
        LocalDate today = LocalDate.now();
        List<Attendance> todayAttendances = attendanceRepository.findByDate(today);

        int totalPresent = (int) todayAttendances.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        int totalAbsent = (int) todayAttendances.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();
        int totalLate = (int) todayAttendances.stream().filter(a -> "LATE".equals(a.getStatus())).count();
        int totalOnLeave = (int) todayAttendances.stream().filter(a -> "LEAVE".equals(a.getStatus())).count();
        long totalAgents = userRepository.countByRole("user");
        if (totalAgents == 0) {
            totalAgents = userRepository.count();
        }

        Map<String, Integer> statusDistribution = new HashMap<>();
        statusDistribution.put("PRESENT", totalPresent);
        statusDistribution.put("ABSENT", totalAbsent);
        statusDistribution.put("LATE", totalLate);
        statusDistribution.put("LEAVE", totalOnLeave);

        List<AttendanceReportDTO> reportDTOs = todayAttendances.stream()
                .map(this::toReportDTO)
                .collect(Collectors.toList());

        double attendanceRate = totalAgents > 0 ? ((double) (totalPresent + totalLate) / totalAgents) * 100 : 0;

        return AttendanceDashboardDTO.builder()
                .date(today)
                .totalPresent(totalPresent)
                .totalAbsent(totalAbsent)
                .totalLate(totalLate)
                .totalOnLeave(totalOnLeave)
                .totalAgents((int) totalAgents)
                .attendances(reportDTOs)
                .statusDistribution(statusDistribution)
                .attendanceRate(Math.round(attendanceRate * 100.0) / 100.0)
                .build();
    }

    private AttendanceDTO toDTO(Attendance attendance) {
        return AttendanceDTO.builder()
                .id(attendance.getId())
                .agentId(attendance.getAgentId())
                .agentUsername(attendance.getAgentUsername())
                .date(attendance.getDate())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .status(attendance.getStatus())
                .reason(attendance.getReason())
                .createdBy(attendance.getCreatedBy())
                .createdAt(attendance.getCreatedAt() != null ? attendance.getCreatedAt().format(DATE_TIME_FORMATTER) : null)
                .build();
    }

    private AttendanceReportDTO toReportDTO(Attendance attendance) {
        String duration = null;
        if (attendance.getCheckInTime() != null && attendance.getCheckOutTime() != null) {
            long minutes = ChronoUnit.MINUTES.between(attendance.getCheckInTime(), attendance.getCheckOutTime());
            long hours = minutes / 60;
            long mins = minutes % 60;
            duration = hours + "h " + mins + "min";
        }

        return AttendanceReportDTO.builder()
                .agentId(attendance.getAgentId())
                .agentUsername(attendance.getAgentUsername())
                .date(attendance.getDate())
                .checkInTime(attendance.getCheckInTime() != null ? attendance.getCheckInTime().format(TIME_FORMATTER) : null)
                .checkOutTime(attendance.getCheckOutTime() != null ? attendance.getCheckOutTime().format(TIME_FORMATTER) : null)
                .status(attendance.getStatus())
                .reason(attendance.getReason())
                .duration(duration)
                .build();
    }
}
