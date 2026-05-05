package com.itaccess.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itaccess.dto.ApplicationDTO;
import com.itaccess.entity.Application;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private ApplicationRepository applicationRepository;
    
    private Application testApplication;
    
    @BeforeEach
    void setUp() {
        testApplication = Application.builder()
                .id(1L)
                .nom("Test App")
                .description("Test Description")
                .version("1.0")
                .environnement("Production")
                .createdBy(1L)
                .build();
    }
    
    @Test
    void getAllApplications_ShouldReturnPageOfApplications_WhenAuthenticated() throws Exception {
        mockMvc.perform(get("/applications")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "id")
                .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));
    }
    
    @Test
    void getApplicationById_ShouldReturnApplication_WhenApplicationExists() throws Exception {
        when(applicationRepository.findById(1L)).thenReturn(java.util.Optional.of(testApplication));
        
        mockMvc.perform(get("/applications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Test App"))
                .andExpect(jsonPath("$.description").value("Test Description"));
    }
    
    @Test
    @WithMockUser
    void createApplication_ShouldReturnCreated_WhenValidData() throws Exception {
        ApplicationDTO applicationDTO = ApplicationDTO.builder()
                .nom("New App")
                .description("New Description")
                .version("2.0")
                .environnement("Development")
                .build();
        
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        
        mockMvc.perform(post("/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(applicationDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Test App"));
    }
    
    @Test
    @WithMockUser
    void updateApplication_ShouldReturnOk_WhenValidData() throws Exception {
        ApplicationDTO applicationDTO = ApplicationDTO.builder()
                .nom("Updated App")
                .description("Updated Description")
                .version("3.0")
                .environnement("Staging")
                .build();
        
        when(applicationRepository.findById(1L)).thenReturn(java.util.Optional.of(testApplication));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        
        mockMvc.perform(put("/applications/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(applicationDTO)))
                .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser
    void deleteApplication_ShouldReturnNoContent_WhenApplicationExists() throws Exception {
        when(applicationRepository.findById(1L)).thenReturn(java.util.Optional.of(testApplication));
        
        mockMvc.perform(delete("/applications/1"))
                .andExpect(status().isNoContent());
    }
}
