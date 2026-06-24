package com.itaccess.service;

import com.itaccess.entity.Attachment;
import com.itaccess.repository.AttachmentRepository;
import com.itaccess.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;

    @Value("${app.upload.attachments-dir:uploads/attachments}")
    private String uploadDir;

    public Attachment uploadAttachment(MultipartFile file, Long bugId, Long testStepId, Long userId) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Impossible de stocker un fichier vide.");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            throw new IOException("Nom de fichier invalide (tentative de traversée de répertoire) : " + originalFileName);
        }

        Path root = resolveWritableUploadDirectory();
        String fileName = UUID.randomUUID().toString() + "_" + originalFileName;
        Path targetPath = root.resolve(fileName);

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

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

    private Path resolveWritableUploadDirectory() throws IOException {
        Path configuredPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            if (!Files.exists(configuredPath)) {
                Files.createDirectories(configuredPath);
            }
            if (Files.isWritable(configuredPath)) {
                return configuredPath;
            }
            log.warn("Configured attachments directory is not writable, falling back to /tmp: {}", configuredPath);
        } catch (IOException e) {
            log.warn("Failed to use configured attachments directory [{}], falling back to /tmp: {}", configuredPath, e.getMessage());
        }

        Path fallbackPath = Paths.get(System.getProperty("java.io.tmpdir"), "uploads", "attachments").toAbsolutePath().normalize();
        if (!Files.exists(fallbackPath)) {
            Files.createDirectories(fallbackPath);
        }
        if (!Files.isWritable(fallbackPath)) {
            throw new IOException("Aucun répertoire d'upload accessible en écriture. Vérifié: " + configuredPath + " et " + fallbackPath);
        }
        return fallbackPath;
    }

    public byte[] downloadAttachment(Long id) throws IOException {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe non trouvée avec l'ID: " + id));
        
        Path path = Paths.get(attachment.getFilePath());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Le fichier physique est introuvable sur le serveur.");
        }
        return Files.readAllBytes(path);
    }
}