package com.itaccess.service;

import com.itaccess.dto.ReportDefinitionDTO;
import com.itaccess.dto.ReportGenerationDTO;
import com.itaccess.entity.ReportGeneration;
import com.itaccess.entity.ReportType;
import com.itaccess.entity.User;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.ApplicationRepository;
import com.itaccess.repository.BugRepository;
import com.itaccess.repository.CompteRepository;
import com.itaccess.repository.ReportGenerationRepository;
import com.itaccess.repository.TestRepository;
import com.itaccess.repository.TestSessionRepository;
import com.itaccess.repository.UserRepository;
import com.itaccess.security.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final List<ReportType> REPORT_TYPES = Arrays.asList(ReportType.values());

    private final ReportGenerationRepository reportGenerationRepository;
    private final ApplicationRepository applicationRepository;
    private final TestSessionRepository testSessionRepository;
    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final CompteRepository compteRepository;
    private final BugRepository bugRepository;

    public List<ReportDefinitionDTO> getDefinitions() {
        return REPORT_TYPES.stream()
                .map(this::toDefinitionDTO)
                .toList();
    }

    @Transactional
    public ReportGenerationDTO generate(String typeId, UserInfo currentUser) {
        ReportType reportType = ReportType.fromId(typeId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de rapport inconnu: " + typeId));

        ReportGeneration report = ReportGeneration.builder()
                .reportType(reportType.getId())
                .title(reportType.getTitle())
                .status("AVAILABLE")
                .generatedAt(LocalDateTime.now())
                .generatedBy(currentUser.getId())
                .generatedByUsername(currentUser.getUsername())
                .content(buildContent(reportType))
                .build();

        return toDTO(reportGenerationRepository.save(report));
    }

    public List<ReportGenerationDTO> getHistory() {
        return reportGenerationRepository.findAllByOrderByGeneratedAtDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ReportGenerationDTO getGeneration(Long id) {
        ReportGeneration report = reportGenerationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé avec l'ID: " + id));
        return toDTO(report);
    }

    public String getGenerationContent(Long id) {
        return reportGenerationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé avec l'ID: " + id))
                .getContent();
    }

    private ReportDefinitionDTO toDefinitionDTO(ReportType reportType) {
        String lastGenerated = reportGenerationRepository.findFirstByReportTypeOrderByGeneratedAtDesc(reportType.getId())
                .map(report -> report.getGeneratedAt().toString())
                .orElse(null);

        return ReportDefinitionDTO.builder()
                .id(reportType.getId())
                .title(reportType.getTitle())
                .description(reportType.getDescription())
                .lastGenerated(lastGenerated)
                .build();
    }

    private ReportGenerationDTO toDTO(ReportGeneration report) {
        return ReportGenerationDTO.builder()
                .id(report.getId())
                .reportType(report.getReportType())
                .title(report.getTitle())
                .type(report.getReportType())
                .status(report.getStatus())
                .generatedAt(report.getGeneratedAt())
                .generatedBy(report.getGeneratedBy())
                .generatedByUsername(report.getGeneratedByUsername())
                .build();
    }

    private String buildContent(ReportType reportType) {
        long totalApplications = applicationRepository.count();
        long totalSessions = testSessionRepository.count();
        long totalTests = testRepository.count();
        long totalUsers = userRepository.count();
        long totalAccounts = compteRepository.count();
        long totalBugs = bugRepository.count();
        long testsOk = safeCount(testRepository.countByStatut("OK"));
        long testsBug = safeCount(testRepository.countByStatut("BUG"));
        long testsEnCours = safeCount(testRepository.countByStatut("EN COURS"));
        long activeUsers = userRepository.findAll().stream()
                .filter(User::getIsActive)
                .count();
        long adminUsers = safeCount(userRepository.countByRole("admin"));

        return switch (reportType) {
            case SECURITY -> buildSecurityReport(totalUsers, activeUsers, adminUsers, totalAccounts);
            case ACCESS -> buildAccessReport(totalUsers, activeUsers, adminUsers);
            case TESTS -> buildTestsReport(totalSessions, totalTests, testsOk, testsBug, testsEnCours, totalBugs);
            case PERFORMANCE -> buildPerformanceReport(totalApplications, totalSessions, totalTests, totalUsers, totalAccounts);
            case COMPLIANCE -> buildComplianceReport(totalUsers, activeUsers, adminUsers, totalAccounts, totalApplications);
        };
    }

    private String buildSecurityReport(long totalUsers, long activeUsers, long adminUsers, long totalAccounts) {
        return "RAPPORT DE SECURITE\n" +
                "Genere le: " + LocalDateTime.now() + "\n\n" +
                "Synthese des acces et des droits utilisateurs\n" +
                "- Utilisateurs totaux: " + totalUsers + "\n" +
                "- Utilisateurs actifs: " + activeUsers + "\n" +
                "- Administrateurs: " + adminUsers + "\n" +
                "- Comptes stockes: " + totalAccounts + "\n\n" +
                "Recommandations:\n" +
                "- Verifier regulierement les comptes inactifs.\n" +
                "- Limiter les droits administrateur aux profils autorises.\n" +
                "- Auditer les acces sensibles avant chaque mise en production.\n";
    }

    private String buildAccessReport(long totalUsers, long activeUsers, long adminUsers) {
        return "JOURNAL DES ACCES\n" +
                "Genere le: " + LocalDateTime.now() + "\n\n" +
                "- Utilisateurs totaux: " + totalUsers + "\n" +
                "- Utilisateurs actifs: " + activeUsers + "\n" +
                "- Administrateurs: " + adminUsers + "\n\n" +
                "Historique disponible dans les journaux d'authentification et d'audit.\n";
    }

    private String buildTestsReport(long totalSessions, long totalTests, long testsOk, long testsBug, long testsEnCours, long totalBugs) {
        return "RAPPORT DE TESTS\n" +
                "Genere le: " + LocalDateTime.now() + "\n\n" +
                "- Sessions de test: " + totalSessions + "\n" +
                "- Tests executes: " + totalTests + "\n" +
                "- Tests OK: " + testsOk + "\n" +
                "- Tests avec bug: " + testsBug + "\n" +
                "- Tests en cours: " + testsEnCours + "\n" +
                "- Bugs declares: " + totalBugs + "\n";
    }

    private String buildPerformanceReport(long totalApplications, long totalSessions, long totalTests, long totalUsers, long totalAccounts) {
        return "PERFORMANCE GLOBALE\n" +
                "Genere le: " + LocalDateTime.now() + "\n\n" +
                "- Applications: " + totalApplications + "\n" +
                "- Sessions de test: " + totalSessions + "\n" +
                "- Tests executes: " + totalTests + "\n" +
                "- Utilisateurs: " + totalUsers + "\n" +
                "- Comptes stockes: " + totalAccounts + "\n";
    }

    private String buildComplianceReport(long totalUsers, long activeUsers, long adminUsers, long totalAccounts, long totalApplications) {
        return "RAPPORT DE CONFORMITE\n" +
                "Genere le: " + LocalDateTime.now() + "\n\n" +
                "- Applications referencees: " + totalApplications + "\n" +
                "- Utilisateurs: " + totalUsers + "\n" +
                "- Utilisateurs actifs: " + activeUsers + "\n" +
                "- Administrateurs: " + adminUsers + "\n" +
                "- Comptes audites: " + totalAccounts + "\n\n" +
                "Points de controle:\n" +
                "- Separation des roles utilisateurs et administrateurs.\n" +
                "- Suivi des comptes actifs et inactifs.\n" +
                "- Association des comptes aux applications concernees.\n";
    }

    private long safeCount(Long value) {
        return value == null ? 0 : value;
    }
}
