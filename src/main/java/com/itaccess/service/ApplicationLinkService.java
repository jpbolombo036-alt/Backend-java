package com.itaccess.service;

import com.itaccess.dto.ApplicationLinkDTO;
import com.itaccess.dto.PageResponse;
import com.itaccess.entity.Application;
import com.itaccess.entity.ApplicationLink;
import com.itaccess.exception.ResourceNotFoundException;
import com.itaccess.repository.ApplicationLinkRepository;
import com.itaccess.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationLinkService {

    private final ApplicationLinkRepository applicationLinkRepository;
    private final ApplicationRepository applicationRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public PageResponse<ApplicationLinkDTO> getAllApplicationLinks(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ApplicationLink> applicationLinkPage = applicationLinkRepository.findAll(pageable);

        List<ApplicationLinkDTO> content = applicationLinkPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResponse.of(content, applicationLinkPage.getNumber(), applicationLinkPage.getSize(), applicationLinkPage.getTotalElements());
    }

    public List<ApplicationLinkDTO> getApplicationLinksByApplicationId(Long applicationId) {
        return applicationLinkRepository.findByApplicationId(applicationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ApplicationLinkDTO getApplicationLinkById(Long id) {
        ApplicationLink applicationLink = applicationLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lien d'application non trouvé avec l'ID: " + id));
        return toDTO(applicationLink);
    }

    @Transactional
    public ApplicationLinkDTO createApplicationLink(ApplicationLinkDTO dto, Long createdBy) {
        Application application = applicationRepository.findById(dto.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application non trouvée avec l'ID: " + dto.getApplicationId()));

        ApplicationLink applicationLink = ApplicationLink.builder()
                .application(application)
                .applicationId(application.getId())
                .nom(dto.getNom())
                .url(dto.getUrl())
                .type(dto.getType())
                .description(dto.getDescription())
                .createdBy(createdBy)
                .build();

        ApplicationLink savedApplicationLink = applicationLinkRepository.save(applicationLink);
        return toDTO(savedApplicationLink);
    }

    @Transactional
    public ApplicationLinkDTO updateApplicationLink(Long id, ApplicationLinkDTO dto, Long userId, String userRole) {
        ApplicationLink applicationLink = applicationLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lien d'application non trouvé avec l'ID: " + id));

        if (!"admin".equals(userRole) && !applicationLink.getCreatedBy().equals(userId)) {
            throw new SecurityException("Non autorisé à mettre à jour ce lien d'application");
        }

        Application application = applicationRepository.findById(dto.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application non trouvée avec l'ID: " + dto.getApplicationId()));

        applicationLink.setApplication(application);
        applicationLink.setApplicationId(application.getId());
        applicationLink.setNom(dto.getNom());
        applicationLink.setUrl(dto.getUrl());
        applicationLink.setType(dto.getType());
        applicationLink.setDescription(dto.getDescription());

        ApplicationLink updatedApplicationLink = applicationLinkRepository.save(applicationLink);
        return toDTO(updatedApplicationLink);
    }

    @Transactional
    public void deleteApplicationLink(Long id, Long userId, String userRole) {
        ApplicationLink applicationLink = applicationLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lien d'application non trouvé avec l'ID: " + id));

        if (!"admin".equals(userRole) && !applicationLink.getCreatedBy().equals(userId)) {
            throw new SecurityException("Non autorisé à supprimer ce lien d'application");
        }

        applicationLinkRepository.deleteById(id);
    }

    private ApplicationLinkDTO toDTO(ApplicationLink applicationLink) {
        ApplicationLinkDTO.ApplicationLinkDTOBuilder builder = ApplicationLinkDTO.builder()
                .id(applicationLink.getId())
                .applicationId(applicationLink.getApplicationId())
                .nom(applicationLink.getNom())
                .url(applicationLink.getUrl())
                .type(applicationLink.getType())
                .description(applicationLink.getDescription())
                .dateCreation(applicationLink.getDateCreation() != null ? applicationLink.getDateCreation().format(DATE_FORMATTER) : null)
                .createdBy(applicationLink.getCreatedBy());

        if (applicationLink.getApplication() != null) {
            builder.application(ApplicationLinkDTO.ApplicationInfoDTO.builder()
                    .id(applicationLink.getApplication().getId())
                    .nom(applicationLink.getApplication().getNom())
                    .build());
        }

        return builder.build();
    }
}
