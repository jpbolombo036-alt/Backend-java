package com.itaccess.service;

import com.itaccess.dto.TestDTO;
import com.itaccess.dto.TestRequest;
import com.itaccess.entity.TestStep;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.ApplicationRepository;
import com.itaccess.repository.TestRepository;
import com.itaccess.repository.TestSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final TestSessionRepository testSessionRepository;
    private final ApplicationRepository applicationRepository;

    public List<TestDTO> getAllTests() {
        return testRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<TestDTO> getTestsBySessionId(Long sessionId) {
        return testRepository.findBySessionId(sessionId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TestDTO getTestById(Long id) {
        TestStep test = testRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test non trouvé avec l'ID: " + id));
        return toDTO(test);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TestDTO createTest(TestRequest request, Long createdBy) {
        if (request.getFonction() == null || request.getFonction().trim().isEmpty()) {
            throw new IllegalArgumentException("La fonction est requise");
        }
        if (request.getStatut() == null || request.getStatut().trim().isEmpty()) {
            throw new IllegalArgumentException("Le statut est requis");
        }

        if (request.getApplicationId() != null && request.getApplicationId() != 0) {
            if (!applicationRepository.existsById(request.getApplicationId())) {
                throw new ResourceNotFoundException("Application non trouvée avec l'ID: " + request.getApplicationId());
            }
        }

        if (request.getSessionId() != null) {
            if (request.getSessionId() == 0) {
                throw new IllegalArgumentException("ID de session invalide: 0");
            }
            if (!testSessionRepository.existsById(request.getSessionId())) {
                throw new ResourceNotFoundException("Session non trouvée avec l'ID: " + request.getSessionId());
            }
        }

        Long appId = (request.getApplicationId() != null && request.getApplicationId() != 0) ? request.getApplicationId() : null;

        Long nextTestNumber = getNextTestNumberForSession(request.getSessionId());

        TestStep test = TestStep.builder()
                .sessionId(request.getSessionId())
                .testNumber(nextTestNumber)
                .applicationId(appId)
                .applicationNom(request.getApplicationNom())
                .version(request.getVersion())
                .environnement(request.getEnvironnement())
                .fonction(request.getFonction())
                .precondition(request.getPrecondition())
                .etapes(request.getEtapes())
                .resultatAttendu(request.getResultatAttendu())
                .resultatObtenu(request.getResultatObtenu())
                .statut(request.getStatut())
                .commentaires(request.getCommentaires())
                .createdBy(createdBy)
                .resolved(request.getResolved() != null ? request.getResolved() : false)
                .build();

        TestStep savedTest = testRepository.save(test);
        return toDTO(savedTest);
    }

    @Transactional
    public TestDTO updateTest(Long id, TestRequest request, Long userId, String userRole) {
        TestStep test = testRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test non trouvé avec l'ID: " + id));

        if (!"admin".equals(userRole) && !test.getCreatedBy().equals(userId)) {
            throw new SecurityException("Non autorisé à modifier ce test");
        }

        if (request.getSessionId() != null) test.setSessionId(request.getSessionId());
        if (request.getApplicationId() != null) test.setApplicationId(request.getApplicationId());
        if (request.getApplicationNom() != null) test.setApplicationNom(request.getApplicationNom());
        if (request.getVersion() != null) test.setVersion(request.getVersion());
        if (request.getEnvironnement() != null) test.setEnvironnement(request.getEnvironnement());
        if (request.getFonction() != null) test.setFonction(request.getFonction());
        if (request.getPrecondition() != null) test.setPrecondition(request.getPrecondition());
        if (request.getEtapes() != null) test.setEtapes(request.getEtapes());
        if (request.getResultatAttendu() != null) test.setResultatAttendu(request.getResultatAttendu());
        if (request.getResultatObtenu() != null) test.setResultatObtenu(request.getResultatObtenu());
        if (request.getStatut() != null) test.setStatut(request.getStatut());
        if (request.getCommentaires() != null) test.setCommentaires(request.getCommentaires());
        if (request.getResolved() != null) test.setResolved(request.getResolved());

        TestStep updatedTest = testRepository.save(test);
        return toDTO(updatedTest);
    }

    @Transactional
    public void deleteTest(Long id, Long userId, String userRole) {
        TestStep test = testRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test non trouvé avec l'ID: " + id));

        if (!"admin".equals(userRole) && !test.getCreatedBy().equals(userId)) {
            throw new SecurityException("Non autorisé à supprimer ce test");
        }
        
        testRepository.delete(test);
    }

    /**
     * Récupère le prochain numéro de test pour une session donnée
     * Les tests commencent à 1 pour chaque nouvelle session
     */
    public Long getNextTestNumberForSession(Long sessionId) {
        // Si sessionId est null, on retourne 1 (pas de session spécifique)
        if (sessionId == null) {
            return 1L;
        }

        // Utilisation d'une approche plus fiable par requête directe au repository
        // Cela évite de charger tous les tests en mémoire
        return testRepository.findMaxTestNumberBySessionId(sessionId)
                .map(max -> max + 1)
                .orElse(1L);
    }

    private TestDTO toDTO(TestStep test) {
        return TestDTO.builder()
                .id(test.getId())
                .sessionId(test.getSessionId())
                .applicationId(test.getApplicationId())
                .applicationNom(test.getApplicationNom())
                .version(test.getVersion())
                .environnement(test.getEnvironnement())
                .fonction(test.getFonction())
                .precondition(test.getPrecondition())
                .etapes(test.getEtapes())
                .resultatAttendu(test.getResultatAttendu())
                .resultatObtenu(test.getResultatObtenu())
                .statut(test.getStatut())
                .commentaires(test.getCommentaires())
                .createdBy(test.getCreatedBy())
                .testNumber(test.getTestNumber())
                .resolved(test.getResolved())
                .build();
    }
}