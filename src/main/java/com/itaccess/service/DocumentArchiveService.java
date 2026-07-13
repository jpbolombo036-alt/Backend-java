package com.itaccess.service;

import com.itaccess.config.B2Properties;
import com.itaccess.dto.DocumentArchiveDTO;
import com.itaccess.dto.DocumentArchiveRequest;
import com.itaccess.entity.DocumentArchive;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.DocumentArchiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentArchiveService {

    private final DocumentArchiveRepository documentArchiveRepository;
    private final AuditService auditService;
    private final B2StorageService b2StorageService;
    private final B2Properties b2Properties;

    @Value("${app.upload.document-archive-dir:uploads/documents}")
    private String localDocumentDir;

    public DocumentArchiveDTO uploadDocument(MultipartFile file, DocumentArchiveRequest request, Long uploadedBy, String username) throws IOException {
        log.info("Starting document archive upload: file={}, size={}, user={}", file.getOriginalFilename(), file.getSize(), uploadedBy);

        if (file.isEmpty()) {
            throw new IOException("Le fichier ne peut pas être vide");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IOException("Nom de fichier invalide");
        }

        String lowerName = originalFileName.toLowerCase();
        String contentType = file.getContentType();
        if (contentType == null) {
            if (lowerName.endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (lowerName.endsWith(".docx")) {
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            } else if (lowerName.endsWith(".doc")) {
                contentType = "application/msword";
            }
        }

        if (!isSupportedContentType(contentType)) {
            throw new IOException("Format de fichier non supporté. Seuls PDF et Word (.doc, .docx) sont autorisés.");
        }

        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        String storageKey = storeFile(file, uniqueFileName, contentType);

        DocumentArchive document = DocumentArchive.builder()
                .fileName(uniqueFileName)
                .originalFileName(originalFileName)
                .filePath(storageKey)
                .fileSize(file.getSize())
                .contentType(contentType)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .tags(request.getTags())
                .author(request.getAuthor())
                .uploadedBy(uploadedBy)
                .uploadedByUsername(username)
                .build();

        DocumentArchive saved = documentArchiveRepository.save(document);
        log.info("Document archived successfully: {} by user {}", originalFileName, uploadedBy);

        auditService.logAction("UPLOAD_DOCUMENT", "Document: " + originalFileName + " | Titre: " + request.getTitle(), uploadedBy);
        return toDTO(saved);
    }

    public DocumentArchiveDTO getDocumentById(Long id) {
        DocumentArchive document = documentArchiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé avec l'ID: " + id));
        return toDTO(document);
    }

    public org.springframework.data.domain.Page<DocumentArchiveDTO> getDocuments(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("uploadDate").descending());
        org.springframework.data.domain.Page<DocumentArchive> result = documentArchiveRepository.findAll(pageable);
        return result.map(this::toDTO);
    }

    public List<DocumentArchiveDTO> getDocumentsByCategory(String category) {
        return documentArchiveRepository.findAll().stream()
                .filter(doc -> category.equals(doc.getCategory()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<DocumentArchiveDTO> searchDocuments(String query) {
        String lowerQuery = query.toLowerCase();
        return documentArchiveRepository.findAll().stream()
                .filter(doc -> doc.getTitle().toLowerCase().contains(lowerQuery)
                        || (doc.getDescription() != null && doc.getDescription().toLowerCase().contains(lowerQuery))
                        || (doc.getTags() != null && doc.getTags().toLowerCase().contains(lowerQuery))
                        || (doc.getAuthor() != null && doc.getAuthor().toLowerCase().contains(lowerQuery)))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Resource downloadDocumentAsResource(Long id) throws IOException {
        DocumentArchive document = documentArchiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé avec l'ID: " + id));

        // Vérifie l'existence du binaire avant toute mutation (miroir APK)
        if (!documentExists(document.getFilePath())) {
            throw new ResourceNotFoundException("Fichier physique non trouvé");
        }

        document.setDownloadCount(document.getDownloadCount() + 1);
        documentArchiveRepository.save(document);

        return loadDocumentResource(document.getFilePath(), document.getOriginalFileName(), document.getContentType());
    }

    public void deleteDocument(Long id, Long userId, String userRole) throws IOException {
        DocumentArchive document = documentArchiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé avec l'ID: " + id));

        // Contrôle de propriété : auteur ou administrateur uniquement
        boolean isAdmin = userRole != null && "admin".equalsIgnoreCase(userRole);
        if (!isAdmin && !userId.equals(document.getUploadedBy())) {
            throw new SecurityException("Vous n'êtes pas autorisé à supprimer ce document");
        }

        deleteStoredFile(document.getFilePath());

        documentArchiveRepository.delete(document);

        auditService.logAction("DELETE_DOCUMENT", "Document: " + document.getOriginalFileName(), userId);
        log.info("Document deleted: {}", document.getOriginalFileName());
    }

    /**
     * Met à jour un document d'archive : métadonnées et/ou remplacement du binaire.
     * Réservé à l'auteur du document ou à un administrateur.
     * La date de mise à jour (update_date) est renseignée automatiquement par @PreUpdate.
     */
    public DocumentArchiveDTO updateDocument(Long id, Long userId, String userRole,
                                             MultipartFile file, String title, String description,
                                             String category, String tags, String author) throws IOException {
        DocumentArchive document = documentArchiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé avec l'ID: " + id));

        boolean isAdmin = userRole != null && "admin".equalsIgnoreCase(userRole);
        if (!isAdmin && !userId.equals(document.getUploadedBy())) {
            throw new SecurityException("Vous n'êtes pas autorisé à modifier ce document");
        }

        // Remplacement éventuel du binaire (PDF/Word uniquement)
        if (file != null && !file.isEmpty()) {
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.isBlank()) {
                throw new IOException("Nom de fichier invalide");
            }

            String contentType = file.getContentType();
            String lowerName = originalFileName.toLowerCase();
            if (contentType == null) {
                if (lowerName.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (lowerName.endsWith(".docx")) {
                    contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                } else if (lowerName.endsWith(".doc")) {
                    contentType = "application/msword";
                }
            }
            if (!isSupportedContentType(contentType)) {
                throw new IOException("Format de fichier non supporté. Seuls PDF et Word (.doc, .docx) sont autorisés.");
            }

            // Supprime l'ancien binaire puis stocke le nouveau (B2 ou local)
            deleteStoredFile(document.getFilePath());
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            String storageKey = storeFile(file, uniqueFileName, contentType);

            document.setFileName(uniqueFileName);
            document.setOriginalFileName(originalFileName);
            document.setFilePath(storageKey);
            document.setFileSize(file.getSize());
            document.setContentType(contentType);
        }

        if (title != null) {
            document.setTitle(title);
        }
        if (description != null) {
            document.setDescription(description);
        }
        if (category != null) {
            document.setCategory(category);
        }
        if (tags != null) {
            document.setTags(tags);
        }
        if (author != null) {
            document.setAuthor(author);
        }

        DocumentArchive saved = documentArchiveRepository.save(document); // @PreUpdate renseigne updateDate
        log.info("Document updated: {}", document.getOriginalFileName());
        return toDTO(saved);
    }

    // -------------------------------------------------------------------------
    // Stockage abstract : B2 si activé, sinon disque local (miroir ApkService)
    // -------------------------------------------------------------------------

    private String storeFile(MultipartFile file, String uniqueFileName, String contentType) throws IOException {
        if (b2Properties.isEnabled()) {
            return b2StorageService.upload(file, b2StorageService.buildObjectKey(uniqueFileName), contentType);
        }
        Path dir = resolveWritableDocumentDirectory();
        Path filePath = dir.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath);
        return filePath.toString();
    }

    private boolean documentExists(String storageKey) {
        if (storageKey == null) {
            return false;
        }
        if (b2Properties.isEnabled()) {
            return b2StorageService.exists(storageKey);
        }
        return Files.exists(Paths.get(storageKey));
    }

    private Resource loadDocumentResource(String storageKey, String originalFileName, String contentType) throws IOException {
        if (b2Properties.isEnabled()) {
            return b2StorageService.downloadAsResource(storageKey, originalFileName, contentType);
        }
        Path path = Paths.get(storageKey);
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Fichier physique non trouvé");
        }
        return new UrlResource(path.toUri());
    }

    private void deleteStoredFile(String storageKey) {
        if (storageKey == null) {
            return;
        }
        if (b2Properties.isEnabled()) {
            b2StorageService.delete(storageKey);
        } else {
            Path path = Paths.get(storageKey);
            if (Files.exists(path)) {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    log.warn("Impossible de supprimer le fichier local {}: {}", storageKey, e.getMessage());
                }
            }
        }
    }

    private Path resolveWritableDocumentDirectory() throws IOException {
        Path configured = Paths.get(localDocumentDir).toAbsolutePath().normalize();
        try {
            if (!Files.exists(configured)) {
                Files.createDirectories(configured);
            }
            if (Files.isWritable(configured)) {
                return configured;
            }
            log.warn("Document dir not writable, falling back to /tmp: {}", configured);
        } catch (IOException e) {
            log.warn("Cannot use configured document dir [{}], falling back to /tmp: {}", configured, e.getMessage());
        }

        Path fallback = Paths.get(System.getProperty("java.io.tmpdir"), "uploads", "documents").toAbsolutePath().normalize();
        if (!Files.exists(fallback)) {
            Files.createDirectories(fallback);
        }
        if (!Files.isWritable(fallback)) {
            throw new IOException("Aucun répertoire d'upload accessible en écriture. Vérifié: " + configured + " et " + fallback);
        }
        return fallback;
    }

    private boolean isSupportedContentType(String contentType) {
        if (contentType == null) return false;
        return contentType.equals("application/pdf")
                || contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || contentType.equals("application/msword");
    }

    private DocumentArchiveDTO toDTO(DocumentArchive document) {
        return DocumentArchiveDTO.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .originalFileName(document.getOriginalFileName())
                .fileSize(document.getFileSize())
                .contentType(document.getContentType())
                .title(document.getTitle())
                .description(document.getDescription())
                .category(document.getCategory())
                .tags(document.getTags())
                .author(document.getAuthor())
                .uploadedBy(document.getUploadedBy())
                .uploadedByUsername(document.getUploadedByUsername())
                .uploadDate(document.getUploadDate())
                .updateDate(document.getUpdateDate())
                .downloadCount(document.getDownloadCount())
                .build();
    }
}
