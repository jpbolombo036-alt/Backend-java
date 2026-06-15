package com.itaccess.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itaccess.dto.BlocNoteDTO;
import com.itaccess.dto.BlocNoteRequest;
import com.itaccess.entity.BlocNote;
import com.itaccess.entity.User;
import com.itaccess.repository.BlocNoteRepository;
import com.itaccess.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BlocNoteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BlocNoteRepository blocNoteRepository;

    @MockBean
    private UserRepository userRepository;

    private BlocNote testBlocNote;
    private BlocNoteRequest blocNoteRequest;

    @BeforeEach
    void setUp() {
        testBlocNote = BlocNote.builder()
                .id(1L)
                .title("Test Note Title")
                .content("Test content for QA note")
                .applicationId(5L)
                .sessionId(10L)
                .testId(2L)
                .status("DRAFT")
                .createdBy(1L)
                .createdByUsername("testuser")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        blocNoteRequest = BlocNoteRequest.builder()
                .title("New Note Title")
                .content("New note content")
                .applicationId(5L)
                .sessionId(10L)
                .testId(2L)
                .status("DRAFT")
                .build();

        User testUser = User.builder()
                .id(1L)
                .username("testuser")
                .role("user")
                .isActive(true)
                .build();

        User adminUser = User.builder()
                .id(2L)
                .username("admin")
                .role("admin")
                .isActive(true)
                .build();

        User otherUser = User.builder()
                .id(99L)
                .username("otheruser")
                .role("user")
                .isActive(true)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
        when(userRepository.findByUsername("admin")).thenReturn(java.util.Optional.of(adminUser));
        when(userRepository.findByUsername("otheruser")).thenReturn(java.util.Optional.of(otherUser));
    }

    @BeforeEach
    void injectCurrentUser() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null && !"anonymousUser".equals(context.getAuthentication().getPrincipal())) {
            String username = context.getAuthentication().getName();
            String role = context.getAuthentication().getAuthorities().stream()
                    .findFirst()
                    .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                    .orElse("user");
            Long id = "admin".equals(username) ? 2L : "otheruser".equals(username) ? 99L : 1L;
            when(userRepository.findByUsername(username))
                    .thenReturn(java.util.Optional.of(User.builder()
                            .id(id)
                            .username(username)
                            .role(role)
                            .isActive(true)
                            .build()));
        }
    }

    @Test
    @WithMockUser(username = "testuser", roles = "user")
    void getAllNotes_ShouldReturnListOfNotes() throws Exception {
        when(blocNoteRepository.findByCreatedByOrderByUpdatedAtDesc(1L)).thenReturn(Arrays.asList(testBlocNote));

        mockMvc.perform(get("/bloc-notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Note Title"))
                .andExpect(jsonPath("$[0].content").value("Test content for QA note"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = "admin")
    void getAllNotes_ShouldReturnAllNotes_WhenAdmin() throws Exception {
        when(blocNoteRepository.findAll()).thenReturn(Arrays.asList(testBlocNote));

        mockMvc.perform(get("/bloc-notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "user")
    void getNoteById_ShouldReturnNote_WhenExists() throws Exception {
        when(blocNoteRepository.findById(1L)).thenReturn(Optional.of(testBlocNote));

        mockMvc.perform(get("/bloc-notes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Note Title"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "user")
    void getNoteById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(blocNoteRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/bloc-notes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "user")
    void createNote_ShouldReturnCreated_WhenValidData() throws Exception {
        when(blocNoteRepository.save(any(BlocNote.class))).thenAnswer(invocation -> {
            BlocNote saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedBy(1L);
            saved.setCreatedByUsername("testuser");
            return saved;
        });

        mockMvc.perform(post("/bloc-notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blocNoteRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("New Note Title"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "user")
    void updateNote_ShouldReturnOk_WhenValidData() throws Exception {
        when(blocNoteRepository.findById(1L)).thenReturn(Optional.of(testBlocNote));
        when(blocNoteRepository.save(any(BlocNote.class))).thenReturn(testBlocNote);

        mockMvc.perform(put("/bloc-notes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blocNoteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "user")
    void deleteNote_ShouldReturnNoContent_WhenExists() throws Exception {
        when(blocNoteRepository.findById(1L)).thenReturn(Optional.of(testBlocNote));

        mockMvc.perform(delete("/bloc-notes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "user")
    void deleteNote_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(blocNoteRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/bloc-notes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "otheruser", roles = "user")
    void updateNote_ShouldReturnForbidden_WhenNotOwner() throws Exception {
        when(blocNoteRepository.findById(1L)).thenReturn(Optional.of(testBlocNote));

        mockMvc.perform(put("/bloc-notes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blocNoteRequest)))
                .andExpect(status().isForbidden());
    }
}