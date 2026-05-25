package com.itaccess.service;

import com.itaccess.dto.TestDTO;
import com.itaccess.dto.TestRequest;
import com.itaccess.entity.Test;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.ApplicationRepository;
import com.itaccess.repository.TestRepository;
import com.itaccess.repository.TestSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test non trouvé avec l'ID: " + id));
        return toDTO(test);
    }
    
    @Transactional
    public TestDTO createTest(TestRequest request, Long createdBy) {
        if (request.getApplicationId() != null && request.getApplicationId() != 0) {
            if (!applicationRepository.existsById(request.getApplicationId())) {
                throw new ResourceNotFoundException("Application non trouvée avec l'ID: " + request.getApplicationId());
            }
        }
        
        if (request.getSessionId() != null) {
            if (!testSessionRepository.existsById(request.getSessionId())) {
                throw new ResourceNotFoundException("Session non trouvée avec l'ID: " + request.getSessionId());
            }
        }
        
        Long appId = (request.getApplicationId() != null && request.getApplicationId() != 0) ? request.getApplicationId() : null;
        
        // Récupérer le prochain ID de test pour cette session (commence à 1 pour chaque session)
        Long nextTestId = getNextTestIdForSession(request.getSessionId());
        
        Test test = Test.builder()
                .id(nextTestId) // Forcer l'ID pour cette session
                .sessionId(request.getSessionId())
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
                .build();
        
        Test savedTest = testRepository.save(test);
        return toDTO(savedTest);
    }
    
    @Transactional
    public TestDTO updateTest(Long id, TestRequest request) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test non trouvé avec l'ID: " + id));
        
        test.setSessionId(request.getSessionId());
        test.setApplicationId(request.getApplicationId());
        test.setApplicationNom(request.getApplicationNom());
        test.setVersion(request.getVersion());
        test.setEnvironnement(request.getEnvironnement());
        test.setFonction(request.getFonction());
        test.setPrecondition(request.getPrecondition());
        test.setEtapes(request.getEtapes());
        test.setResultatAttendu(request.getResultatAttendu());
        test.setResultatObtenu(request.getResultatObtenu());
        test.setStatut(request.getStatut());
        test.setCommentaires(request.getCommentaires());
        
        Test updatedTest = testRepository.save(test);
        return toDTO(updatedTest);
    }
    
    @Transactional
    public void deleteTest(Long id) {
        if (!testRepository.existsById(id)) {
            throw new ResourceNotFoundException("Test non trouvé avec l'ID: " + id);
        }
        testRepository.deleteById(id);
    }
    
    /**
     * Récupère le prochain ID de test pour une session donnée
     * Les tests commencent à 1 pour chaque nouvelle session
     */
    private Long getNextTestIdForSession(Long sessionId) {
        // Récupérer tous les tests existants pour cette session
        List<Test> existingTests = testRepository.findBySessionId(sessionId);
        
        if (existingTests.isEmpty()) {
            return 1L; // Premier test de la session
        }
        
        // Trouver l'ID maximum et ajouter 1
        Long maxId = existingTests.stream()
                .mapToLong(Test::getId)
                .max()
                .orElse(0L);
        
        return maxId + 1;
    }
    
    private TestDTO toDTO(Test test) {
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
                .build();
    }
}