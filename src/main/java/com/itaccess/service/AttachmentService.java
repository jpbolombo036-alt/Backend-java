<<<<<<< HEAD
package com.itaccess.service;

import com.itaccess.entity.Attachment;
import com.itaccess.repository.AttachmentRepository;
import com.itaccess.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
        if (file.isEmpty()) {
            throw new IOException("Impossible de stocker un fichier vide.");
        }

        Path root = Paths.get(uploadDir);
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            throw new IOException("Nom de fichier invalide (tentative de traversée de répertoire) : " + originalFileName);
        }

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

    public byte[] downloadAttachment(Long id) throws IOException {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe non trouvée avec l'ID: " + id));
        
        Path path = Paths.get(attachment.getFilePath());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Le fichier physique est introuvable sur le serveur.");
        }
        return Files.readAllBytes(path);
    }
=======
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
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
}