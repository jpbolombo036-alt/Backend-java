package com.itaccess.repository;

import com.itaccess.entity.ReportGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportGenerationRepository extends JpaRepository<ReportGeneration, Long> {

    Optional<ReportGeneration> findFirstByReportTypeOrderByGeneratedAtDesc(String reportType);

    List<ReportGeneration> findAllByOrderByGeneratedAtDesc();
}
