package com.itaccess.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itaccess.dto.TestDTO;
import com.itaccess.dto.TestRequest;
import com.itaccess.repository.TestRepository;
import com.itaccess.repository.TestSessionRepository;
import com.itaccess.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TestRepository testRepository;

    @MockBean
    private TestSessionRepository testSessionRepository;

    @MockBean
    private ApplicationRepository applicationRepository;

<<<<<<< HEAD
    private com.itaccess.entity.TestStep testStep;
=======
    private com.itaccess.entity.Test test;
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
    private TestDTO testDTO;
    private TestRequest testRequest;

    @BeforeEach
    void setUp() {
<<<<<<< HEAD
        testStep = com.itaccess.entity.TestStep.builder()
=======
        test = com.itaccess.entity.Test.builder()
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
                .id(1L)
                .sessionId(10L)
                .applicationId(5L)
                .applicationNom("TestApp")
                .version("1.0")
                .environnement("TEST")
                .fonction("Test fonction")
                .precondition("Precondition")
                .etapes("Etapes")
                .resultatAttendu("Attendu")
                .resultatObtenu("Obtenu")
                .statut("PASS")
                .commentaires("Commentaire")
                .createdBy(1L)
                .testNumber(2L) // Nouveau champ
                .build();

        testDTO = TestDTO.builder()
                .id(1L)
                .sessionId(10L)
                .applicationId(5L)
                .applicationNom("TestApp")
                .version("1.0")
                .environnement("TEST")
                .fonction("Test fonction")
                .precondition("Precondition")
                .etapes("Etapes")
                .resultatAttendu("Attendu")
                .resultatObtenu("Obtenu")
                .statut("PASS")
                .commentaires("Commentaire")
                .createdBy(1L)
                .testNumber(2L) // Nouveau champ
                .build();

        testRequest = TestRequest.builder()
                .sessionId(10L)
                .applicationId(5L)
                .applicationNom("TestApp")
                .version("1.0")
                .environnement("TEST")
                .fonction("Test fonction")
                .precondition("Precondition")
                .etapes("Etapes")
                .resultatAttendu("Attendu")
                .resultatObtenu("Obtenu")
                .statut("PASS")
                .commentaires("Commentaire")
                .build();
    }

    @Test
<<<<<<< HEAD
    @WithMockUser(username = "admin", roles = "admin")
    void getAllTests_ShouldReturnListOfTests() throws Exception {
        when(testRepository.findAll()).thenReturn(Arrays.asList(testStep));
=======
    @WithMockUser(roles = "user")
    void getAllTests_ShouldReturnListOfTests() throws Exception {
        when(testRepository.findAll()).thenReturn(Arrays.asList(test));
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0

        mockMvc.perform(get("/tests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].testNumber").value(2)) // Nouveau champ
                .andExpect(jsonPath("$[0].sessionId").value(10))
                .andExpect(jsonPath("$[0].applicationId").value(5))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
<<<<<<< HEAD
    @WithMockUser(username = "admin", roles = "admin")
    void getTestsBySessionId_ShouldReturnFilteredTests() throws Exception {
        when(testRepository.findBySessionId(10L)).thenReturn(Arrays.asList(testStep));
=======
    @WithMockUser(roles = "user")
    void getTestsBySessionId_ShouldReturnFilteredTests() throws Exception {
        when(testRepository.findBySessionId(10L)).thenReturn(Arrays.asList(test));
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0

        mockMvc.perform(get("/tests")
                .param("sessionId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].testNumber").value(2)) // Nouveau champ
                .andExpect(jsonPath("$[0].sessionId").value(10));
    }

    @Test
<<<<<<< HEAD
    @WithMockUser(username = "admin", roles = "admin")
    void getTestById_ShouldReturnTest_WhenExists() throws Exception {
        when(testRepository.findById(1L)).thenReturn(Optional.of(testStep));
=======
    @WithMockUser(roles = "user")
    void getTestById_ShouldReturnTest_WhenExists() throws Exception {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0

        mockMvc.perform(get("/tests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.testNumber").value(2)) // Nouveau champ
                .andExpect(jsonPath("$.sessionId").value(10));
    }

    @Test
<<<<<<< HEAD
    @WithMockUser(username = "admin", roles = "admin")
=======
    @WithMockUser(roles = "user")
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
    void getTestById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(testRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/tests/99"))
                .andExpect(status().isNotFound());
    }

    @Test
<<<<<<< HEAD
    @WithMockUser(username = "admin", roles = "admin")
=======
    @WithMockUser(roles = "user")
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
    void createTest_ShouldReturnCreated_WhenValidData() throws Exception {
        when(applicationRepository.existsById(5L)).thenReturn(true);
        when(testSessionRepository.existsById(10L)).thenReturn(true);
        // Mock du save pour retourner l'entité avec un ID généré
<<<<<<< HEAD
        when(testRepository.save(any(com.itaccess.entity.TestStep.class))).thenAnswer(invocation -> {
            com.itaccess.entity.TestStep saved = invocation.getArgument(0);
=======
        when(testRepository.save(any(com.itaccess.entity.Test.class))).thenAnswer(invocation -> {
            com.itaccess.entity.Test saved = invocation.getArgument(0);
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
            saved.setId(1L); // Simuler l'ID généré par la BD
            return saved;
        });

        mockMvc.perform(post("/tests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.testNumber").value(1)) // Premier test de la session
                .andExpect(jsonPath("$.sessionId").value(10));
    }

    @Test
<<<<<<< HEAD
    @WithMockUser(username = "admin", roles = "admin")
    void createTest_ShouldReturnBadRequest_WhenSessionIdIsZero() throws Exception {
        testRequest.setSessionId(0L);
        when(applicationRepository.existsById(5L)).thenReturn(true);
=======
    @WithMockUser(roles = "user")
    void createTest_ShouldReturnBadRequest_WhenSessionIdIsZero() throws Exception {
        testRequest.setSessionId(0L);
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0

        mockMvc.perform(post("/tests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
<<<<<<< HEAD
    @WithMockUser(username = "admin", roles = "admin")
    void updateTest_ShouldReturnOk_WhenValidData() throws Exception {
        when(testRepository.findById(1L)).thenReturn(Optional.of(testStep));
        when(testRepository.save(any(com.itaccess.entity.TestStep.class))).thenReturn(testStep);
=======
    @WithMockUser(roles = "user")
    void updateTest_ShouldReturnOk_WhenValidData() throws Exception {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(testRepository.save(any(com.itaccess.entity.Test.class))).thenReturn(test);
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0

        mockMvc.perform(put("/tests/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.testNumber").value(2));
    }

    @Test
<<<<<<< HEAD
    @WithMockUser(username = "admin", roles = "admin")
    void deleteTest_ShouldReturnNoContent_WhenExists() throws Exception {
        when(testRepository.findById(1L)).thenReturn(Optional.of(testStep));
=======
    @WithMockUser(roles = "user")
    void deleteTest_ShouldReturnNoContent_WhenExists() throws Exception {
        when(testRepository.existsById(1L)).thenReturn(true);
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0

        mockMvc.perform(delete("/tests/1"))
                .andExpect(status().isNoContent());
    }

    @Test
<<<<<<< HEAD
    @WithMockUser(username = "admin", roles = "admin")
    void deleteTest_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(testRepository.findById(99L)).thenReturn(Optional.empty());
=======
    @WithMockUser(roles = "user")
    void deleteTest_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(testRepository.existsById(99L)).thenReturn(false);
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0

        mockMvc.perform(delete("/tests/99"))
                .andExpect(status().isNotFound());
    }
}