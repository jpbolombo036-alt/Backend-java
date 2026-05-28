package com.itaccess.repository;

import com.itaccess.entity.ApkFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApkFileRepository extends JpaRepository<ApkFile, Long> {
    
    List<ApkFile> findByApplicationId(Long applicationId);
    
    Optional<ApkFile> findByFileName(String fileName);
    
    List<ApkFile> findByUploadedBy(Long uploadedBy);
}
