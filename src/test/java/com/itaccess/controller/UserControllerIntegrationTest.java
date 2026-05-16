package com.itaccess.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itaccess.dto.UserDTO;
import com.itaccess.entity.User;
import com.itaccess.repository.UserRepository;
import com.itaccess.security.JwtTokenProvider;
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
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private UserRepository userRepository;
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .hashedPassword("encodedPassword")
                .role("user")
                .isActive(true)
                .build();
    }
    
    @Test
    @WithMockUser(roles = "admin")
    void getAllUsers_ShouldReturnPageOfUsers_WhenAdmin() throws Exception {
        mockMvc.perform(get("/users")
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
    @WithMockUser(roles = "user")
    void getAllUsers_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void getCurrentUser_ShouldReturnCurrentUser_WhenAuthenticated() throws Exception {
        when(userRepository.findByUsername("user")).thenReturn(java.util.Optional.of(testUser));
        
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(roles = "admin")
    void createUser_ShouldReturnCreated_WhenValidData() throws Exception {
        UserDTO userDTO = UserDTO.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .role("user")
                .build();
        
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"));
    }
    
    @Test
    @WithMockUser(roles = "admin")
    void deleteUser_ShouldReturnNoContent_WhenUserExists() throws Exception {
        when(userRepository.existsById(1L)).thenReturn(true);
        
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());
    }
}
