package com.itaccess.service;

import com.itaccess.entity.DocumentArchive;
import com.itaccess.repository.DocumentArchiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentArchiveMigrationService {

    private final DocumentArchiveRepository documentArchiveRepository;
    private final B2StorageService b2StorageService;

    @Value("${app.upload.document-archive-dir:uploads/documents}")
    private String localDocumentDir;

    public static class MigrationReport {
        public int total;
        public int migrated;
        public int skipped;
        public int failed;
        public List<String> errors = new ArrayList<>();
    }

    public MigrationReport migrateAll() {
        List<DocumentArchive> all = documentArchiveRepository.findAll();
        MigrationReport report = new MigrationReport();
        report.total = all.size();

        for (DocumentArchive doc : all) {
            String currentPath = doc.getFilePath();

            if (currentPath == null || currentPath.startsWith("document-archive/")) {
                report.skipped++;
                continue;
            }

            Path localPath = Paths.get(currentPath);
            if (!Files.isRegularFile(localPath)) {
                report.failed++;
                report.errors.add("ID=" + doc.getId() + " fichier local introuvable: " + currentPath);
                continue;
            }

            try {
                String objectKey = b2StorageService.buildObjectKey(doc.getFileName());
                if (b2StorageService.exists(objectKey)) {
                    report.skipped++;
                    continue;
                }

                long size = Files.size(localPath);
                String contentType = doc.getContentType();
                if (contentType == null) {
                    contentType = guessContentType(doc.getOriginalFileName());
                }
                b2StorageService.upload(localPath, objectKey, contentType, size);

                doc.setFilePath(objectKey);
                documentArchiveRepository.save(doc);
                report.migrated++;
                log.info("Migrated document ID={} key={}", doc.getId(), objectKey);
            } catch (Exception e) {
                report.failed++;
                report.errors.add("ID=" + doc.getId() + " erreur: " + e.getMessage());
                log.error("Migration failed for document ID={}", doc.getId(), e);
            }
        }

        log.info("Migration termine: total={}, migrated={}, skipped={}, failed={}",
                report.total, report.migrated, report.skipped, report.failed);
        return report;
    }

    private String guessContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".doc")) return "application/msword";
        return "application/octet-stream";
    }
}
