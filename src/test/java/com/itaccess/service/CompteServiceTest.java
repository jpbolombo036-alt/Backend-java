package com.itaccess.service;

import com.itaccess.dto.CompteDTO;
import com.itaccess.dto.CompteRequest;
import com.itaccess.entity.Application;
import com.itaccess.entity.Compte;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.ApplicationRepository;
import com.itaccess.repository.CompteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompteServiceTest {
    
    @Mock
    private CompteRepository compteRepository;
    
    @Mock
    private ApplicationRepository applicationRepository;
    
    @InjectMocks
    private CompteService compteService;
    
    private Compte testCompte;
    private Application testApplication;
    
    @BeforeEach
    void setUp() {
        testApplication = Application.builder()
                .id(1L)
                .nom("Test App")
                .build();
        
        testCompte = Compte.builder()
                .id(1L)
                .applicationId(1L)
                .username("testuser")
                .code("secret123")
                .role("admin")
                .commentaire("Test compte")
                .createdBy(1L)
                .build();
    }
    
    @Test
    void getCompteById_ShouldReturnCompte_WhenCompteExists() {
        when(compteRepository.findById(1L)).thenReturn(Optional.of(testCompte));
        
        CompteDTO result = compteService.getCompteById(1L);
        
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("admin", result.getRole());
        verify(compteRepository, times(1)).findById(1L);
    }
    
    @Test
    void getCompteById_ShouldThrowException_WhenCompteNotFound() {
        when(compteRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> compteService.getCompteById(1L));
        verify(compteRepository, times(1)).findById(1L);
    }
    
    @Test
    void createCompte_ShouldReturnCompte_WhenValidData() {
        CompteRequest request = CompteRequest.builder()
                .applicationId(1L)
                .username("newuser")
                .code("newsecret")
                .role("user")
                .commentaire("New compte")
                .build();
        
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(compteRepository.save(any(Compte.class))).thenReturn(testCompte);
        
        CompteDTO result = compteService.createCompte(request, 1L);
        
        assertNotNull(result);
        verify(compteRepository, times(1)).save(any(Compte.class));
    }
    
    @Test
    void createCompte_ShouldThrowException_WhenApplicationNotFound() {
        CompteRequest request = CompteRequest.builder()
                .applicationId(999L)
                .username("newuser")
                .code("newsecret")
                .build();
        
        when(applicationRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> compteService.createCompte(request, 1L));
        verify(compteRepository, never()).save(any(Compte.class));
    }
    
    @Test
    void updateCompte_ShouldUpdateCompte_WhenAdmin() {
        CompteRequest request = CompteRequest.builder()
                .applicationId(1L)
                .username("updateduser")
                .code("updatedsecret")
                .role("superadmin")
                .commentaire("Updated compte")
                .build();
        
        when(compteRepository.findById(1L)).thenReturn(Optional.of(testCompte));
        when(compteRepository.save(any(Compte.class))).thenReturn(testCompte);
        
        CompteDTO result = compteService.updateCompte(1L, request, 1L, "admin");
        
        assertNotNull(result);
        verify(compteRepository, times(1)).save(any(Compte.class));
    }
    
    @Test
    void updateCompte_ShouldUpdateCompte_WhenOwner() {
        testCompte.setCreatedBy(1L);
        CompteRequest request = CompteRequest.builder()
                .username("updateduser")
                .commentaire("Updated compte")
                .build();
        
        when(compteRepository.findById(1L)).thenReturn(Optional.of(testCompte));
        when(compteRepository.save(any(Compte.class))).thenReturn(testCompte);
        
        CompteDTO result = compteService.updateCompte(1L, request, 1L, "user");
        
        assertNotNull(result);
        verify(compteRepository, times(1)).save(any(Compte.class));
    }
    
    @Test
    void updateCompte_ShouldThrowException_WhenNotOwnerAndNotAdmin() {
        testCompte.setCreatedBy(2L);
        CompteRequest request = CompteRequest.builder()
                .username("updateduser")
                .build();
        
        when(compteRepository.findById(1L)).thenReturn(Optional.of(testCompte));
        
        assertThrows(SecurityException.class, () -> 
            compteService.updateCompte(1L, request, 1L, "user"));
        verify(compteRepository, never()).save(any(Compte.class));
    }
    
    @Test
    void deleteCompte_ShouldDeleteCompte_WhenAdmin() {
        when(compteRepository.findById(1L)).thenReturn(Optional.of(testCompte));
        
        compteService.deleteCompte(1L, 1L, "admin");
        
        verify(compteRepository, times(1)).deleteById(1L);
    }
    
    @Test
    void deleteCompte_ShouldDeleteCompte_WhenOwner() {
        testCompte.setCreatedBy(1L);
        when(compteRepository.findById(1L)).thenReturn(Optional.of(testCompte));
        
        compteService.deleteCompte(1L, 1L, "user");
        
        verify(compteRepository, times(1)).deleteById(1L);
    }
    
    @Test
    void deleteCompte_ShouldThrowException_WhenNotOwnerAndNotAdmin() {
        testCompte.setCreatedBy(2L);
        when(compteRepository.findById(1L)).thenReturn(Optional.of(testCompte));
        
        assertThrows(SecurityException.class, () -> 
            compteService.deleteCompte(1L, 1L, "user"));
        verify(compteRepository, never()).deleteById(anyLong());
    }
}
