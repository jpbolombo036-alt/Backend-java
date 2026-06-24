package com.itaccess.service;

import com.itaccess.dto.DocumentArchiveDTO;
import com.itaccess.dto.DocumentArchiveRequest;
import com.itaccess.entity.DocumentArchive;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.DocumentArchiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
        String objectKey = b2StorageService.upload(file, b2StorageService.buildObjectKey(uniqueFileName), contentType);

        DocumentArchive document = DocumentArchive.builder()
                .fileName(uniqueFileName)
                .originalFileName(originalFileName)
                .filePath(objectKey)
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

        document.setDownloadCount(document.getDownloadCount() + 1);
        documentArchiveRepository.save(document);

        String objectKey = document.getFilePath();
        if (objectKey == null) {
            objectKey = b2StorageService.buildObjectKey(document.getFileName());
        }
        return b2StorageService.downloadAsResource(objectKey, document.getOriginalFileName(), document.getContentType());
    }

    public void deleteDocument(Long id, Long userId) throws IOException {
        DocumentArchive document = documentArchiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé avec l'ID: " + id));

        String objectKey = document.getFilePath();
        if (objectKey == null) {
            objectKey = b2StorageService.buildObjectKey(document.getFileName());
        }
        b2StorageService.delete(objectKey);

        documentArchiveRepository.delete(document);

        auditService.logAction("DELETE_DOCUMENT", "Document: " + document.getOriginalFileName(), userId);
        log.info("Document deleted: {}", document.getOriginalFileName());
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
                .downloadCount(document.getDownloadCount())
                .build();
    }
}
