package com.itaccess.controller;

import com.itaccess.dto.DocumentArchiveDTO;
import com.itaccess.dto.DocumentArchiveRequest;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.DocumentArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/document-archive")
@RequiredArgsConstructor
@Tag(name = "Document Archive", description = "Gestion des archives documents PDF et Word de l'entreprise")
public class DocumentArchiveController {

    private final DocumentArchiveService documentArchiveService;

    @PostMapping("/upload")
    @Operation(summary = "Uploader un document", description = "Upload un document PDF ou Word avec ses métadonnées dans l'archive de l'entreprise")
    public ResponseEntity<?> uploadDocument(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute DocumentArchiveRequest request) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Le fichier ne peut pas être vide");
            }

            DocumentArchiveDTO document = documentArchiveService.uploadDocument(file, request, currentUser.getId(), currentUser.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(document);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'upload: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur inattendue: " + e.getMessage());
        }
    }

    @GetMapping("/download/{id}")
    @Operation(summary = "Télécharger un document", description = "Télécharge un document de l'archive par son ID")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        try {
            Resource fileContent = documentArchiveService.downloadDocumentAsResource(id);
            DocumentArchiveDTO document = documentArchiveService.getDocumentById(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", document.getOriginalFileName());
            headers.setContentLength(fileContent.contentLength());
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            String mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            if (document.getContentType() != null) {
                mediaType = document.getContentType();
            }
            headers.setContentType(MediaType.parseMediaType(mediaType));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    @Operation(summary = "Liste des documents", description = "Retourne tous les documents de l'archive avec pagination")
    public ResponseEntity<org.springframework.data.domain.Page<DocumentArchiveDTO>> getAllDocuments(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(documentArchiveService.getDocuments(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Document par ID", description = "Retourne les métadonnées d'un document de l'archive")
    public ResponseEntity<DocumentArchiveDTO> getDocumentById(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(documentArchiveService.getDocumentById(id));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Documents par catégorie", description = "Retourne les documents filtrés par catégorie")
    public ResponseEntity<List<DocumentArchiveDTO>> getDocumentsByCategory(
            @PathVariable String category,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(documentArchiveService.getDocumentsByCategory(category));
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des documents", description = "Recherche des documents par titre, description, tags ou auteur")
    public ResponseEntity<List<DocumentArchiveDTO>> searchDocuments(
            @RequestParam String q,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(documentArchiveService.searchDocuments(q));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un document", description = "Met à jour les métadonnées et/ou le binaire d'un document (auteur ou admin)")
    public ResponseEntity<DocumentArchiveDTO> updateDocument(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "author", required = false) String author) {
        try {
            DocumentArchiveDTO updated = documentArchiveService.updateDocument(id, currentUser.getId(),
                    currentUser.getRole(), file, title, description, category, tags, author);
            return ResponseEntity.ok(updated);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un document", description = "Supprime un document de l'archive (propriétaire ou admin uniquement)")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        try {
            documentArchiveService.deleteDocument(id, currentUser.getId(), currentUser.getRole());
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
