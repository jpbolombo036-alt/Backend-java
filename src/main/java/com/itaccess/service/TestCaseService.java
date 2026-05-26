package com.itaccess.service;

import com.itaccess.entity.TestCase;
import com.itaccess.entity.TestStep;
import com.itaccess.repository.TestCaseRepository;
import com.itaccess.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final TestRepository testRepository;

    /**
     * Clone un scénario de test (TestCase) vers une session active (TestSession)
     * Transforme les étapes théoriques en étapes d'exécution (TestStep)
     */
    @Transactional
    public void instantiateTestCaseInSession(Long testCaseId, Long sessionId, Long createdBy) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new RuntimeException("Scénario non trouvé"));

        // Pour chaque étape définie dans le scénario, on crée une ligne d'exécution
        List<TestStep> stepsToExecute = testCase.getSteps().stream()
                .map(templateStep -> TestStep.builder()
                        .testCaseId(testCaseId)
                        .sessionId(sessionId)
                        .applicationId(testCase.getApplicationId())
                        .fonction(templateStep.getFonction())
                        .precondition(templateStep.getPrecondition())
                        .etapes(templateStep.getEtapes())
                        .resultatAttendu(templateStep.getResultatAttendu())
                        .statut("EN COURS")
                        .createdBy(createdBy)
                        .testNumber(templateStep.getTestNumber())
                        .build())
                .collect(Collectors.toList());

        testRepository.saveAll(stepsToExecute);
    }

    public List<TestCase> getByApplication(Long applicationId) {
        return testCaseRepository.findByApplicationId(applicationId);
    }

    public TestCase createTestCase(TestCase testCase) {
        return testCaseRepository.save(testCase);
    }
}