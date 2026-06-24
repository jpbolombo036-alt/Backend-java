package com.itaccess.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_archives")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(length = 255)
    private String author;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "uploaded_by_username", length = 100)
    private String uploadedByUsername;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @Column(name = "download_count")
    @Builder.Default
    private Integer downloadCount = 0;

    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
        if (downloadCount == null) {
            downloadCount = 0;
        }
    }
}
