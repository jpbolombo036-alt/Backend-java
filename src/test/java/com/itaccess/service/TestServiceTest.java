package com.itaccess.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.itaccess.dto.TestDTO;
import com.itaccess.dto.TestRequest;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.ApplicationRepository;
import com.itaccess.repository.TestRepository;
import com.itaccess.repository.TestSessionRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private TestSessionRepository testSessionRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private TestService testService;

    private com.itaccess.entity.Test test;
    private TestDTO testDTO;
    private TestRequest testRequest;

    @BeforeEach
    void setUp() {
        test = new com.itaccess.entity.Test();
        test.setId(1L);
        test.setSessionId(10L);
        test.setApplicationId(5L);
        test.setApplicationNom("TestApp");
        test.setVersion("1.0");
        test.setEnvironnement("TEST");
        test.setFonction("Test fonction");
        test.setPrecondition("Precondition");
        test.setEtapes("Etapes");
        test.setResultatAttendu("Attendu");
        test.setResultatObtenu("Obtenu");
        test.setStatut("PASS");
        test.setCommentaires("Commentaire");
        test.setCreatedBy(1L);
        // Nouveau champ testNumber
        test.setTestNumber(2L);

        testDTO = new TestDTO();
        testDTO.setId(1L);
        testDTO.setSessionId(10L);
        testDTO.setApplicationId(5L);
        testDTO.setApplicationNom("TestApp");
        testDTO.setVersion("1.0");
        testDTO.setEnvironnement("TEST");
        testDTO.setFonction("Test fonction");
        testDTO.setPrecondition("Precondition");
        testDTO.setEtapes("Etapes");
        testDTO.setResultatAttendu("Attendu");
        testDTO.setResultatObtenu("Obtenu");
        testDTO.setStatut("PASS");
        testDTO.setCommentaires("Commentaire");
        testDTO.setCreatedBy(1L);
        // Nouveau champ testNumber
        testDTO.setTestNumber(2L);

        testRequest = new TestRequest();
        testRequest.setSessionId(10L);
        testRequest.setApplicationId(5L);
        testRequest.setApplicationNom("TestApp");
        testRequest.setVersion("1.0");
        testRequest.setEnvironnement("TEST");
        testRequest.setFonction("Test fonction");
        testRequest.setPrecondition("Precondition");
        testRequest.setEtapes("Etapes");
        testRequest.setResultatAttendu("Attendu");
        testRequest.setResultatObtenu("Obtenu");
        testRequest.setStatut("PASS");
        testRequest.setCommentaires("Commentaire");
    }

    @Test
    void getAllTests_returnsListOfTestDTOs() {
        when(testRepository.findAll()).thenReturn(Arrays.asList(test));

        List<TestDTO> result = testService.getAllTests();

        assertEquals(1, result.size());
        assertEquals(testDTO.getId(), result.get(0).getId());
        assertEquals(testDTO.getTestNumber(), result.get(0).getTestNumber());
        verify(testRepository, times(1)).findAll();
    }

    @Test
    void getTestsBySessionId_returnsListOfTestDTOs() {
        when(testRepository.findBySessionId(10L)).thenReturn(Arrays.asList(test));

        List<TestDTO> result = testService.getTestsBySessionId(10L);

        assertEquals(1, result.size());
        assertEquals(testDTO.getId(), result.get(0).getId());
        assertEquals(testDTO.getTestNumber(), result.get(0).getTestNumber());
        verify(testRepository, times(1)).findBySessionId(10L);
    }

    @Test
    void getTestById_returnsTestDTO_whenExists() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));

        TestDTO result = testService.getTestById(1L);

        assertEquals(testDTO.getId(), result.getId());
        assertEquals(testDTO.getTestNumber(), result.getTestNumber());
        verify(testRepository, times(1)).findById(1L);
    }

    @Test
    void getTestById_throwsResourceNotFoundException_whenNotExists() {
        when(testRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> testService.getTestById(99L));
        verify(testRepository, times(1)).findById(99L);
    }

    @Test
    void createTest_createsAndReturnsTestDTO() {
        // Préparer les mocks pour la logique de création
        when(applicationRepository.existsById(5L)).thenReturn(true);
        when(testSessionRepository.existsById(10L)).thenReturn(true);
        when(testRepository.findBySessionId(10L)).thenReturn(Arrays.asList()); // Liste vide -> premier test = 1
        when(testRepository.save(any(com.itaccess.entity.Test.class))).thenReturn(test);

        TestDTO result = testService.createTest(testRequest, 1L);

        assertNotNull(result);
        
        // Capturer l'objet envoyé au repository pour vérifier le testNumber calculé
        ArgumentCaptor<com.itaccess.entity.Test> testCaptor = ArgumentCaptor.forClass(com.itaccess.entity.Test.class);
        verify(testRepository).save(testCaptor.capture());
        
        com.itaccess.entity.Test savedTest = testCaptor.getValue();
        assertEquals(1L, savedTest.getTestNumber()); // Doit être 1 car findBySessionId a retourné une liste vide
        assertEquals("TestApp", savedTest.getApplicationNom());
        verify(testRepository, times(1)).save(any(com.itaccess.entity.Test.class));
    }

    @Test
    void createTest_throwsResourceNotFoundException_whenApplicationNotFound() {
        when(applicationRepository.existsById(5L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> testService.createTest(testRequest, 1L));
        verify(applicationRepository, times(1)).existsById(5L);
    }

    @Test
    void createTest_throwsResourceNotFoundException_whenSessionNotFound() {
        when(applicationRepository.existsById(5L)).thenReturn(true);
        when(testSessionRepository.existsById(10L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> testService.createTest(testRequest, 1L));
        verify(testSessionRepository, times(1)).existsById(10L);
    }

    @Test
    void createTest_throwsResourceNotFoundException_whenSessionIdIsZero() {
        testRequest.setSessionId(0L);
        
        assertThrows(ResourceNotFoundException.class, () -> testService.createTest(testRequest, 1L));
        // Aucune interaction avec les repositories attendue car la validation échoue avant
    }

    @Test
    void updateTest_updatesAndReturnsTestDTO() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(testRepository.save(any(com.itaccess.entity.Test.class))).thenReturn(test);

        TestDTO result = testService.updateTest(1L, testRequest);

        assertNotNull(result);
        assertEquals(testDTO.getId(), result.getId());
        assertEquals(testDTO.getTestNumber(), result.getTestNumber());
        verify(testRepository, times(1)).findById(1L);
        verify(testRepository, times(1)).save(any(com.itaccess.entity.Test.class));
    }

    @Test
    void updateTest_throwsResourceNotFoundException_whenNotExists() {
        when(testRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> testService.updateTest(99L, testRequest));
        verify(testRepository, times(1)).findById(99L);
    }

    @Test
    void deleteTest_deletesTest_whenExists() {
        when(testRepository.existsById(1L)).thenReturn(true);

        testService.deleteTest(1L);

        verify(testRepository, times(1)).existsById(1L);
        verify(testRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteTest_throwsResourceNotFoundException_whenNotExists() {
        when(testRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> testService.deleteTest(99L));
        verify(testRepository, times(1)).existsById(99L);
    }

    @Test
    void getNextTestNumberForSession_returnsOne_whenNoExistingTests() {
        when(testRepository.findBySessionId(10L)).thenReturn(Arrays.asList());

        Long result = testService.getNextTestNumberForSession(10L);

        assertEquals(1L, result);
    }

    @Test
    void getNextTestNumberForSession_returnsNextNumber_whenExistingTests() {
        com.itaccess.entity.Test test1 = new com.itaccess.entity.Test();
        test1.setTestNumber(1L);
        test1.setSessionId(10L);
        com.itaccess.entity.Test test2 = new com.itaccess.entity.Test();
        test2.setTestNumber(3L);
        test2.setSessionId(10L);
        when(testRepository.findBySessionId(10L)).thenReturn(Arrays.asList(test1, test2));

        Long result = testService.getNextTestNumberForSession(10L);

        assertEquals(4L, result);
    }

    @Test
    void getNextTestNumberForSession_returnsOne_whenSessionIdIsNull() {
        Long result = testService.getNextTestNumberForSession(null);

        assertEquals(1L, result);
    }

}