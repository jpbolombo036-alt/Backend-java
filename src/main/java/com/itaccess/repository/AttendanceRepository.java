package com.itaccess.repository;

import com.itaccess.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByAgentId(Long agentId);

    List<Attendance> findByAgentIdOrderByDateDesc(Long agentId);

    List<Attendance> findByDate(LocalDate date);

    Page<Attendance> findByAgentId(Long agentId, Pageable pageable);

    Page<Attendance> findByDate(LocalDate date, Pageable pageable);

    List<Attendance> findByAgentIdAndDateBetween(Long agentId, LocalDate start, LocalDate end);

    long countByStatus(String status);

    long countByDateAndStatus(LocalDate date, String status);

    List<Attendance> findByStatus(String status);
}
