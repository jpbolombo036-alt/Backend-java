package com.itaccess.repository;

import com.itaccess.entity.ApkFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApkFileRepository extends JpaRepository<ApkFile, Long> {
    
    List<ApkFile> findByApplicationId(Long applicationId);
    
    Page<ApkFile> findByApplicationId(Long applicationId, Pageable pageable);
    
    Optional<ApkFile> findByFileName(String fileName);
    
    List<ApkFile> findByUploadedBy(Long uploadedBy);
}
