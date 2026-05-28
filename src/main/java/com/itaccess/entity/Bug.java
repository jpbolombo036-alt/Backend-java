<<<<<<< HEAD
package com.itaccess.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bugs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bug {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_step_id")
    private Long testStepId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 50)
    private String severity; // CRITICAL, MAJOR, MINOR

    @Column(length = 50)
    private String priority; // HIGH, MEDIUM, LOW

    @Column(columnDefinition = "TEXT")
    private String reproducibility;

    @Column(length = 50)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_step_id", insertable = false, updatable = false)
    private TestStep testStep;

    @OneToMany(mappedBy = "bugId", cascade = CascadeType.ALL)
    private List<Attachment> attachments;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
=======
package com.itaccess.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bugs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bug {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_step_id")
    private Long testStepId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 50)
    private String severity; // CRITICAL, MAJOR, MINOR

    @Column(length = 50)
    private String priority; // HIGH, MEDIUM, LOW

    @Column(columnDefinition = "TEXT")
    private String reproducibility;

    @Column(length = 50)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_step_id", insertable = false, updatable = false)
    private TestStep testStep;

    @OneToMany(mappedBy = "bugId", cascade = CascadeType.ALL)
    private List<Attachment> attachments;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
}