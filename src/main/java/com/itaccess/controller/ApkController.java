package com.itaccess.controller;

import com.itaccess.dto.ApkFileDTO;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.ApkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/apk")
@RequiredArgsConstructor
@Tag(name = "APK Management", description = "Gestion des fichiers APK")
public class ApkController {
    
    private final ApkService apkService;
    
    @PostMapping("/upload")
    @Operation(summary = "Uploader un APK", description = "Upload un fichier APK avec ses métadonnées")
    public ResponseEntity<?> uploadApk(
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "applicationId", required = false) Long applicationId,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "packageName", required = false) String packageName,
            @RequestParam(value = "description", required = false) String description) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Le fichier ne peut pas être vide");
            }
            
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".apk")) {
                return ResponseEntity.badRequest().body("Seuls les fichiers APK sont autorisés");
            }
            
            ApkFileDTO apkFile = apkService.uploadApk(file, applicationId, currentUser.getId(), 
                    version, packageName, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(apkFile);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'upload: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur inattendue: " + e.getMessage());
        }
    }
    
    @GetMapping("/download/{id}")
    @Operation(summary = "Télécharger un APK", description = "Télécharge un fichier APK par son ID")
    public ResponseEntity<byte[]> downloadApk(@PathVariable Long id) {
        try {
            byte[] fileContent = apkService.downloadApk(id);
            ApkFileDTO apkFile = apkService.getApkById(id);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.android.package-archive"));
            headers.setContentDispositionFormData("attachment", apkFile.getOriginalFileName());
            headers.setContentLength(fileContent.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping
    @Operation(summary = "Liste des APK", description = "Retourne tous les fichiers APK")
    public ResponseEntity<List<ApkFileDTO>> getAllApks() {
        return ResponseEntity.ok(apkService.getAllApks());
    }
    
    @GetMapping("/application/{applicationId}")
    @Operation(summary = "APK par application", description = "Retourne les APK d'une application")
    public ResponseEntity<List<ApkFileDTO>> getApksByApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(apkService.getApksByApplication(applicationId));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "APK par ID", description = "Retourne les métadonnées d'un APK")
    public ResponseEntity<ApkFileDTO> getApkById(@PathVariable Long id) {
        return ResponseEntity.ok(apkService.getApkById(id));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un APK", description = "Supprime un fichier APK (authentification requise)")
    public ResponseEntity<Void> deleteApk(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        try {
            apkService.deleteApk(id);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
