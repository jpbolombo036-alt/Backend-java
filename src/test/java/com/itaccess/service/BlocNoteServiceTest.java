package com.itaccess.service;

import com.itaccess.dto.BlocNoteDTO;
import com.itaccess.dto.BlocNoteRequest;
import com.itaccess.entity.BlocNote;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.BlocNoteRepository;
import com.itaccess.security.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlocNoteServiceTest {

    @Mock
    private BlocNoteRepository blocNoteRepository;

    @InjectMocks
    private BlocNoteService blocNoteService;

    private BlocNote testBlocNote;
    private UserInfo testUser;
    private UserInfo adminUser;
    private BlocNoteRequest blocNoteRequest;

    @BeforeEach
    void setUp() {
        testBlocNote = BlocNote.builder()
                .id(1L)
                .title("Test Note")
                .content("Test content")
                .applicationId(5L)
                .sessionId(10L)
                .testId(2L)
                .status("DRAFT")
                .createdBy(1L)
                .createdByUsername("testuser")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testUser = new UserInfo(1L, "testuser", "user");

        adminUser = new UserInfo(2L, "admin", "admin");

        blocNoteRequest = BlocNoteRequest.builder()
                .title("New Note")
                .content("New content")
                .applicationId(5L)
                .sessionId(10L)
                .testId(2L)
                .status("DRAFT")
                .build();
    }

    @Test
    void getAll_returnsAllNotes_whenAdmin() {
        when(blocNoteRepository.findAll()).thenReturn(Arrays.asList(testBlocNote));

        List<BlocNoteDTO> result = blocNoteService.getAll(adminUser);

        assertEquals(1, result.size());
        assertEquals(testBlocNote.getId(), result.get(0).getId());
        verify(blocNoteRepository, times(1)).findAll();
    }

    @Test
    void getAll_returnsUserNotes_whenUser() {
        when(blocNoteRepository.findByCreatedByOrderByUpdatedAtDesc(1L)).thenReturn(Arrays.asList(testBlocNote));

        List<BlocNoteDTO> result = blocNoteService.getAll(testUser);

        assertEquals(1, result.size());
        verify(blocNoteRepository, times(1)).findByCreatedByOrderByUpdatedAtDesc(1L);
    }

    @Test
    void get_returnsBlocNoteDTO_whenExists() {
        when(blocNoteRepository.findById(1L)).thenReturn(Optional.of(testBlocNote));

        BlocNoteDTO result = blocNoteService.get(1L);

        assertEquals(testBlocNote.getId(), result.getId());
        assertEquals(testBlocNote.getTitle(), result.getTitle());
        verify(blocNoteRepository, times(1)).findById(1L);
    }

    @Test
    void get_throwsResourceNotFoundException_whenNotExists() {
        when(blocNoteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> blocNoteService.get(99L));
        verify(blocNoteRepository, times(1)).findById(99L);
    }

    @Test
    void create_returnsBlocNoteDTO() {
        when(blocNoteRepository.save(any(BlocNote.class))).thenAnswer(invocation -> {
            BlocNote saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        BlocNoteDTO result = blocNoteService.create(blocNoteRequest, testUser);

        assertNotNull(result);
        assertEquals(blocNoteRequest.getTitle(), result.getTitle());
        assertEquals(blocNoteRequest.getContent(), result.getContent());
        assertEquals(testUser.getId(), result.getCreatedBy());
        assertEquals(testUser.getUsername(), result.getCreatedByUsername());
        verify(blocNoteRepository, times(1)).save(any(BlocNote.class));
    }

    @Test
    void update_returnsBlocNoteDTO_whenAuthorized() {
        when(blocNoteRepository.findById(1L)).thenReturn(Optional.of(testBlocNote));
        when(blocNoteRepository.save(any(BlocNote.class))).thenReturn(testBlocNote);

        BlocNoteDTO result = blocNoteService.update(1L, blocNoteRequest, testUser);

        assertNotNull(result);
        verify(blocNoteRepository, times(1)).save(any(BlocNote.class));
    }

    @Test
    void update_throwsSecurityException_whenUnauthorized() {
        testBlocNote.setCreatedBy(99L);
        when(blocNoteRepository.findById(1L)).thenReturn(Optional.of(testBlocNote));

        assertThrows(SecurityException.class, () -> blocNoteService.update(1L, blocNoteRequest, testUser));
        verify(blocNoteRepository, never()).save(any(BlocNote.class));
    }

    @Test
    void update_throwsResourceNotFoundException_whenNotExists() {
        when(blocNoteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> blocNoteService.update(99L, blocNoteRequest, testUser));
    }

    @Test
    void delete_deletesNote_whenOwner() {
        when(blocNoteRepository.findById(1L)).thenReturn(Optional.of(testBlocNote));

        blocNoteService.delete(1L, testUser);

        verify(blocNoteRepository, times(1)).delete(any(BlocNote.class));
    }

    @Test
    void delete_deletesNote_whenAdmin() {
        when(blocNoteRepository.findById(1L)).thenReturn(Optional.of(testBlocNote));

        blocNoteService.delete(1L, adminUser);

        verify(blocNoteRepository, times(1)).delete(any(BlocNote.class));
    }

    @Test
    void delete_throwsSecurityException_whenUnauthorized() {
        UserInfo otherUser = new UserInfo(99L, "other", "user");
        when(blocNoteRepository.findById(1L)).thenReturn(Optional.of(testBlocNote));

        assertThrows(SecurityException.class, () -> blocNoteService.delete(1L, otherUser));
        verify(blocNoteRepository, never()).delete(any(BlocNote.class));
    }

    @Test
    void delete_throwsResourceNotFoundException_whenNotExists() {
        when(blocNoteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> blocNoteService.delete(99L, testUser));
    }
}