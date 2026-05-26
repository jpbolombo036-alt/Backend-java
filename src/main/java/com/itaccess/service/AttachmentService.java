package com.itaccess.service;

import com.itaccess.entity.Attachment;
import com.itaccess.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;

    @Value("${app.upload.attachments-dir:uploads/attachments}")
    private String uploadDir;

    public Attachment uploadAttachment(MultipartFile file, Long bugId, Long testStepId, Long userId) throws IOException {
        Path root = Paths.get(uploadDir);
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }

        String originalFileName = file.getOriginalFilename();
        String fileName = UUID.randomUUID().toString() + "_" + originalFileName;
        Path targetPath = root.resolve(fileName);

        Files.copy(file.getInputStream(), targetPath);

        Attachment attachment = Attachment.builder()
                .fileName(fileName)
                .originalFileName(originalFileName)
                .filePath(targetPath.toString())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .bugId(bugId)
                .testStepId(testStepId)
                .createdBy(userId)
                .build();

        return attachmentRepository.save(attachment);
    }

    public byte[] downloadAttachment(Long id) throws IOException {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fichier non trouvé"));
        return Files.readAllBytes(Paths.get(attachment.getFilePath()));
    }
}