package com.itaccess.controller;

import com.itaccess.entity.Attachment;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.AttachmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "Gestion des pièces jointes (screenshots, logs)")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Uploader une pièce jointe", description = "Upload un fichier et le lie à un bug ou une étape de test")
    public ResponseEntity<Attachment> upload(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long bugId,
            @RequestParam(required = false) Long testStepId) throws IOException {
        return ResponseEntity.ok(attachmentService.uploadAttachment(file, bugId, testStepId, currentUser.getId()));
    }

    @GetMapping("/download/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Télécharger une pièce jointe", description = "Récupère le contenu binaire d'un fichier")
    public ResponseEntity<byte[]> download(@PathVariable Long id) throws IOException {
        byte[] content = attachmentService.downloadAttachment(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .body(content);
    }
}