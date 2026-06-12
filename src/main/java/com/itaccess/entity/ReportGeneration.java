package com.itaccess.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_generations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String reportType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 50)
    @Builder.Default
    private String status = "AVAILABLE";

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "generated_by")
    private Long generatedBy;

    @Column(name = "generated_by_username", length = 255)
    private String generatedByUsername;

    @Column(columnDefinition = "TEXT")
    private String content;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "AVAILABLE";
        }
    }
}
