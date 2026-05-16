package com.itaccess.service;

import com.itaccess.dto.ApplicationDTO;
import com.itaccess.entity.Application;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.ApplicationRepository;
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
class ApplicationServiceTest {
    
    @Mock
    private ApplicationRepository applicationRepository;
    
    @InjectMocks
    private ApplicationService applicationService;
    
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
    void getApplicationById_ShouldReturnApplication_WhenApplicationExists() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        
        ApplicationDTO result = applicationService.getApplicationById(1L);
        
        assertNotNull(result);
        assertEquals("Test App", result.getNom());
        assertEquals("Test Description", result.getDescription());
        verify(applicationRepository, times(1)).findById(1L);
    }
    
    @Test
    void getApplicationById_ShouldThrowException_WhenApplicationNotFound() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> applicationService.getApplicationById(1L));
        verify(applicationRepository, times(1)).findById(1L);
    }
    
    @Test
    void createApplication_ShouldReturnApplication_WhenValidData() {
        ApplicationDTO applicationDTO = ApplicationDTO.builder()
                .nom("New App")
                .description("New Description")
                .version("2.0")
                .environnement("Development")
                .build();
        
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        
        ApplicationDTO result = applicationService.createApplication(applicationDTO, 1L);
        
        assertNotNull(result);
        verify(applicationRepository, times(1)).save(any(Application.class));
    }
    
    @Test
    void updateApplication_ShouldUpdateApplication_WhenAdmin() {
        ApplicationDTO applicationDTO = ApplicationDTO.builder()
                .nom("Updated App")
                .description("Updated Description")
                .version("3.0")
                .environnement("Staging")
                .build();
        
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        
        ApplicationDTO result = applicationService.updateApplication(1L, applicationDTO, 1L, "admin");
        
        assertNotNull(result);
        verify(applicationRepository, times(1)).save(any(Application.class));
    }
    
    @Test
    void updateApplication_ShouldUpdateApplication_WhenOwner() {
        testApplication.setCreatedBy(1L);
        ApplicationDTO applicationDTO = ApplicationDTO.builder()
                .nom("Updated App")
                .description("Updated Description")
                .build();
        
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        
        ApplicationDTO result = applicationService.updateApplication(1L, applicationDTO, 1L, "user");
        
        assertNotNull(result);
        verify(applicationRepository, times(1)).save(any(Application.class));
    }
    
    @Test
    void updateApplication_ShouldThrowException_WhenNotOwnerAndNotAdmin() {
        testApplication.setCreatedBy(2L);
        ApplicationDTO applicationDTO = ApplicationDTO.builder()
                .nom("Updated App")
                .build();
        
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        
        assertThrows(SecurityException.class, () -> 
            applicationService.updateApplication(1L, applicationDTO, 1L, "user"));
        verify(applicationRepository, never()).save(any(Application.class));
    }
    
    @Test
    void deleteApplication_ShouldDeleteApplication_WhenAdmin() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        
        applicationService.deleteApplication(1L, 1L, "admin");
        
        verify(applicationRepository, times(1)).deleteById(1L);
    }
    
    @Test
    void deleteApplication_ShouldDeleteApplication_WhenOwner() {
        testApplication.setCreatedBy(1L);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        
        applicationService.deleteApplication(1L, 1L, "user");
        
        verify(applicationRepository, times(1)).deleteById(1L);
    }
    
    @Test
    void deleteApplication_ShouldThrowException_WhenNotOwnerAndNotAdmin() {
        testApplication.setCreatedBy(2L);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        
        assertThrows(SecurityException.class, () -> 
            applicationService.deleteApplication(1L, 1L, "user"));
        verify(applicationRepository, never()).deleteById(anyLong());
    }
}
