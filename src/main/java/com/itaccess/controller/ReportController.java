package com.itaccess.controller;

import com.itaccess.dto.ReportDefinitionDTO;
import com.itaccess.dto.ReportGenerationDTO;
import com.itaccess.security.CurrentUser;
import com.itaccess.security.UserInfo;
import com.itaccess.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Génération et historique des rapports")
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Types de rapports", description = "Retourne les rapports disponibles")
    public ResponseEntity<List<ReportDefinitionDTO>> getReportTypes() {
        return ResponseEntity.ok(reportService.getDefinitions());
    }

    @PostMapping("/{type}/generate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Générer un rapport", description = "Génère un rapport et conserve son historique")
    public ResponseEntity<ReportGenerationDTO> generateReport(
            @PathVariable String type,
            @Parameter(hidden = true) @CurrentUser UserInfo currentUser) {
        return ResponseEntity.ok(reportService.generate(type, currentUser));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Historique des rapports", description = "Retourne l'historique des rapports générés")
    public ResponseEntity<List<ReportGenerationDTO>> getHistory() {
        return ResponseEntity.ok(reportService.getHistory());
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Télécharger un rapport", description = "Télécharge le contenu texte d'un rapport généré")
    public ResponseEntity<String> downloadReport(@PathVariable Long id) {
        ReportGenerationDTO report = reportService.getGeneration(id);
        String filename = report.getTitle().toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}0-9]+", "-")
                .replaceAll("^-|-$", "") + ".txt";

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(reportService.getGenerationContent(id));
    }
}
