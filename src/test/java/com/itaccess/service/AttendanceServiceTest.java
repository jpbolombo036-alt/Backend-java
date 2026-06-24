package com.itaccess.service;

import com.itaccess.dto.AttendanceDTO;
import com.itaccess.entity.Attendance;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.AttendanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private Attendance testAttendance;

    @BeforeEach
    void setUp() {
        testAttendance = Attendance.builder()
                .id(1L)
                .agentId(2L)
                .agentUsername("agent1")
                .date(LocalDate.of(2024, 1, 15))
                .checkInTime(LocalTime.of(8, 30))
                .checkOutTime(null)
                .status("PRESENT")
                .createdBy(2L)
                .build();
    }

    @Test
    void getAttendanceById_ShouldReturnAttendance_WhenExists() {
        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(testAttendance));

        AttendanceDTO result = attendanceService.getAttendanceById(1L);

        assertNotNull(result);
        assertEquals("agent1", result.getAgentUsername());
        verify(attendanceRepository, times(1)).findById(1L);
    }

    @Test
    void getAttendanceById_ShouldThrowException_WhenNotFound() {
        when(attendanceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attendanceService.getAttendanceById(1L));
    }

    @Test
    void checkIn_ShouldCreateAttendance_WhenNoExisting() {
        when(attendanceRepository.findByAgentIdAndDateBetween(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(testAttendance);

        AttendanceDTO result = attendanceService.checkIn(2L, "agent1");

        assertNotNull(result);
        verify(attendanceRepository, times(1)).save(any(Attendance.class));
    }

    @Test
    void checkOut_ShouldUpdateAttendance_WhenExists() {
        testAttendance.setCheckOutTime(null);
        when(attendanceRepository.findByAgentIdAndDateBetween(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(testAttendance));
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(testAttendance);

        AttendanceDTO result = attendanceService.checkOut(2L);

        assertNotNull(result);
        verify(attendanceRepository, times(1)).save(any(Attendance.class));
    }
}
