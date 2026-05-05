package com.itaccess.service;

import com.itaccess.dto.ApplicationDTO;
import com.itaccess.dto.PageResponse;
import com.itaccess.entity.Application;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {
    
    private final ApplicationRepository applicationRepository;
    
    public PageResponse<ApplicationDTO> getAllApplications(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Application> applicationPage = applicationRepository.findAll(pageable);
        
        List<ApplicationDTO> content = applicationPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return PageResponse.of(content, applicationPage.getNumber(), applicationPage.getSize(), applicationPage.getTotalElements());
    }
    
    public ApplicationDTO getApplicationById(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application non trouvée avec l'ID: " + id));
        return toDTO(application);
    }
    
    @Transactional
    public ApplicationDTO createApplication(ApplicationDTO dto, Long createdBy) {
        Application application = Application.builder()
                .nom(dto.getNom())
                .description(dto.getDescription())
                .version(dto.getVersion())
                .environnement(dto.getEnvironnement())
                .createdBy(createdBy)
                .build();
        
        Application savedApplication = applicationRepository.save(application);
        return toDTO(savedApplication);
    }
    
    @Transactional
    public ApplicationDTO updateApplication(Long id, ApplicationDTO dto, Long userId, String userRole) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application non trouvée avec l'ID: " + id));
        
        if (!"admin".equals(userRole) && !application.getCreatedBy().equals(userId)) {
            throw new SecurityException("Non autorisé à mettre à jour cette application");
        }
        
        application.setNom(dto.getNom());
        application.setDescription(dto.getDescription());
        application.setVersion(dto.getVersion());
        application.setEnvironnement(dto.getEnvironnement());
        
        Application updatedApplication = applicationRepository.save(application);
        return toDTO(updatedApplication);
    }
    
    @Transactional
    public void deleteApplication(Long id, Long userId, String userRole) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application non trouvée avec l'ID: " + id));
        
        if (!"admin".equals(userRole) && !application.getCreatedBy().equals(userId)) {
            throw new SecurityException("Non autorisé à supprimer cette application");
        }
        
        applicationRepository.deleteById(id);
    }
    
    private ApplicationDTO toDTO(Application application) {
        return ApplicationDTO.builder()
                .id(application.getId())
                .nom(application.getNom())
                .description(application.getDescription())
                .version(application.getVersion())
                .environnement(application.getEnvironnement())
                .dateCreation(application.getDateCreation() != null ? application.getDateCreation().toString() : null)
                .createdBy(application.getCreatedBy())
                .build();
    }
}