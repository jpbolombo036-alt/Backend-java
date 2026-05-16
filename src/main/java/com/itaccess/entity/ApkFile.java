package com.itaccess.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "apk_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApkFile {
    
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
    
    @Column(length = 50)
    private String version;
    
    @Column(length = 100)
    private String packageName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "application_id")
    private Long applicationId;
    
    @Column(name = "uploaded_by")
    private Long uploadedBy;
    
    @Column(name = "upload_date")
    private LocalDateTime uploadDate;
    
    @Column(name = "download_count")
    private Integer downloadCount = 0;
    
    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
        if (downloadCount == null) {
            downloadCount = 0;
        }
    }
}
