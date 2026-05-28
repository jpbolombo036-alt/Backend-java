package com.itaccess.service;

import com.itaccess.dto.TestDTO;
import com.itaccess.dto.TestSessionDTO;
import com.itaccess.dto.TestSessionRequest;
import com.itaccess.entity.Application;
<<<<<<< HEAD
import com.itaccess.entity.TestStep;
=======
import com.itaccess.entity.Test;
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
import com.itaccess.entity.TestSession;
import com.itaccess.entity.User;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.ApplicationRepository;
import com.itaccess.repository.TestRepository;
import com.itaccess.repository.TestSessionRepository;
import com.itaccess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

<<<<<<< HEAD
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
=======
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestSessionService {

    private final TestSessionRepository testSessionRepository;
    private final TestRepository testRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    
<<<<<<< HEAD
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public List<TestSessionDTO> getAllTestSessions() {
        return toOptimizedDTOList(testSessionRepository.findAll());
    }
    
    public List<TestSessionDTO> getTestSessionsByUser(Long userId) {
        return toOptimizedDTOList(testSessionRepository.findByCreatedBy(userId));
=======
    public List<TestSessionDTO> getAllTestSessions() {
        return testSessionRepository.findAll().stream()
                .map(this::toDTOWithStats)
                .collect(Collectors.toList());
    }
    
    public List<TestSessionDTO> getTestSessionsByUser(Long userId) {
        return testSessionRepository.findByCreatedBy(userId).stream()
                .map(this::toDTOWithStats)
                .collect(Collectors.toList());
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
    }
    
    public TestSessionDTO getTestSessionById(Long id) {
        TestSession session = testSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session non trouvée avec l'ID: " + id));
        return toDTOWithStats(session);
    }
    
    @Transactional
    public TestSessionDTO createTestSession(TestSessionRequest request, Long createdBy) {
        TestSession session = TestSession.builder()
                .nom(request.getNom())
                .description(request.getDescription())
                .applicationId(request.getApplicationId())
                .environnement(request.getEnvironnement())
                .version(request.getVersion())
                .nomDocument(request.getNomDocument())
                .statut(request.getStatut() != null ? request.getStatut() : "En cours")
                .createdBy(createdBy)
                .build();
        
        TestSession savedSession = testSessionRepository.save(session);
        return toDTOWithStats(savedSession);
    }
    
    @Transactional
    public TestSessionDTO updateTestSession(Long id, TestSessionRequest request) {
        TestSession session = testSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session non trouvée avec l'ID: " + id));
        
        session.setNom(request.getNom());
        session.setDescription(request.getDescription());
        session.setApplicationId(request.getApplicationId());
        session.setEnvironnement(request.getEnvironnement());
        session.setVersion(request.getVersion());
        session.setNomDocument(request.getNomDocument());
        session.setStatut(request.getStatut());
        
        TestSession updatedSession = testSessionRepository.save(session);
        return toDTOWithStats(updatedSession);
    }
    
    @Transactional
    public void deleteTestSession(Long id) {
        TestSession session = testSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session non trouvée avec l'ID: " + id));
        
        testRepository.deleteBySessionId(id);
        testSessionRepository.deleteById(id);
    }
    
<<<<<<< HEAD
    private List<TestSessionDTO> toOptimizedDTOList(List<TestSession> sessions) {
        if (sessions.isEmpty()) return Collections.emptyList();

        List<Long> sessionIds = sessions.stream().map(TestSession::getId).collect(Collectors.toList());
        List<Long> appIds = sessions.stream().map(TestSession::getApplicationId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> userIds = sessions.stream().map(TestSession::getCreatedBy).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, List<TestStep>> testsBySession = testRepository.findBySessionIdIn(sessionIds).stream()
                .collect(Collectors.groupingBy(TestStep::getSessionId));
        Map<Long, String> appNames = applicationRepository.findAllById(appIds).stream()
                .collect(Collectors.toMap(Application::getId, Application::getNom));
        Map<Long, String> userNames = userRepository.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        return sessions.stream()
                .map(s -> buildDTO(s, 
                        testsBySession.getOrDefault(s.getId(), Collections.emptyList()),
                        appNames.get(s.getApplicationId()),
                        userNames.get(s.getCreatedBy())))
                .collect(Collectors.toList());
    }

    private TestSessionDTO toDTOWithStats(TestSession session) {
        List<TestStep> tests = testRepository.findBySessionId(session.getId());
        String appName = session.getApplicationId() != null ? 
                applicationRepository.findById(session.getApplicationId()).map(Application::getNom).orElse(null) : null;
        String userName = session.getCreatedBy() != null ? 
                userRepository.findById(session.getCreatedBy()).map(User::getUsername).orElse(null) : null;
        
        return buildDTO(session, tests, appName, userName);
    }

    private TestSessionDTO buildDTO(TestSession session, List<TestStep> tests, String appNom, String userNom) {
=======
    private TestSessionDTO toDTOWithStats(TestSession session) {
        List<Test> tests = testRepository.findBySessionId(session.getId());

        String applicationNom = null;
        if (session.getApplicationId() != null) {
            applicationNom = applicationRepository.findById(session.getApplicationId())
                    .map(Application::getNom)
                    .orElse(null);
        }

        String createdByUsername = null;
        if (session.getCreatedBy() != null) {
            createdByUsername = userRepository.findById(session.getCreatedBy())
                    .map(User::getUsername)
                    .orElse(null);
        }

>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
        long testsOk = tests.stream().filter(t -> "OK".equals(t.getStatut())).count();
        long testsBug = tests.stream().filter(t -> "BUG".equals(t.getStatut())).count();
        long testsEnCours = tests.stream().filter(t -> "EN COURS".equals(t.getStatut())).count();

        return TestSessionDTO.builder()
                .id(session.getId())
                .nom(session.getNom())
                .description(session.getDescription())
                .applicationId(session.getApplicationId())
<<<<<<< HEAD
                .applicationNom(appNom)
=======
                .applicationNom(applicationNom)
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
                .environnement(session.getEnvironnement())
                .version(session.getVersion())
                .nomDocument(session.getNomDocument())
                .dateCreation(session.getDateCreation())
                .statut(session.getStatut())
                .createdBy(session.getCreatedBy())
<<<<<<< HEAD
                .createdByUsername(userNom)
=======
                .createdByUsername(createdByUsername)
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
                .tests(tests.stream().map(this::toTestDTO).collect(Collectors.toList()))
                .totalTests(tests.size())
                .testsOk((int) testsOk)
                .testsBug((int) testsBug)
                .testsEnCours((int) testsEnCours)
                .build();
    }
    
<<<<<<< HEAD
    private TestDTO toTestDTO(TestStep test) {
=======
    private TestDTO toTestDTO(Test test) {
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
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
                .build();
    }
}