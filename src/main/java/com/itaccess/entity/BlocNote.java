package com.itaccess.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bloc_notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlocNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "test_id")
    private Long testId;

    @Column(length = 50)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_username", length = 255)
    private String createdByUsername;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = "DRAFT";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
