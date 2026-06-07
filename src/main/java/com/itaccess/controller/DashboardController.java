package com.itaccess.controller;

import com.itaccess.dto.DashboardStatsDTO;
import com.itaccess.repository.ApplicationRepository;
import com.itaccess.repository.TestSessionRepository;
import com.itaccess.repository.TestRepository;
import com.itaccess.repository.UserRepository;
import com.itaccess.repository.CompteRepository;
import com.itaccess.repository.BugRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ApplicationRepository applicationRepository;
    private final TestSessionRepository testSessionRepository;
    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final CompteRepository compteRepository;
    private final BugRepository bugRepository;

    @GetMapping("/stats")
    public DashboardStatsDTO getStats() {
        long totalApplications = applicationRepository.count();
        long totalSessions = testSessionRepository.count();
        long totalTests = testRepository.count();
        long totalUsers = userRepository.countByRole("USER");
        long totalAccounts = compteRepository.count();
        long testsOk = testRepository.countByStatut("OK");
        long testsBug = testRepository.countByStatut("BUG");
        long testsEnCours = testRepository.countByStatut("EN COURS");
        long totalBugReports = bugRepository.count();

        int totalTestsCount = (int) totalTests;
        int rateOk = totalTestsCount > 0 ? (int) ((testsOk * 100) / totalTestsCount) : 0;
        int rateBug = totalTestsCount > 0 ? (int) ((testsBug * 100) / totalTestsCount) : 0;
        int ratePending = totalTestsCount > 0 ? (int) ((testsEnCours * 100) / totalTestsCount) : 0;

        return DashboardStatsDTO.builder()
                .applications((int) totalApplications)
                .sessions((int) totalSessions)
                .tests(totalTestsCount)
                .users((int) totalUsers)
                .accounts((int) totalAccounts)
                .testsOk((int) testsOk)
                .testsBug((int) testsBug)
                .testsEnCours((int) testsEnCours)
                .testsRateOk(rateOk)
                .testsRateBug(rateBug)
                .testsRatePending(ratePending)
                .activeAccounts((int) totalAccounts)
                .recentSessions((int) totalSessions)
                .bugReports((int) totalBugReports)
                .build();
    }
}